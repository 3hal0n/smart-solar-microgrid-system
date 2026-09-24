// ============================================================
// File: ScanQrScreen.kt
// Purpose: Scan QR destination for the Home nav-graph shell (scan a
//          prosumer's transaction QR, verify against the server,
//          finalize the transfer), per architecture.md §6 "Android —
//          Operator mode, QR Scanner | ui/operator/ScanQrScreen.kt | Dinil"
//          (moved from Migara 2026-09-24 — see architecture.md §0/§7).
// Author: Shalon (stub only — see TODO below)
// ============================================================
package com.smartmicrogrid.ui.operator

import androidx.compose.runtime.Composable
import com.smartmicrogrid.ui.home.PlaceholderScreen

// TODO(Dinil): replace this placeholder with the real QR scanner (CameraX/ZXing) that posts to
// your own POST /reservations/verify-qr endpoint. HomeActivity's nav graph already routes here by
// name; nothing in ui/home needs to change when you do this.
@Composable
fun ScanQrScreen() {
    PlaceholderScreen(title = "Scan QR", owner = "Dinil")
}
