package com.sonofasleep.watertheplantapp.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
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
    val onCameraClicked: () -> Unit
) : RecyclerView.Adapter<PlantIconAdapter.PlantImageViewHolder>() {

    // For storing previous position
    private var checkedPosition = -1

    class PlantImageViewHolder(
        private var binding: PlantImageCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PlantIconItem) {
            binding.cardImage.apply {
                scaleType = ImageView.ScaleType.CENTER
                setImageResource(item.iconNormal)
            }
        }

        fun bindUri(uri: Uri) {
            binding.cardImage.apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                setImageURI(uri)
            }
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
        val item: PlantIconItem = plantIconsList[position]

        if (position == 0 && viewModel.iconPhotoUri.value != null) {
            holder.bindUri(viewModel.iconPhotoUri.value!!)
        } else {
            holder.bind(item)
        }

        if (viewModel.chosenPlantIconPosition.value != null) {
            checkedPosition = viewModel.chosenPlantIconPosition.value!!
        }

        // Single selection logic
        if (checkedPosition == position) {
            if (checkedPosition == 0 && viewModel.iconPhotoUri.value == null) {
                // if moved to camera and don't made photo so it not be checked
                holder.setUnChecked()
            } else {
                holder.setChecked()
            }
        } else {
            holder.setUnChecked()
        }

        holder.itemView.setOnClickListener {

            setSingleSelection(position)

            // Registering choice
            viewModel.setChosenPlantPosition(position)

            // Starting camera
            if (position == 0) {
                // We need icon only for drawn plants
                viewModel.setPlantIcon(null)
                onCameraClicked()
            } else {
                viewModel.setPlantIcon(item)
            }
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
    }
}