package com.sonofasleep.watertheplantapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import com.sonofasleep.watertheplantapp.database.PlantDatabase

class PlantApplication : Application() {

    val database: PlantDatabase by lazy { PlantDatabase.getDatabase(this) }

    companion object {
        const val CHANNEL_ID = "plant_reminder_id"
    }

    override fun onCreate() {
        super.onCreate()

        // Create the NotificationChannel
        val name = getString(R.string.channel_name)
        val descriptionText = getString(R.string.channel_description)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val soundUri =
            Uri.parse("android.resource://" + applicationContext.packageName + "/" + R.raw.test_notification_sound)
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_ALARM)
            .build()
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
            setSound(soundUri, audioAttributes)
        }
        // Register the channel with the system
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}