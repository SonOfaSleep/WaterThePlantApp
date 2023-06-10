package com.sonofasleep.watertheplantapp.utilities

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object DayNightThemeSetter {

    fun setDayKnightTheme(dayNightMode: Int = 2, context: Context) {
        // 0 = day, 1 = night, 2 = system default

        // These is recommended way by google, but changing through UiMode don't work =(
//        if (android.os.Build.VERSION.SDK_INT >= 31) {
//            Log.d(DEBUG_TAG, "Version > 31")
//            val uiManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
//            uiManager.nightMode = when (dayNightMode) {
//                0 -> UiModeManager.MODE_NIGHT_NO
//                1 -> UiModeManager.MODE_NIGHT_YES
//                else -> UiModeManager.MODE_NIGHT_AUTO
//            }
//        } else {
//            Log.d(DEBUG_TAG, "Version < 31")
//            val mode = when (dayNightMode) {
//                0 -> AppCompatDelegate.MODE_NIGHT_NO
//                1 -> AppCompatDelegate.MODE_NIGHT_YES
//                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
//            }
//            AppCompatDelegate.setDefaultNightMode(mode)
//        }
        val mode = when (dayNightMode) {
            0 -> AppCompatDelegate.MODE_NIGHT_NO
            1 -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}