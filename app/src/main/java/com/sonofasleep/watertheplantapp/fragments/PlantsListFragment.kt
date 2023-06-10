package com.sonofasleep.watertheplantapp.fragments

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.RecyclerView
import com.sonofasleep.watertheplantapp.PlantApplication
import com.sonofasleep.watertheplantapp.R
import com.sonofasleep.watertheplantapp.adapters.PlantsAdapter
import com.sonofasleep.watertheplantapp.databinding.FragmentPlantsListBinding
import com.sonofasleep.watertheplantapp.viewmodels.OnLongClickEnabled
import com.sonofasleep.watertheplantapp.viewmodels.PlantViewModel
import com.sonofasleep.watertheplantapp.viewmodels.PlantViewModelFactory

class PlantsListFragment : Fragment(), SearchView.OnQueryTextListener {

    private var _binding: FragmentPlantsListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PlantViewModel by activityViewModels {
        PlantViewModelFactory(
            (activity?.application as PlantApplication).database.plantDao(),
            activity?.application as PlantApplication
        )
    }

    // For choosing SortType and sort icon for menu
    private var isOrderedAscIcon: Boolean = true

    private var actionMode: ActionMode? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PlantsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlantsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        // Setting up searchView and onQueryListener
        val search = binding.toolbar.plantListToolbar.menu.findItem(R.id.app_bar_search)
        val searchView = search.actionView as SearchView
        searchView.isSubmitButtonEnabled = true
        searchView.setOnQueryTextListener(this)

        // Setting navigation
        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        view.findViewById<Toolbar>(R.id.plant_list_toolbar)
            .setupWithNavController(navController, appBarConfiguration)

        // RecyclerView binding and click action on item
        recyclerView = binding.recyclerView
        adapter = PlantsAdapter(viewModel) { plant ->
            if (viewModel.longClickEnabled.value == OnLongClickEnabled.FALSE) {
                val action = PlantsListFragmentDirections
                    .actionPlantsListFragmentToDetailPlantFragment(plant.id)
                findNavController().navigate(action)
            }
        }

        // RecyclerView adapter
        recyclerView.adapter = adapter

        // Observing viewModel allPlants liveData
        // Showing helper addPlant image when list is empty
        addAllPlantsObserver()

        // Observing data store preference
        viewModel.sortTypeIsASC.observe(this.viewLifecycleOwner) {
            isOrderedAscIcon = it
        }

        /**
         * Showing welcome image, only if no plants where added before.
         */
        if (viewModel.appsFirstLaunch()) {
            // Hiding buttons
            val searchButton = binding.toolbar.plantListToolbar.menu.findItem(R.id.app_bar_search)
            val sortButton = binding.toolbar.plantListToolbar.menu.findItem(R.id.app_bar_sorting)
            val settingsButton =
                binding.toolbar.plantListToolbar.menu.findItem(R.id.app_bar_settings)
            searchButton.isVisible = false
            sortButton.isVisible = false
            settingsButton.isVisible = false
            binding.apply {
                addPlantWelcomeImage.visibility = View.VISIBLE
                addPlantWelcomeText1.visibility = View.VISIBLE
                addPlantWelcomeText2.visibility = View.VISIBLE
            }
        }

        /**
         *  Handling scroll behavior
         */
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                // Showing elevation of toolbar when can scroll
                if (recyclerView.canScrollVertically(-1)) {
                    binding.toolbar.materialToolbar.elevation = 50f
                } else {
                    binding.toolbar.materialToolbar.elevation = 0f
                }

                // Hiding addButton when scrolling down
                if (dy > 0 && viewModel.longClickEnabled.value == OnLongClickEnabled.FALSE) {
                    binding.fab.hide()
                } else {
                    binding.fab.show()
                }
            }
        })

        /**
         * Contextual action bar behaviour
         */
        val callback = object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                mode?.menuInflater?.inflate(R.menu.contextual_action_bar, menu)
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode?, p1: Menu?): Boolean {
                return false
            }

            override fun onActionItemClicked(mode: ActionMode?, p1: MenuItem?): Boolean {
                return when (p1?.itemId) {
                    R.id.delete_button -> {
                        // Handle delete icon press
                        deleteAlertDialog(mode, viewModel.longClickChosenPlants.value?.size ?: 1)
                        true
                    }

                    else -> false
                }
            }

            override fun onDestroyActionMode(p0: ActionMode?) {
                // Maybe change state here?
                viewModel.apply {
                    changeLongClickStateToFalse()
                    emptyChosenPlantsList()
                }
            }
        }

        // Observing onLongClickState
        viewModel.longClickEnabled.observe(this.viewLifecycleOwner) {
            if (it == OnLongClickEnabled.TRUE) {
                actionMode = activity?.startActionMode(callback)
                binding.fab.hide()
                adapter.notifyDataSetChanged()
            } else {
                binding.fab.show()
                adapter.notifyDataSetChanged()
            }
        }

        // Observing list of chosen plants when long click mode enabled
        viewModel.longClickChosenPlants.observe(this.viewLifecycleOwner) {
            if (actionMode != null) {
                actionMode!!.title = getString(R.string.long_click_chosen_text, it.size)
            }
        }

        // Menu item clickListener
        binding.toolbar.plantListToolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.app_bar_sorting -> {
                    isOrderedAscIcon = !isOrderedAscIcon

                    viewModel.saveSortTypeToDataStore(isOrderedAscIcon)

                    true
                }

                R.id.app_bar_settings -> {
                    findNavController().navigate(
                        PlantsListFragmentDirections.actionPlantsListFragmentToSettingsFragment()
                    )
                    true
                }

                else -> false
            }
        }

        binding.fab.setOnClickListener {
            findNavController().navigate(R.id.action_plantsListFragment_to_addPlantFragment)
        }
    }

    override fun onStop() {
        // I decided that i don't wont to store actionMode state if app is closed
        if (actionMode != null) {
            viewModel.changeLongClickStateToFalse()
            actionMode!!.finish()
        }
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        actionMode = null
    }

    private fun deleteAlertDialog(mode: ActionMode?, plantQuantity: Int) {
        val alertDialog: AlertDialog? = activity?.let {

            val builder = AlertDialog.Builder(it)
            val plantPluralForTitle = context?.resources?.getQuantityString(
                R.plurals.alert_dialog_plant_plural,
                plantQuantity
            )
            builder.apply {

                setTitle(getString(R.string.alert_dialog_title, plantQuantity, plantPluralForTitle))
                setMessage(getString(R.string.alert_dialog_text, plantPluralForTitle))

                setNegativeButton(R.string.alert_dialog_button_cancel) { dialogInterface, _ ->
                    dialogInterface.cancel()
                }
                setPositiveButton(R.string.alert_dialog_button_ok) { _, _ ->
                    viewModel.apply {
                        deleteChosenPlantsWhenLongClickModeEnabled()
                        mode?.finish()
                    }
                }
            }
            builder.create()
            builder.show()
        }
        // Setting font only to message in alert dialog. Main fontStyle set in themes (AlertDialogTheme)
        val textView = alertDialog?.findViewById<TextView>(android.R.id.message)
        val typeFace =
            ResourcesCompat.getFont(requireContext(), R.font.montserrat_alternates_regular)
        textView?.typeface = typeFace
    }

    private fun setIcon() {
        val sortItem = binding.toolbar.plantListToolbar.menu.findItem(R.id.app_bar_sorting)
        val icon = when (isOrderedAscIcon) {
            true -> R.drawable.ic_sort_asc
            else -> R.drawable.ic_sort_desc
        }
        sortItem.setIcon(icon)
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return true
    }

    override fun onQueryTextChange(query: String?): Boolean {
        // The main logic is to switch observing between allPlants and searchResults
        if (!query.isNullOrBlank()) {
            removeAllPlantsObserver()
            addSearchObserver()
            viewModel.changeSearchQuery(query)
        } else {
            addAllPlantsObserver()
            removeSearchObserver()
        }
        return true
    }

    /**
     * This functions are needed for not messing with observable list. If we will observe
     * two different lists and submitting to one adapter it will cause bug
     * (If you switch on/off item when searching, list will be updated and redrawn;
     * And when not searching, it will still be observing last search request at random)
     *
     * P.S. This approach seems to work
     */
    private fun addAllPlantsObserver() {
        if (!viewModel.allPlants.hasObservers()) {
            viewModel.allPlants.observe(this.viewLifecycleOwner) {
                // Setting "No plants" image and text visibility
                val visibility: Int =
                    if (it.isNullOrEmpty() && !viewModel.appsFirstLaunch()) View.VISIBLE else View.INVISIBLE
                binding.apply {
                    noPlantsImage.visibility = visibility
                    noPlantsText.visibility = visibility
                }
                adapter.submitList(it)
                setIcon()
            }
        }
    }

    private fun removeAllPlantsObserver() {
        viewModel.allPlants.removeObservers(this.viewLifecycleOwner)
    }

    private fun addSearchObserver() {
        binding.fab.hide()
        if (!viewModel.searchResults.hasObservers()) {
            viewModel.searchResults.observe(this.viewLifecycleOwner) {
                adapter.submitList(it)
            }
        }
    }

    private fun removeSearchObserver() {
        binding.fab.show()
        viewModel.searchResults.removeObservers(this.viewLifecycleOwner)
    }
}