// ============================================================
// File: ScanQrScreen.kt
// Purpose: Grid Operator QR scanner. CameraX preview + ML Kit
//          decode -> POST /api/reservations/verify-qr. Shows the
//          server's response (or rejection message) verbatim;
//          no business rule is computed on the client.
// Author: Dinil
// ============================================================
package com.smartmicrogrid.ui.operator

import android.Manifest
import android.content.Context
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.smartmicrogrid.data.local.AppDbHelper
import com.smartmicrogrid.data.local.ReservationCacheDao
import com.smartmicrogrid.data.local.VerifiedScan
import com.smartmicrogrid.data.remote.ApiClient
import com.smartmicrogrid.data.remote.dto.ApiErrorBody
import com.smartmicrogrid.data.remote.dto.VerifyQrRequest
import com.smartmicrogrid.data.remote.dto.VerifyQrResponse
import com.smartmicrogrid.util.QrScannerHelper
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScanQrScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // Camera permission gate.
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    // UI state.
    var result by remember { mutableStateOf<VerifyQrResponse?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var scanResetKey by remember { mutableStateOf(0) }  // bump to restart scanner

    // Ask for permission on first composition.
    LaunchedEffect(Unit) {
        //if (!cameraPermission.hasPermission) cameraPermission.launchPermissionRequest()
        if (cameraPermission.status.isGranted)  cameraPermission.launchPermissionRequest()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Scan Prosumer QR", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Point the camera at the prosumer's transaction QR. The server verifies " +
                    "the token and completes the reservation.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Camera preview + overlay.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black)
        ) {
//            if (cameraPermission.hasPermission) {
            if (cameraPermission.status.isGranted)  {
                key(scanResetKey) {
                    CameraPreview(
                        onQrDecoded = { token ->
                            if (busy) return@CameraPreview
                            busy = true
                            errorMessage = null
                            scope.launch {
                                try {
                                    val resp = callVerify(context, token)
                                    if (resp.isSuccessful && resp.body() != null) {
                                        val body = resp.body()!!
                                        result = body
                                        cacheVerifiedScan(context, body)
                                    } else {
                                        errorMessage = parseError(resp.errorBody()?.string())
                                            ?: "Verification failed (${resp.code()})"
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Network error: ${e.message}"
                                } finally {
                                    busy = false
                                }
                            }
                        },
                        onError = { errorMessage = it }
                    )
                }
            } else {
                Text(
                    "Camera permission required to scan QR codes.",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
            }

            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        // Result card or error.
        result?.let { r ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("✓ Verified", style = MaterialTheme.typography.titleMedium)
                    Text("Reservation: ${r.reservationId}")
                    Text("Prosumer: ${r.prosumerName}")
                    Text("Station: ${r.stationName}")
                    Text("Slot: ${r.slotNumber}")
                    Text("Status: ${r.status}")
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        result = null
                        errorMessage = null
                        scanResetKey++      // restart the scanner
                    }) { Text("Scan another") }
                }
            }
        }

        if (errorMessage != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("✗ ${errorMessage!!}", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        errorMessage = null
                        scanResetKey++
                    }) { Text("Retry scan") }
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(
    onQrDecoded: (String) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analyzer = ImageAnalysis.Builder()
                    .setTargetResolution(Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(
                            Executors.newSingleThreadExecutor(),
                            QrScannerHelper(onDecoded = onQrDecoded, onError = onError)
                        )
                    }
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analyzer
                    )
                } catch (e: Exception) {
                    onError("Camera bind failed: ${e.message}")
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}

// Calls the verify-qr endpoint. Runs the network call off the main thread.
private suspend fun callVerify(context: Context, token: String) = withContext(Dispatchers.IO) {
    ApiClient.service.verifyQr(VerifyQrRequest(token))
}

// Writes a successful verification into the operator's local scan history.
private suspend fun cacheVerifiedScan(context: Context, r: VerifyQrResponse) = withContext(Dispatchers.IO) {
    val db = AppDbHelper(context).writableDatabase
    ReservationCacheDao.upsert(db, VerifiedScan(
        reservationId = r.reservationId,
        prosumerName = r.prosumerName,
        stationName = r.stationName,
        slotNumber = r.slotNumber,
        status = r.status,
        verifiedAt = System.currentTimeMillis()
    ))
}

// Extracts a human-readable message from an API error body.
private fun parseError(body: String?): String? {
    if (body.isNullOrBlank()) return null
    return try {
        val err = Gson().fromJson(body, ApiErrorBody::class.java)
        err.message ?: err.code
    } catch (_: Exception) { null }
}