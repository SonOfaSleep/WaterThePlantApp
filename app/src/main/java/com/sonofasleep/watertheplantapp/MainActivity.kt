package com.sonofasleep.watertheplantapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.sonofasleep.watertheplantapp.databinding.ActivityMainBinding
import com.sonofasleep.watertheplantapp.utilities.DayNightThemeSetter
import com.sonofasleep.watertheplantapp.utilities.WaterItPreferences

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appPreferences: WaterItPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setting day/night theme according to preference chosen in settings
        appPreferences = WaterItPreferences(applicationContext)
        DayNightThemeSetter.setDayKnightTheme(
            appPreferences.readDayNightPreference(),
            applicationContext
        )

        installSplashScreen()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}