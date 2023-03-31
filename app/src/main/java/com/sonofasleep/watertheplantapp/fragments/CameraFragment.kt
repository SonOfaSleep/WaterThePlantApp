package com.sonofasleep.watertheplantapp.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.app.SharedElementCallback
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.sonofasleep.watertheplantapp.PlantApplication
import com.sonofasleep.watertheplantapp.R
import com.sonofasleep.watertheplantapp.const.DEBUG_TAG
import com.sonofasleep.watertheplantapp.databinding.FragmentCameraBinding
import com.sonofasleep.watertheplantapp.viewmodels.AddNewPlantViewModel
import com.sonofasleep.watertheplantapp.viewmodels.AddNewPlantViewModelFactory


class CameraFragment : Fragment() {
    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddNewPlantViewModel by viewModels {
        AddNewPlantViewModelFactory(
            (activity?.application as PlantApplication).database.plantDao(),
            activity?.application as PlantApplication
        )
    }
    private val navArgs: AddPlantFragmentArgs by navArgs()

    lateinit var myView: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        myView = view
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

        // Setting status bar to black
        setStatusBarDark(myView)


    }

    override fun onDestroyView() {
        super.onDestroyView()

        resetStatusBarColor(myView)
        _binding = null
    }





    private fun startCamera() {}

    private fun takePhoto() {}

    private fun setStatusBarDark(view: View) {
        activity?.window?.statusBarColor = requireContext().getColor(R.color.dark_theme_background)
        WindowInsetsControllerCompat(requireActivity().window, view).isAppearanceLightStatusBars = false
    }

    private fun resetStatusBarColor(view: View) {
        when (requireContext().resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
            Configuration.UI_MODE_NIGHT_YES -> {
                return
            }
            Configuration.UI_MODE_NIGHT_NO -> {
                activity?.window?.statusBarColor = requireContext().getColor(R.color.white)
                WindowInsetsControllerCompat(requireActivity().window, view).isAppearanceLightStatusBars = true
            }
            Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                Log.d(DEBUG_TAG, "Nigh not specified")
            }
        }
    }
}