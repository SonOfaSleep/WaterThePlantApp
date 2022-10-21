package com.sonofasleep.watertheplantapp.viewmodels

import androidx.lifecycle.*
import com.sonofasleep.watertheplantapp.database.Plant
import com.sonofasleep.watertheplantapp.database.PlantDao
import com.sonofasleep.watertheplantapp.model.PlantIconItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PlantViewModel(val dao: PlantDao) : ViewModel() {

    private val _icon = MutableLiveData<PlantIconItem?>(null)
    val icon: LiveData<PlantIconItem?> = _icon

    val allPlants: LiveData<List<Plant>> = dao.getAllOrderedASC().asLiveData()

    fun insertPlant(image: Int, name: String, reminderFrequency: Int) {
        val newPlant = Plant(image = image, name = name, reminderFrequency = reminderFrequency)

        // Launching coroutine to insert in database
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertNewPlant(newPlant)
        }
    }

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