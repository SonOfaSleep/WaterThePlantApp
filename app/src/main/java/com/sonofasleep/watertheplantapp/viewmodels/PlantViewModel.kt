package com.sonofasleep.watertheplantapp.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.sonofasleep.watertheplantapp.R
import com.sonofasleep.watertheplantapp.model.PlantIconItem

class PlantViewModel : ViewModel() {

    private val _icon = MutableLiveData<PlantIconItem?>(null)
    val icon: LiveData<PlantIconItem?> = _icon

    fun setPlantIcon(item: PlantIconItem) {
        _icon.value = item
    }

    fun hasNoIcon(): Boolean {
        return _icon.value == null
    }
}