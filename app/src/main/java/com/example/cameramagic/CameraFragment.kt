package com.example.cameramagic

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Camera
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.ImageReader
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.util.concurrent.Executors

const val CAMERA_PERMISSION_REQUEST_CODE = 100

class CameraFragment: Fragment() {
    private lateinit var previewView: PreviewView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_camera, container, false)
        previewView = view.findViewById(R.id.preview_view)
        Log.d("MAGIC/CAMERA", "View created, checking/requesting permissions")

        if (!hasCameraPermissions()) {
            requestCameraPermissions()
        } else {
            startCamera()
        }

        return view
    }

    private fun startCamera() {
        Log.d("MAGIC/Camera", "Permissions OK, starting camera")
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Define where the camera preview should go
            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            // Setup the Image Analyzer
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also{
                    it.setAnalyzer(Executors.newSingleThreadExecutor()) { image ->
                        performAnalysis(image)
                        image.close()
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    // imageAnalyzer
                )
                Log.d("MAGIC/CAMERA", "Camera preview bound to $cameraSelector")
            } catch (e: CameraAccessException) {
                Log.e("CAMERA/MAGIC", "Error opening/binding the camera", e)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun performAnalysis(image: ImageProxy) {
        Log.i("MAGIC/CAMERA", "Starting image analysis")
        val detector = ObjectDetector.createFromFileAndOptions(
            requireContext(),
            // TODO - Use a correct (mobile friendly) model
            "MobileNet_V2.tflite",
            ObjectDetector.ObjectDetectorOptions.builder().setMaxResults(5).build()
        )

        image?.let {
            val tensorImage = TensorImage.fromBitmap(previewView.bitmap)
            val results = detector.detect(tensorImage)
            for (result in results) {
                Log.i("MAGIC/CAMERA/DETECT",
                    "Detected: ${result.categories.firstOrNull()?.label}")
            }
        }
    }

    private fun hasCameraPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(), android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    @Deprecated("uses `requestPermission` which itself is deprecated. Replace with the correct solution.")
    private fun requestCameraPermissions() {
        requestPermissions(
            arrayOf(android.Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            }
        } else {
            Toast.makeText(context, "Camera Permission Required", Toast.LENGTH_LONG)
        }
    }
}