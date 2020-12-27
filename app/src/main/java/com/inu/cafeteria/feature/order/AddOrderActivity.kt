/**
 * This file is part of INU Cafeteria.
 *
 * Copyright (C) 2020 INU Global App Center <potados99@gmail.com>
 *
 * INU Cafeteria is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * INU Cafeteria is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.inu.cafeteria.feature.order

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.mlkit.common.MlKitException
import com.inu.cafeteria.common.base.BaseActivity
import com.inu.cafeteria.common.extension.observe
import com.inu.cafeteria.databinding.AddOrderActivityBinding
import timber.log.Timber

/**
 * We use Activity directly.
 */
class AddOrderActivity : BaseActivity() {

    private lateinit var binding: AddOrderActivityBinding
    private val viewModel: AddOrderViewModel by viewModels()

    private val ticketProcessor = TicketRecognitionProcessor()
    private var cameraProvider: ProcessCameraProvider? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = AddOrderActivityBinding.inflate(layoutInflater).apply {
            setContentView(root)
        }

        prepareProcessor()
        startCameraWhenReady()
    }

    private fun prepareProcessor() {
        ticketProcessor.onOrderRecognized = {
            Toast.makeText(this, "대기번호: ${it.waitingNumber}, POS번호: ${it.posNumber}", Toast.LENGTH_SHORT).show()
        }
    }

    // -----------------Code for camera-------------------------------------------------------------
    private fun startCameraWhenReady() {
        observe(viewModel.getProcessorCameraProvider()) {
            cameraProvider = it
            startCamera()
        }
    }

    private fun startCamera() {
        try {
            bindAllCameraUseCases(cameraProvider ?: return)
        } catch (e: Exception) {
            Timber.e("Use case binding failed: ${e.message}")
        }
    }

    private fun bindAllCameraUseCases(cameraProvider: ProcessCameraProvider) {
        cameraProvider.unbindAll()

        bindPreviewUseCase(cameraProvider)
        bindAnalysisUseCase(cameraProvider)
    }

    private fun bindPreviewUseCase(cameraProvider: ProcessCameraProvider) {
        val previewUseCase = Preview.Builder().build().apply {
            setSurfaceProvider(binding.previewView.createSurfaceProvider())
        }

        cameraProvider.bindToLifecycle(this, cameraSelector, previewUseCase)
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    private fun bindAnalysisUseCase(cameraProvider: ProcessCameraProvider) {
        val analyzer = { imageProxy: ImageProxy ->
            try {
                ticketProcessor.processImageProxy(imageProxy)
            } catch (e: MlKitException) {
                Timber.e("Exception while processing image proxy: ${e.message}")
            }
        }

        val analysisUseCase = ImageAnalysis.Builder().build().apply {
            setAnalyzer(ContextCompat.getMainExecutor(this@AddOrderActivity), analyzer)
        }

        cameraProvider.bindToLifecycle(this, cameraSelector, analysisUseCase)
    }

    // -----------------Code for permissions--------------------------------------------------------
    override val requiredPermissions: Array<String>
        get() = REQUIRED_PERMISSIONS

    override fun onAllPermissionsGranted() {
        startCamera()
    }

    override fun onPermissionNotGranted() {
        finish()
    }

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    }
}