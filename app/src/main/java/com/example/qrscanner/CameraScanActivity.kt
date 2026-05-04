package com.example.qrscanner

import android.content.pm.PackageManager
import android.hardware.Camera
import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.qrscanner.databinding.ActivityCameraScanBinding
import com.journeyapps.barcodescanner.CameraPreview
import com.journeyapps.barcodescanner.CaptureManager
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.camera.CameraConfigurationUtils
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

class CameraScanActivity : AppCompatActivity(), DecoratedBarcodeView.TorchListener {

    private lateinit var binding: ActivityCameraScanBinding
    private lateinit var captureManager: CaptureManager
    private var torchOn = false
    private var zoomRatio = 1.0
    private var maxZoomRatio = 1.0
    private val capabilitiesLoaded = AtomicBoolean(false)

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
        binding.seekZoom.max = ZOOM_SEEKBAR_STEPS
        binding.seekZoom.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    updateZoomFromProgress(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        binding.zoomControls.isVisible = false

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
        updateZoomUi()
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
            val maxRatio = extractMaxZoomRatio(params)
            runOnUiThread {
                maxZoomRatio = maxRatio.coerceAtLeast(1.0)
                binding.zoomControls.isVisible = supportsZoom && maxZoomRatio > 1.0
                zoomRatio = zoomRatio.coerceIn(1.0, maxZoomRatio)
                updateZoomUi()
            }
            params
        }
    }

    private fun updateZoomFromProgress(progress: Int) {
        val newRatio = progressToRatio(progress)
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
        updateZoomUi()
    }

    private fun updateZoomUi() {
        binding.textZoom.text = getString(R.string.zoom_ratio_format, zoomRatio)
        if (maxZoomRatio > 1.0) {
            val progress = ratioToProgress(zoomRatio)
            if (binding.seekZoom.progress != progress) {
                binding.seekZoom.progress = progress
            }
        }
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

    private fun progressToRatio(progress: Int): Double {
        if (maxZoomRatio <= 1.0) {
            return 1.0
        }
        val fraction = progress.toDouble() / ZOOM_SEEKBAR_STEPS.toDouble()
        return (1.0 + (maxZoomRatio - 1.0) * fraction).coerceIn(1.0, maxZoomRatio)
    }

    private fun ratioToProgress(ratio: Double): Int {
        if (maxZoomRatio <= 1.0) {
            return 0
        }
        val fraction = (ratio - 1.0) / (maxZoomRatio - 1.0)
        return (fraction * ZOOM_SEEKBAR_STEPS).roundToInt().coerceIn(0, ZOOM_SEEKBAR_STEPS)
    }

    private companion object {
        const val ZOOM_RATIO_DIVISOR = 100.0
        const val ZOOM_SEEKBAR_STEPS = 100
    }
}
