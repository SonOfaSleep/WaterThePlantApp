package com.sonofasleep.watertheplantapp.viewmodels

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.*
import com.sonofasleep.watertheplantapp.alarm.AlarmUtilities
import com.sonofasleep.watertheplantapp.data.DataStoreRepository
import com.sonofasleep.watertheplantapp.database.Plant
import com.sonofasleep.watertheplantapp.database.PlantDao
import com.sonofasleep.watertheplantapp.model.PlantIconItem
import com.sonofasleep.watertheplantapp.utilities.FileManager.deleteImageFile
import com.sonofasleep.watertheplantapp.utilities.WaterItPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AddNewPlantViewModel(private val dao: PlantDao, private val application: Application) :
    ViewModel() {

    // DataStore instance
    private val dataStore = DataStoreRepository(application.applicationContext)
    val numberOfPermissionTry: LiveData<Int> = dataStore.readNumberOfPermissionTry.asLiveData()

    // Preferences
    private val appPreferences = WaterItPreferences(application.applicationContext)

    // For setting plantId if it's first entry here from PlantListFragment
    private val _init = MutableLiveData(true)
    val init: LiveData<Boolean> = _init

    // PlantId needs for deciding to create new plant or edit old one
    private val _plantId = MutableLiveData<Long>(-1)
    val plantId: LiveData<Long> = _plantId

    // Old plant is needed for canceling alarm
    private val _oldPlant = MutableLiveData<Plant?>(null)
    val oldPlant: LiveData<Plant?> = _oldPlant

    // For plant icon choose in recyclerView
    // When not in AddPlantFragment icon is null
    private val _iconDrawable = MutableLiveData<PlantIconItem?>(null)
    val iconDrawable: LiveData<PlantIconItem?> = _iconDrawable

    private val _iconPhotoUri = MutableLiveData<Uri?>(null)
    val iconPhotoUri: LiveData<Uri?> = _iconPhotoUri

    private val _saveImage = MutableLiveData(false)
    val saveImage: LiveData<Boolean> = _saveImage

    private val _name = MutableLiveData<String?>(null)
    val name: LiveData<String?> = _name

    private val _notes = MutableLiveData<String?>("")
    val notes: LiveData<String?> = _notes

    private val _sliderValue = MutableLiveData<Int>(1)
    val sliderValue: LiveData<Int> = _sliderValue

    private val _chosenPlantIconPosition = MutableLiveData<Int?>(null)
    val chosenPlantIconPosition: LiveData<Int?> = _chosenPlantIconPosition

    // For knowing when navigating to camera, if false = reset viewModel values
    private val _goingToCamera = MutableLiveData(false)
    val goingToCamera: LiveData<Boolean> = _goingToCamera

    // Time values for plant
    private val _hour = MutableLiveData<Int>(10)
    val hour: LiveData<Int> = _hour
    private val _minutes = MutableLiveData<Int>(0)
    val minutes: LiveData<Int> = _minutes

    // AlarmManager instance
    private val alarmManager: AlarmManager = application.applicationContext
        .getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private val alarmUtilities = AlarmUtilities(application.applicationContext, alarmManager)

    fun setInit(boolean: Boolean) {
        _init.value = boolean
    }

    fun setPlantId(plantId: Long) {
        _plantId.value = plantId
    }

    fun setImageUri(uri: Uri?) {
        _iconPhotoUri.value = uri
    }

    fun setSaveImage(boolean: Boolean) {
        _saveImage.value = boolean
    }

    fun setOldPlant(plant: Plant) {
        _oldPlant.value = plant
    }

    fun setPlantIcon(item: PlantIconItem?) {
        _iconDrawable.value = item
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

    fun setChosenPlantPosition(position: Int) {
        _chosenPlantIconPosition.value = position
    }

    fun setGoingToCamera(boolean: Boolean = true) {
        _goingToCamera.value = boolean
    }

    fun resetViewModelValues() {
        _oldPlant.value = null
        _iconDrawable.value = null
        _iconPhotoUri.value = null
        _saveImage.value = false
        _name.value = null
        _notes.value = ""
        _sliderValue.value = 1
        _hour.value = 10
        _minutes.value = 0
        _chosenPlantIconPosition.value = null
        _init.value =
            true // If not set this here it will be false on init sometimes, have no idea why
    }

    fun isIconNotNull(): Boolean = _iconDrawable.value != null

    fun isImageUriNotNull(): Boolean = _iconPhotoUri.value != null

    fun isNameValid(name: String): Boolean = name.isNotBlank()

    fun getPlantAsLiveData(id: Long): LiveData<Plant> = dao.getPlantByIdAsFlow(id).asLiveData()

    fun getPlant(id: Long): Plant = dao.getPlantById(id)

    fun insertPlantStartAlarm(
        iconDrawable: PlantIconItem? = null,
        iconPhotoImageUri: Uri? = null,
        name: String,
        reminderFrequency: Int,
        notes: String,
        timeHours: Int,
        timeMinutes: Int
    ) {
        val newPlant = Plant(
            name = name,
            reminderFrequency = reminderFrequency,
            description = notes,
            timeHour = timeHours,
            timeMin = timeMinutes
        )

        // Plant can have only one icon (iconDrawable or iconPhotoUri)
        val plantForSave = if (iconPhotoImageUri != null) {
            newPlant.copy(photoImageUri = iconPhotoImageUri.toString())
        } else {
            newPlant.copy(image = iconDrawable)
        }

        // Launching coroutine to insert in database and get plantId
        viewModelScope.launch(Dispatchers.IO) {
            // We need ID to set unique pending intent and we don't have it until we insert it in database
            val plantId = dao.insertNewPlant(plantForSave)
            alarmUtilities.setExactAlarm(plantForSave, plantId)
        }
    }

    fun updatePlantAndAlarm(
        id: Long,
        image: PlantIconItem? = null,
        imageUri: Uri? = null,
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
            photoImageUri = imageUri?.toString(),
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

        // deleting old image if oldPlant had one and it's different from new one
        if (oldPlant.photoImageUri != null && oldPlant.photoImageUri != newPlant.photoImageUri) {
            deleteImageFile(Uri.parse(oldPlant.photoImageUri))
        }
    }

    fun saveNumberOfPermissionTry(numberOfTry: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            dataStore.saveNumberOfPermissionTry(numberOfTry, application.applicationContext)
        }
    }

    fun appsFirstLaunch(): Boolean = appPreferences.readFirstLaunch()
    fun changeFirstLaunch() = appPreferences.saveFirstLaunch()
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