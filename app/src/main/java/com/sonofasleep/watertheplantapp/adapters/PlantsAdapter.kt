package com.sonofasleep.watertheplantapp.adapters

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sonofasleep.watertheplantapp.R
import com.sonofasleep.watertheplantapp.databinding.PlantItemBinding
import com.sonofasleep.watertheplantapp.database.Plant
import com.sonofasleep.watertheplantapp.viewmodels.OnLongClickEnabled
import com.sonofasleep.watertheplantapp.viewmodels.PlantViewModel

class PlantsAdapter(
    private val viewModel: PlantViewModel,
    val onItemClicked: (Plant) -> Unit
) :
    ListAdapter<Plant, PlantsAdapter.PlantsViewHolder>(DiffCallback) {

    companion object DiffCallback : DiffUtil.ItemCallback<Plant>() {

        override fun areItemsTheSame(oldItem: Plant, newItem: Plant): Boolean {
            return oldItem.id == newItem.id
        }

        // Because plant.image is an instance of a class link to it everytime is different, hence
        // areContentsTheSame returns false every time even when nothing changed
        // link looks like this = image=com.sonofasleep.watertheplantapp.model.PlantIconItem@7d184bc
        // so we need to compare every property of plant, oldItem == newItem don't work!
        override fun areContentsTheSame(oldItem: Plant, newItem: Plant): Boolean {
            return (oldItem.id == newItem.id
                    && oldItem.notifications == newItem.notifications
                    && oldItem.image.iconNormal == newItem.image.iconNormal
                    && oldItem.image.iconDry == newItem.image.iconDry
                    && oldItem.timeToWater == newItem.timeToWater
                    && oldItem.name == newItem.name
                    && oldItem.description == newItem.description
                    && oldItem.reminderFrequency == newItem.reminderFrequency
                    && oldItem.timeHour == newItem.timeHour
                    && oldItem.timeMin == newItem.timeMin)
        }
    }

    class PlantsViewHolder(private var binding: PlantItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        val switch = binding.notifSwitch
        val wateringButton = binding.waterCheckButton
        val cardView = binding.itemCardView

        fun bind(plant: Plant, viewModel: PlantViewModel) {
            binding.apply {

                plantName.text = plant.name

                if (viewModel.longClickEnabled.value == OnLongClickEnabled.FALSE) {
                    itemCardView.isChecked = false

                    if (plant.timeToWater) {
                        wateringReminder.visibility = View.VISIBLE
                        wateringJoke.visibility = View.VISIBLE
                        waterCheckButton.visibility = View.VISIBLE

                        calenderIcon.visibility = View.INVISIBLE
                        clockIcon.visibility = View.INVISIBLE
                        notifSwitch.visibility = View.INVISIBLE
                        wateringType.visibility = View.INVISIBLE
                        wateringTime.visibility = View.INVISIBLE

                        // Set different image when needs watering
                        cardImage.clearColorFilter()
                        cardImage.setImageResource(plant.image.iconDry)
                    } else {
                        waterCheckButton.visibility = View.INVISIBLE
                        notifSwitch.visibility = View.VISIBLE
                        wateringType.visibility = View.VISIBLE
                        wateringTime.visibility = View.VISIBLE
                        wateringReminder.visibility = View.INVISIBLE
                        wateringJoke.visibility = View.INVISIBLE

                        wateringType.text = itemView.context
                            .getString(
                                R.string.reminder_frequency_int_days,
                                plant.reminderFrequency,
                                getPlural(plant.reminderFrequency)
                            )
                        wateringTime.text = itemView.context.getString(
                            R.string.reminder_frequency_hour_min,
                            viewModel.timeFormat(plant.timeHour, plant.timeMin)
                        )

                        if (plant.notifications) {
                            notifSwitch.isChecked = true
                            mainBackground.background.clearColorFilter()
                            cardImage.clearColorFilter()
                            cardImage.setImageResource(plant.image.iconNormal)
                        } else {
                            notifSwitch.isChecked = false
                            cardImage.setImageResource(plant.image.iconNormal)
                            val colorMatrix = ColorMatrix()
                            colorMatrix.setSaturation(0F)
                            val filter = ColorMatrixColorFilter(colorMatrix)
                            cardImage.colorFilter = filter
                            mainBackground.background.colorFilter = filter
                        }
                    }
                } else {
                    // Long click enabled
                    notifSwitch.visibility = View.INVISIBLE
                }
            }
        }

        private fun getPlural(days: Int): String {
            return itemView.context.resources
                .getQuantityString(R.plurals.plural_day, days, days)
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