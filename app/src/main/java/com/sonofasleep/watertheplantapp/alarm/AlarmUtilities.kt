package com.sonofasleep.watertheplantapp.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.core.content.ContextCompat
import com.sonofasleep.watertheplantapp.const.PLANT_ICON
import com.sonofasleep.watertheplantapp.const.PLANT_ID
import com.sonofasleep.watertheplantapp.const.PLANT_NAME
import com.sonofasleep.watertheplantapp.database.Plant
import java.util.*


/**
 * This class handles setting and deleting alarms
 */
class AlarmUtilities(private val context: Context, private val alarmManager: AlarmManager) {

    private fun createAlarmIntent(plant: Plant, plantId: Long): PendingIntent {
        val alarmIntent = Intent(context, AlarmReceiver::class.java)
        alarmIntent.putExtra(PLANT_ICON, plant.image?.iconDry)
        alarmIntent.putExtra(PLANT_NAME, plant.name)
        alarmIntent.putExtra(PLANT_ID, plantId)

        // We need this data to create unique pending intents. Each plant will have unique ID and
        // unique pendingIntent.
        alarmIntent.data = Uri.parse("scheme:///$plantId")

        return PendingIntent.getBroadcast(
            context,
            0,
            alarmIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    // plantId need only for newly created plants. For others it's ok to use plant.ID
    fun setExactAlarm(plant: Plant, plantId: Long) {
        val dayInMillis: Long = 86400000 // 24 hours in mls

        val timeOfDay: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, plant.timeHour)
            set(Calendar.MINUTE, plant.timeMin)
        }
        val currentTime = System.currentTimeMillis()

        var timeUntilAlarm = if (currentTime >= timeOfDay.timeInMillis) {
            timeOfDay.timeInMillis + dayInMillis
        } else {
            timeOfDay.timeInMillis
        }

        // If delay is 1 day we add zero, if 2 days add one more day etc.
        timeUntilAlarm += dayInMillis * (plant.reminderFrequency - 1)

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            timeUntilAlarm,
            createAlarmIntent(plant, plantId)
        )
    }

    fun cancelAlarm(plant: Plant) {
        alarmManager.cancel(createAlarmIntent(plant, plant.id))
    }
}