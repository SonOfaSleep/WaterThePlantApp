package com.sonofasleep.watertheplantapp.fragments

import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.sonofasleep.watertheplantapp.PlantApplication
import com.sonofasleep.watertheplantapp.R
import com.sonofasleep.watertheplantapp.const.*
import com.sonofasleep.watertheplantapp.databinding.FragmentCameraBinding
import com.sonofasleep.watertheplantapp.viewmodels.AddNewPlantViewModel
import com.sonofasleep.watertheplantapp.viewmodels.AddNewPlantViewModelFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class CameraFragment : Fragment() {
    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    private val viewModel: AddNewPlantViewModel by activityViewModels {
        AddNewPlantViewModelFactory(
            (activity?.application as PlantApplication).database.plantDao(),
            activity?.application as PlantApplication
        )
    }
    private val navArgs: AddPlantFragmentArgs by navArgs()

    // This view is for changing color of toolbar
    lateinit var myView: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        myView = view
        // Hiding buttons
        val searchButton = binding.toolbar.plantListToolbar.menu.findItem(R.id.app_bar_search)
        val sortButton = binding.toolbar.plantListToolbar.menu.findItem(R.id.app_bar_sorting)
        val settingsButton = binding.toolbar.plantListToolbar.menu.findItem(R.id.app_bar_settings)
        searchButton.isVisible = false
        sortButton.isVisible = false
        settingsButton.isVisible = false

        // Setting navigation
        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(navController.graph)

        view.findViewById<Toolbar>(R.id.plant_list_toolbar)
            .setupWithNavController(navController, appBarConfiguration)

        // Setting status and navigation bars to black (if theme is not dark already)
        setStatusAndNavigationBarsDark(myView)

        // Animations for shutter button
        val animationScaleDown = AnimationUtils.loadAnimation(requireContext(), R.anim.scale_down)
        val animationScaleUp = AnimationUtils.loadAnimation(requireContext(), R.anim.scale_up)

        // Starting camera
        if (!CameraPermissionFragment.hasPermissions(requireContext())) {
            // Make sure that all permissions are still present, since the
            // user could have removed them while the app was in paused state.
            // Initially camera permission check was onResume, but it caused null pointer exception in binding
            findNavController().navigate(
                CameraFragmentDirections.actionCameraFragmentToAddPlantFragment()
            )
        } else {
            startCamera()
        }

        // Camera shutter button
        binding.makePhotoButton.setOnClickListener {
            it.isClickable = false // To not press second time and create crash
            it.startAnimation(animationScaleDown)
            it.startAnimation(animationScaleUp)
            simulateFlash()
            takePhoto()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        viewModel.setGoingToCamera(false)

        resetStatusAndNavigationBarsColor(myView)
        _binding = null
        imageCapture = null

        cameraExecutor.shutdown()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Setting height of viewFinder to width (I wont it to be square)
            val widthInPx = binding.viewFinder.width
            binding.viewFinder.layoutParams.height = widthInPx

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture
                .Builder()
                .build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            val viewPort = ViewPort.Builder(Rational(300, 300), Surface.ROTATION_0).build()
            val useCaseGroup = UseCaseGroup.Builder()
                .addUseCase(preview)
                .addUseCase(imageCapture!!)
                .setViewPort(viewPort)
                .build()

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, useCaseGroup
                )

            } catch (exc: Exception) {
                Log.e(DEBUG_TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        // imageCapture is initialized in startCamera()
        val imageCapture = imageCapture ?: return

        // Folder for images
        val outputDirectory = File(context?.applicationContext?.filesDir, CAMERA_IMAGE_FOLDER)
        if (!outputDirectory.exists()) {
            outputDirectory.mkdir()
        }

        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                FILENAME_FORMAT,
                Locale.getDefault()
            ).format(System.currentTimeMillis()) + ".jpg"
        )

        // Output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    viewModel.setImageUri(outputFileResults.savedUri)
                    navigateToAddPlant()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(DEBUG_TAG, "Error when saving image: ${exception.message}")

                    val errorMessage = resources.getString(R.string.image_capture_error)
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()

                    navigateToAddPlant()
                }
            }
        )
    }

    private fun setStatusAndNavigationBarsDark(view: View) {
        activity?.window?.apply {
            statusBarColor = requireContext().getColor(R.color.dark_theme_background)
            navigationBarColor = Color.BLACK
        }
        WindowInsetsControllerCompat(requireActivity().window, view).isAppearanceLightStatusBars =
            false
    }

    private fun resetStatusAndNavigationBarsColor(view: View) {
        when (requireContext().resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
            Configuration.UI_MODE_NIGHT_YES -> {
                return
            }

            Configuration.UI_MODE_NIGHT_NO -> {
                activity?.window?.apply {
                    statusBarColor = requireContext().getColor(R.color.white)
                    WindowInsetsControllerCompat(
                        requireActivity().window,
                        view
                    ).isAppearanceLightStatusBars = true
                    navigationBarColor = Color.WHITE
                }
            }

            Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                Log.e(DEBUG_TAG, "Night not specified")
            }
        }
    }


    private fun simulateFlash() {
        // Display flash animation to indicate that photo was captured
        binding.root.postDelayed({
            binding.root.foreground = ColorDrawable(Color.BLACK)
            binding.root.postDelayed(
                { binding.root.foreground = null }, ANIMATION_FAST_MILLIS
            )
        }, ANIMATION_SLOW_MILLIS)
    }

    private fun navigateToAddPlant() {
        val action = CameraFragmentDirections.actionCameraFragmentToAddPlantFragment()
        findNavController().navigate(action)
    }
}