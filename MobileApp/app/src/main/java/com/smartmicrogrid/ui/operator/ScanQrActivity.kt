// ============================================================
// File: ScanQrActivity.kt
// Purpose: Thin Activity host for ScanQrScreen - needed so the
//          scanner can also be launched as a standalone Intent
//          outside the Home bottom-nav shell.
// Author: Dinil
// ============================================================
package com.smartmicrogrid.ui.operator

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.smartmicrogrid.ui.theme.SmartMicrogridTheme

class ScanQrActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartMicrogridTheme {
                ScanQrScreen()
            }
        }
    }

    companion object {
        fun intentFor(context: Context): Intent =
            Intent(context, ScanQrActivity::class.java)
    }
}