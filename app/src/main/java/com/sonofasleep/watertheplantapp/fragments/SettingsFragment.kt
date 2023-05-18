package com.sonofasleep.watertheplantapp.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.sonofasleep.watertheplantapp.R
import com.sonofasleep.watertheplantapp.databinding.FragmentSettingsBinding


class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Hiding buttons
        val searchButton = binding.toolbar.plantListToolbar.menu.findItem(R.id.app_bar_search)
        val sortButton = binding.toolbar.plantListToolbar.menu.findItem(R.id.app_bar_sorting)
        val settingsButton = binding.toolbar.plantListToolbar.menu.findItem(R.id.app_bar_settings)
        searchButton.isVisible = false
        sortButton.isVisible = false
        settingsButton.isVisible = false

        // Setting navigation
        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(navController.graph)

        view.findViewById<Toolbar>(R.id.plant_list_toolbar)
            .setupWithNavController(navController, appBarConfiguration)

        // Populate Language menu
        val languages = resources.getStringArray(R.array.app_languages)
        val adapter = ArrayAdapter(requireContext(), R.layout.language_list_item, languages)
        binding.languageMenuAutoComplete.apply {
            setDropDownBackgroundDrawable(
                ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.language_dropdown_bg,
                    null
                )
            )
            setAdapter(adapter)
        }
    }
}