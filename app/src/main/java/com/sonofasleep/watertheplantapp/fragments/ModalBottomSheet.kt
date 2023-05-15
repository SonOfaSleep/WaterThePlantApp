package com.sonofasleep.watertheplantapp.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sonofasleep.watertheplantapp.databinding.AddPlantBottomSheetBinding

class ModalBottomSheet : BottomSheetDialogFragment() {

    private var _binding: AddPlantBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = AddPlantBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.cancelButton.setOnClickListener {
            this.dismiss()
        }

        binding.settingsButton.setOnClickListener {
            this.dismiss()
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", requireActivity().packageName, null)
            intent.data = uri
            startActivity(intent)
        }
    }

    companion object {
        const val TAG = "AddPlantBottomSheet"
    }
}