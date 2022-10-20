package com.sonofasleep.watertheplantapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sonofasleep.watertheplantapp.databinding.PlantImageCardBinding
import com.sonofasleep.watertheplantapp.model.PlantIconItem
import com.sonofasleep.watertheplantapp.viewmodels.PlantViewModel

class PlantIconAdapter(
    private val viewModel: PlantViewModel,
    private val plantIconsList: List<PlantIconItem>
)
    : RecyclerView.Adapter<PlantIconAdapter.PlantImageViewHolder>() {

    private var checkedPosition = -1

    class PlantImageViewHolder(
        private var binding: PlantImageCardBinding
    ): RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PlantIconItem) {
            binding.cardImage.setImageResource(item.image)
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

        if (checkedPosition == position) {
            holder.setChecked()
        } else {
            holder.setUnChecked()
        }

        holder.itemView.setOnClickListener {
            setSingleSelection(position)
            viewModel.setPlantIcon(item)
        }
    }

    override fun getItemCount(): Int {
        return plantIconsList.size
    }

    private fun setSingleSelection(position: Int) {
        if (position == RecyclerView.NO_POSITION) return

        notifyItemChanged(checkedPosition)
        checkedPosition = position
        notifyItemChanged(checkedPosition)
    }
}