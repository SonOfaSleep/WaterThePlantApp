package com.sonofasleep.watertheplantapp.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.sonofasleep.watertheplantapp.PlantApplication
import com.sonofasleep.watertheplantapp.R
import com.sonofasleep.watertheplantapp.const.DEBUG_TAG
import com.sonofasleep.watertheplantapp.databinding.FragmentSettingsBinding
import com.sonofasleep.watertheplantapp.utilities.DayNightTheme
import com.sonofasleep.watertheplantapp.viewmodels.PlantViewModel
import com.sonofasleep.watertheplantapp.viewmodels.PlantViewModelFactory
import java.util.Locale


class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PlantViewModel by activityViewModels {
        PlantViewModelFactory(
            (activity?.application as PlantApplication).database.plantDao(),
            activity?.application as PlantApplication
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()

        // Setting adapter for drop down menu here or it will shrink to one option if set it in onViewCreated
        val languages = resources.getStringArray(R.array.app_languages)
        val adapter = ArrayAdapter(requireContext(), R.layout.language_list_item, languages)
        binding.languageMenuAutoComplete.setAdapter(adapter)

        val currentLocale = if (!AppCompatDelegate.getApplicationLocales().isEmpty) {
            // Fetches the current Application Locale from the list
            AppCompatDelegate.getApplicationLocales()[0]
        } else {
            // Fetches the default System Locale
            Locale.getDefault()
        }

        // Setting default value in drop down menu (current language)
        // Only if language was chosen before
        if (currentLocale != null && !AppCompatDelegate.getApplicationLocales().isEmpty) {
            val language = when {
                "uk" in currentLocale.toString() -> languages[1]
                "ru" in currentLocale.toString() -> languages[2]
                else -> languages[0]
            }
            binding.languageMenuAutoComplete.setText(language, false)
        }
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

        /**
         * Setting different fonts for the language hint.
         * Changing it in xml don't work ¯\_(ツ)_/¯
         */
        val typeface = ResourcesCompat.getFont(requireContext(), R.font.montserrat_alternates_bold)
        binding.languageMenu.typeface = typeface

        /**
         * Language picker block
         */
        binding.languageMenuAutoComplete.apply {
            // Setting popup menu background
            setDropDownBackgroundDrawable(
                ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.language_dropdown_bg,
                    null
                )
            )
            setOnItemClickListener { _, _, position, _ ->
                // position 0 = english, 1 = ukrainian, 2 = russian
                val localeList = when (position) {
                    1 -> LocaleListCompat.forLanguageTags("uk")
                    2 -> LocaleListCompat.forLanguageTags("ru")
                    else -> LocaleListCompat.forLanguageTags("en")
                }
                AppCompatDelegate.setApplicationLocales(localeList)
                this.clearListSelection()
                this.dismissDropDown()
                this.clearFocus()
            }
        }
        /**
         * Language picker block
         */


        // Setting day/night preference
        setDayNightButtonChecked()
        binding.apply {
            dayNightButtonGroup.setOnCheckedChangeListener { _, _ ->
                when (true) {
                    buttonOne.isChecked -> setDayNightTheme(0)
                    buttonTwo.isChecked -> setDayNightTheme(1)
                    buttonTree.isChecked -> setDayNightTheme(2)
                    else -> Log.w(DEBUG_TAG, "Impossible choice in day/knight scheme")
                }
            }
        }
    }

    private fun setDayNightButtonChecked() {
        val dayNight = viewModel.readDayNightPreference()
        binding.apply {
            when (dayNight) {
                0 -> buttonOne.isChecked = true
                1 -> buttonTwo.isChecked = true
                2 -> buttonTree.isChecked = true
            }
        }
    }

    private fun setDayNightTheme(dayNight: Int) {
        viewModel.savaDayNightPreference(dayNight)
        DayNightTheme.setDayKnightTheme(dayNight, requireContext())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}