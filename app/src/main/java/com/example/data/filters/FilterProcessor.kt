package com.example.data.filters

import android.graphics.*
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object FilterProcessor {

    suspend fun processCapturedPhoto(
        sourceBitmap: Bitmap,
        filter: SnaporaFilter,
        faceTracker: FaceTracker?,
        isFrontCamera: Boolean
    ): Bitmap = withContext(Dispatchers.Default) {
        if (filter.id == "original" && filter.propType == FacePropType.NONE) {
            return@withContext sourceBitmap
        }

        // Create working mutable bitmap
        val outputBitmap = Bitmap.createBitmap(
            sourceBitmap.width,
            sourceBitmap.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(outputBitmap)

        // 1. Apply Color Matrix (Brightness, Contrast, Saturation, Monochrome)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val combinedMatrix = ColorMatrix()

        // Saturation
        if (filter.saturationBoost != 1f) {
            val satMatrix = ColorMatrix()
            satMatrix.setSaturation(filter.saturationBoost)
            combinedMatrix.postConcat(satMatrix)
        }

        // Brightness & Contrast
        if (filter.brightnessBoost != 0f || filter.contrastBoost != 1f) {
            val scale = filter.contrastBoost
            val translate = (filter.brightnessBoost * 255f) + (1f - scale) * 128f
            val bcMatrix = ColorMatrix(
                floatArrayOf(
                    scale, 0f, 0f, 0f, translate,
                    0f, scale, 0f, 0f, translate,
                    0f, 0f, scale, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            combinedMatrix.postConcat(bcMatrix)
        }

        paint.colorFilter = ColorMatrixColorFilter(combinedMatrix)
        canvas.drawBitmap(sourceBitmap, 0f, 0f, paint)

        // 2. Beauty Glow / Highlight Bloom
        if (filter.beautyGlow) {
            val beautyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
                color = Color.argb(40, 255, 235, 238)
            }
            canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), beautyPaint)
        }

        // 3. Color Tint Overlay
        if (filter.colorOverlay != androidx.compose.ui.graphics.Color.Transparent) {
            val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = filter.colorOverlay.toArgb()
            }
            canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), tintPaint)
        }

        // 4. Vignette Shadow (for Vintage, Noir, Cinematic)
        if (filter.vignetteColor != null) {
            val cx = canvas.width / 2f
            val cy = canvas.height / 2f
            val radius = Math.max(cx, cy) * 1.2f
            val vignetteShader = RadialGradient(
                cx, cy, radius,
                intArrayOf(Color.TRANSPARENT, filter.vignetteColor.toArgb()),
                floatArrayOf(0.45f, 1.0f),
                Shader.TileMode.CLAMP
            )
            val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = vignetteShader
            }
            canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), vignettePaint)
        }

        // 5. Detect and Render Face Props onto the Bitmap
        if (filter.propType != FacePropType.NONE && faceTracker != null) {
            try {
                val faces = faceTracker.detectFacesInBitmap(outputBitmap, isFrontCamera)
                for (face in faces) {
                    drawFacePropOnCanvas(canvas, face, filter.propType)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        outputBitmap
    }

    fun drawFacePropOnCanvas(canvas: Canvas, face: TrackedFace, propType: FacePropType) {
        val bounds = face.bounds
        val leftEye = face.leftEye
        val rightEye = face.rightEye
        val nose = face.noseBase
        val mouth = face.mouthCenter

        // Determine face angle in degrees
        var rollAngle = face.eulerZ
        if (leftEye != null && rightEye != null) {
            val dy = (rightEye.y - leftEye.y).toDouble()
            val dx = (rightEye.x - leftEye.x).toDouble()
            rollAngle = Math.toDegrees(atan2(dy, dx)).toFloat()
        }

        val eyeCenter = if (leftEye != null && rightEye != null) {
            PointF((leftEye.x + rightEye.x) / 2f, (leftEye.y + rightEye.y) / 2f)
        } else {
            PointF(bounds.centerX(), bounds.top + bounds.height() * 0.35f)
        }

        val eyeDist = if (leftEye != null && rightEye != null) {
            val dx = rightEye.x - leftEye.x
            val dy = rightEye.y - leftEye.y
            Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        } else {
            bounds.width() * 0.42f
        }

        canvas.save()

        when (propType) {
            FacePropType.CYBER_SHADES -> {
                // Futuristic Cyber Visor
                canvas.translate(eyeCenter.x, eyeCenter.y)
                canvas.rotate(rollAngle)

                val visorWidth = eyeDist * 2.3f
                val visorHeight = visorWidth * 0.38f

                // Outer Glow / Border
                val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = visorHeight * 0.12f
                    color = Color.parseColor("#00F0FF")
                }
                val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = Color.argb(220, 15, 5, 35)
                }
                val neonLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = visorHeight * 0.08f
                    color = Color.parseColor("#FF007A")
                }

                val rect = RectF(-visorWidth / 2f, -visorHeight / 2f, visorWidth / 2f, visorHeight / 2f)
                canvas.drawRoundRect(rect, 14f, 14f, glassPaint)
                canvas.drawRoundRect(rect, 14f, 14f, glowPaint)
                canvas.drawLine(-visorWidth * 0.4f, 0f, visorWidth * 0.4f, 0f, neonLinePaint)
            }

            FacePropType.RETRO_SUNGLASSES -> {
                // Dark Wayfarer Sunglasses
                canvas.translate(eyeCenter.x, eyeCenter.y)
                canvas.rotate(rollAngle)

                val glassRadius = eyeDist * 0.52f
                val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = Color.parseColor("#18181B")
                }
                val lensPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = Color.parseColor("#27272A")
                }
                val bridgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = glassRadius * 0.25f
                    color = Color.parseColor("#18181B")
                }

                val leftOffset = -eyeDist * 0.55f
                val rightOffset = eyeDist * 0.55f

                // Left Lens & Frame
                canvas.drawCircle(leftOffset, 0f, glassRadius, framePaint)
                canvas.drawCircle(leftOffset, 0f, glassRadius * 0.82f, lensPaint)

                // Right Lens & Frame
                canvas.drawCircle(rightOffset, 0f, glassRadius, framePaint)
                canvas.drawCircle(rightOffset, 0f, glassRadius * 0.82f, lensPaint)

                // Bridge
                canvas.drawLine(leftOffset, 0f, rightOffset, 0f, bridgePaint)
            }

            FacePropType.CAT_EARS -> {
                // Adorable Cat Ears anchored to head top
                val headTopX = bounds.centerX()
                val headTopY = bounds.top + bounds.height() * 0.08f
                canvas.translate(headTopX, headTopY)
                canvas.rotate(rollAngle)

                val earSize = bounds.width() * 0.32f
                val earSpread = bounds.width() * 0.38f

                val outerEarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = Color.parseColor("#FF69B4")
                }
                val innerEarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = Color.parseColor("#FFF0F5")
                }

                // Left Ear Path
                val leftPath = Path().apply {
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
                canvas.drawPath(leftPath, outerEarPaint)
                canvas.drawPath(leftInnerPath, innerEarPaint)

                // Right Ear Path
                val rightPath = Path().apply {
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
                canvas.drawPath(rightPath, outerEarPaint)
                canvas.drawPath(rightInnerPath, innerEarPaint)
            }

            FacePropType.PARTY_CROWN -> {
                // Festive Gold Crown
                val headTopX = bounds.centerX()
                val headTopY = bounds.top - bounds.height() * 0.05f
                canvas.translate(headTopX, headTopY)
                canvas.rotate(rollAngle)

                val crownWidth = bounds.width() * 0.6f
                val crownHeight = crownWidth * 0.55f

                val crownPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = Color.parseColor("#FFD700")
                }
                val gemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = Color.parseColor("#FF007A")
                }

                val crownPath = Path().apply {
                    moveTo(-crownWidth / 2f, 0f)
                    lineTo(-crownWidth / 2f, -crownHeight * 0.8f)
                    lineTo(-crownWidth * 0.25f, -crownHeight * 0.4f)
                    lineTo(0f, -crownHeight)
                    lineTo(crownWidth * 0.25f, -crownHeight * 0.4f)
                    lineTo(crownWidth / 2f, -crownHeight * 0.8f)
                    lineTo(crownWidth / 2f, 0f)
                    close()
                }
                canvas.drawPath(crownPath, crownPaint)
                canvas.drawCircle(0f, -crownHeight * 0.85f, crownWidth * 0.06f, gemPaint)
                canvas.drawCircle(-crownWidth * 0.45f, -crownHeight * 0.7f, crownWidth * 0.05f, gemPaint)
                canvas.drawCircle(crownWidth * 0.45f, -crownHeight * 0.7f, crownWidth * 0.05f, gemPaint)
            }

            FacePropType.GENTLEMAN_MOUSTACHE -> {
                // Curled Moustache between nose and mouth
                val mX = nose?.x ?: bounds.centerX()
                val mY = if (nose != null && mouth != null) (nose.y + mouth.y) / 2f else bounds.centerY() + bounds.height() * 0.2f
                canvas.translate(mX, mY)
                canvas.rotate(rollAngle)

                val mWidth = bounds.width() * 0.45f
                val mPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = Color.parseColor("#271B11")
                }

                val mPath = Path().apply {
                    moveTo(-mWidth / 2f, -mWidth * 0.1f)
                    quadTo(-mWidth * 0.25f, mWidth * 0.2f, 0f, 0f)
                    quadTo(mWidth * 0.25f, mWidth * 0.2f, mWidth / 2f, -mWidth * 0.1f)
                    quadTo(mWidth * 0.35f, -mWidth * 0.05f, 0f, mWidth * 0.08f)
                    quadTo(-mWidth * 0.35f, -mWidth * 0.05f, -mWidth / 2f, -mWidth * 0.1f)
                    close()
                }
                canvas.drawPath(mPath, mPaint)
            }

            FacePropType.FLOATING_HEARTS -> {
                // Floating Hearts above head
                val headX = bounds.centerX()
                val headY = bounds.top - bounds.height() * 0.18f
                canvas.translate(headX, headY)
                canvas.rotate(rollAngle)

                val heartSize = bounds.width() * 0.18f
                drawHeart(canvas, -bounds.width() * 0.28f, -heartSize * 0.4f, heartSize * 0.85f, Color.parseColor("#FF2E93"))
                drawHeart(canvas, 0f, -heartSize * 0.9f, heartSize * 1.15f, Color.parseColor("#FF4D8D"))
                drawHeart(canvas, bounds.width() * 0.28f, -heartSize * 0.3f, heartSize * 0.9f, Color.parseColor("#FF2E93"))
            }

            FacePropType.SPARKLE_AURA -> {
                // Glittering Sparkles near eyes and cheeks
                canvas.translate(eyeCenter.x, eyeCenter.y)
                canvas.rotate(rollAngle)

                val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = Color.parseColor("#FFE600")
                }

                val spread = eyeDist * 1.35f
                drawStar(canvas, -spread, -eyeDist * 0.4f, eyeDist * 0.22f, starPaint)
                drawStar(canvas, spread, -eyeDist * 0.35f, eyeDist * 0.20f, starPaint)
                drawStar(canvas, 0f, -eyeDist * 0.8f, eyeDist * 0.25f, starPaint)
                drawStar(canvas, -spread * 0.8f, eyeDist * 0.6f, eyeDist * 0.18f, starPaint)
                drawStar(canvas, spread * 0.8f, eyeDist * 0.55f, eyeDist * 0.16f, starPaint)
            }

            FacePropType.FLORAL_CROWN -> {
                // Flower Crown Wreath
                val headX = bounds.centerX()
                val headY = bounds.top + bounds.height() * 0.05f
                canvas.translate(headX, headY)
                canvas.rotate(rollAngle)

                val wreathWidth = bounds.width() * 0.8f
                val stemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = wreathWidth * 0.035f
                    color = Color.parseColor("#2E7D32")
                }
                canvas.drawLine(-wreathWidth / 2f, 0f, wreathWidth / 2f, 0f, stemPaint)

                val flowerColors = intArrayOf(
                    Color.parseColor("#FF4081"),
                    Color.parseColor("#FF80AB"),
                    Color.parseColor("#FFEB3B"),
                    Color.parseColor("#FF4081"),
                    Color.parseColor("#FF80AB")
                )

                val flowerCount = 5
                val step = wreathWidth / (flowerCount - 1)
                for (i in 0 until flowerCount) {
                    val fx = -wreathWidth / 2f + i * step
                    val fPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.FILL
                        color = flowerColors[i]
                    }
                    val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.FILL
                        color = Color.parseColor("#FFF59D")
                    }
                    val fRadius = wreathWidth * 0.08f
                    canvas.drawCircle(fx, 0f, fRadius, fPaint)
                    canvas.drawCircle(fx, 0f, fRadius * 0.4f, centerPaint)
                }
            }

            FacePropType.ANGEL_HALO -> {
                // Radiant Halo above head
                val headX = bounds.centerX()
                val headY = bounds.top - bounds.height() * 0.25f
                canvas.translate(headX, headY)
                canvas.rotate(rollAngle)

                val haloWidth = bounds.width() * 0.65f
                val haloHeight = haloWidth * 0.28f

                val haloGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = haloHeight * 0.35f
                    color = Color.argb(130, 255, 235, 59)
                }
                val haloCore = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = haloHeight * 0.18f
                    color = Color.parseColor("#FFFDE7")
                }

                val oval = RectF(-haloWidth / 2f, -haloHeight / 2f, haloWidth / 2f, haloHeight / 2f)
                canvas.drawOval(oval, haloGlow)
                canvas.drawOval(oval, haloCore)
            }

            FacePropType.STAR_BLUSH -> {
                // Cute stars on cheeks
                val leftCheek = face.leftCheek ?: PointF(bounds.left + bounds.width() * 0.25f, bounds.centerY() + bounds.height() * 0.1f)
                val rightCheek = face.rightCheek ?: PointF(bounds.right - bounds.width() * 0.25f, bounds.centerY() + bounds.height() * 0.1f)

                val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = Color.parseColor("#FF69B4")
                }

                drawStar(canvas, leftCheek.x, leftCheek.y, bounds.width() * 0.09f, starPaint)
                drawStar(canvas, rightCheek.x, rightCheek.y, bounds.width() * 0.09f, starPaint)
            }

            FacePropType.NONE -> {}
        }

        canvas.restore()
    }

    private fun drawHeart(canvas: Canvas, x: Float, y: Float, size: Float, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            this.color = color
        }
        val path = Path().apply {
            moveTo(x, y + size * 0.3f)
            cubicTo(x - size * 0.5f, y - size * 0.4f, x - size, y + size * 0.1f, x, y + size)
            cubicTo(x + size, y + size * 0.1f, x + size * 0.5f, y - size * 0.4f, x, y + size * 0.3f)
            close()
        }
        canvas.drawPath(path, paint)
    }

    private fun drawStar(canvas: Canvas, cx: Float, cy: Float, radius: Float, paint: Paint) {
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
        canvas.drawPath(path, paint)
    }
}
