package com.sonofasleep.watertheplantapp.viewmodels

import android.app.Application
import androidx.lifecycle.*
import androidx.work.*
import com.sonofasleep.watertheplantapp.const.REMINDER_WORKER_TAG
import com.sonofasleep.watertheplantapp.data.DataStoreRepository
import com.sonofasleep.watertheplantapp.database.Plant
import com.sonofasleep.watertheplantapp.database.PlantDao
import com.sonofasleep.watertheplantapp.model.PlantIconItem
import com.sonofasleep.watertheplantapp.worker.ReminderWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

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

    // WorkManager instance
    private val workManager = WorkManager.getInstance(application)

    // Workers logs
    val workStatusByTag = workManager.getWorkInfosByTagLiveData(REMINDER_WORKER_TAG)


    // Save sort type to data store
    fun saveSortTypeToDataStore(isASC: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            dataStore.saveSortType(isASC, application.applicationContext)
        }
    }

    private fun createPeriodicWorkRequest(plant: Plant): PeriodicWorkRequest {

        // Data instance with the icon, name and id passed to it
        val data = Data.Builder()
            .putInt(ReminderWorker.plantIconKey, plant.image)
            .putString(ReminderWorker.plantNameKey, plant.name)
            .build()

        /**
         * Main interval parameter
         */
        val timeUnit = TimeUnit.MINUTES

        // Setting periodic work request with plantID as Tag
        return PeriodicWorkRequest.Builder(
            ReminderWorker::class.java,
            plant.reminderFrequency.toLong(),
            timeUnit
        )
            .setInputData(data)
            .setInitialDelay(plant.reminderFrequency.toLong(), timeUnit)
            .addTag(REMINDER_WORKER_TAG)
            .build()
    }

    private fun startPeriodicWork(periodicWorkRequest: PeriodicWorkRequest) {
        workManager.enqueue(periodicWorkRequest)
    }

    fun switchWork(plant: Plant) {
        if (plant.notifications) {
            if (plant.workId != null) {
                workManager.cancelWorkById(plant.workId)
            }
            val newPlant = plant.copy(notifications = false, workId = null)
            viewModelScope.launch {
                dao.update(newPlant)
            }
        } else {
            val newWork = createPeriodicWorkRequest(plant)
            startPeriodicWork(newWork)
            val newPlantWithWorkId = plant.copy(notifications = true, workId = newWork.id)
            viewModelScope.launch {
                dao.update(newPlantWithWorkId)
            }
        }
    }

    fun insertPlantStartWork(image: Int, name: String, reminderFrequency: Int, notes: String) {
        val newPlant = Plant(
            image = image,
            name = name,
            reminderFrequency = reminderFrequency,
            description = notes
        )
        // Launching coroutine to insert in database
        viewModelScope.launch(Dispatchers.IO) {

            // Inserting new plant in dataBase and scheduling reminder
            val workRequest = createPeriodicWorkRequest(newPlant)
            val newPlantWithId = newPlant.copy(
                notifications = true,
                workId = workRequest.id
            )

            val rowId = dao.insertNewPlant(newPlantWithId)
            startPeriodicWork(workRequest)
        }
    }

    fun updatePlantAndWork(
        id: Long,
        image: Int,
        name: String,
        notes: String,
        reminderFrequency: Int,
        oldPlant: Plant
    ) {
        val newPlant = Plant(
            id = id,
            image = image,
            name = name,
            description = notes,
            reminderFrequency = reminderFrequency
        )
        // Launching coroutine to update in database
        viewModelScope.launch(Dispatchers.IO) {
            if (oldPlant.workId != null) {
                workManager.cancelWorkById(oldPlant.workId)
            }
            val newWork = createPeriodicWorkRequest(newPlant)
            val newPlantWithWorkId = newPlant.copy(notifications = true, workId = newWork.id)
            startPeriodicWork(newWork)
            dao.update(newPlantWithWorkId)
        }
    }

    fun deletePlantCancelWork(plant: Plant) {
        // Launching coroutine to delete in database
        viewModelScope.launch(Dispatchers.IO) {
            if (plant.workId != null) {
                workManager.cancelWorkById(plant.workId)
            }
            dao.delete(plant)
        }
    }

    fun getPlant(id: Long): LiveData<Plant> = dao.getPlantById(id).asLiveData()

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