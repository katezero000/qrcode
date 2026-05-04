package com.example.qrscanner

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.qrscanner.databinding.ActivityCameraScanBinding
import com.journeyapps.barcodescanner.CameraPreview
import com.journeyapps.barcodescanner.CaptureManager
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.camera.CameraConfigurationUtils

class CameraScanActivity : AppCompatActivity(), DecoratedBarcodeView.TorchListener {

    private lateinit var binding: ActivityCameraScanBinding
    private lateinit var captureManager: CaptureManager
    private var torchOn = false
    private var zoomRatio = 1.0
    private var maxZoomRatio = 1.0
    private var capabilitiesLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraScanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        binding.barcodeScanner.setTorchListener(this)

        captureManager = CaptureManager(this, binding.barcodeScanner)
        captureManager.initializeFromIntent(intent, savedInstanceState)
        captureManager.decode()

        binding.buttonTorch.setOnClickListener { toggleTorch() }
        binding.buttonZoomIn.setOnClickListener { updateZoom(ZOOM_STEP) }
        binding.buttonZoomOut.setOnClickListener { updateZoom(-ZOOM_STEP) }

        val hasFlash = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
        binding.buttonTorch.isVisible = hasFlash

        binding.barcodeScanner.barcodeView.addStateListener(object :
            CameraPreview.StateListener {
            override fun previewSized() {}

            override fun previewStarted() {
                if (!capabilitiesLoaded) {
                    capabilitiesLoaded = true
                    loadCameraCapabilities()
                }
            }

            override fun previewStopped() {}

            override fun cameraError(error: Exception) {}

            override fun cameraClosed() {}
        })

        updateTorchLabel()
        updateZoomLabel()
    }

    override fun onResume() {
        super.onResume()
        captureManager.onResume()
    }

    override fun onPause() {
        super.onPause()
        captureManager.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        captureManager.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        captureManager.onSaveInstanceState(outState)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        captureManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onTorchOn() {
        torchOn = true
        updateTorchLabel()
    }

    override fun onTorchOff() {
        torchOn = false
        updateTorchLabel()
    }

    private fun toggleTorch() {
        if (torchOn) {
            binding.barcodeScanner.setTorchOff()
        } else {
            binding.barcodeScanner.setTorchOn()
        }
    }

    private fun loadCameraCapabilities() {
        binding.barcodeScanner.changeCameraParameters { params ->
            val supportsZoom = params.isZoomSupported
            val ratios = params.zoomRatios
            val maxRatio = if (supportsZoom && ratios != null && ratios.isNotEmpty() && params.maxZoom in ratios.indices) {
                ratios[params.maxZoom] / ZOOM_RATIO_DIVISOR
            } else {
                1.0
            }
            runOnUiThread {
                maxZoomRatio = maxRatio.coerceAtLeast(1.0)
                binding.zoomControls.isVisible = supportsZoom
                zoomRatio = zoomRatio.coerceIn(1.0, maxZoomRatio)
                updateZoomLabel()
            }
            params
        }
    }

    private fun updateZoom(delta: Double) {
        val newRatio = (zoomRatio + delta).coerceIn(1.0, maxZoomRatio)
        if (newRatio != zoomRatio) {
            zoomRatio = newRatio
            applyZoom()
        }
    }

    private fun applyZoom() {
        binding.barcodeScanner.changeCameraParameters { params ->
            CameraConfigurationUtils.setZoom(params, zoomRatio)
            params
        }
        updateZoomLabel()
    }

    private fun updateZoomLabel() {
        binding.textZoom.text = getString(R.string.zoom_ratio_format, zoomRatio)
    }

    private fun updateTorchLabel() {
        binding.buttonTorch.text = if (torchOn) {
            getString(R.string.torch_off)
        } else {
            getString(R.string.torch_on)
        }
    }

    private companion object {
        const val ZOOM_STEP = 0.25
        const val ZOOM_RATIO_DIVISOR = 100.0
    }
}
