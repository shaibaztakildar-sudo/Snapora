package com.example.ui.screens.camera

import android.graphics.PointF
import android.graphics.RectF
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.filters.FacePropType
import com.example.data.filters.SnaporaFilter
import com.example.data.filters.TrackedFace
import com.example.ui.theme.SnaporaPink
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FaceEffectOverlay(
    trackedFace: TrackedFace?,
    selectedFilter: SnaporaFilter,
    isFrontCamera: Boolean,
    modifier: Modifier = Modifier
) {
    if (selectedFilter.propType == FacePropType.NONE) return

    val infiniteTransition = rememberInfiniteTransition(label = "faceEffectAnim")
    val pulseAnim by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val shimmerAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    Box(modifier = modifier.fillMaxSize()) {
        if (trackedFace != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasW = size.width
                val canvasH = size.height

                // Map coordinates from sensor/image space to screen space
                val imgW = trackedFace.imageWidth.toFloat().coerceAtLeast(1f)
                val imgH = trackedFace.imageHeight.toFloat().coerceAtLeast(1f)

                fun mapX(x: Float): Float {
                    return if (isFrontCamera) {
                        (1f - (x / imgW)) * canvasW
                    } else {
                        (x / imgW) * canvasW
                    }
                }

                fun mapY(y: Float): Float {
                    return (y / imgH) * canvasH
                }

                val leftEye = trackedFace.leftEye
                val rightEye = trackedFace.rightEye
                val nose = trackedFace.noseBase
                val bounds = trackedFace.bounds

                val mappedBounds = RectF(
                    mapX(if (isFrontCamera) bounds.right else bounds.left),
                    mapY(bounds.top),
                    mapX(if (isFrontCamera) bounds.left else bounds.right),
                    mapY(bounds.bottom)
                )

                val mappedLeftEye = leftEye?.let { PointF(mapX(it.x), mapY(it.y)) }
                val mappedRightEye = rightEye?.let { PointF(mapX(it.x), mapY(it.y)) }
                val mappedNose = nose?.let { PointF(mapX(it.x), mapY(it.y)) }

                val eyeCenter = if (mappedLeftEye != null && mappedRightEye != null) {
                    PointF((mappedLeftEye.x + mappedRightEye.x) / 2f, (mappedLeftEye.y + mappedRightEye.y) / 2f)
                } else {
                    PointF(mappedBounds.centerX(), mappedBounds.top + mappedBounds.height() * 0.35f)
                }

                val eyeDist = if (mappedLeftEye != null && mappedRightEye != null) {
                    val dx = mappedRightEye.x - mappedLeftEye.x
                    val dy = mappedRightEye.y - mappedLeftEye.y
                    Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                } else {
                    mappedBounds.width() * 0.42f
                }

                var rollAngle = if (isFrontCamera) -trackedFace.eulerZ else trackedFace.eulerZ
                if (mappedLeftEye != null && mappedRightEye != null) {
                    val dy = (mappedRightEye.y - mappedLeftEye.y).toDouble()
                    val dx = (mappedRightEye.x - mappedLeftEye.x).toDouble()
                    rollAngle = Math.toDegrees(atan2(dy, dx)).toFloat()
                }

                when (selectedFilter.propType) {
                    FacePropType.CYBER_SHADES -> {
                        // Neon cyber sunglasses
                        translate(eyeCenter.x, eyeCenter.y) {
                            rotate(rollAngle) {
                                val visorW = eyeDist * 2.3f * pulseAnim
                                val visorH = visorW * 0.38f

                                val rect = androidx.compose.ui.geometry.Rect(
                                    -visorW / 2f, -visorH / 2f, visorW / 2f, visorH / 2f
                                )
                                drawRoundRect(
                                    color = Color(0xDC0F0523),
                                    topLeft = Offset(-visorW / 2f, -visorH / 2f),
                                    size = Size(visorW, visorH),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(14f, 14f)
                                )
                                drawRoundRect(
                                    color = Color(0xFF00F0FF),
                                    topLeft = Offset(-visorW / 2f, -visorH / 2f),
                                    size = Size(visorW, visorH),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(14f, 14f),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = visorH * 0.12f)
                                )
                                drawLine(
                                    color = Color(0xFFFF007A),
                                    start = Offset(-visorW * 0.4f, 0f),
                                    end = Offset(visorW * 0.4f, 0f),
                                    strokeWidth = visorH * 0.08f
                                )
                            }
                        }
                    }

                    FacePropType.RETRO_SUNGLASSES -> {
                        // Dark Retro Sunglasses
                        translate(eyeCenter.x, eyeCenter.y) {
                            rotate(rollAngle) {
                                val lensRadius = eyeDist * 0.52f
                                val leftOffset = -eyeDist * 0.55f
                                val rightOffset = eyeDist * 0.55f

                                // Left Lens
                                drawCircle(Color(0xFF18181B), lensRadius, Offset(leftOffset, 0f))
                                drawCircle(Color(0xFF27272A), lensRadius * 0.82f, Offset(leftOffset, 0f))

                                // Right Lens
                                drawCircle(Color(0xFF18181B), lensRadius, Offset(rightOffset, 0f))
                                drawCircle(Color(0xFF27272A), lensRadius * 0.82f, Offset(rightOffset, 0f))

                                // Bridge
                                drawLine(
                                    color = Color(0xFF18181B),
                                    start = Offset(leftOffset, 0f),
                                    end = Offset(rightOffset, 0f),
                                    strokeWidth = lensRadius * 0.25f
                                )
                            }
                        }
                    }

                    FacePropType.CAT_EARS -> {
                        // Cat Ears
                        val headTopX = mappedBounds.centerX()
                        val headTopY = mappedBounds.top + mappedBounds.height() * 0.08f
                        translate(headTopX, headTopY) {
                            rotate(rollAngle) {
                                val earSize = mappedBounds.width() * 0.32f * pulseAnim
                                val earSpread = mappedBounds.width() * 0.38f

                                val leftEarPath = Path().apply {
                                    moveTo(-earSpread, 0f)
                                    lineTo(-earSpread - earSize * 0.6f, -earSize * 1.2f)
                                    lineTo(-earSpread + earSize * 0.5f, -earSize * 0.6f)
                                    close()
                                }
                                val leftInnerPath = Path().apply {
                                    moveTo(-earSpread * 0.95f, 0f)
                                    lineTo(-earSpread - earSize * 0.5f, -earSize * 1.05f)
                                    lineTo(-earSpread + earSize * 0.35f, -earSize * 0.55f)
                                    close()
                                }
                                drawPath(leftEarPath, Color(0xFFFF69B4))
                                drawPath(leftInnerPath, Color(0xFFFFF0F5))

                                val rightEarPath = Path().apply {
                                    moveTo(earSpread, 0f)
                                    lineTo(earSpread + earSize * 0.6f, -earSize * 1.2f)
                                    lineTo(earSpread - earSize * 0.5f, -earSize * 0.6f)
                                    close()
                                }
                                val rightInnerPath = Path().apply {
                                    moveTo(earSpread * 0.95f, 0f)
                                    lineTo(earSpread + earSize * 0.5f, -earSize * 1.05f)
                                    lineTo(earSpread - earSize * 0.35f, -earSize * 0.55f)
                                    close()
                                }
                                drawPath(rightEarPath, Color(0xFFFF69B4))
                                drawPath(rightInnerPath, Color(0xFFFFF0F5))
                            }
                        }
                    }

                    FacePropType.PARTY_CROWN -> {
                        // Party Crown
                        val headTopX = mappedBounds.centerX()
                        val headTopY = mappedBounds.top - mappedBounds.height() * 0.06f
                        translate(headTopX, headTopY) {
                            rotate(rollAngle) {
                                val crownW = mappedBounds.width() * 0.6f * pulseAnim
                                val crownH = crownW * 0.55f

                                val crownPath = Path().apply {
                                    moveTo(-crownW / 2f, 0f)
                                    lineTo(-crownW / 2f, -crownH * 0.8f)
                                    lineTo(-crownW * 0.25f, -crownH * 0.4f)
                                    lineTo(0f, -crownH)
                                    lineTo(crownW * 0.25f, -crownH * 0.4f)
                                    lineTo(crownW / 2f, -crownH * 0.8f)
                                    lineTo(crownW / 2f, 0f)
                                    close()
                                }
                                drawPath(crownPath, Color(0xFFFFD700))
                                drawCircle(Color(0xFFFF007A), crownW * 0.06f, Offset(0f, -crownH * 0.85f))
                                drawCircle(Color(0xFF00E5FF), crownW * 0.05f, Offset(-crownW * 0.45f, -crownH * 0.7f))
                                drawCircle(Color(0xFF00E5FF), crownW * 0.05f, Offset(crownW * 0.45f, -crownH * 0.7f))
                            }
                        }
                    }

                    FacePropType.GENTLEMAN_MOUSTACHE -> {
                        // Curled Moustache
                        val mX = mappedNose?.x ?: mappedBounds.centerX()
                        val mY = mappedNose?.y?.plus(mappedBounds.height() * 0.09f) ?: (mappedBounds.centerY() + mappedBounds.height() * 0.2f)
                        translate(mX, mY) {
                            rotate(rollAngle) {
                                val mW = mappedBounds.width() * 0.45f
                                val mPath = Path().apply {
                                    moveTo(-mW / 2f, -mW * 0.1f)
                                    quadraticTo(-mW * 0.25f, mW * 0.2f, 0f, 0f)
                                    quadraticTo(mW * 0.25f, mW * 0.2f, mW / 2f, -mW * 0.1f)
                                    quadraticTo(mW * 0.35f, -mW * 0.05f, 0f, mW * 0.08f)
                                    quadraticTo(-mW * 0.35f, -mW * 0.05f, -mW / 2f, -mW * 0.1f)
                                    close()
                                }
                                drawPath(mPath, Color(0xFF271B11))
                            }
                        }
                    }

                    FacePropType.FLOATING_HEARTS -> {
                        // Pulsing hearts
                        val headX = mappedBounds.centerX()
                        val headY = mappedBounds.top - mappedBounds.height() * 0.18f
                        translate(headX, headY) {
                            rotate(rollAngle) {
                                val heartSize = mappedBounds.width() * 0.18f * pulseAnim
                                drawHeartScope(-mappedBounds.width() * 0.28f, -heartSize * 0.4f, heartSize * 0.85f, Color(0xFFFF2E93))
                                drawHeartScope(0f, -heartSize * 0.9f, heartSize * 1.15f, Color(0xFFFF4D8D))
                                drawHeartScope(mappedBounds.width() * 0.28f, -heartSize * 0.3f, heartSize * 0.9f, Color(0xFFFF2E93))
                            }
                        }
                    }

                    FacePropType.SPARKLE_AURA -> {
                        // Rotating sparkles
                        translate(eyeCenter.x, eyeCenter.y) {
                            rotate(rollAngle) {
                                val spread = eyeDist * 1.35f
                                drawStarScope(-spread, -eyeDist * 0.4f, eyeDist * 0.22f * pulseAnim, Color(0xFFFFE600))
                                drawStarScope(spread, -eyeDist * 0.35f, eyeDist * 0.20f, Color(0xFFFFE600))
                                drawStarScope(0f, -eyeDist * 0.85f, eyeDist * 0.25f * pulseAnim, Color(0xFFFFE600))
                                drawStarScope(-spread * 0.8f, eyeDist * 0.6f, eyeDist * 0.18f, Color(0xFFFFE600))
                                drawStarScope(spread * 0.8f, eyeDist * 0.55f, eyeDist * 0.16f * pulseAnim, Color(0xFFFFE600))
                            }
                        }
                    }

                    FacePropType.FLORAL_CROWN -> {
                        // Flower Crown
                        val headX = mappedBounds.centerX()
                        val headY = mappedBounds.top + mappedBounds.height() * 0.05f
                        translate(headX, headY) {
                            rotate(rollAngle) {
                                val wreathW = mappedBounds.width() * 0.8f
                                drawLine(
                                    color = Color(0xFF2E7D32),
                                    start = Offset(-wreathW / 2f, 0f),
                                    end = Offset(wreathW / 2f, 0f),
                                    strokeWidth = wreathW * 0.035f
                                )
                                val flowerColors = listOf(Color(0xFFFF4081), Color(0xFFFF80AB), Color(0xFFFFEB3B), Color(0xFFFF4081), Color(0xFFFF80AB))
                                val step = wreathW / 4f
                                for (i in 0..4) {
                                    val fx = -wreathW / 2f + i * step
                                    val fRadius = wreathW * 0.08f * pulseAnim
                                    drawCircle(flowerColors[i], fRadius, Offset(fx, 0f))
                                    drawCircle(Color(0xFFFFF59D), fRadius * 0.4f, Offset(fx, 0f))
                                }
                            }
                        }
                    }

                    FacePropType.ANGEL_HALO -> {
                        // Floating halo
                        val headX = mappedBounds.centerX()
                        val headY = mappedBounds.top - mappedBounds.height() * 0.24f
                        translate(headX, headY) {
                            rotate(rollAngle) {
                                val haloW = mappedBounds.width() * 0.65f * pulseAnim
                                val haloH = haloW * 0.28f

                                drawOval(
                                    color = Color(0x88FFEB3B),
                                    topLeft = Offset(-haloW / 2f, -haloH / 2f),
                                    size = Size(haloW, haloH),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = haloH * 0.35f)
                                )
                                drawOval(
                                    color = Color(0xFFFFFDE7),
                                    topLeft = Offset(-haloW / 2f, -haloH / 2f),
                                    size = Size(haloW, haloH),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = haloH * 0.18f)
                                )
                            }
                        }
                    }

                    FacePropType.STAR_BLUSH -> {
                        // Cheek stars
                        val leftCheek = trackedFace.leftCheek
                        val rightCheek = trackedFace.rightCheek
                        val mappedLCheek = if (leftCheek != null) PointF(mapX(leftCheek.x), mapY(leftCheek.y)) else PointF(mappedBounds.left + mappedBounds.width() * 0.25f, mappedBounds.centerY() + mappedBounds.height() * 0.1f)
                        val mappedRCheek = if (rightCheek != null) PointF(mapX(rightCheek.x), mapY(rightCheek.y)) else PointF(mappedBounds.right - mappedBounds.width() * 0.25f, mappedBounds.centerY() + mappedBounds.height() * 0.1f)

                        drawStarScope(mappedLCheek.x, mappedLCheek.y, mappedBounds.width() * 0.09f * pulseAnim, Color(0xFFFF69B4))
                        drawStarScope(mappedRCheek.x, mappedRCheek.y, mappedBounds.width() * 0.09f * pulseAnim, Color(0xFFFF69B4))
                    }

                    FacePropType.NONE -> {}
                }
            }
        } else {
            // Searching for face indicator
            Surface(
                color = Color.Black.copy(alpha = 0.55f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 64.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Face, contentDescription = null, tint = SnaporaPink, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Face effect active • Looking for face ✨",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawHeartScope(x: Float, y: Float, size: Float, color: Color) {
    val path = Path().apply {
        moveTo(x, y + size * 0.3f)
        cubicTo(x - size * 0.5f, y - size * 0.4f, x - size, y + size * 0.1f, x, y + size)
        cubicTo(x + size, y + size * 0.1f, x + size * 0.5f, y - size * 0.4f, x, y + size * 0.3f)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawStarScope(cx: Float, cy: Float, radius: Float, color: Color) {
    val path = Path()
    val innerRadius = radius * 0.4f
    val points = 5
    var angle = -Math.PI / 2.0
    val step = Math.PI / points

    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) radius else innerRadius
        val x = (cx + r * cos(angle)).toFloat()
        val y = (cy + r * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        angle += step
    }
    path.close()
    drawPath(path, color)
}
