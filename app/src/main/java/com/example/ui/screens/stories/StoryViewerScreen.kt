package com.example.ui.screens.stories

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.StoryEntity
import com.example.data.repository.StoryRepository
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryViewerScreen(
    authorUserId: Long,
    currentUserId: Long,
    storyRepository: StoryRepository,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val storyGroups by storyRepository.getActiveStoryGroups(currentUserId).collectAsState(initial = emptyList())
    val authorGroup = storyGroups.find { it.user.id == authorUserId }
    val stories = authorGroup?.stories ?: emptyList()

    var currentIndex by remember { mutableIntStateOf(0) }
    var progress by remember { mutableFloatStateOf(0f) }
    var isPaused by remember { mutableStateOf(false) }
    var showViewersSheet by remember { mutableStateOf(false) }

    val currentStory: StoryEntity? = stories.getOrNull(currentIndex)
    val viewers by (if (currentStory != null) storyRepository.getStoryViewers(currentStory.id)
    else kotlinx.coroutines.flow.flowOf(emptyList())).collectAsState(initial = emptyList())

    val isMyStory = authorUserId == currentUserId

    // Story view record
    LaunchedEffect(currentStory?.id) {
        if (currentStory != null) {
            storyRepository.recordView(currentStory.id, currentUserId)
        }
    }

    // Story progress auto-advance timer
    LaunchedEffect(currentIndex, stories.size, isPaused) {
        if (stories.isNotEmpty() && !isPaused) {
            progress = 0f
            val storyDurationMs = 5000L
            val stepMs = 50L
            val totalSteps = (storyDurationMs / stepMs).toInt()

            for (i in 0..totalSteps) {
                if (isPaused) break
                delay(stepMs)
                progress = (i.toFloat() / totalSteps).coerceIn(0f, 1f)
            }

            if (!isPaused && progress >= 1f) {
                if (currentIndex < stories.size - 1) {
                    currentIndex++
                } else {
                    onClose()
                }
            }
        }
    }

    if (stories.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text("No active stories found", color = Color.White)
        }
        return
    }

    val story = currentStory ?: return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Fullscreen Story Media
        AsyncImage(
            model = File(story.mediaUri),
            contentDescription = "Story Media",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Filter Tint Overlay
        val filterColor = when (story.filter) {
            "Warm" -> Color(0xFFFF9900).copy(alpha = 0.18f)
            "Neon" -> SnaporaPink.copy(alpha = 0.18f)
            "Cyber" -> SnaporaCyan.copy(alpha = 0.18f)
            "Mono" -> Color.Black.copy(alpha = 0.25f)
            else -> Color.Transparent
        }
        if (filterColor != Color.Transparent) {
            Box(modifier = Modifier.fillMaxSize().background(filterColor))
        }

        // Tap gestures for Prev / Next / Pause
        Row(modifier = Modifier.fillMaxSize()) {
            // Left 35% -> Prev
            Box(
                modifier = Modifier
                    .weight(0.35f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (currentIndex > 0) {
                            currentIndex--
                        }
                    }
            )
            // Middle 30% -> Pause/Resume
            Box(
                modifier = Modifier
                    .weight(0.30f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isPaused = !isPaused
                    }
            )
            // Right 35% -> Next
            Box(
                modifier = Modifier
                    .weight(0.35f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (currentIndex < stories.size - 1) {
                            currentIndex++
                        } else {
                            onClose()
                        }
                    }
            )
        }

        // Caption in Center
        if (story.caption.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(vertical = 12.dp, horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = story.caption,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        // Emoji Sticker
        if (story.emojiOverlay.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 100.dp, end = 24.dp)
            ) {
                Text(text = story.emojiOverlay, fontSize = 54.sp)
            }
        }

        // Top Bars: Segmented Progress + Author Info + Actions
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Segmented Progress Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                stories.forEachIndexed { idx, _ ->
                    val segProgress = when {
                        idx < currentIndex -> 1f
                        idx == currentIndex -> progress
                        else -> 0f
                    }
                    LinearProgressIndicator(
                        progress = { segProgress },
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(CircleShape),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.35f)
                    )
                }
            }

            // Header: Author Avatar, Name, Timestamp, Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SnaporaPurple),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (authorGroup?.user?.displayName ?: "S").take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = authorGroup?.user?.displayName ?: "Snapora User",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        val hoursAgo = ((System.currentTimeMillis() - story.createdAt) / (1000 * 3600)).coerceAtLeast(0)
                        Text(
                            text = "${hoursAgo}h ago",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 11.sp
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Delete Story Button (if author or admin)
                    if (isMyStory) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    val res = storyRepository.deleteStory(story.id, currentUserId)
                                    if (res.isSuccess) {
                                        Toast.makeText(context, "Story deleted", Toast.LENGTH_SHORT).show()
                                        onClose()
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete story", tint = SnaporaRed)
                        }
                    }

                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }
        }

        // Bottom Bar: Viewers Count (for author)
        if (isMyStory) {
            Surface(
                onClick = {
                    isPaused = true
                    showViewersSheet = true
                },
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${viewers.size} Viewers",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Viewers Bottom Sheet
        if (showViewersSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showViewersSheet = false
                    isPaused = false
                },
                containerColor = DarkSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Story Viewers (${viewers.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (viewers.isEmpty()) {
                        Text("No one has viewed this story yet.", color = TextSecondary, fontSize = 14.sp)
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 280.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(viewers) { viewerItem ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(SnaporaPink),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = viewerItem.viewer.displayName.take(1).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(viewerItem.viewer.displayName, color = Color.White, fontWeight = FontWeight.SemiBold)
                                        Text("@${viewerItem.viewer.username}", color = TextSecondary, fontSize = 12.sp)
                                    }
                                    Icon(Icons.Default.Check, contentDescription = null, tint = SnaporaGreen, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}
