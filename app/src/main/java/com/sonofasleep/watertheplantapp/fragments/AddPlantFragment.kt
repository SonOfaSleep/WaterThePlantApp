package com.sonofasleep.watertheplantapp.fragments

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat.getFont
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.slider.Slider
import com.sonofasleep.watertheplantapp.PlantApplication
import com.sonofasleep.watertheplantapp.R
import com.sonofasleep.watertheplantapp.adapters.PlantIconAdapter
import com.sonofasleep.watertheplantapp.const.DEBUG_TAG
import com.sonofasleep.watertheplantapp.const.NOTIFICATION_REQUEST_CODE
import com.sonofasleep.watertheplantapp.data.IconSource
import com.sonofasleep.watertheplantapp.databinding.FragmentAddPlantBinding
import com.sonofasleep.watertheplantapp.model.PlantIconItem
import com.sonofasleep.watertheplantapp.viewmodels.AddNewPlantViewModel
import com.sonofasleep.watertheplantapp.viewmodels.AddNewPlantViewModelFactory
import java.util.Calendar

class AddPlantFragment : Fragment() {
    private var _binding: FragmentAddPlantBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddNewPlantViewModel by activityViewModels {
        AddNewPlantViewModelFactory(
            (activity?.application as PlantApplication).database.plantDao(),
            activity?.application as PlantApplication
        )
    }

    private val navArgs: AddPlantFragmentArgs by navArgs()
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddPlantBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Hiding buttons
        val searchButton = binding.toolbar.plantListToolbar.menu.findItem(R.id.app_bar_search)
        val sortButton = binding.toolbar.plantListToolbar.menu.findItem(R.id.app_bar_sorting)
        searchButton.isVisible = false
        sortButton.isVisible = false

        // Setting navigation
        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(navController.graph)

        view.findViewById<Toolbar>(R.id.plant_list_toolbar)
            .setupWithNavController(navController, appBarConfiguration)

        // Setting recyclerView
        recyclerView = binding.recyclerView

        val adapter = PlantIconAdapter(
            viewModel,
            IconSource.imageList,
            viewModel.icon.value,

            onCameraClicked = {
                val action = AddPlantFragmentDirections.actionAddPlantFragmentToCameraPermissionFragment()
                findNavController().navigate(action)
            }
        )

        recyclerView.adapter = adapter

        // Plant id will be > 0 if it's edit not new one (default value is 0L)
        val plantId = navArgs.plantId

        /**
         * Setting different fonts for the label and input text
         * Changing it in xml don't work ¯\_(ツ)_/¯
         */
        val typeface = getFont(requireContext(), R.font.montserrat_alternates_bold)
        binding.apply {
            filledPlantName.typeface = typeface
            filledNotes.typeface = typeface
        }

        /**
         * If plantId is > 0 than it's edit not new one
         * viewModel.init is true only at first enter. We need it for updating viewModels values
         * when editing plant.
         */
        if (viewModel.init.value!! && plantId > 0) {
            binding.toolbar.plantListToolbar.title = getString(R.string.edit_plant_toolbar_title)

            viewModel.getPlantAsLiveData(plantId).observe(this.viewLifecycleOwner) { plant ->
                viewModel.apply {
                    setOldPlant(plant)
                    setPlantIcon(PlantIconItem(plant.image.iconNormal, plant.image.iconDry))
                    setName(plant.name)
                    setNotes(plant.description)
                    setSliderValue(plant.reminderFrequency)
                    setPlantTime(plant.timeHour, plant.timeMin)
                    setInitFalse()
                }
                bindPlant()
            }
        } else {
            binding.toolbar.plantListToolbar.title = getString(R.string.new_plant_toolbar_title)
            // Current time for time button in new plant creation
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minutes = calendar.get(Calendar.MINUTE)
            viewModel.setPlantTime(hour, minutes)

            bindPlant()
        }

        binding.nameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(p0: Editable?) {
                viewModel.setName(p0.toString())
            }
        })

        binding.notesEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(p0: Editable?) {
                viewModel.setNotes(p0.toString())
            }
        })

        binding.wateringSlider.addOnChangeListener(
            Slider.OnChangeListener { _, value, _ -> viewModel.setSliderValue(value.toInt()) })

        binding.timeButton.setOnClickListener {
            val newFragment = TimePickerFragment(viewModel)
            newFragment.show(parentFragmentManager, "Time picker")
        }

        binding.saveButton.setOnClickListener {

            if (Build.VERSION.SDK_INT >= 33) {
                if (!checkForNotificationPermission()) {
                    askForNotificationPermission()
                }
            }

            // Plant will be created no matter the user answer and i decided to not wait for him
            if (plantId > 0) updatePlant() else addPlant()
        }
    }

    private fun bindPlant() {
        binding.apply {
            nameEditText.setText(viewModel.name.value, TextView.BufferType.SPANNABLE)
            notesEditText.setText(viewModel.notes.value, TextView.BufferType.SPANNABLE)
            wateringSlider.value = (viewModel.sliderValue.value?.toFloat() ?: 1.0) as Float
            timeButton.text = viewModel.getTime()
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkForNotificationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun askForNotificationPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
            NOTIFICATION_REQUEST_CODE
        )
    }

    private fun updatePlant() {
        if (checkEntry()) {
            viewModel.updatePlantAndAlarm(
                id = navArgs.plantId,
                image = viewModel.icon.value!!,
                name = viewModel.name.value!!,
                notes = viewModel.notes.value!!,
                reminderFrequency = viewModel.sliderValue.value!!,
                timeHours = viewModel.hour.value!!,
                timeMinutes = viewModel.minutes.value!!,
                oldPlant = viewModel.oldPlant.value!!
            )
        }
        viewModel.resetFragmentValues()
        findNavController().navigate(R.id.action_addPlantFragment_to_plantsListFragment)
    }

    private fun addPlant() {
        // Checking if plant is valid than adding it to room
        if (checkEntry()) {
            viewModel.insertPlantStartAlarm(
                viewModel.icon.value!!,
                viewModel.name.value!!,
                viewModel.sliderValue.value!!,
                viewModel.notes.value!!,
                viewModel.hour.value!!,
                viewModel.minutes.value!!
            )
            viewModel.resetFragmentValues()
            findNavController().navigate(R.id.action_addPlantFragment_to_plantsListFragment)
        }
    }

    private fun isIconNotNull(): Boolean {
        return viewModel.isIconNotNull()
    }

    private fun isPhotoNotNull(): Boolean {
        return viewModel.isPhotoNotNull()
    }

    private fun isNameValid(): Boolean {
        return viewModel.isNameValid(binding.nameEditText.text.toString())
    }

    private fun checkEntry(): Boolean {
        val toastMessage: String
        return if (!isIconNotNull() && !isNameValid() && !isPhotoNotNull()) {
            toastMessage = getText(R.string.choose_icon_and_name).toString()
            makeToast(toastMessage)
            false
        } else if (!isIconNotNull() && !isPhotoNotNull()) {
            toastMessage = getText(R.string.choose_plant_icon).toString()
            makeToast(toastMessage)
            false
        } else if (!isNameValid()) {
            toastMessage = getText(R.string.choose_plant_name).toString()
            makeToast(toastMessage)
            false
        } else {
            true
        }
    }

    private fun makeToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}