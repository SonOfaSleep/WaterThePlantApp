package com.sonofasleep.watertheplantapp.alarm

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sonofasleep.watertheplantapp.BuildConfig
import com.sonofasleep.watertheplantapp.MainActivity
import com.sonofasleep.watertheplantapp.PlantApplication
import com.sonofasleep.watertheplantapp.R
import com.sonofasleep.watertheplantapp.const.PLANT_ICON
import com.sonofasleep.watertheplantapp.const.PLANT_ID
import com.sonofasleep.watertheplantapp.const.PLANT_NAME
import com.sonofasleep.watertheplantapp.database.PlantDao
import com.sonofasleep.watertheplantapp.database.PlantDatabase
import com.sonofasleep.watertheplantapp.logs.LogUtils
import kotlinx.coroutines.*

class AlarmReceiver : BroadcastReceiver() {

    lateinit var dao: PlantDao

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context?, intent: Intent?) {

        val logUtils = LogUtils(context!!)

        val icon = intent?.getIntExtra(PLANT_ICON, R.drawable.ic_launcher_foreground)
        val name = intent?.getStringExtra(PLANT_NAME)
        val plantID = intent?.getLongExtra(PLANT_ID, 0L)

        fun getBitmap(drawableRes: Int): Bitmap? {
            val drawable: Drawable? = ContextCompat.getDrawable(context!!, drawableRes)
            val canvas = Canvas()
            val bitmap = drawable?.let {
                Bitmap.createBitmap(
                    it.intrinsicWidth,
                    drawable.intrinsicHeight,
                    Bitmap.Config.ARGB_8888
                )
            }
            canvas.setBitmap(bitmap)
            drawable?.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
            drawable?.draw(canvas)
            return bitmap
        }

        try {
            // Create an explicit intent for an Activity in your app
            val notifIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent: PendingIntent =
                PendingIntent.getActivity(
                    context,
                    0,
                    notifIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )

            val builder = NotificationCompat.Builder(
                context,
                PlantApplication.CHANNEL_ID
            )
                .setSmallIcon(R.drawable.ic_status_bar_v2_24) // Apps icon
                .setLargeIcon(getBitmap(icon!!))
                .setContentTitle(name)
                .setContentText(context.getString(R.string.reminder_text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            with(NotificationManagerCompat.from(context)) {
                // notificationId is a unique int for each notification that you must define
                notify(plantID!!.toInt(), builder.build())
            }

            /**
             * Updating timeToWater to true and saving to database
             */
            dao = PlantDatabase.getDatabase(context).plantDao()
            if (plantID != null && plantID != 0L) {

                GlobalScope.launch(Dispatchers.IO) {
                    val plant = dao.getPlantById(plantID)
                    val newPlant = plant.copy(timeToWater = true)
                    dao.update(newPlant)
                }
            }

            if (BuildConfig.DEBUG) {
                logUtils.saveLogToFile("Notify successful\n")
            }

        } catch (e: Exception) {
            e.printStackTrace()

            if (BuildConfig.DEBUG) {
                logUtils.saveLogToFile(e.stackTraceToString())
            }

            return
        }
    }
}