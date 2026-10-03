package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "Alex",
    val age: Int = 28,
    val gender: String = "Female",
    val weightKg: Float = 65f,
    val heightCm: Float = 170f,
    val fitnessGoal: String = "Weight Loss", // "Weight Loss", "Muscle Gain", "Maintenance"
    val activityLevel: String = "Moderate", // "Sedentary", "Light", "Moderate", "Very Active"
    val bmr: Int = 1450,
    val targetCalories: Int = 1750,
    val targetProtein: Int = 120,
    val targetCarbs: Int = 170,
    val targetFat: Int = 55
)

@Entity(tableName = "meal_logs")
data class MealLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mealType: String, // "Breakfast", "Lunch", "Dinner", "Snacks"
    val foodName: String,
    val portionDescription: String = "1 standard serving",
    val portionMultiplier: Float = 1.0f,
    val calories: Int,
    val proteinG: Float,
    val carbsG: Float,
    val fatG: Float,
    val fiberG: Float = 0f,
    val dateEpochDay: Long,
    val imageUri: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "fridge_items")
data class FridgeItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String = "Produce", // Produce, Protein, Dairy, Grains, Pantry
    val quantity: String = "1",
    val unit: String = "item",
    val isSelected: Boolean = false,
    val addedDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_recipes")
data class SavedRecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val prepTime: String = "15 mins",
    val cookTime: String = "20 mins",
    val servings: Int = 2,
    val calories: Int,
    val proteinG: Float,
    val carbsG: Float,
    val fatG: Float,
    val ingredientsJson: String,
    val instructionsJson: String,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
