package com.sonofasleep.watertheplantapp.database

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sonofasleep.watertheplantapp.model.PlantIconItem

@Entity(tableName = "plants_table")
data class Plant(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @Embedded
    val image: PlantIconItem? = null, // Plant icon if drawable was chosen
    val photoImageUri: String? = null, // Plant icon if image was taken via camera
    val name: String,
    @ColumnInfo(name = "reminder_frequency")
    val reminderFrequency: Int,
    val description: String = "",
    val notifications: Boolean = true,
    @ColumnInfo(name = "time_hour")
    val timeHour: Int = 10,
    @ColumnInfo(name = "time_min")
    val timeMin: Int = 0,
    @ColumnInfo(name = "time_to_water")
    val timeToWater: Boolean = false
)