package com.rst.recipeappopsc6312

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class CameraScannerActivity : AppCompatActivity() {

    private lateinit var cameraPreview: PreviewView
    private var imageCapture: ImageCapture? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_scanner)

        cameraPreview = findViewById(R.id.cameraPreview)
        findViewById<Button>(R.id.buttonCapture).setOnClickListener { takePhoto() }

        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(cameraPreview.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (e: Exception) {
                Toast.makeText(this, R.string.toast_failed, Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        imageCapture.takePicture(ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                val mediaImage = imageProxy.image
                if (mediaImage != null) {
                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    recognizeObjects(image) // Call the new object recognition function
                }
                imageProxy.close()
            }
            override fun onError(exception: ImageCaptureException) {
                Toast.makeText(baseContext, R.string.toast_photo_error, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun recognizeObjects(image: InputImage) {
        // Configure the object detector
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableClassification()  // We need this to get the name of the object
            .build()
        val objectDetector = ObjectDetection.getClient(options)

        objectDetector.process(image)
            .addOnSuccessListener { detectedObjects ->
                if (detectedObjects.isEmpty()) {
                    Toast.makeText(this, R.string.toast_object_error, Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Get the name of the most confident result
                val topResult = detectedObjects.maxByOrNull { obj ->
                    obj.labels.maxByOrNull { it.confidence }?.confidence ?: 0f
                }

                val topLabel = topResult?.labels?.firstOrNull()?.text

                if (topLabel != null) {
                    // Send the recognized object name back to the ScanFragment
                    val resultIntent = Intent()
                    resultIntent.putStringArrayListExtra("scanned_ingredients", arrayListOf(topLabel))
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                } else {
                    Toast.makeText(this, R.string.toast_classify_error, Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, R.string.toast_recognition_error, Toast.LENGTH_SHORT).show()
            }
    }
}