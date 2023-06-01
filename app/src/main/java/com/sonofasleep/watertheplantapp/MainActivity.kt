package com.sonofasleep.watertheplantapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.sonofasleep.watertheplantapp.databinding.ActivityMainBinding
import com.sonofasleep.watertheplantapp.utilities.DayNightTheme
import com.sonofasleep.watertheplantapp.utilities.DayNightThemePreference

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dayNightPreferences: DayNightThemePreference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setting day/night theme according to preference chosen in settings
        dayNightPreferences = DayNightThemePreference(applicationContext)
        DayNightTheme.setDayKnightTheme(
            dayNightPreferences.readDayNightPreference(),
            applicationContext
        )

        installSplashScreen()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}