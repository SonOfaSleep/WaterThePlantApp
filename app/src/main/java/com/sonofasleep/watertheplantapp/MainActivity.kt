package com.sonofasleep.watertheplantapp

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.asLiveData
import com.sonofasleep.watertheplantapp.const.DEBUG_TAG
import com.sonofasleep.watertheplantapp.data.DataStoreRepository
import com.sonofasleep.watertheplantapp.data.dataStore
import com.sonofasleep.watertheplantapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dataStore = DataStoreRepository(applicationContext)
        val dayNight = dataStore.readDayNightMode.asLiveData()
        Log.d(DEBUG_TAG, "dayNight = ${dayNight.value}")

        installSplashScreen()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}