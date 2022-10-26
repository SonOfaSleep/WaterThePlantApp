package com.sonofasleep.watertheplantapp.viewmodels

import androidx.lifecycle.*
import com.sonofasleep.watertheplantapp.database.Plant
import com.sonofasleep.watertheplantapp.database.PlantDao
import com.sonofasleep.watertheplantapp.database.SortType
import com.sonofasleep.watertheplantapp.model.PlantIconItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

class PlantViewModel(private val dao: PlantDao) : ViewModel() {

    private val _icon = MutableLiveData<PlantIconItem?>(null)
    val icon: LiveData<PlantIconItem?> = _icon

    // Getting a states of flow, witch returns new list of Plants every time we change SortType
    private val sortFlow = MutableStateFlow(SortType.NONE)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val plantListFlow = sortFlow
        .flatMapLatest {
            when(it) {
                SortType.NONE -> dao.getAllOrderedASC()
                SortType.ASCENDING -> dao.getAllOrderedASC()
                else -> dao.getAllOrderedDESC()
            }
        }

    // List of all plants using flow
    val allPlants: LiveData<List<Plant>> = plantListFlow.asLiveData()

    // Changing sortFlow's sortType will emit new flow, and change list of all plants
    fun changeSortType(sortType: SortType) {
        sortFlow.value = sortType
    }

    fun insertPlant(image: Int, name: String, reminderFrequency: Int, notes: String) {
        val newPlant = if (notes.isBlank()) {
            Plant(
                image = image,
                name = name,
                reminderFrequency = reminderFrequency)
        } else {
            Plant(
                image = image,
                name = name,
                reminderFrequency = reminderFrequency,
                description = notes)
        }
        // Launching coroutine to insert in database
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertNewPlant(newPlant)
        }
    }

    fun deletePlant(plant: Plant) {
        viewModelScope.launch(Dispatchers.IO) {
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
class PlantViewModelFactory(private val dao: PlantDao): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlantViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlantViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}