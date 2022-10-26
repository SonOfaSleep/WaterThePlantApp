package com.sonofasleep.watertheplantapp.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantDao {

    @Query("SELECT * FROM PlantsTable ORDER BY name ASC")
    fun getAllOrderedASC(): Flow<List<Plant>>

    @Query("SELECT * FROM PlantsTable ORDER BY name DESC")
    fun getAllOrderedDESC(): Flow<List<Plant>>

    @Query("SELECT * FROM PlantsTable WHERE name = :name")
    fun findByName(name: String): Flow<Plant>

    @Query("SELECT * FROM PlantsTable WHERE id = :id")
    fun getPlantById(id: Long): Flow<Plant>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNewPlant(plant: Plant)

    @Update
    suspend fun update(plant: Plant)

    @Delete
    suspend fun delete(plant: Plant)
}

enum class SortType {
    ASCENDING, DESCENDING, NONE
}