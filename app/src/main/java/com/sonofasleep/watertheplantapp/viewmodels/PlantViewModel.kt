package com.sonofasleep.watertheplantapp.viewmodels

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.work.*
import com.sonofasleep.watertheplantapp.alarm.AlarmUtilities
import com.sonofasleep.watertheplantapp.data.DataStoreRepository
import com.sonofasleep.watertheplantapp.database.Plant
import com.sonofasleep.watertheplantapp.database.PlantDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

enum class OnLongClickEnabled { TRUE, FALSE }

class PlantViewModel(private val dao: PlantDao, private val application: Application) :
    ViewModel() {

    // DataStore instance
    private val dataStore = DataStoreRepository(application.applicationContext)
    val sortTypeIsASC: LiveData<Boolean> = dataStore.readSortType.asLiveData()

    // List of all plants (observing isSortASC preference in data store)
    val allPlants: LiveData<List<Plant>> = sortTypeIsASC.switchMap {
        when (it) {
            true -> dao.getAllOrderedASC().asLiveData()
            else -> dao.getAllOrderedDESC().asLiveData()
        }
    }

    // List of all searched plants
    private val searchQuery = MutableLiveData<String>()
    val searchResults: LiveData<List<Plant>> = searchQuery.switchMap {
        getSearchResult(it)
    }

    // AlarmManager instance
    private val alarmManager: AlarmManager = application.applicationContext
        .getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val alarmUtilities = AlarmUtilities(application.applicationContext, alarmManager)

    // OnLongClick state
    private val _longClickEnabled = MutableLiveData(OnLongClickEnabled.FALSE)
    val longClickEnabled: LiveData<OnLongClickEnabled> = _longClickEnabled

    // Chosen plant list when long click state enabled
    private val _longClickChosenPlants = MutableLiveData<List<Plant>>()
    val longClickChosenPlants: LiveData<List<Plant>> = _longClickChosenPlants

    // For changing _longClickEnabled state
    fun changeLongClickStateToTrue() {
        _longClickEnabled.value = OnLongClickEnabled.TRUE
    }

    fun changeLongClickStateToFalse() {
        _longClickEnabled.value = OnLongClickEnabled.FALSE
    }

    /**
     * Long click list functions
     */
    fun addPlantToChosenList(plant: Plant) {
        val list = _longClickChosenPlants.value?.toMutableList() ?: mutableListOf<Plant>()
        list.add(plant)
        _longClickChosenPlants.value = list
    }

    fun removePlantFromChosenList(plant: Plant) {
        val list = _longClickChosenPlants.value?.toMutableList() ?: mutableListOf<Plant>()
        list.removeIf { it.id == plant.id }
        _longClickChosenPlants.value = list
    }

    fun emptyChosenPlantsList() {
        _longClickChosenPlants.value = mutableListOf<Plant>()
    }

    fun deleteChosenPlantsWhenLongClickModeEnabled() {
        val list = _longClickChosenPlants.value
        list?.forEach { deletePlantCancelAlarm(it) }

        emptyChosenPlantsList()
        changeLongClickStateToFalse()
    }

    // Cancel the alarm or setExactAlarm
    fun switchWork(plant: Plant) {
        val newPlant = if (plant.notifications) {
            plant.copy(notifications = false)
        } else {
            plant.copy(notifications = true)
        }
        if (plant.notifications) {
            // Canceling alarm
            alarmUtilities.cancelAlarm(plant)
        } else {
            // Setting alarm
            alarmUtilities.setExactAlarm(plant, plant.id)
        }
        viewModelScope.launch { dao.update(newPlant) }
    }

    // For setting new alarm when pressing watering button
    fun wateringDone(plant: Plant) {
        val newPlant = plant.copy(timeToWater = false)
        viewModelScope.launch { dao.update(newPlant) }
        alarmUtilities.setExactAlarm(plant, plant.id)
    }

    fun deletePlantCancelAlarm(plant: Plant) {
        alarmUtilities.cancelAlarm(plant)

        viewModelScope.launch(Dispatchers.IO) {
            dao.delete(plant)
        }
    }

    fun getPlant(id: Long): LiveData<Plant> = dao.getPlantByIdAsFlow(id).asLiveData()

    fun changeSearchQuery(query: String) {
        searchQuery.value = query
    }

    private fun getSearchResult(query: String): LiveData<List<Plant>> {
        // % - needed for SQL LIKE operator
        val searchQuery = "%$query%"
        return dao.findByName(searchQuery).asLiveData()
    }

    // Save sort type to data store
    fun saveSortTypeToDataStore(isASC: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            dataStore.saveSortType(isASC, application.applicationContext)
        }
    }

    fun timeFormat(hour: Int, min: Int): String {
        return String.format("%02d:%02d", hour, min)
    }
}

/**
 * Tip: The creation of the ViewModel factory is mostly boilerplate code,
 * so you can reuse this code for future ViewModel factories.
 */
class PlantViewModelFactory(
    private val dao: PlantDao,
    private val application: Application
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlantViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlantViewModel(dao, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}