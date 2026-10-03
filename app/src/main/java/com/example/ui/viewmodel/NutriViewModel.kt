package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.AnalysisResult
import com.example.data.api.GeneratedRecipe
import com.example.data.api.ScannedFoodItem
import com.example.data.local.FridgeItemEntity
import com.example.data.local.MealLogEntity
import com.example.data.local.SavedRecipeEntity
import com.example.data.local.UserProfileEntity
import com.example.data.repository.NutriRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class AppTab {
    DASHBOARD,
    SCANNER,
    FRIDGE,
    PROFILE
}

data class ScannerUiState(
    val currentBitmap: Bitmap? = null,
    val isAnalyzing: Boolean = false,
    val analysisResult: AnalysisResult? = null,
    val portionMultiplier: Float = 1.0f,
    val selectedMealType: String = "Lunch",
    val isResultModalOpen: Boolean = false,
    val errorMessage: String? = null,
    val isManualEntryOpen: Boolean = false,
    val isBarcodeScannerOpen: Boolean = false
)

data class FridgeUiState(
    val isGeneratingRecipe: Boolean = false,
    val currentRecipe: GeneratedRecipe? = null,
    val isRecipeModalOpen: Boolean = false,
    val isAddDialogOpen: Boolean = false,
    val selectedCategoryFilter: String = "All"
)

class NutriViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NutriRepository(application)

    // Current navigation tab
    private val _currentTab = MutableStateFlow(AppTab.DASHBOARD)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    fun navigateTo(tab: AppTab) {
        _currentTab.value = tab
    }

    // --- Profile & Targets ---
    val userProfile: StateFlow<UserProfileEntity?> = repository.getUserProfile()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // --- Daily Meal Logs ---
    val todayMeals: StateFlow<List<MealLogEntity>> = repository.getTodayMeals()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Derived nutrition totals
    val dailyProgress = combine(todayMeals, userProfile) { meals, profile ->
        val totalCalories = meals.sumOf { it.calories }
        val totalProtein = meals.sumOf { it.proteinG.toDouble() }.toFloat()
        val totalCarbs = meals.sumOf { it.carbsG.toDouble() }.toFloat()
        val totalFat = meals.sumOf { it.fatG.toDouble() }.toFloat()

        val targetCal = profile?.targetCalories ?: 2000
        val targetProt = profile?.targetProtein ?: 130
        val targetCarb = profile?.targetCarbs ?: 220
        val targetFat = profile?.targetFat ?: 65

        val calProgress = (totalCalories.toFloat() / targetCal.toFloat()).coerceIn(0f, 1f)
        val calRemaining = (targetCal - totalCalories).coerceAtLeast(0)

        DailyProgressState(
            caloriesConsumed = totalCalories,
            caloriesTarget = targetCal,
            caloriesRemaining = calRemaining,
            calorieProgress = calProgress,
            proteinConsumed = totalProtein,
            proteinTarget = targetProt,
            carbsConsumed = totalCarbs,
            carbsTarget = targetCarb,
            fatConsumed = totalFat,
            fatTarget = targetFat
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DailyProgressState()
    )

    // --- Scanner State ---
    private val _scannerState = MutableStateFlow(ScannerUiState())
    val scannerState: StateFlow<ScannerUiState> = _scannerState.asStateFlow()

    fun startAnalysis(bitmap: Bitmap) {
        _scannerState.update {
            it.copy(
                currentBitmap = bitmap,
                isAnalyzing = true,
                errorMessage = null,
                isResultModalOpen = true,
                portionMultiplier = 1.0f
            )
        }
        viewModelScope.launch {
            val result = repository.analyzeFoodImage(bitmap)
            result.onSuccess { analysis ->
                _scannerState.update {
                    it.copy(
                        isAnalyzing = false,
                        analysisResult = analysis
                    )
                }
            }.onFailure { err ->
                _scannerState.update {
                    it.copy(
                        isAnalyzing = false,
                        errorMessage = err.message ?: "Analysis failed. You can use manual entry."
                    )
                }
            }
        }
    }

    fun toggleFoodItemSelection(itemId: String) {
        _scannerState.update { state ->
            val updatedItems = state.analysisResult?.items?.map { item ->
                if (item.id == itemId) item.copy(isSelected = !item.isSelected) else item
            }
            state.copy(
                analysisResult = state.analysisResult?.copy(items = updatedItems ?: emptyList())
            )
        }
    }

    fun setPortionMultiplier(multiplier: Float) {
        _scannerState.update { it.copy(portionMultiplier = multiplier) }
    }

    fun setSelectedMealType(type: String) {
        _scannerState.update { it.copy(selectedMealType = type) }
    }

    fun dismissResultModal() {
        _scannerState.update { it.copy(isResultModalOpen = false) }
    }

    fun logSelectedItemsToDashboard() {
        val state = _scannerState.value
        val items = state.analysisResult?.items?.filter { it.isSelected } ?: return
        if (items.isEmpty()) return

        val multiplier = state.portionMultiplier
        val mealType = state.selectedMealType
        val todayEpochDay = LocalDate.now().toEpochDay()

        viewModelScope.launch {
            val mealEntities = items.map { item ->
                MealLogEntity(
                    mealType = mealType,
                    foodName = item.name,
                    portionDescription = "${(multiplier * 100).toInt()}% of ${item.estimatedPortion}",
                    portionMultiplier = multiplier,
                    calories = (item.baseCalories * multiplier).toInt(),
                    proteinG = item.baseProteinG * multiplier,
                    carbsG = item.baseCarbsG * multiplier,
                    fatG = item.baseFatG * multiplier,
                    fiberG = item.baseFiberG * multiplier,
                    dateEpochDay = todayEpochDay
                )
            }
            repository.logMeals(mealEntities)
            _scannerState.update { it.copy(isResultModalOpen = false) }
            _currentTab.value = AppTab.DASHBOARD
        }
    }

    fun addSelectedItemsToFridge() {
        val state = _scannerState.value
        val items = state.analysisResult?.items?.filter { it.isSelected } ?: return
        if (items.isEmpty()) return

        viewModelScope.launch {
            items.forEach { item ->
                repository.addFridgeItem(
                    FridgeItemEntity(
                        name = item.name,
                        category = item.category,
                        quantity = "1",
                        unit = "portion",
                        isSelected = false
                    )
                )
            }
            _scannerState.update { it.copy(isResultModalOpen = false) }
            _currentTab.value = AppTab.FRIDGE
        }
    }

    // Manual Food Logging
    fun showManualEntry(show: Boolean) {
        _scannerState.update { it.copy(isManualEntryOpen = show) }
    }

    fun showBarcodeScanner(show: Boolean) {
        _scannerState.update { it.copy(isBarcodeScannerOpen = show) }
    }

    fun logManualMeal(
        name: String,
        mealType: String,
        portion: String,
        calories: Int,
        protein: Float,
        carbs: Float,
        fat: Float
    ) {
        viewModelScope.launch {
            repository.logMeal(
                MealLogEntity(
                    mealType = mealType,
                    foodName = name,
                    portionDescription = portion,
                    portionMultiplier = 1.0f,
                    calories = calories,
                    proteinG = protein,
                    carbsG = carbs,
                    fatG = fat,
                    dateEpochDay = LocalDate.now().toEpochDay()
                )
            )
            _scannerState.update { it.copy(isManualEntryOpen = false) }
            _currentTab.value = AppTab.DASHBOARD
        }
    }

    fun deleteMealLog(id: Long) {
        viewModelScope.launch {
            repository.deleteMeal(id)
        }
    }

    // --- Fridge & Recipe Generation State ---
    val fridgeItems: StateFlow<List<FridgeItemEntity>> = repository.getFridgeItems()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val savedRecipes: StateFlow<List<SavedRecipeEntity>> = repository.getSavedRecipes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _fridgeUiState = MutableStateFlow(FridgeUiState())
    val fridgeUiState: StateFlow<FridgeUiState> = _fridgeUiState.asStateFlow()

    fun toggleFridgeSelection(id: Long, isSelected: Boolean) {
        viewModelScope.launch {
            repository.toggleFridgeItemSelection(id, isSelected)
        }
    }

    fun addFridgeIngredient(name: String, category: String, quantity: String, unit: String) {
        viewModelScope.launch {
            repository.addFridgeItem(
                FridgeItemEntity(
                    name = name,
                    category = category,
                    quantity = quantity,
                    unit = unit,
                    isSelected = true
                )
            )
            _fridgeUiState.update { it.copy(isAddDialogOpen = false) }
        }
    }

    fun deleteFridgeIngredient(id: Long) {
        viewModelScope.launch {
            repository.deleteFridgeItem(id)
        }
    }

    fun setCategoryFilter(category: String) {
        _fridgeUiState.update { it.copy(selectedCategoryFilter = category) }
    }

    fun showAddIngredientDialog(show: Boolean) {
        _fridgeUiState.update { it.copy(isAddDialogOpen = show) }
    }

    fun dismissRecipeModal() {
        _fridgeUiState.update { it.copy(isRecipeModalOpen = false) }
    }

    fun generateMealWithSelectedIngredients() {
        val selected = fridgeItems.value.filter { it.isSelected }.map { it.name }
        val goal = userProfile.value?.fitnessGoal ?: "Balanced Nutrition"

        _fridgeUiState.update {
            it.copy(
                isGeneratingRecipe = true,
                isRecipeModalOpen = true,
                currentRecipe = null
            )
        }

        viewModelScope.launch {
            val result = repository.generateRecipe(
                ingredients = if (selected.isNotEmpty()) selected else listOf("Fresh Greens", "Healthy Protein", "Grains"),
                dietaryGoal = goal
            )
            result.onSuccess { recipe ->
                _fridgeUiState.update {
                    it.copy(
                        isGeneratingRecipe = false,
                        currentRecipe = recipe
                    )
                }
            }.onFailure {
                _fridgeUiState.update { it.copy(isGeneratingRecipe = false) }
            }
        }
    }

    fun saveCurrentRecipeToFavorites() {
        val recipe = _fridgeUiState.value.currentRecipe ?: return
        viewModelScope.launch {
            val entity = SavedRecipeEntity(
                title = recipe.recipeName,
                description = recipe.description,
                prepTime = recipe.prepTime,
                cookTime = recipe.cookTime,
                servings = recipe.servings,
                calories = recipe.caloriesPerServing,
                proteinG = recipe.proteinG,
                carbsG = recipe.carbsG,
                fatG = recipe.fatG,
                ingredientsJson = org.json.JSONArray(recipe.ingredients).toString(),
                instructionsJson = org.json.JSONArray(recipe.instructions).toString(),
                isFavorite = true
            )
            repository.saveRecipe(entity)
        }
    }

    fun viewSavedRecipeDetail(recipe: SavedRecipeEntity) {
        val ingList = try {
            val arr = org.json.JSONArray(recipe.ingredientsJson)
            List(arr.length()) { arr.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
        val instList = try {
            val arr = org.json.JSONArray(recipe.instructionsJson)
            List(arr.length()) { arr.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }

        val gen = GeneratedRecipe(
            recipeName = recipe.title,
            description = recipe.description,
            prepTime = recipe.prepTime,
            cookTime = recipe.cookTime,
            servings = recipe.servings,
            caloriesPerServing = recipe.calories,
            proteinG = recipe.proteinG,
            carbsG = recipe.carbsG,
            fatG = recipe.fatG,
            ingredients = ingList,
            instructions = instList,
            chefTips = "Saved to your recipe book."
        )
        _fridgeUiState.update {
            it.copy(
                currentRecipe = gen,
                isRecipeModalOpen = true,
                isGeneratingRecipe = false
            )
        }
    }

    fun deleteSavedRecipe(id: Long) {
        viewModelScope.launch {
            repository.deleteRecipe(id)
        }
    }

    // --- Profile & Goals Calculation ---
    fun updateProfile(
        name: String,
        age: Int,
        gender: String,
        weightKg: Float,
        heightCm: Float,
        fitnessGoal: String,
        activityLevel: String
    ) {
        // Mifflin-St Jeor equation:
        // Men: 10 * weight (kg) + 6.25 * height (cm) - 5 * age + 5
        // Women: 10 * weight (kg) + 6.25 * height (cm) - 5 * age - 161
        val baseBmr = if (gender.equals("Male", ignoreCase = true)) {
            (10f * weightKg) + (6.25f * heightCm) - (5f * age) + 5
        } else {
            (10f * weightKg) + (6.25f * heightCm) - (5f * age) - 161
        }
        val bmr = baseBmr.toInt().coerceAtLeast(1000)

        val activityMultiplier = when (activityLevel) {
            "Sedentary" -> 1.2f
            "Light" -> 1.375f
            "Moderate" -> 1.55f
            "Very Active" -> 1.725f
            else -> 1.4f
        }
        val tdee = (bmr * activityMultiplier).toInt()

        val (targetCal, proteinSplit, carbsSplit, fatSplit) = when (fitnessGoal) {
            "Weight Loss" -> {
                // Calorie deficit: TDEE - 400
                val cal = (tdee - 400).coerceAtLeast(1200)
                // 30% protein (4 kcal/g), 40% carbs (4 kcal/g), 30% fat (9 kcal/g)
                Quad(cal, 0.30f, 0.40f, 0.30f)
            }
            "Muscle Gain" -> {
                // Surplus: TDEE + 350
                val cal = tdee + 350
                // 30% protein, 45% carbs, 25% fat
                Quad(cal, 0.30f, 0.45f, 0.25f)
            }
            else -> { // Maintenance
                val cal = tdee
                // 25% protein, 50% carbs, 25% fat
                Quad(cal, 0.25f, 0.50f, 0.25f)
            }
        }

        val targetProt = ((targetCal * proteinSplit) / 4f).toInt()
        val targetCarb = ((targetCal * carbsSplit) / 4f).toInt()
        val targetFat = ((targetCal * fatSplit) / 9f).toInt()

        val updated = UserProfileEntity(
            id = 1,
            name = name,
            age = age,
            gender = gender,
            weightKg = weightKg,
            heightCm = heightCm,
            fitnessGoal = fitnessGoal,
            activityLevel = activityLevel,
            bmr = bmr,
            targetCalories = targetCal,
            targetProtein = targetProt,
            targetCarbs = targetCarb,
            targetFat = targetFat
        )

        viewModelScope.launch {
            repository.saveUserProfile(updated)
        }
    }
}

data class DailyProgressState(
    val caloriesConsumed: Int = 0,
    val caloriesTarget: Int = 2000,
    val caloriesRemaining: Int = 2000,
    val calorieProgress: Float = 0f,
    val proteinConsumed: Float = 0f,
    val proteinTarget: Int = 130,
    val carbsConsumed: Float = 0f,
    val carbsTarget: Int = 220,
    val fatConsumed: Float = 0f,
    val fatTarget: Int = 65
)

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
