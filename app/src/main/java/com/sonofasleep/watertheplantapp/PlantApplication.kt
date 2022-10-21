package com.sonofasleep.watertheplantapp

import android.app.Application
import com.sonofasleep.watertheplantapp.database.PlantDatabase

class PlantApplication : Application() {

    val database: PlantDatabase by lazy { PlantDatabase.getDatabase(this) }
}