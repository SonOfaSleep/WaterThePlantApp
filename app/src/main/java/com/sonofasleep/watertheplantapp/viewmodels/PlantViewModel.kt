package com.sonofasleep.watertheplantapp.viewmodels

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.*
import androidx.work.*
import com.sonofasleep.watertheplantapp.const.REMINDER_WORKER_TAG
import com.sonofasleep.watertheplantapp.database.Plant
import com.sonofasleep.watertheplantapp.database.PlantDao
import com.sonofasleep.watertheplantapp.database.SortType
import com.sonofasleep.watertheplantapp.model.PlantIconItem
import com.sonofasleep.watertheplantapp.worker.ReminderWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit

class PlantViewModel(private val dao: PlantDao, private val application: Application) :
    ViewModel() {

    // For plant icon choose in recyclerView
    // When not in AddPlantFragment icon is null
    private val _icon = MutableLiveData<PlantIconItem?>(null)
    val icon: LiveData<PlantIconItem?> = _icon

    // Getting a states of flow, witch returns new list of Plants every time we change SortType
    private val sortFlow = MutableStateFlow(SortType.NONE)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val plantListFlow = sortFlow
        .flatMapLatest {
            when (it) {
                SortType.NONE -> dao.getAllOrderedASC()
                SortType.ASCENDING -> dao.getAllOrderedASC()
                else -> dao.getAllOrderedDESC()
            }
        }

    // List of all plants using flow
    val allPlants: LiveData<List<Plant>> = plantListFlow.asLiveData()

    // WorkManager instance
    private val workManager = WorkManager.getInstance(application)

    // Workers logs
    val workStatusByTag = workManager.getWorkInfosByTagLiveData(REMINDER_WORKER_TAG)


    // Changing sortFlow's sortType will emit new flow, and change list of all plants
    fun changeSortType(sortType: SortType) {
        sortFlow.value = sortType
    }

    private fun createPeriodicWorkRequest(plant: Plant): PeriodicWorkRequest {

        // Data instance with the icon, name and id passed to it
        val data = Data.Builder()
            .putInt(ReminderWorker.plantIconKey, plant.image)
            .putString(ReminderWorker.plantNameKey, plant.name)
            .build()

        val timeUnit = TimeUnit.MINUTES
        // Setting periodic work request with plantID as Tag
        val workRequest = PeriodicWorkRequest.Builder(
            ReminderWorker::class.java,
            plant.reminderFrequency.toLong(),
            timeUnit
        )
            .setInputData(data)
            .setInitialDelay(plant.reminderFrequency.toLong(), timeUnit)
            .addTag(REMINDER_WORKER_TAG)
            .build()
        return workRequest
    }

    private fun startPeriodicWork(periodicWorkRequest: PeriodicWorkRequest) {
        workManager.enqueue(periodicWorkRequest)
    }

    @Synchronized
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