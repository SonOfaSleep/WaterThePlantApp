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

    // For first entering addFragment ONLY (true = first time; every other time it will be false)
    private val _init = MutableLiveData<Boolean>(true)
    val init: LiveData<Boolean> = _init

    // Old plant is needed for canceling alarm
    private val _oldPlant = MutableLiveData<Plant?>(null)
    val oldPlant: LiveData<Plant?> = _oldPlant

    // For plant icon choose in recyclerView
    // When not in AddPlantFragment icon is null
    private val _icon = MutableLiveData<PlantIconItem?>(null)
    val icon: LiveData<PlantIconItem?> = _icon

    private val _name = MutableLiveData<String?>(null)
    val name: LiveData<String?> = _name

    private val _notes = MutableLiveData<String?>("")
    val notes: LiveData<String?> = _notes

    private val _sliderValue = MutableLiveData<Int>(1)
    val sliderValue: LiveData<Int> = _sliderValue

    // Time values for plant
    private val _hour = MutableLiveData<Int>(10)
    val hour: LiveData<Int> = _hour
    private val _minutes = MutableLiveData<Int>(0)
    val minutes: LiveData<Int> = _minutes

    // AlarmManager instance
    private val alarmManager: AlarmManager = application.applicationContext
        .getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private val alarmUtilities = AlarmUtilities(application.applicationContext, alarmManager)

    fun setInitFalse() {
        _init.value = false
    }

    fun setOldPlant(plant: Plant) {
        _oldPlant.value = plant
    }

    fun setPlantIcon(item: PlantIconItem) {
        _icon.value = item
    }

    fun setName(name: String) {
        _name.value = name
    }

    fun setNotes(notes: String) {
        _notes.value = notes
    }

    fun setSliderValue(value: Int) {
        _sliderValue.value = value
    }

    fun setPlantTime(hour: Int, minutes: Int) {
        _hour.value = hour
        _minutes.value = minutes
    }

    fun resetFragmentValues() {
        _init.value = true
        _oldPlant.value = null
        _icon.value = null
        _name.value = null
        _notes.value = ""
        _sliderValue.value = 1
        _hour.value = 10
        _minutes.value = 0
    }

    fun isIconNotNull(): Boolean = _icon.value != null

    fun isNameValid(name: String): Boolean = name.isNotBlank()

    fun getPlantAsLiveData(id: Long): LiveData<Plant> = dao.getPlantByIdAsFlow(id).asLiveData()

    fun getPlant(id: Long): Plant = dao.getPlantById(id)

    fun insertPlantStartAlarm(
        image: PlantIconItem,
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
        image: PlantIconItem,
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
    fun getTime(): String = timeFormat(hour.value!!, minutes.value!!)
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