package com.sonofasleep.watertheplantapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sonofasleep.watertheplantapp.databinding.PlantImageCardBinding
import com.sonofasleep.watertheplantapp.model.PlantIconItem
import com.sonofasleep.watertheplantapp.viewmodels.AddNewPlantViewModel

/**
 * This adapters handles onClick listener, allowing only one cardView to be checked at a time.
 * Also it's holds reference to viewModel for storing selected image.
 */
class PlantIconAdapter(
    private val viewModel: AddNewPlantViewModel,
    private val plantIconsList: List<PlantIconItem>,
    private var selectedIcon: PlantIconItem? = null,
    val onCameraClicked: () -> Unit
) : RecyclerView.Adapter<PlantIconAdapter.PlantImageViewHolder>() {

    // For storing previous position
    private var checkedPosition = -1

    class PlantImageViewHolder(
        private var binding: PlantImageCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PlantIconItem) {
            binding.cardImage.setImageResource(item.iconNormal)
        }

        fun setChecked() {
            binding.cardView.isChecked = true
        }

        fun setUnChecked() {
            binding.cardView.isChecked = false
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantImageViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return PlantImageViewHolder(
            PlantImageCardBinding.inflate(layoutInflater, parent, false)
        )
    }

    override fun onBindViewHolder(holder: PlantImageViewHolder, position: Int) {
        val item = plantIconsList[position]
        holder.bind(item)

        // Single selection logic
        if (checkedPosition == position && position != 0) {
            holder.setChecked()
        } else {
            holder.setUnChecked()
        }

        // For checking previously selected icon in recyclerView
        // and updating checkedPosition for correct work of single selection logic above
        if (selectedIcon?.iconNormal == item.iconNormal) {
            holder.setChecked()
            checkedPosition =
                plantIconsList.indexOfFirst { it.iconNormal == selectedIcon?.iconNormal }
            selectedIcon = null
        }

        holder.itemView.setOnClickListener {
            setSingleSelection(position)

            // Changing viewModels icon
            viewModel.setPlantIcon(item)
        }
    }

    override fun getItemCount(): Int {
        return plantIconsList.size
    }

    // This function enables to set single checked card at a time.
    // notifyItemChanged() function change first previous card to UnChecked than current to Checked
    private fun setSingleSelection(position: Int) {
        if (position == RecyclerView.NO_POSITION) return

        notifyItemChanged(checkedPosition)
        checkedPosition = position
        notifyItemChanged(checkedPosition)

        if (position == 0) {
            onCameraClicked()
        }
    }
}