package com.sonofasleep.watertheplantapp.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
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
import com.google.android.material.slider.Slider
import com.sonofasleep.watertheplantapp.PlantApplication
import com.sonofasleep.watertheplantapp.R
import com.sonofasleep.watertheplantapp.adapters.PlantIconAdapter
import com.sonofasleep.watertheplantapp.const.DEBUG_TAG
import com.sonofasleep.watertheplantapp.data.IconSource
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

        // Plant id will be > 0 if it's edit not new one (default value is 0L)
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
         * If plantId is > 0 than it's edit not new one
         * viewModel.init is true only at first enter. We need it for updating viewModels values
         * when editing plant.
         */
        if (viewModel.init.value!! && plantId > 0) {

            viewModel.getPlantAsLiveData(plantId).observe(this.viewLifecycleOwner) { plant ->
                viewModel.apply {
                    setOldPlant(plant)
                    setPlantIcon(PlantIconItem(plant.image))
                    setName(plant.name)
                    setNotes(plant.description)
                    setSliderValue(plant.reminderFrequency)
                    setPlantTime(plant.timeHour, plant.timeMin)
                    setInitFalse()
                }
                bindPlant()
            }
        } else {
            bindPlant()
        }

        viewModel.name.observe(this.viewLifecycleOwner) {
            Log.d(DEBUG_TAG, "Name is = $it")
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
            if (plantId > 0) updatePlant() else addPlant()
        }
    }

    private fun bindPlant() {
        Log.d(DEBUG_TAG, "binding")
        recyclerView.adapter = PlantIconAdapter(
            viewModel,
            IconSource.imageList,
            viewModel.icon.value
        )
        binding.apply {
            nameEditText.setText(viewModel.name.value, TextView.BufferType.SPANNABLE)
            notesEditText.setText(viewModel.notes.value, TextView.BufferType.SPANNABLE)
            wateringSlider.value = (viewModel.sliderValue.value?.toFloat() ?: 1.0) as Float
            timeButton.text = viewModel.getTime()
        }
    }

    private fun updatePlant() {
        if (checkEntry()) {
            viewModel.updatePlantAndAlarm(
                id = navArgs.plantId,
                image = viewModel.icon.value!!.image,
                name = binding.nameEditText.text.toString(),
                notes = binding.notesEditText.text.toString(),
                reminderFrequency = binding.wateringSlider.value.toInt(),
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
                viewModel.icon.value!!.image,
                binding.nameEditText.text.toString(),
                binding.wateringSlider.value.toInt(),
                binding.notesEditText.text.toString(),
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