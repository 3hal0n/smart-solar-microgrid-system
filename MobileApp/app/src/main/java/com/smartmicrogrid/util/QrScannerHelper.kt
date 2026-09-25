// ============================================================
// File: QrScannerHelper.kt
// Purpose: Thin wrapper around CameraX + ML Kit barcode scanner.
//          Exposes a single analyze() ImageAnalysis.Analyzer that
//          calls back with the decoded QR string exactly once.
// Author: Dinil
// ============================================================
package com.smartmicrogrid.util

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class QrScannerHelper(
    private val onDecoded: (String) -> Unit,
    private val onError: (String) -> Unit = {}
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()
    @Volatile private var handled = false

    // Analyzes a camera frame; fires onDecoded once per scan session.
    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        if (handled) {
            imageProxy.close()
            return
        }
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val qr = barcodes.firstOrNull { it.format == Barcode.FORMAT_QR_CODE }
                val raw = qr?.rawValue
                if (!raw.isNullOrBlank() && !handled) {
                    handled = true
                    onDecoded(raw)
                }
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Scan failed")
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    // Resets the handled flag — call before restarting a new scan.
    fun reset() {
        handled = false
    }
}