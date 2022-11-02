package com.sonofasleep.watertheplantapp.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "PlantsTable")
data class Plant(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val image: Int,
    val name: String,
    @ColumnInfo(name = "reminder_frequency")
    val reminderFrequency: Int,
    val description: String = "",
    val notifications: Boolean = true
)