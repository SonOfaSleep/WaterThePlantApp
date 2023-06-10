package com.sonofasleep.watertheplantapp.utilities

import android.content.Context
import com.sonofasleep.watertheplantapp.const.DAY_NIGHT_PREFERENCE
import com.sonofasleep.watertheplantapp.const.FIRST_LAUNCH
import com.sonofasleep.watertheplantapp.const.SHARED_PREFERENCES

class WaterItPreferences(context: Context) {
    private val sharedPreferences =
        context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE)

    fun saveFirstLaunch() {
        val editor = sharedPreferences.edit()
        editor.apply {
            putBoolean(FIRST_LAUNCH, false)
            apply()
        }
    }

    fun saveDayNighPreference(dayNight: Int) {
        val editor = sharedPreferences.edit()
        editor.apply {
            putInt(DAY_NIGHT_PREFERENCE, dayNight)
            apply()
        }
    }

    fun readFirstLaunch(): Boolean {
        return sharedPreferences.getBoolean(FIRST_LAUNCH, true)
    }

    fun readDayNightPreference(): Int {
        // 0 = day; 1 = night; default 2 = follow system day/night theme
        return sharedPreferences.getInt(DAY_NIGHT_PREFERENCE, 2)
    }
}