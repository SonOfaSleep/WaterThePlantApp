package com.sonofasleep.watertheplantapp.worker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.sonofasleep.watertheplantapp.BuildConfig
import com.sonofasleep.watertheplantapp.MainActivity
import com.sonofasleep.watertheplantapp.PlantApplication.Companion.CHANNEL_ID
import com.sonofasleep.watertheplantapp.R
import com.sonofasleep.watertheplantapp.const.DEBUG_DIR
import com.sonofasleep.watertheplantapp.logs.LogUtils
import java.io.File

class ReminderWorker(val context: Context, workerParameters: WorkerParameters) :
    Worker(context, workerParameters) {

    private val notificationId = 1988
    private val logUtils = LogUtils(context)

    companion object {
        const val plantIconKey = "plantIconKey"
        const val plantNameKey = "plantNameKey"
    }

    override fun doWork(): Result {

        return try {

            // Create an explicit intent for an Activity in your app
            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent: PendingIntent =
                PendingIntent.getActivity(
                    applicationContext,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE
                )

            val plantIcon: Int = inputData.getInt(plantIconKey, R.drawable.ic_launcher_foreground)
            val plantName = inputData.getString(plantNameKey)

            val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Apps icon
                .setLargeIcon(
                    BitmapFactory.decodeResource(
                        applicationContext.resources,
                        plantIcon
                    )
                )
                .setContentTitle("$plantName")
                .setContentText(applicationContext.getString(R.string.reminder_text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            with(NotificationManagerCompat.from(applicationContext)) {
                // notificationId is a unique int for each notification that you must define
                notify(notificationId, builder.build())
            }

            if (BuildConfig.DEBUG) {
                logUtils.saveLogToFile("Job successful\n")
            }

            Result.success()

        } catch (e: Exception) {
            e.printStackTrace()

            if (BuildConfig.DEBUG) {
                logUtils.saveLogToFile(e.stackTraceToString())
            }

            Result.failure()
        }
    }
}