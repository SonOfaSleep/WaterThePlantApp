package com.sonofasleep.watertheplantapp.fragments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.ScaleDrawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat.getFont
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.sonofasleep.watertheplantapp.PlantApplication
import com.sonofasleep.watertheplantapp.R
import com.sonofasleep.watertheplantapp.const.DEBUG_TAG
import com.sonofasleep.watertheplantapp.database.Plant
import com.sonofasleep.watertheplantapp.databinding.FragmentDetailPlantBinding
import com.sonofasleep.watertheplantapp.viewmodels.PlantViewModel
import com.sonofasleep.watertheplantapp.viewmodels.PlantViewModelFactory

class DetailPlantFragment : Fragment() {
    private var _binding: FragmentDetailPlantBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PlantViewModel by activityViewModels {
        PlantViewModelFactory(
            (activity?.application as PlantApplication).database.plantDao(),
            activity?.application as PlantApplication
        )
    }

    private lateinit var plant: Plant
    private val navigationArgs: DetailPlantFragmentArgs by navArgs()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailPlantBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val plantId = navigationArgs.plantId

        // Inflating toolbarMenuLayout
        binding.toolbar.plantListToolbar.inflateMenu(R.menu.menu_main)

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

        // Observing liveData plant and binding it to UI
        viewModel.getPlant(plantId).observe(this.viewLifecycleOwner) {
            plant = it
            bindPlant()
        }

        // Delete button action
        binding.deleteButton.setOnClickListener {
            deleteAlertDialog()
        }

        // Edit button action
        binding.editButton.setOnClickListener {
            val action = DetailPlantFragmentDirections
                .actionDetailPlantFragmentToAddPlantFragment().setPlantId(plant.id)
            findNavController().navigate(action)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun deleteAlertDialog() {
        val alertDialog: AlertDialog? = activity?.let {

            val builder = AlertDialog.Builder(it)
            builder.apply {

                setTitle(R.string.alert_dialog_title)
                setMessage(R.string.alert_dialog_text)

                setNegativeButton(R.string.alert_dialog_button_cancel) { dialogInterface, _ ->
                    dialogInterface.cancel()
                }
                setPositiveButton(R.string.alert_dialog_button_ok) { _, _ ->
                    viewModel.deletePlantCancelAlarm(plant)
                    findNavController()
                        .navigate(R.id.action_detailPlantFragment_to_plantsListFragment)
                }
            }
            builder.create()
            builder.show()
        }
        // Setting font only to message in alert dialog. Main fontStyle set in themes (AlertDialogTheme)
        val textView = alertDialog?.findViewById<TextView>(android.R.id.message)
        val typeFace = getFont(requireContext(), R.font.montserrat_alternates_regular)
        textView?.typeface = typeFace
    }

    private fun bindPlant() {
        binding.apply {
            setCardImageAccordingToScreenSize()
            toolbar.plantListToolbar.title = plant.name
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
            wateringTime.text = getString(
                R.string.reminder_frequency_hour_min,
                viewModel.timeFormat(plant.timeHour, plant.timeMin)
            )
        }
    }

    private fun setCardImageAccordingToScreenSize() {
        binding.cardImage.setImageResource(plant.image.iconNormal)
        val displayHeight = context?.resources?.displayMetrics?.heightPixels ?: 1000
        // Getting default view size in pixels. Default is set in layout file in dp
        // For small screens i want it to be half the size
        val imageSizeInPx = binding.cardImage.height
        val notesSizeInPx = binding.notesCardView.height
        val newHeights = when {
            displayHeight <= 1000 -> imageSizeInPx / 2
            else -> imageSizeInPx
        }
        val newNotesHeights = when {
            displayHeight <= 1000 -> notesSizeInPx / 2
            else -> notesSizeInPx
        }

        binding.cardImage.layoutParams.height = newHeights
        binding.cardImage.layoutParams.width = newHeights
        binding.notesCardView.layoutParams.height = newNotesHeights
    }
}

fun convertPixelsToDp(px: Float, context: Context): Float {
    return px / (context.resources
        .displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
}