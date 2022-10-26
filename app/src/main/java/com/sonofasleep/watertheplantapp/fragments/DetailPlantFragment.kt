package com.sonofasleep.watertheplantapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.sonofasleep.watertheplantapp.PlantApplication
import com.sonofasleep.watertheplantapp.R
import com.sonofasleep.watertheplantapp.database.Plant
import com.sonofasleep.watertheplantapp.databinding.FragmentDetailPlantBinding
import com.sonofasleep.watertheplantapp.viewmodels.PlantViewModel
import com.sonofasleep.watertheplantapp.viewmodels.PlantViewModelFactory

class DetailPlantFragment : Fragment() {
    private var _binding: FragmentDetailPlantBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PlantViewModel by activityViewModels {
        PlantViewModelFactory(
            (activity?.application as PlantApplication).database.plantDao()
        )
    }

    private lateinit var plant: Plant
    private val navigationArgs: DetailPlantFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDetailPlantBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val plantId = navigationArgs.plantId

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

        // Observing liveData plant and binding it to UI
        viewModel.getPlant(plantId).observe(this.viewLifecycleOwner) {
            plant = it
            bindPlant()
        }

        // Delete button action
        binding.deleteButton.setOnClickListener {
            viewModel.deletePlant(plant)
            findNavController().navigate(R.id.action_detailPlantFragment_to_plantsListFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun bindPlant() {
        binding.apply {
            cardImage.setImageResource(plant.image)
            plantName.text = plant.name
            wateringType.text = getString(
                R.string.reminder_frequency_int_days,
                plant.reminderFrequency,
                context?.resources?.getQuantityString(
                    R.plurals.plural_day,
                    plant.reminderFrequency,
                    plant.reminderFrequency
                ) ?: "Ups, something went wrong"
            )
            notesText.text = plant.description
        }
    }
}