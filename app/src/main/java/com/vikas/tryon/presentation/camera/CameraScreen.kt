package com.vikas.tryon.presentation.camera

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Size
import android.view.PixelCopy
import android.view.View
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    onNavigateBack: () -> Unit,
    onOpenGarments: () -> Unit,
    onOpenMeasurements: () -> Unit,
    onOpenOutfits: () -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)
    val isLandmarkerReady by viewModel.isLandmarkerReady.collectAsState()
    val poseResult by viewModel.poseResult.collectAsState()
    val smoothedLandmarks by viewModel.smoothedLandmarks.collectAsState()
    val selectedGarment by viewModel.selectedGarment.collectAsState()
    val garmentBitmap by viewModel.garmentBitmap.collectAsState()
    val isFrontCamera by viewModel.isFrontCamera.collectAsState()
    val showGarment by viewModel.showGarment.collectAsState()
    val outfitSaved by viewModel.outfitSaved.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val rootView = LocalView.current
    val activityContext = LocalContext.current

    // When true, UI chrome (buttons, skeleton, chips) is hidden for one frame
    // so the screenshot contains only the camera feed + garment overlay.
    var isCapturing by remember { mutableStateOf(false) }

    LaunchedEffect(isCapturing) {
        if (isCapturing) {
            // Wait two frames so the hidden-UI recomposition is actually on screen
            withFrameNanos {}
            withFrameNanos {}
            captureScreen(rootView, activityContext) { bitmap ->
                if (bitmap != null) viewModel.saveOutfit(bitmap)
            }
            isCapturing = false
        }
    }

    LaunchedEffect(outfitSaved) {
        if (outfitSaved) {
            snackbarHostState.showSnackbar("Outfit saved!")
            viewModel.consumeOutfitSaved()
        }
    }

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    if (!cameraPermission.status.isGranted) {
        PermissionDeniedScreen(
            onRequestPermission = { cameraPermission.launchPermissionRequest() },
            onNavigateBack = onNavigateBack
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 80.dp)
        )
        // Camera Preview
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

        DisposableEffect(Unit) {
            onDispose { cameraExecutor.shutdown() }
        }

        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { previewView ->
                bindCamera(
                    context = previewView.context,
                    lifecycleOwner = lifecycleOwner,
                    previewView = previewView,
                    isFrontCamera = isFrontCamera,
                    cameraExecutor = cameraExecutor,
                    onFrameAvailable = viewModel::processImageProxy
                )
            }
        )

        // Pose overlay
        if (smoothedLandmarks != null) {
            if (!isCapturing) {
                PoseLandmarkOverlay(
                    landmarks = smoothedLandmarks,
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (showGarment && selectedGarment != null) {
                GarmentOverlay(
                    landmarks = smoothedLandmarks,
                    garment = selectedGarment,
                    garmentBitmap = garmentBitmap,
                    isFrontCamera = isFrontCamera,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Top bar
        if (!isCapturing) Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            if (!isLandmarkerReady) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        "Loading model...",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            if (selectedGarment != null) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        selectedGarment!!.name,
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Bottom controls
        if (!isCapturing) Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Secondary controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CameraControlButton(
                    icon = Icons.Default.Checkroom,
                    label = "Garments",
                    onClick = onOpenGarments
                )
                CameraControlButton(
                    icon = if (showGarment) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    label = if (showGarment) "Hide" else "Show",
                    onClick = viewModel::toggleGarmentOverlay
                )
                CameraControlButton(
                    icon = Icons.Default.Straighten,
                    label = "Measure",
                    onClick = onOpenMeasurements
                )
                CameraControlButton(
                    icon = Icons.Default.CollectionsBookmark,
                    label = "Outfits",
                    onClick = onOpenOutfits
                )
            }

            // Save Outfit button — only visible when a garment is active and pose detected
            if (selectedGarment != null && smoothedLandmarks != null) {
                Button(
                    onClick = { isCapturing = true },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save Outfit", style = MaterialTheme.typography.labelMedium)
                }
            }

            // Main controls row
            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flip camera
                IconButton(
                    onClick = viewModel::toggleCamera,
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Flip camera",
                        tint = Color.White, modifier = Modifier.size(26.dp))
                }
            }
        }

        // Pose detection status indicator
        if (smoothedLandmarks != null && !isCapturing) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 60.dp),
                color = Color(0xFF4CAF50).copy(alpha = 0.8f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.FaceRetouchingNatural, contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(14.dp))
                    Text("Pose Detected", color = Color.White,
                        style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun CameraControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .background(Color.White.copy(alpha = 0.2f), CircleShape)
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun PermissionDeniedScreen(
    onRequestPermission: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.CameraAlt, contentDescription = null,
            modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("Camera Permission Required", style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text("Camera access is needed for real-time try-on and pose detection.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRequestPermission) {
            Text("Grant Permission")
        }
        TextButton(onClick = onNavigateBack) { Text("Go Back") }
    }
}

/**
 * Screenshots the current window content (camera preview + garment overlay).
 * PixelCopy is required because the camera preview renders on a separate
 * surface that ordinary View drawing cannot read.
 */
private fun captureScreen(view: View, context: Context, onResult: (Bitmap?) -> Unit) {
    val activity = context as? Activity
    if (activity == null || view.width == 0 || view.height == 0) {
        onResult(null)
        return
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val location = IntArray(2)
        view.getLocationInWindow(location)
        try {
            PixelCopy.request(
                activity.window,
                Rect(location[0], location[1], location[0] + view.width, location[1] + view.height),
                bitmap,
                { result ->
                    onResult(if (result == PixelCopy.SUCCESS) bitmap else null)
                },
                Handler(Looper.getMainLooper())
            )
        } catch (e: Exception) {
            onResult(null)
        }
    } else {
        // API 24-25 fallback: works because PreviewView runs in COMPATIBLE
        // (TextureView) mode, whose content is drawable through the view hierarchy.
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bitmap))
        onResult(bitmap)
    }
}

private fun bindCamera(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewView: PreviewView,
    isFrontCamera: Boolean,
    cameraExecutor: java.util.concurrent.ExecutorService,
    onFrameAvailable: (ImageProxy) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(640, 480))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    onFrameAvailable(imageProxy)
                }
            }

        val cameraSelector = if (isFrontCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner, cameraSelector, preview, imageAnalysis
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }, ContextCompat.getMainExecutor(context))
}
