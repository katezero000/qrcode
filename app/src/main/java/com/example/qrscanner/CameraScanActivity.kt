package com.example.qrscanner

import android.content.pm.PackageManager
import android.hardware.Camera
import android.os.Bundle
import androidx.core.view.isVisible
import com.example.qrscanner.databinding.ActivityCameraScanBinding
import com.journeyapps.barcodescanner.CameraPreview
import com.journeyapps.barcodescanner.CaptureActivity
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.camera.CameraConfigurationUtils
import java.util.concurrent.atomic.AtomicBoolean

class CameraScanActivity : CaptureActivity(), DecoratedBarcodeView.TorchListener {

    private lateinit var binding: ActivityCameraScanBinding
    private var torchOn = false
    private var zoomRatio = 1.0
    private var maxZoomRatio = 1.0
    private val capabilitiesLoaded = AtomicBoolean(false)

    override fun initializeContent(): DecoratedBarcodeView {
        binding = ActivityCameraScanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        return binding.barcodeScanner
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar?.hide()

        binding.barcodeScanner.setTorchListener(this)

        binding.buttonTorch.setOnClickListener { toggleTorch() }
        binding.buttonZoomIn.setOnClickListener { updateZoom(ZOOM_STEP) }
        binding.buttonZoomOut.setOnClickListener { updateZoom(-ZOOM_STEP) }

        val hasFlash = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
        binding.buttonTorch.isVisible = hasFlash

        binding.barcodeScanner.barcodeView.addStateListener(object :
            CameraPreview.StateListener {
            override fun previewSized() {}

            override fun previewStarted() {
                if (capabilitiesLoaded.compareAndSet(false, true)) {
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
            val maxRatio = extractMaxZoomRatio(params)
            runOnUiThread {
                maxZoomRatio = maxRatio.coerceAtLeast(1.0)
                binding.zoomControls.isVisible = supportsZoom && maxZoomRatio > 1.0
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

    private fun extractMaxZoomRatio(params: Camera.Parameters): Double {
        val ratios = params.zoomRatios
        val maxZoom = params.maxZoom
        return if (params.isZoomSupported &&
            ratios != null &&
            ratios.isNotEmpty() &&
            maxZoom >= 0 &&
            maxZoom in ratios.indices
        ) {
            ratios[maxZoom] / ZOOM_RATIO_DIVISOR
        } else {
            1.0
        }
    }

    private companion object {
        const val ZOOM_STEP = 0.25
        const val ZOOM_RATIO_DIVISOR = 100.0
    }
}
