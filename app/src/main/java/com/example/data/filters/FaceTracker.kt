package com.example.data.filters

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class TrackedFace(
    val bounds: RectF,
    val leftEye: PointF?,
    val rightEye: PointF?,
    val noseBase: PointF?,
    val mouthCenter: PointF?,
    val leftEar: PointF?,
    val rightEar: PointF?,
    val leftCheek: PointF?,
    val rightCheek: PointF?,
    val eulerX: Float,
    val eulerY: Float,
    val eulerZ: Float,
    val imageWidth: Int,
    val imageHeight: Int,
    val isFrontCamera: Boolean
) {
    // Calculates face center in normalized coordinates (0..1)
    val normalizedCenter: PointF
        get() = PointF(bounds.centerX() / imageWidth, bounds.centerY() / imageHeight)

    // Calculates face width in normalized coordinates (0..1)
    val normalizedWidth: Float
        get() = bounds.width() / imageWidth

    // Calculates face height in normalized coordinates (0..1)
    val normalizedHeight: Float
        get() = bounds.height() / imageHeight
}

class FaceTracker {

    private val detectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setMinFaceSize(0.15f)
        .build()

    private val detector: FaceDetector = FaceDetection.getClient(detectorOptions)

    @Volatile
    private var isBusy = false

    @OptIn(ExperimentalGetImage::class)
    fun createAnalyzer(
        isFrontCamera: () -> Boolean,
        onFaceDetected: (TrackedFace?) -> Unit
    ): ImageAnalysis.Analyzer {
        return ImageAnalysis.Analyzer { imageProxy ->
            if (isBusy) {
                imageProxy.close()
                return@Analyzer
            }

            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                isBusy = true
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

                // When image is rotated 90 or 270 degrees, effective width and height swap
                val isRotated = rotationDegrees == 90 || rotationDegrees == 270
                val effectiveWidth = if (isRotated) imageProxy.height else imageProxy.width
                val effectiveHeight = if (isRotated) imageProxy.width else imageProxy.height

                detector.process(inputImage)
                    .addOnSuccessListener { faces ->
                        if (faces.isNotEmpty()) {
                            val primaryFace = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
                            if (primaryFace != null) {
                                val tracked = convertFace(
                                    face = primaryFace,
                                    imgWidth = effectiveWidth,
                                    imgHeight = effectiveHeight,
                                    isFront = isFrontCamera()
                                )
                                onFaceDetected(tracked)
                            } else {
                                onFaceDetected(null)
                            }
                        } else {
                            onFaceDetected(null)
                        }
                    }
                    .addOnFailureListener {
                        onFaceDetected(null)
                    }
                    .addOnCompleteListener {
                        isBusy = false
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }
    }

    suspend fun detectFacesInBitmap(bitmap: Bitmap, isFrontCamera: Boolean): List<TrackedFace> =
        suspendCancellableCoroutine { continuation ->
            try {
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                detector.process(inputImage)
                    .addOnSuccessListener { faces ->
                        val result = faces.map {
                            convertFace(it, bitmap.width, bitmap.height, isFrontCamera)
                        }
                        if (continuation.isActive) {
                            continuation.resume(result)
                        }
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) {
                            continuation.resume(emptyList())
                        }
                    }
            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resume(emptyList())
                }
            }
        }

    private fun convertFace(
        face: Face,
        imgWidth: Int,
        imgHeight: Int,
        isFront: Boolean
    ): TrackedFace {
        val rawBounds = face.boundingBox

        val leftEyeLandmark = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
        val rightEyeLandmark = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position
        val noseLandmark = face.getLandmark(FaceLandmark.NOSE_BASE)?.position
        val mouthBottomLandmark = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position
        val leftEarLandmark = face.getLandmark(FaceLandmark.LEFT_EAR)?.position
        val rightEarLandmark = face.getLandmark(FaceLandmark.RIGHT_EAR)?.position
        val leftCheekLandmark = face.getLandmark(FaceLandmark.LEFT_CHEEK)?.position
        val rightCheekLandmark = face.getLandmark(FaceLandmark.RIGHT_CHEEK)?.position

        return TrackedFace(
            bounds = RectF(
                rawBounds.left.toFloat(),
                rawBounds.top.toFloat(),
                rawBounds.right.toFloat(),
                rawBounds.bottom.toFloat()
            ),
            leftEye = leftEyeLandmark,
            rightEye = rightEyeLandmark,
            noseBase = noseLandmark,
            mouthCenter = mouthBottomLandmark,
            leftEar = leftEarLandmark,
            rightEar = rightEarLandmark,
            leftCheek = leftCheekLandmark,
            rightCheek = rightCheekLandmark,
            eulerX = face.headEulerAngleX,
            eulerY = face.headEulerAngleY,
            eulerZ = face.headEulerAngleZ,
            imageWidth = imgWidth,
            imageHeight = imgHeight,
            isFrontCamera = isFront
        )
    }

    fun close() {
        try {
            detector.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
