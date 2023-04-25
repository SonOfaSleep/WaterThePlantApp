package com.sonofasleep.watertheplantapp.adapters

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sonofasleep.watertheplantapp.R
import com.sonofasleep.watertheplantapp.const.DEBUG_TAG
import com.sonofasleep.watertheplantapp.databinding.PlantItemBinding
import com.sonofasleep.watertheplantapp.database.Plant
import com.sonofasleep.watertheplantapp.viewmodels.OnLongClickEnabled
import com.sonofasleep.watertheplantapp.viewmodels.PlantViewModel

class PlantsAdapter(
    private val viewModel: PlantViewModel, val onItemClicked: (Plant) -> Unit
) : ListAdapter<Plant, PlantsAdapter.PlantsViewHolder>(DiffCallback) {

    companion object DiffCallback : DiffUtil.ItemCallback<Plant>() {

        override fun areItemsTheSame(oldItem: Plant, newItem: Plant): Boolean {
            return oldItem.id == newItem.id
        }

        // Because plant.image is an instance of a class link to it everytime is different, hence
        // areContentsTheSame returns false every time even when nothing changed
        // link looks like this = image=com.sonofasleep.watertheplantapp.model.PlantIconItem@7d184bc
        // so we need to compare every property of plant, oldItem == newItem don't work!
        override fun areContentsTheSame(oldItem: Plant, newItem: Plant): Boolean {
            return (oldItem.id == newItem.id &&
                    oldItem.notifications == newItem.notifications &&
                    oldItem.image?.iconNormal == newItem.image?.iconNormal &&
                    oldItem.image?.iconDry == newItem.image?.iconDry &&
                    oldItem.photoImageUri == newItem.photoImageUri &&
                    oldItem.timeToWater == newItem.timeToWater &&
                    oldItem.name == newItem.name &&
                    oldItem.description == newItem.description &&
                    oldItem.reminderFrequency == newItem.reminderFrequency &&
                    oldItem.timeHour == newItem.timeHour &&
                    oldItem.timeMin == newItem.timeMin
                    )
        }
    }

    class PlantsViewHolder(private var binding: PlantItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        val switch = binding.notifSwitch
        val wateringButton = binding.waterCheckButton
        val cardView = binding.itemCardView

        fun bind(plant: Plant, viewModel: PlantViewModel) {

            val longClickEnabled = viewModel.longClickEnabled.value == OnLongClickEnabled.TRUE

            binding.apply {

                // PLANT NAME
                plantName.text = plant.name

                /**
                 * ↓ PLANT CARD IMAGE BLOCK ↓
                 */
                if (plant.photoImageUri != null) {
                    cardImage.apply {
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        setImageURI(Uri.parse(plant.photoImageUri))
                    }
                } else {
                    // if photoImageUri == null than plant.image != null
                    val image = if (!plant.timeToWater) {
                        plant.image!!.iconNormal
                    } else {
                        plant.image!!.iconDry
                    }
                    cardImage.apply {
                        scaleType = ImageView.ScaleType.CENTER
                        setImageResource(image)
                    }
                }
                // Coloring background to grey
                if (plant.notifications) {
                    mainBackground.background.clearColorFilter()
                    cardImage.clearColorFilter()
                } else {
                    mainBackground.background.colorFilter = getColorFilter()
                    cardImage.colorFilter = getColorFilter()
                }
                /**
                 * ↑ PLANT CARD IMAGE BLOCK ↑
                 */


                // ↓ WATERING TYPE, TIME AND REMINDER BLOCK ↓
                val type = itemView.context.getString(
                    R.string.reminder_frequency_int_days,
                    plant.reminderFrequency,
                    getPlural(plant.reminderFrequency)
                )
                wateringType.text = type

                val time = itemView.context.getString(
                    R.string.reminder_frequency_hour_min,
                    viewModel.timeFormat(plant.timeHour, plant.timeMin)
                )
                wateringTime.text = time

                val visibility = if (plant.timeToWater) View.INVISIBLE else View.VISIBLE
                val reminderVisibility = if (plant.timeToWater) View.VISIBLE else View.INVISIBLE

                calenderIcon.visibility = visibility
                clockIcon.visibility = visibility
                wateringType.visibility = visibility
                wateringTime.visibility = visibility

                wateringReminder.visibility = reminderVisibility
                // ↑ WATERING TYPE AND TIME BLOCK ↑


                // ↓ SWITCH AND WATERING BUTTON BLOCK ↓
                notifSwitch.isChecked = plant.notifications

                val switchVisibility =
                    if (longClickEnabled || plant.timeToWater) View.INVISIBLE else View.VISIBLE
                notifSwitch.visibility = switchVisibility

                val waterCheckButtonVisibility = if (plant.timeToWater && !longClickEnabled) {
                    View.VISIBLE
                } else {
                    View.INVISIBLE
                }
                waterCheckButton.visibility = waterCheckButtonVisibility
                // ↑ SWITCH AND WATERING BUTTON BLOCK ↑


                // CARD CHECKED BEHAVIOUR DURING LONG CLICK STATE
                val chosenList = viewModel.longClickChosenPlants.value ?: listOf()
                var bool = false
                for (i in chosenList) {
                    if (i.id == plant.id) bool = true
                }
                cardView.isChecked = bool
            }
        }

        private fun getPlural(days: Int): String {
            return itemView.context.resources.getQuantityString(R.plurals.plural_day, days, days)
        }

        private fun getColorFilter(): ColorMatrixColorFilter {
            val colorMatrix = ColorMatrix()
            colorMatrix.setSaturation(0F)
            return ColorMatrixColorFilter(colorMatrix)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantsViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return PlantsViewHolder(
            PlantItemBinding.inflate(layoutInflater, parent, false)
        )
    }

    override fun onBindViewHolder(holder: PlantsViewHolder, position: Int) {
        val currentPlant = getItem(position)

        holder.cardView.setOnClickListener {

            if (viewModel.longClickEnabled.value == OnLongClickEnabled.FALSE) {
                // Regular click
                onItemClicked(currentPlant)
            } else {
                // Long click enabled behaviour
                changeChecked(holder, currentPlant)
            }
        }

        holder.cardView.setOnLongClickListener {
            if (viewModel.longClickEnabled.value == OnLongClickEnabled.FALSE) {
                viewModel.changeLongClickStateToTrue()
            }
            changeChecked(holder, currentPlant)
            true
        }

        holder.switch.setOnClickListener {
            holder.itemView.isClickable = false
            it.isClickable = false
            viewModel.switchWork(currentPlant)
        }

        holder.wateringButton.setOnClickListener {
            holder.itemView.isClickable = false
            it.isClickable = false
            viewModel.wateringDone(currentPlant)
        }

        holder.bind(currentPlant, viewModel)
    }

    private fun changeChecked(holder: PlantsViewHolder, currentPlant: Plant) {
        if (holder.cardView.isChecked) {
            holder.cardView.isChecked = false
            viewModel.removePlantFromChosenList(currentPlant)
        } else {
            holder.cardView.isChecked = true
            viewModel.addPlantToChosenList(currentPlant)
        }
    }
}