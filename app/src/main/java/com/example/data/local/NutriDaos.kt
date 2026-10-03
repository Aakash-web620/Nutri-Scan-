package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfileEntity)
}

@Dao
interface MealLogDao {
    @Query("SELECT * FROM meal_logs WHERE dateEpochDay = :epochDay ORDER BY timestamp DESC")
    fun getMealsForDay(epochDay: Long): Flow<List<MealLogEntity>>

    @Query("SELECT * FROM meal_logs ORDER BY timestamp DESC")
    fun getAllMeals(): Flow<List<MealLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeals(meals: List<MealLogEntity>)

    @Delete
    suspend fun deleteMeal(meal: MealLogEntity)

    @Query("DELETE FROM meal_logs WHERE id = :id")
    suspend fun deleteMealById(id: Long)
}

@Dao
interface FridgeItemDao {
    @Query("SELECT * FROM fridge_items ORDER BY category ASC, name ASC")
    fun getAllFridgeItems(): Flow<List<FridgeItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: FridgeItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<FridgeItemEntity>)

    @Update
    suspend fun updateItem(item: FridgeItemEntity)

    @Query("UPDATE fridge_items SET isSelected = :selected WHERE id = :id")
    suspend fun updateSelection(id: Long, selected: Boolean)

    @Delete
    suspend fun deleteItem(item: FridgeItemEntity)

    @Query("DELETE FROM fridge_items WHERE id = :id")
    suspend fun deleteItemById(id: Long)
}

@Dao
interface SavedRecipeDao {
    @Query("SELECT * FROM saved_recipes ORDER BY createdAt DESC")
    fun getAllRecipes(): Flow<List<SavedRecipeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: SavedRecipeEntity): Long

    @Delete
    suspend fun deleteRecipe(recipe: SavedRecipeEntity)

    @Query("DELETE FROM saved_recipes WHERE id = :id")
    suspend fun deleteRecipeById(id: Long)
}
