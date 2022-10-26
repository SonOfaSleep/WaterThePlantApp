package com.sonofasleep.watertheplantapp.fragments

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.RecyclerView
import com.sonofasleep.watertheplantapp.PlantApplication
import com.sonofasleep.watertheplantapp.R
import com.sonofasleep.watertheplantapp.adapters.PlantIconAdapter
import com.sonofasleep.watertheplantapp.data.IconSource
import com.sonofasleep.watertheplantapp.databinding.FragmentAddPlantBinding
import com.sonofasleep.watertheplantapp.viewmodels.PlantViewModel
import com.sonofasleep.watertheplantapp.viewmodels.PlantViewModelFactory

class AddPlantFragment : Fragment() {
    private var _binding: FragmentAddPlantBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PlantViewModel by activityViewModels {
        PlantViewModelFactory(
            (activity?.application as PlantApplication).database.plantDao()
        )
    }

    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddPlantBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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

        // Setting recyclerView adapter
        recyclerView = binding.recyclerView
        recyclerView.adapter = PlantIconAdapter(viewModel, IconSource.imageList)

        // Save button action
        binding.saveButton.setOnClickListener {
            if (checkEntry()) {
                viewModel.insertPlant(
                    viewModel.icon.value!!.image,
                    binding.plantNameEditText.text.toString(),
                    binding.wateringSlider.value.toInt(),
                    binding.notesEditText.text.toString()
                )
                viewModel.resetIcon()
                findNavController().navigate(R.id.action_addPlantFragment_to_plantsListFragment)
            }
        }
    }

    private fun isIconNotNull(): Boolean {
        return viewModel.isIconNotNull()
    }
    private fun isNameValid(): Boolean {
        return viewModel.isNameValid(binding.plantNameEditText.text.toString())
    }
    private fun checkEntry(): Boolean {
        var toastMessage = ""
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