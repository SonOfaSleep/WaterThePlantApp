package com.sonofasleep.watertheplantapp.adapters

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sonofasleep.watertheplantapp.R
import com.sonofasleep.watertheplantapp.databinding.PlantItemBinding
import com.sonofasleep.watertheplantapp.database.Plant
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

        override fun areContentsTheSame(oldItem: Plant, newItem: Plant): Boolean {
            return oldItem == newItem
        }
    }

    class PlantsViewHolder(private var binding: PlantItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        val switch = binding.notifSwitch

        fun bind(plant: Plant) {
            binding.apply {
                plantName.text = plant.name
                wateringType.text = itemView.context
                    .getString(
                        R.string.reminder_frequency_int_days,
                        plant.reminderFrequency,
                        getPlural(plant.reminderFrequency)
                    )
                if (plant.notifications) {
                    notifSwitch.isChecked = true
                    cardImage.setImageResource(plant.image)
                } else {
                    notifSwitch.isChecked = false
                    cardImage.setImageResource(plant.image)
                    val colorMatrix = ColorMatrix()
                    colorMatrix.setSaturation(0F)
                    val filter = ColorMatrixColorFilter(colorMatrix)
                    cardImage.colorFilter = filter
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

        holder.itemView.setOnClickListener { onItemClicked(currentPlant) }

        holder.switch.setOnClickListener {
            holder.itemView.isClickable = false
            it.isClickable = false
            viewModel.switchWork(currentPlant)
        }

        holder.bind(currentPlant)
    }
}