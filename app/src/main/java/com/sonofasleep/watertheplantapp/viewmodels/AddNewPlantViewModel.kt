package com.sonofasleep.watertheplantapp.viewmodels

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import com.sonofasleep.watertheplantapp.alarm.AlarmUtilities
import com.sonofasleep.watertheplantapp.database.Plant
import com.sonofasleep.watertheplantapp.database.PlantDao
import com.sonofasleep.watertheplantapp.model.PlantIconItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AddNewPlantViewModel(private val dao: PlantDao, private val application: Application) :
    ViewModel() {

    // For plant icon choose in recyclerView
    // When not in AddPlantFragment icon is null
    private val _icon = MutableLiveData<PlantIconItem?>(null)
    val icon: LiveData<PlantIconItem?> = _icon

    // AlarmManager instance
    private val alarmManager: AlarmManager = application.applicationContext
        .getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val alarmUtilities = AlarmUtilities(application.applicationContext, alarmManager)

    fun setPlantIcon(item: PlantIconItem) {
        _icon.value = item
    }

    fun resetIcon() {
        _icon.value = null
    }

    fun isIconNotNull(): Boolean = _icon.value != null

    fun isNameValid(name: String): Boolean = name.isNotBlank()

    fun getPlant(id: Long): LiveData<Plant> = dao.getPlantByIdAsFlow(id).asLiveData()

    fun insertPlantStartAlarm(
        image: Int,
        name: String,
        reminderFrequency: Int,
        notes: String,
        timeHours: Int,
        timeMinutes: Int
    ) {
        val newPlant = Plant(
            image = image,
            name = name,
            reminderFrequency = reminderFrequency,
            description = notes,
            timeHour = timeHours,
            timeMin = timeMinutes
        )

        // Launching coroutine to insert in database and get plantId
        viewModelScope.launch(Dispatchers.IO) {
            // We need ID to set unique pending intent and we don't have it until we insert it in database
            val plantId = dao.insertNewPlant(newPlant)
            alarmUtilities.setExactAlarm(newPlant, plantId)
        }
    }

    fun updatePlantAndAlarm(
        id: Long,
        image: Int,
        name: String,
        notes: String,
        reminderFrequency: Int,
        timeHours: Int,
        timeMinutes: Int,
        oldPlant: Plant
    ) {
        val newPlant = Plant(
            id = id,
            image = image,
            name = name,
            description = notes,
            reminderFrequency = reminderFrequency,
            notifications = true,
            timeHour = timeHours,
            timeMin = timeMinutes
        )
        alarmUtilities.apply {
            cancelAlarm(oldPlant)
            setExactAlarm(newPlant, newPlant.id)
        }

        viewModelScope.launch(Dispatchers.IO) {
            dao.update(newPlant)
        }
    }

    fun timeFormat(hour: Int, min: Int): String = String.format("%02d:%02d", hour, min)
}

class AddNewPlantViewModelFactory(
    private val dao: PlantDao,
    private val application: Application
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddNewPlantViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddNewPlantViewModel(dao, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}