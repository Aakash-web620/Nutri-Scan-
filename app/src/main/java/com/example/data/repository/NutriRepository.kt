package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.example.data.api.AnalysisResult
import com.example.data.api.GeminiClient
import com.example.data.api.GeneratedRecipe
import com.example.data.local.AppDatabase
import com.example.data.local.FridgeItemEntity
import com.example.data.local.MealLogEntity
import com.example.data.local.SavedRecipeEntity
import com.example.data.local.UserProfileEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDate

class NutriRepository(private val context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val userProfileDao = database.userProfileDao()
    private val mealLogDao = database.mealLogDao()
    private val fridgeItemDao = database.fridgeItemDao()
    private val savedRecipeDao = database.savedRecipeDao()

    init {
        // Initialize default seed data asynchronously if empty
        CoroutineScope(Dispatchers.IO).launch {
            seedInitialDataIfNeeded()
        }
    }

    // --- User Profile ---
    fun getUserProfile(): Flow<UserProfileEntity?> = userProfileDao.getUserProfile()

    suspend fun saveUserProfile(profile: UserProfileEntity) {
        userProfileDao.insertOrUpdateProfile(profile)
    }

    // --- Meal Logs ---
    fun getTodayMeals(): Flow<List<MealLogEntity>> {
        val todayEpochDay = LocalDate.now().toEpochDay()
        return mealLogDao.getMealsForDay(todayEpochDay)
    }

    suspend fun logMeal(meal: MealLogEntity): Long {
        return mealLogDao.insertMeal(meal)
    }

    suspend fun logMeals(meals: List<MealLogEntity>) {
        mealLogDao.insertMeals(meals)
    }

    suspend fun deleteMeal(id: Long) {
        mealLogDao.deleteMealById(id)
    }

    // --- Fridge Inventory ---
    fun getFridgeItems(): Flow<List<FridgeItemEntity>> = fridgeItemDao.getAllFridgeItems()

    suspend fun addFridgeItem(item: FridgeItemEntity): Long {
        return fridgeItemDao.insertItem(item)
    }

    suspend fun toggleFridgeItemSelection(id: Long, isSelected: Boolean) {
        fridgeItemDao.updateSelection(id, isSelected)
    }

    suspend fun deleteFridgeItem(id: Long) {
        fridgeItemDao.deleteItemById(id)
    }

    // --- Saved Recipes ---
    fun getSavedRecipes(): Flow<List<SavedRecipeEntity>> = savedRecipeDao.getAllRecipes()

    suspend fun saveRecipe(recipe: SavedRecipeEntity): Long {
        return savedRecipeDao.insertRecipe(recipe)
    }

    suspend fun deleteRecipe(id: Long) {
        savedRecipeDao.deleteRecipeById(id)
    }

    // --- Gemini AI Actions ---
    suspend fun analyzeFoodImage(bitmap: Bitmap): Result<AnalysisResult> {
        return GeminiClient.analyzeFoodImage(bitmap)
    }

    suspend fun generateRecipe(ingredients: List<String>, dietaryGoal: String): Result<GeneratedRecipe> {
        return GeminiClient.generateRecipeFromIngredients(ingredients, dietaryGoal)
    }

    // --- Seeding ---
    private suspend fun seedInitialDataIfNeeded() {
        val profile = userProfileDao.getUserProfile().firstOrNull()
        if (profile == null) {
            val defaultProfile = UserProfileEntity(
                id = 1,
                name = "Alex Morgan",
                age = 28,
                gender = "Female",
                weightKg = 65f,
                heightCm = 170f,
                fitnessGoal = "Weight Loss",
                activityLevel = "Moderate",
                bmr = 1445,
                targetCalories = 1800,
                targetProtein = 125,
                targetCarbs = 175,
                targetFat = 55
            )
            userProfileDao.insertOrUpdateProfile(defaultProfile)
        }

        val todayEpochDay = LocalDate.now().toEpochDay()
        val meals = mealLogDao.getMealsForDay(todayEpochDay).firstOrNull()
        if (meals.isNullOrEmpty()) {
            val initialMeals = listOf(
                MealLogEntity(
                    mealType = "Breakfast",
                    foodName = "Greek Yogurt Parfait & Fresh Berries",
                    portionDescription = "1 cup yogurt + 1/2 cup blueberries",
                    portionMultiplier = 1.0f,
                    calories = 290,
                    proteinG = 22f,
                    carbsG = 34f,
                    fatG = 6f,
                    fiberG = 4f,
                    dateEpochDay = todayEpochDay
                ),
                MealLogEntity(
                    mealType = "Lunch",
                    foodName = "Grilled Salmon & Quinoa Superbowl",
                    portionDescription = "1 plate (350g)",
                    portionMultiplier = 1.0f,
                    calories = 520,
                    proteinG = 42f,
                    carbsG = 45f,
                    fatG = 16f,
                    fiberG = 6f,
                    dateEpochDay = todayEpochDay
                ),
                MealLogEntity(
                    mealType = "Snacks",
                    foodName = "Organic Gala Apple with Almond Butter",
                    portionDescription = "1 apple + 1 tbsp butter",
                    portionMultiplier = 1.0f,
                    calories = 195,
                    proteinG = 4f,
                    carbsG = 25f,
                    fatG = 9f,
                    fiberG = 5f,
                    dateEpochDay = todayEpochDay
                )
            )
            mealLogDao.insertMeals(initialMeals)
        }

        val fridgeItems = fridgeItemDao.getAllFridgeItems().firstOrNull()
        if (fridgeItems.isNullOrEmpty()) {
            val initialFridge = listOf(
                FridgeItemEntity(name = "Fresh Atlantic Salmon", category = "Protein", quantity = "2", unit = "fillets", isSelected = true),
                FridgeItemEntity(name = "Hass Avocados", category = "Produce", quantity = "2", unit = "pcs", isSelected = true),
                FridgeItemEntity(name = "Organic Baby Spinach", category = "Produce", quantity = "1", unit = "bag", isSelected = true),
                FridgeItemEntity(name = "Pasture-Raised Eggs", category = "Protein", quantity = "6", unit = "pcs", isSelected = true),
                FridgeItemEntity(name = "Greek Yogurt (0% Fat)", category = "Dairy", quantity = "500", unit = "g"),
                FridgeItemEntity(name = "Artisan Sourdough Loaf", category = "Grains", quantity = "4", unit = "slices"),
                FridgeItemEntity(name = "Cherry Tomatoes", category = "Produce", quantity = "250", unit = "g"),
                FridgeItemEntity(name = "Organic White Quinoa", category = "Grains", quantity = "400", unit = "g"),
                FridgeItemEntity(name = "Extra Virgin Olive Oil", category = "Pantry", quantity = "1", unit = "bottle"),
                FridgeItemEntity(name = "English Cucumber", category = "Produce", quantity = "1", unit = "pc")
            )
            fridgeItemDao.insertItems(initialFridge)
        }

        val recipes = savedRecipeDao.getAllRecipes().firstOrNull()
        if (recipes.isNullOrEmpty()) {
            val defaultRecipe = SavedRecipeEntity(
                title = "Zesty Lemon Salmon & Herb Quinoa",
                description = "Crispy pan-seared salmon rested on fluffy garlic-herb quinoa and wilted baby greens.",
                prepTime = "12 mins",
                cookTime = "18 mins",
                servings = 2,
                calories = 490,
                proteinG = 44f,
                carbsG = 38f,
                fatG = 15f,
                ingredientsJson = """["2 fresh salmon fillets (150g each)", "1 cup cooked quinoa", "2 cups baby spinach", "1 tbsp extra virgin olive oil", "1 clove fresh garlic, minced", "Juice of 1/2 fresh lemon", "Sea salt & cracked black pepper"]""",
                instructionsJson = """["Rinse and cook quinoa according to package instructions with a pinch of sea salt.", "Pat salmon fillets dry and season generously with salt, pepper, and lemon zest.", "Heat olive oil in a skillet over medium-high heat. Sear salmon skin-side down for 4-5 mins until crisp, then flip for 3 mins.", "In the same pan, flash-sauté baby spinach and garlic for 90 seconds until vibrant and tender.", "Divide warm quinoa into bowls, top with greens and rested salmon fillet, and drizzle fresh lemon juice."]""",
                isFavorite = true
            )
            savedRecipeDao.insertRecipe(defaultRecipe)
        }
    }
}
