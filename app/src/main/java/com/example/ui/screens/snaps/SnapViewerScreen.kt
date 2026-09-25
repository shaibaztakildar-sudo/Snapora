package com.example.ui.screens.snaps

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.SnapEntity
import com.example.data.repository.SnapRepository
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun SnapViewerScreen(
    snapId: Long,
    currentUserId: Long,
    snapRepository: SnapRepository,
    onClose: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var snap by remember { mutableStateOf<SnapEntity?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(snapId) {
        val result = snapRepository.openSnap(snapId, currentUserId)
        if (result.isSuccess) {
            val openedSnap = result.getOrNull()
            snap = openedSnap
            if (openedSnap != null) {
                val totalMs = openedSnap.durationSeconds * 1000L
                val startTime = System.currentTimeMillis()
                while (progress > 0f) {
                    delay(50L)
                    val elapsed = System.currentTimeMillis() - startTime
                    progress = (1f - (elapsed.toFloat() / totalMs)).coerceIn(0f, 1f)
                    if (progress <= 0f) {
                        break
                    }
                }
                // Expire backend
                snapRepository.expireSnap(snapId)
                onClose()
            }
        } else {
            errorMessage = result.exceptionOrNull()?.message ?: "Unable to view snap"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable {
                // Tapping anywhere dismisses and immediately expires
                coroutineScope.launch {
                    snapRepository.expireSnap(snapId)
                    onClose()
                }
            }
    ) {
        if (errorMessage != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = SnaporaAmber,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage ?: "",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = SnaporaPurple)
                ) {
                    Text("Go Back")
                }
            }
        } else if (snap != null) {
            val currentSnap = snap!!

            // Fullscreen Snap Image
            AsyncImage(
                model = File(currentSnap.mediaUri),
                contentDescription = "Disappearing Snap",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Optional Filter Tint
            val filterColor = when (currentSnap.filter) {
                "Warm" -> Color(0xFFFF9900).copy(alpha = 0.18f)
                "Neon" -> SnaporaPink.copy(alpha = 0.18f)
                "Cyber" -> SnaporaCyan.copy(alpha = 0.18f)
                "Mono" -> Color.Black.copy(alpha = 0.25f)
                else -> Color.Transparent
            }
            if (filterColor != Color.Transparent) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(filterColor)
                )
            }

            // Caption Overlay
            if (currentSnap.caption.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(vertical = 12.dp, horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentSnap.caption,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            // Emoji Sticker Overlay
            if (currentSnap.emojiOverlay.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 90.dp, end = 24.dp)
                ) {
                    Text(text = currentSnap.emojiOverlay, fontSize = 54.sp)
                }
            }

            // Top Countdown Progress Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = SnaporaPink,
                    trackColor = Color.White.copy(alpha = 0.3f),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Disappearing Snap",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                snapRepository.expireSnap(snapId)
                                onClose()
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SnaporaPurple)
            }
        }
    }
}
