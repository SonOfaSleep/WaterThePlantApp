package com.sonofasleep.watertheplantapp.alarm

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import com.sonofasleep.watertheplantapp.database.PlantDao
import com.sonofasleep.watertheplantapp.database.PlantDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    lateinit var dao: PlantDao
    lateinit var alarmUtilities: AlarmUtilities

    override fun onReceive(context: Context?, intent: Intent?) {

        // Reset all alarms on restart
        if (intent?.action == "android.intent.action.BOOT_COMPLETED") {

            if (context != null) {
                dao = PlantDatabase.getDatabase(context).plantDao()

                val alarmManager =
                    context.applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmUtilities = AlarmUtilities(context.applicationContext, alarmManager)

                GlobalScope.launch(Dispatchers.IO) {
                    val allPlants = dao.getAllPlantsAsList()
                    for (plant in allPlants) {
                        if (plant.notifications) {
                            alarmUtilities.setExactAlarm(plant, plant.id)
                        }
                    }
                }
            }
        }
    }
}