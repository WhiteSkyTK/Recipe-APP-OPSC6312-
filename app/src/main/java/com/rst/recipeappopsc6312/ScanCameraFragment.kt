package com.rst.recipeappopsc6312

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanCameraFragment : Fragment() {
    private val TAG = "ScanCameraFragment"
    private lateinit var cameraPreviewView: PreviewView
    private lateinit var lottieAnimationView: LottieAnimationView
    private lateinit var cameraExecutor: ExecutorService
    private var imageAnalyzer: ImageAnalysis? = null

    private val viewModel: ScanViewModel by activityViewModels {
        val db = AppDatabase.getDatabase(requireContext())
        val repo = ShoppingRepository(db.shoppingDao(), db.recipeDao(), db.scanHistoryDao(), com.google.firebase.firestore.FirebaseFirestore.getInstance(), com.google.firebase.storage.FirebaseStorage.getInstance())
        ViewModelFactory(repo)
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(context, getString(R.string.scan_camera_permission_needed), Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_scan_camera, container, false)
        cameraPreviewView = view.findViewById(R.id.cameraPreviewView)
        lottieAnimationView = view.findViewById(R.id.lottieAnimationView)
        cameraExecutor = Executors.newSingleThreadExecutor()

        view.findViewById<FloatingActionButton>(R.id.buttonScan).setOnClickListener {
            analyzeCurrentImage()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(cameraPreviewView.surfaceProvider)
            }
            imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview, imageAnalyzer)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun analyzeCurrentImage() {
        lottieAnimationView.visibility = View.VISIBLE

        imageAnalyzer?.setAnalyzer(cameraExecutor) { imageProxy ->
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                // Call the new ViewModel function for object detection
                viewModel.processImageFromCamera(
                    image = image,
                    onSuccess = { labels ->
                        activity?.runOnUiThread {
                            if (labels.isNotEmpty()) {
                                labels.forEach { viewModel.addIngredient(it) }
                                lottieAnimationView.visibility = View.GONE
                                Toast.makeText(context, getString(R.string.scan_camera_ingredients_found, labels.joinToString()), Toast.LENGTH_SHORT).show()
                            } else {
                                lottieAnimationView.visibility = View.GONE
                                Toast.makeText(context, getString(R.string.scan_camera_no_ingredients_identified), Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onFailure = { e ->
                        activity?.runOnUiThread {
                            lottieAnimationView.visibility = View.GONE
                            Toast.makeText(context, getString(R.string.scan_camera_detection_failed), Toast.LENGTH_SHORT).show()
                        }
                        Log.e(TAG, "Object detection failed", e)
                    },
                    onComplete = {
                        imageProxy.close()
                        // Stop analyzing after one frame to prevent continuous scanning
                        imageAnalyzer?.clearAnalyzer()
                    }
                )
            }
        }
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
