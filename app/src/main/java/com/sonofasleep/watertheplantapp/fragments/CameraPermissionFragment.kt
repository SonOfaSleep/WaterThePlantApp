package com.sonofasleep.watertheplantapp.fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.sonofasleep.watertheplantapp.PlantApplication
import com.sonofasleep.watertheplantapp.viewmodels.AddNewPlantViewModel
import com.sonofasleep.watertheplantapp.viewmodels.AddNewPlantViewModelFactory

private var PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.CAMERA)

/**
 * The sole purpose of this fragment is to request permissions and, once granted, display the
 * camera fragment to the user.
 */
class CameraPermissionFragment : Fragment() {

    private val viewModel: AddNewPlantViewModel by activityViewModels {
        AddNewPlantViewModelFactory(
            (activity?.application as PlantApplication).database.plantDao(),
            activity?.application as PlantApplication
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // add the storage access permission request for Android 9 and below.
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val permissionList = PERMISSIONS_REQUIRED.toMutableList()
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            PERMISSIONS_REQUIRED = permissionList.toTypedArray()
        }

        if (!hasPermissions(requireContext())) {
            // Request camera-related permissions
            activityResultLauncher.launch(PERMISSIONS_REQUIRED)
        } else {
            navigateToCamera()
        }
    }

    private fun navigateToCamera() {
        val action =
            CameraPermissionFragmentDirections.actionCameraPermissionFragmentToCameraFragment()
        findNavController().navigate(action)
    }

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())
        { permissions ->
            // Handle Permission granted/rejected
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in PERMISSIONS_REQUIRED && it.value == false)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                viewModel.apply {
                    setGoingToCamera(false) // Need for deleting image file if there is one
                    // Counting number of requested permissions
                    val numberOfTry: Int = viewModel.numberOfPermissionTry.value ?: 0
                    viewModel.saveNumberOfPermissionTry(numberOfTry + 1)
                }
                val action =
                    CameraPermissionFragmentDirections.actionCameraPermissionFragmentToAddPlantFragment()
                findNavController().navigate(action)
            } else {
                navigateToCamera()
            }
        }

    companion object {
        /** Convenience method used to check if all permissions required by this app are granted */
        fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}