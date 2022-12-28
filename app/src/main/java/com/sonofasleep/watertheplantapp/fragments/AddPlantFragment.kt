package com.sonofasleep.watertheplantapp.fragments

import android.os.Bundle
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.RecyclerView
import com.sonofasleep.watertheplantapp.PlantApplication
import com.sonofasleep.watertheplantapp.R
import com.sonofasleep.watertheplantapp.adapters.PlantIconAdapter
import com.sonofasleep.watertheplantapp.data.IconSource
import com.sonofasleep.watertheplantapp.database.Plant
import com.sonofasleep.watertheplantapp.databinding.FragmentAddPlantBinding
import com.sonofasleep.watertheplantapp.model.PlantIconItem
import com.sonofasleep.watertheplantapp.viewmodels.AddNewPlantViewModel
import com.sonofasleep.watertheplantapp.viewmodels.AddNewPlantViewModelFactory

class AddPlantFragment : Fragment() {
    private var _binding: FragmentAddPlantBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddNewPlantViewModel by viewModels {
        AddNewPlantViewModelFactory(
            (activity?.application as PlantApplication).database.plantDao(),
            activity?.application as PlantApplication
        )
    }

    private val navArgs: AddPlantFragmentArgs by navArgs()
    private lateinit var recyclerView: RecyclerView
    private lateinit var plant: Plant

    // Default plant time (don't need them when using TimePickerFragment)
//    private var plantTimeHours: Int = 10
//    private var plantTimeMinutes: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddPlantBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Setting recyclerView
        recyclerView = binding.recyclerView

        // Plant id if it's edit not new one
        val plantId = navArgs.plantId

        // Inflating toolbarMenuLayout
        binding.plantListToolbar.inflateMenu(R.menu.menu_main)

        // Hiding buttons
        val searchButton = binding.plantListToolbar.menu.findItem(R.id.app_bar_search)
        val sortButton = binding.plantListToolbar.menu.findItem(R.id.app_bar_sorting)
        searchButton.isVisible = false
        sortButton.isVisible = false

        // Setting navigation
        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(navController.graph)

        view.findViewById<Toolbar>(R.id.plant_list_toolbar)
            .setupWithNavController(navController, appBarConfiguration)

        /**
         * If plantId is >0 than it's edit not new one
         */
        if (plantId > 0) {
            // Observing liveData plant and binding it to UI
            viewModel.getPlant(plantId).observe(this.viewLifecycleOwner) {
                plant = it
//                plantTimeHours = plant.timeHour
//                plantTimeMinutes = plant.timeMin
                recyclerView.adapter =
                    PlantIconAdapter(viewModel, IconSource.imageList, PlantIconItem(plant.image))
                bindPlant(plant)
            }
        } else {
            recyclerView.adapter = PlantIconAdapter(viewModel, IconSource.imageList)
            binding.saveButton.setOnClickListener { addPlant() }
        }

        binding.timeButton.setOnClickListener {
//            createTimePicker()
            val newFragment = TimePickerFragment()
            newFragment.show(parentFragmentManager, "Time picker")
        }
    }

//    private fun createTimePicker() {
//        val isSystem24Hour = DateFormat.is24HourFormat(context)
//        val clockFormat = if (isSystem24Hour) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H
//
//        val picker = MaterialTimePicker.Builder()
//            .setTimeFormat(clockFormat)
//            .setHour(plantTimeHours)
//            .setMinute(plantTimeMinutes)
//            .setTitleText(R.string.choose_time)
//            .build()
//        picker.show(childFragmentManager, "timePicker")
//
//        picker.addOnPositiveButtonClickListener {
//            plantTimeHours = picker.hour
//            plantTimeMinutes = picker.minute
//
//            binding.timeButton.text = viewModel.timeFormat(plantTimeHours, plantTimeMinutes)
//        }
//    }

    private fun bindPlant(plant: Plant) {
        viewModel.setPlantIcon(PlantIconItem(plant.image))
        binding.apply {
            nameEditText.setText(plant.name, TextView.BufferType.SPANNABLE)
            notesEditText.setText(plant.description, TextView.BufferType.SPANNABLE)
            wateringSlider.value = plant.reminderFrequency.toFloat()
            timeButton.text = viewModel.timeFormat(plant.timeHour, plant.timeMin)

            saveButton.setOnClickListener { updatePlant() }
        }
    }

    private fun updatePlant() {
        if (checkEntry()) {
            val time = binding.timeButton.text.toString()
            viewModel.updatePlantAndAlarm(
                id = navArgs.plantId,
                image = viewModel.icon.value!!.image,
                name = binding.nameEditText.text.toString(),
                notes = binding.notesEditText.text.toString(),
                reminderFrequency = binding.wateringSlider.value.toInt(),
                timeHours = viewModel.parseHour(time),
                timeMinutes = viewModel.parseMinutes(time),
                oldPlant = plant
            )
        }
        viewModel.resetIcon()
        findNavController().navigate(R.id.action_addPlantFragment_to_plantsListFragment)
    }

    private fun addPlant() {
        // Checking if plant is valid than adding it to room
        if (checkEntry()) {
            val time = binding.timeButton.text.toString()
            viewModel.insertPlantStartAlarm(
                viewModel.icon.value!!.image,
                binding.nameEditText.text.toString(),
                binding.wateringSlider.value.toInt(),
                binding.notesEditText.text.toString(),
                viewModel.parseHour(time),
                viewModel.parseMinutes(time)
            )
            viewModel.resetIcon()
            findNavController().navigate(R.id.action_addPlantFragment_to_plantsListFragment)
        }
    }

    private fun isIconNotNull(): Boolean {
        return viewModel.isIconNotNull()
    }

    private fun isNameValid(): Boolean {
        return viewModel.isNameValid(binding.nameEditText.text.toString())
    }

    private fun checkEntry(): Boolean {
        val toastMessage: String
        return if (!isIconNotNull() && !isNameValid()) {
            toastMessage = getText(R.string.choose_icon_and_name).toString()
            makeToast(toastMessage)
            false
        } else if (!isIconNotNull()) {
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