package com.sonofasleep.watertheplantapp.fragments

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.RecyclerView
import com.sonofasleep.watertheplantapp.PlantApplication
import com.sonofasleep.watertheplantapp.R
import com.sonofasleep.watertheplantapp.adapters.PlantsAdapter
import com.sonofasleep.watertheplantapp.database.SortType
import com.sonofasleep.watertheplantapp.databinding.FragmentPlantsListBinding
import com.sonofasleep.watertheplantapp.viewmodels.PlantViewModel
import com.sonofasleep.watertheplantapp.viewmodels.PlantViewModelFactory


class PlantsListFragment : Fragment() {
    private var _binding: FragmentPlantsListBinding? = null
    private val binding get() = _binding!!

    private var isOrderedAscIcon: Boolean = true

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
        _binding = FragmentPlantsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        // Inflating toolbarMenuLayout
        binding.plantListToolbar.inflateMenu(R.menu.menu_main)
        setIcon()

        // Setting navigation
        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        view.findViewById<Toolbar>(R.id.plant_list_toolbar)
            .setupWithNavController(navController, appBarConfiguration)

        // RecyclerView
        recyclerView = binding.recyclerView
        val adapter = PlantsAdapter()

        // Observing viewModel allPlants liveData
        // Showing helper addPlant image when list is empty
        viewModel.allPlants.observe(this.viewLifecycleOwner) {
            binding.addPlantReminderImage.isVisible = it.isNullOrEmpty()
            adapter.submitList(it)
        }
        recyclerView.adapter = adapter

        // Menu item clickListener
        binding.plantListToolbar.setOnMenuItemClickListener {
            when(it.itemId) {
                R.id.app_bar_sorting -> {
                    isOrderedAscIcon = !isOrderedAscIcon
                    setIcon()
                    sortPlantList()
                    true
                }
                else -> false
            }
        }

        binding.fab.setOnClickListener {
            findNavController().navigate(R.id.action_plantsListFragment_to_addPlantFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setIcon() {
        val sortItem = binding.plantListToolbar.menu.findItem(R.id.app_bar_sorting)
        val icon = when (isOrderedAscIcon) {
            true -> R.drawable.ic_sort_asc
            else -> R.drawable.ic_sort_desc
        }
        sortItem.setIcon(icon)
    }

    private fun sortPlantList() {
        if (isOrderedAscIcon) {
            viewModel.changeSortType(SortType.ASCENDING)
        } else {
            viewModel.changeSortType(SortType.DESCENDING)
        }
    }
}