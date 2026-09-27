// ============================================================
// File: QrCodeHelper.kt
// Purpose: Utility to convert a string (QR token) into a Compose
//          compatible ImageBitmap using ZXing's QRCodeWriter.
//          The app only renders the token; it does not generate
//          or validate the cryptographic signature (FAT service pattern).
// Author: Migara
// ============================================================

package com.smartmicrogrid.util

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

object QrCodeHelper {

    /**
     * Generates a QR code ImageBitmap from the given text.
     *
     * @param text The string to encode (e.g., the qrToken from the API).
     * @param size The width and height of the QR code in pixels (default 512).
     * @return An ImageBitmap ready to be displayed in Jetpack Compose, or null if generation fails.
     */
    fun generateQrCodeBitmap(text: String, size: Int = 512): ImageBitmap? {
        return try {
            // Configure QR code with minimal margin for a cleaner look
            val hints = mapOf(EncodeHintType.MARGIN to 1)

            // Encode the text into a bit matrix
            val bitMatrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)

            val width = bitMatrix.width
            val height = bitMatrix.height

            // Create an Android Bitmap and populate it with black/white pixels
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }

            // Convert Android Bitmap to Compose ImageBitmap
            bmp.asImageBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}