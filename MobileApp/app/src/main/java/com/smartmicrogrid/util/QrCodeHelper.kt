package com.smartmicrogrid.util

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.util.HashMap

object QrCodeHelper {
    fun generateQrCodeBitmap(text: String, size: Int = 512): ImageBitmap? {
        return try {
            val writer = QRCodeWriter()

            // Use java.util.HashMap explicitly to avoid IDE confusion
            val hints = HashMap<EncodeHintType, Any>()
            hints[EncodeHintType.MARGIN] = 1

            val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height

            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            // Use basic while loops to completely avoid the '..' vs 'until' IDE bug
            var x = 0
            while (x < width) {
                var y = 0
                while (y < height) {
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                    y++
                }
                x++
            }

            bmp.asImageBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}