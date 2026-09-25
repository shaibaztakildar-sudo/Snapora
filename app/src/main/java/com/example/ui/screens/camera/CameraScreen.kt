package com.example.ui.screens.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.filters.*
import com.example.data.repository.MediaStorageManager
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

enum class CameraFlashMode {
    OFF, ON, AUTO
}

enum class CaptureMode {
    PHOTO, VIDEO
}

@SuppressLint("MissingPermission")
@Composable
fun CameraScreen(
    mediaStorageManager: MediaStorageManager,
    onMediaCaptured: (mediaPath: String, mediaType: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] ?: false
        hasAudioPermission = permissions[Manifest.permission.RECORD_AUDIO] ?: false
    }

    // Photo/Video picker launcher for gallery import
    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val isVideo = context.contentResolver.getType(uri)?.contains("video") == true
                val path = mediaStorageManager.copyUriToInternal(uri, "snap")
                if (path != null) {
                    onMediaCaptured(path, if (isVideo) "VIDEO" else "IMAGE")
                }
            }
        }
    }

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableStateOf(CameraFlashMode.OFF) }
    var captureMode by remember { mutableStateOf(CaptureMode.PHOTO) }

    // Real-Time Filters & Face Tracking
    val faceTracker = remember { FaceTracker() }
    var trackedFace by remember { mutableStateOf<TrackedFace?>(null) }
    var selectedFilter by remember { mutableStateOf(FilterRegistry.allFilters.first()) }
    var isFilterCarouselOpen by remember { mutableStateOf(false) }

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingDurationSeconds by remember { mutableIntStateOf(0) }
    var isCapturingPhoto by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }

    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }

    // Clean up camera and face tracker on dispose
    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                activeRecording?.stop()
                activeRecording = null
                isRecording = false
                val provider = ProcessCameraProvider.getInstance(context).get()
                provider.unbindAll()
                faceTracker.close()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }

    // Safe dynamic camera lifecycle binding with real-time ImageAnalysis for face effects
    LaunchedEffect(hasCameraPermission, lensFacing, captureMode, flashMode, previewView, selectedFilter.id) {
        if (!hasCameraPermission || previewView == null) return@LaunchedEffect

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView!!.surfaceProvider)
                }

                // Verify lens facing availability, fallback if not available
                var actualFacing = lensFacing
                var selector = CameraSelector.Builder().requireLensFacing(actualFacing).build()
                if (!cameraProvider.hasCamera(selector)) {
                    val fallback = if (actualFacing == CameraSelector.LENS_FACING_BACK) {
                        CameraSelector.LENS_FACING_FRONT
                    } else {
                        CameraSelector.LENS_FACING_BACK
                    }
                    val altSelector = CameraSelector.Builder().requireLensFacing(fallback).build()
                    if (cameraProvider.hasCamera(altSelector)) {
                        actualFacing = fallback
                        selector = altSelector
                        lensFacing = fallback
                    }
                }

                if (captureMode == CaptureMode.PHOTO) {
                    val imgCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setFlashMode(
                            when (flashMode) {
                                CameraFlashMode.ON -> ImageCapture.FLASH_MODE_ON
                                CameraFlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
                                CameraFlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
                            }
                        )
                        .build()
                    imageCapture = imgCapture
                    videoCapture = null

                    // Bind ImageAnalysis for Face Tracking if a face prop is active
                    val analysis = if (selectedFilter.propType != FacePropType.NONE) {
                        try {
                            ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build().also {
                                    it.setAnalyzer(
                                        mainExecutor,
                                        faceTracker.createAnalyzer({ actualFacing == CameraSelector.LENS_FACING_FRONT }) { face ->
                                            trackedFace = face
                                        }
                                    )
                                }
                        } catch (e: Exception) {
                            null
                        }
                    } else {
                        trackedFace = null
                        null
                    }

                    try {
                        val cam = if (analysis != null) {
                            cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, imgCapture, analysis)
                        } else {
                            cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, imgCapture)
                        }
                        cameraControl = cam.cameraControl
                    } catch (e: Exception) {
                        // Fallback if 3 concurrent use cases exceed device stream limit
                        val cam = cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, imgCapture)
                        cameraControl = cam.cameraControl
                    }
                } else {
                    // Video Mode: Safely verify video encoder capabilities
                    var videoBound = false
                    try {
                        val cameraInfo: CameraInfo? = cameraProvider.availableCameraInfos.firstOrNull { info: CameraInfo ->
                            selector.filter(listOf(info)).isNotEmpty()
                        }
                        val supportedQualities: List<Quality> = cameraInfo?.let { info ->
                            Recorder.getVideoCapabilities(info).getSupportedQualities(DynamicRange.SDR)
                        } ?: emptyList()

                        if (supportedQualities.isNotEmpty()) {
                            val qualitySelector = QualitySelector.fromOrderedList(
                                supportedQualities,
                                FallbackStrategy.lowerQualityOrHigherThan(supportedQualities.last())
                            )
                            val recorder = Recorder.Builder()
                                .setQualitySelector(qualitySelector)
                                .build()
                            val vidCapture = VideoCapture.withOutput(recorder)
                            videoCapture = vidCapture
                            imageCapture = null

                            val cam = cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, vidCapture)
                            cameraControl = cam.cameraControl
                            cameraControl?.enableTorch(flashMode == CameraFlashMode.ON)
                            videoBound = true
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    if (!videoBound) {
                        // If no supported video encoder profiles, fallback to photo capture
                        val imgCapture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        imageCapture = imgCapture
                        videoCapture = null
                        val cam = cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, imgCapture)
                        cameraControl = cam.cameraControl
                        captureMode = CaptureMode.PHOTO
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, mainExecutor)
    }

    // Recording duration timer
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingDurationSeconds = 0
            while (isRecording) {
                delay(1000L)
                recordingDurationSeconds++
                if (recordingDurationSeconds >= 60) {
                    activeRecording?.stop()
                    activeRecording = null
                    isRecording = false
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            // 1. Live Camera ViewFinder
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        previewView = this
                    }
                }
            )

            // 2. Live Color Grading & Visual Filter Overlays
            if (selectedFilter.colorOverlay != Color.Transparent) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(selectedFilter.colorOverlay)
                )
            }

            if (selectedFilter.beautyGlow) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFFFE0E6).copy(alpha = 0.08f))
                )
            }

            if (selectedFilter.vignetteColor != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color.Transparent, selectedFilter.vignetteColor!!),
                                radius = 950f
                            )
                        )
                )
            }

            // 3. Real-Time Face Effects Overlay (Sunglasses, Hats, Ears, Hearts, Moustache, etc.)
            FaceEffectOverlay(
                trackedFace = trackedFace,
                selectedFilter = selectedFilter,
                isFrontCamera = lensFacing == CameraSelector.LENS_FACING_FRONT
            )
        } else {
            // Permission Request State
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Camera Permission",
                        tint = SnaporaPink,
                        modifier = Modifier.size(44.dp)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Snapora Camera Access",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Snapora uses your device camera to apply live filters, face effects, and capture real-time moments to share with friends.",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        permissionLauncher.launch(
                            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SnaporaPurple),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Grant Camera Permission", fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Choose from Gallery instead")
                }
            }
        }

        // Camera UI Controls Overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Controls Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flash Control Button
                IconButton(
                    onClick = {
                        flashMode = when (flashMode) {
                            CameraFlashMode.OFF -> CameraFlashMode.ON
                            CameraFlashMode.ON -> CameraFlashMode.AUTO
                            CameraFlashMode.AUTO -> CameraFlashMode.OFF
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = when (flashMode) {
                            CameraFlashMode.ON -> Icons.Default.FlashOn
                            CameraFlashMode.AUTO -> Icons.Default.FlashAuto
                            CameraFlashMode.OFF -> Icons.Default.FlashOff
                        },
                        contentDescription = "Flash mode: $flashMode",
                        tint = if (flashMode != CameraFlashMode.OFF) SnaporaAmber else Color.White
                    )
                }

                // Recording Timer Badge (if recording)
                AnimatedVisibility(visible = isRecording) {
                    Surface(
                        color = SnaporaRed,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = String.format("%02d:%02d", recordingDurationSeconds / 60, recordingDurationSeconds % 60),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // Switch Camera (Front / Back)
                IconButton(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .testTag("camera_switch_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FlipCameraAndroid,
                        contentDescription = "Flip camera",
                        tint = Color.White
                    )
                }
            }

            // Bottom Section: Filter Carousel + Shutter & Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Filter Carousel Drawer
                AnimatedVisibility(
                    visible = isFilterCarouselOpen,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    FilterCarousel(
                        selectedFilter = selectedFilter,
                        onFilterSelected = { filter ->
                            selectedFilter = filter
                        },
                        onClose = { isFilterCarouselOpen = false },
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // Mode Selector (Photo / Video)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "PHOTO",
                        fontSize = 12.sp,
                        fontWeight = if (captureMode == CaptureMode.PHOTO) FontWeight.Bold else FontWeight.Normal,
                        color = if (captureMode == CaptureMode.PHOTO) SnaporaPink else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier
                            .clickable { if (!isRecording) captureMode = CaptureMode.PHOTO }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                    Text(
                        text = "VIDEO",
                        fontSize = 12.sp,
                        fontWeight = if (captureMode == CaptureMode.VIDEO) FontWeight.Bold else FontWeight.Normal,
                        color = if (captureMode == CaptureMode.VIDEO) SnaporaPink else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier
                            .clickable { if (!isRecording) captureMode = CaptureMode.VIDEO }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Gallery Picker Button
                    IconButton(
                        onClick = {
                            galleryPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                            .testTag("gallery_picker_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Pick from gallery",
                            tint = Color.White
                        )
                    }

                    // Main Shutter / Record Button
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .border(4.dp, if (isRecording) SnaporaRed else Color.White, CircleShape)
                            .padding(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isRecording) SnaporaRed
                                else if (captureMode == CaptureMode.VIDEO) SnaporaRed.copy(alpha = 0.8f)
                                else Color.White
                            )
                            .clickable {
                                if (captureMode == CaptureMode.PHOTO) {
                                    // Capture Photo
                                    val currentCapture = imageCapture
                                    if (currentCapture == null) {
                                        Toast.makeText(context, "Camera initializing, please try again", Toast.LENGTH_SHORT).show()
                                        return@clickable
                                    }
                                    val file = File(context.cacheDir, "snap_${System.currentTimeMillis()}.jpg")
                                    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

                                    isCapturingPhoto = true
                                    currentCapture.takePicture(
                                        outputOptions,
                                        mainExecutor,
                                        object : ImageCapture.OnImageSavedCallback {
                                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                                isCapturingPhoto = false
                                                coroutineScope.launch {
                                                    try {
                                                        val rawBitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                                                        val processedBitmap = FilterProcessor.processCapturedPhoto(
                                                            sourceBitmap = rawBitmap,
                                                            filter = selectedFilter,
                                                            faceTracker = faceTracker,
                                                            isFrontCamera = lensFacing == CameraSelector.LENS_FACING_FRONT
                                                        )
                                                        val savedPath = mediaStorageManager.saveBitmap(
                                                            processedBitmap,
                                                            "snap"
                                                        )
                                                        onMediaCaptured(savedPath, "IMAGE")
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                        Toast.makeText(context, "Error processing photo", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }

                                            override fun onError(exception: ImageCaptureException) {
                                                isCapturingPhoto = false
                                                exception.printStackTrace()
                                                Toast.makeText(context, "Failed to capture photo", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                } else {
                                    // Capture Video
                                    val currentVidCapture = videoCapture
                                    if (currentVidCapture == null) {
                                        Toast.makeText(context, "Video recording is not available on this camera", Toast.LENGTH_SHORT).show()
                                        return@clickable
                                    }
                                    if (isRecording) {
                                        activeRecording?.stop()
                                        activeRecording = null
                                        isRecording = false
                                    } else {
                                        val outputFile = mediaStorageManager.createVideoOutputFile("snap")
                                        val outputOptions = FileOutputOptions.Builder(outputFile).build()

                                        val recordingBuilder = currentVidCapture.output
                                            .prepareRecording(context, outputOptions)

                                        if (hasAudioPermission) {
                                            recordingBuilder.withAudioEnabled()
                                        }

                                        activeRecording = recordingBuilder.start(mainExecutor) { recordEvent ->
                                            when (recordEvent) {
                                                is VideoRecordEvent.Start -> {
                                                    isRecording = true
                                                }
                                                is VideoRecordEvent.Finalize -> {
                                                    isRecording = false
                                                    if (!recordEvent.hasError()) {
                                                        onMediaCaptured(outputFile.absolutePath, "VIDEO")
                                                    } else {
                                                        Toast.makeText(context, "Video recording error: ${recordEvent.cause?.message ?: "unknown"}", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            .testTag("camera_shutter_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCapturingPhoto) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = SnaporaPurple,
                                strokeWidth = 3.dp
                            )
                        } else if (isRecording) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White)
                            )
                        }
                    }

                    // Real-Time Filter & Face Effects Carousel Button
                    IconButton(
                        onClick = { isFilterCarouselOpen = !isFilterCarouselOpen },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(
                                if (selectedFilter.id != "original") SnaporaPurple
                                else Color.Black.copy(alpha = 0.5f)
                            )
                            .border(
                                1.5.dp,
                                if (selectedFilter.id != "original") SnaporaPink else Color.White.copy(alpha = 0.4f),
                                CircleShape
                            )
                            .testTag("camera_filters_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Filters & Effects",
                            tint = if (selectedFilter.id != "original") Color.White else SnaporaPink
                        )
                    }
                }
            }
        }
    }
}
