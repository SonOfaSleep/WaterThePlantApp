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

    @Query("SELECT * FROM plants_table ORDER BY name ASC")
    fun getAllOrderedASC(): Flow<List<Plant>>

    @Query("SELECT * FROM plants_table ORDER BY name DESC")
    fun getAllOrderedDESC(): Flow<List<Plant>>

    @Query("SELECT * FROM plants_table WHERE name LIKE :name")
    fun findByName(name: String): Flow<List<Plant>>

    @Query("SELECT * FROM plants_table WHERE id = :id")
    fun getPlantByIdAsFlow(id: Long): Flow<Plant>

    @Query("SELECT * FROM plants_table WHERE id = :id")
    fun getPlantById(id: Long): Plant

    @Query("SELECT * FROM plants_table")
    fun getAllPlantsAsList(): List<Plant>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNewPlant(plant: Plant): Long

    @Update
    suspend fun update(plant: Plant)

    @Delete
    suspend fun delete(plant: Plant)
}