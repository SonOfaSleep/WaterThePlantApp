package com.sonofasleep.watertheplantapp.viewmodels

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import androidx.work.*
import com.sonofasleep.watertheplantapp.alarm.AlarmReceiver
import com.sonofasleep.watertheplantapp.const.DEBUG_TAG
import com.sonofasleep.watertheplantapp.const.PLANT_ICON
import com.sonofasleep.watertheplantapp.const.PLANT_ID
import com.sonofasleep.watertheplantapp.const.PLANT_NAME
import com.sonofasleep.watertheplantapp.data.DataStoreRepository
import com.sonofasleep.watertheplantapp.database.Plant
import com.sonofasleep.watertheplantapp.database.PlantDao
import com.sonofasleep.watertheplantapp.model.PlantIconItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class PlantViewModel(private val dao: PlantDao, private val application: Application) :
    ViewModel() {

    // For plant icon choose in recyclerView
    // When not in AddPlantFragment icon is null
    private val _icon = MutableLiveData<PlantIconItem?>(null)
    val icon: LiveData<PlantIconItem?> = _icon

    // DataStore instance
    private val dataStore = DataStoreRepository(application.applicationContext)
    val sortTypeIsASC: LiveData<Boolean> = dataStore.readSortType.asLiveData()

    // List of all plants (observing isSortASC preference in data store)
    val allPlants: LiveData<List<Plant>> = Transformations.switchMap(sortTypeIsASC) {
        when (it) {
            true -> dao.getAllOrderedASC().asLiveData()
            else -> dao.getAllOrderedDESC().asLiveData()
        }
    }

    // List of all searched plants
    private val searchQuery = MutableLiveData<String>()
    val searchResults: LiveData<List<Plant>> = Transformations.switchMap(searchQuery) {
        getSearchResult(it)
    }

    // AlarmManager instance
    private val alarmManager: AlarmManager = application.applicationContext
        .getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun createAlarmIntent(plant: Plant, plantId: Long): PendingIntent {
        val alarmIntent = Intent(application.applicationContext, AlarmReceiver::class.java)
        alarmIntent.putExtra(PLANT_ICON, plant.image)
        alarmIntent.putExtra(PLANT_NAME, plant.name)
        alarmIntent.putExtra(PLANT_ID, plantId)

        // We need this data to create unique pending intents. Each plant will have unique ID and
        // unique pendingIntent.
        alarmIntent.data = Uri.parse("scheme:///$plantId")

        return PendingIntent.getBroadcast(
            application.applicationContext,
            0,
            alarmIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    // plantId need only for newly created plants. For others it's ok to use plant.ID
    private fun setExactAlarm(plant: Plant, plantId: Long) {
        val dayInMillis: Long = 86400000

        val timeOfDay: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, plant.timeHour)
            set(Calendar.MINUTE, plant.timeMin)
        }
        val currentTime = System.currentTimeMillis()

        var timeUntilAlarm = if (currentTime >= timeOfDay.timeInMillis) {
            timeOfDay.timeInMillis + dayInMillis
        } else {
            timeOfDay.timeInMillis
        }

        // If delay is 1 day we add zero, if 2 days add one more day etc.
        timeUntilAlarm += dayInMillis * (plant.reminderFrequency - 1)

        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            timeUntilAlarm,
            createAlarmIntent(plant, plantId)
        )
    }

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
            setExactAlarm(newPlant, plantId)
        }
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
            alarmManager.cancel(createAlarmIntent(plant, plant.id))
        } else {
            // Setting alarm
            setExactAlarm(plant, plant.id)
        }
        viewModelScope.launch { dao.update(newPlant) }
    }

    // For setting new alarm when pressing watering button
    fun wateringDone(plant: Plant) {
        val newPlant = plant.copy(timeToWater = false)
        viewModelScope.launch { dao.update(newPlant) }
        setExactAlarm(plant, plant.id)
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
        alarmManager.cancel(createAlarmIntent(oldPlant, oldPlant.id))
        setExactAlarm(newPlant, newPlant.id)

        viewModelScope.launch(Dispatchers.IO) {
            dao.update(newPlant)
        }
    }

    fun deletePlantCancelAlarm(plant: Plant) {
        alarmManager.cancel(createAlarmIntent(plant, plant.id))

        viewModelScope.launch(Dispatchers.IO) {
            dao.delete(plant)
        }
    }

    fun getPlant(id: Long): LiveData<Plant> = dao.getPlantByIdAsFlow(id).asLiveData()

    fun setPlantIcon(item: PlantIconItem) {
        _icon.value = item
    }

    fun resetIcon() {
        _icon.value = null
    }

    fun isIconNotNull(): Boolean = _icon.value != null
    fun isNameValid(name: String): Boolean = name.isNotBlank()

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

    //    fun insertPlantStartWork(image: Int, name: String, reminderFrequency: Int, notes: String) {
//        val newPlant = Plant(
//            image = image,
//            name = name,
//            reminderFrequency = reminderFrequency,
//            description = notes
//        )
//        // Launching coroutine to insert in database
//        viewModelScope.launch(Dispatchers.IO) {
//
//            // Inserting new plant in dataBase and scheduling reminder
//            val workRequest = createPeriodicWorkRequest(newPlant)
//            val newPlantWithId = newPlant.copy(
//                notifications = true,
//                workId = workRequest.id
//            )
//
//            dao.insertNewPlant(newPlantWithId)
//            startPeriodicWork(workRequest)
//        }
//    }

    //    private fun createPeriodicWorkRequest(plant: Plant): PeriodicWorkRequest {
//
//        // Data instance with the icon, name and id passed to it
//        val data = Data.Builder()
//            .putInt(ReminderWorker.plantIconKey, plant.image)
//            .putString(ReminderWorker.plantNameKey, plant.name)
//            .build()
//
//        /**
//         * Main interval parameter
//         */
//        val timeUnit = TimeUnit.DAYS
//
//        // Setting periodic work request with plantID as Tag
//        return PeriodicWorkRequest.Builder(
//            ReminderWorker::class.java,
//            plant.reminderFrequency.toLong(),
//            timeUnit
//        )
//            .setInputData(data)
//            .setInitialDelay(plant.reminderFrequency.toLong(), timeUnit)
//            .addTag(REMINDER_WORKER_TAG)
//            .build()
//    }

//    private fun startPeriodicWork(periodicWorkRequest: PeriodicWorkRequest) {
//        workManager.enqueue(periodicWorkRequest)
//    }

    //    fun deletePlantCancelWork(plant: Plant) {
//        // Launching coroutine to delete in database
//        viewModelScope.launch(Dispatchers.IO) {
//            if (plant.workId != null) {
//                workManager.cancelWorkById(plant.workId)
//            }
//            dao.delete(plant)
//        }
//    }

    //    fun updatePlantAndWork(
//        id: Long,
//        image: Int,
//        name: String,
//        notes: String,
//        reminderFrequency: Int,
//        oldPlant: Plant
//    ) {
//        val newPlant = Plant(
//            id = id,
//            image = image,
//            name = name,
//            description = notes,
//            reminderFrequency = reminderFrequency
//        )
//        // Launching coroutine to update in database
//        viewModelScope.launch(Dispatchers.IO) {
//            if (oldPlant.workId != null) {
//                workManager.cancelWorkById(oldPlant.workId)
//            }
//            val newWork = createPeriodicWorkRequest(newPlant)
//            val newPlantWithWorkId = newPlant.copy(notifications = true, workId = newWork.id)
//            startPeriodicWork(newWork)
//            dao.update(newPlantWithWorkId)
//        }
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