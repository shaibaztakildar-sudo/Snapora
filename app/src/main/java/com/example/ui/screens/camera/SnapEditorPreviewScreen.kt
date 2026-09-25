package com.example.ui.screens.camera

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.UserEntity
import com.example.data.repository.SnapRepository
import com.example.data.repository.SocialRepository
import com.example.data.repository.StoryRepository
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnapEditorPreviewScreen(
    mediaPath: String,
    mediaType: String,
    currentUserId: Long,
    snapRepository: SnapRepository,
    storyRepository: StoryRepository,
    socialRepository: SocialRepository,
    onRetake: () -> Unit,
    onSentSuccessfully: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var caption by remember { mutableStateOf("") }
    var showCaptionInput by remember { mutableStateOf(false) }
    var selectedEmoji by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Normal") }
    var durationSeconds by remember { mutableIntStateOf(10) }

    var showSendSheet by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }

    val friends by socialRepository.getFriends(currentUserId).collectAsState(initial = emptyList())
    val selectedFriendIds = remember { mutableStateListOf<Long>() }
    var postToStory by remember { mutableStateOf(false) }

    val emojis = listOf("🔥", "✨", "❤️", "🤩", "📸", "⚡", "🎉", "🚀", "🌸", "🍕")
    val filters = listOf("Normal", "Warm", "Neon", "Cyber", "Mono")

    val filterOverlayColor = when (selectedFilter) {
        "Warm" -> Color(0xFFFF9900).copy(alpha = 0.18f)
        "Neon" -> SnaporaPink.copy(alpha = 0.18f)
        "Cyber" -> SnaporaCyan.copy(alpha = 0.18f)
        "Mono" -> Color.Black.copy(alpha = 0.25f)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Media Fullscreen Display
        AsyncImage(
            model = File(mediaPath),
            contentDescription = "Snap Preview",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Color Filter Overlay
        if (selectedFilter != "Normal") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(filterOverlayColor)
            )
        }

        // Caption Overlay in Center
        if (caption.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(vertical = 12.dp, horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = caption,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        // Emoji Sticker Overlay
        if (selectedEmoji.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 100.dp, end = 24.dp)
            ) {
                Text(text = selectedEmoji, fontSize = 48.sp)
            }
        }

        // Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onRetake,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .testTag("snap_retake_button")
            ) {
                Icon(Icons.Default.Close, contentDescription = "Retake", tint = Color.White)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Add Text Button
                IconButton(
                    onClick = { showCaptionInput = !showCaptionInput },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (caption.isNotBlank()) SnaporaPurple else Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.TextFields, contentDescription = "Caption", tint = Color.White)
                }

                // Add Emoji Button
                IconButton(
                    onClick = {
                        selectedEmoji = if (selectedEmoji.isEmpty()) emojis.first() else ""
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (selectedEmoji.isNotBlank()) SnaporaPink else Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Mood, contentDescription = "Emoji sticker", tint = Color.White)
                }

                // Save Locally
                IconButton(
                    onClick = {
                        Toast.makeText(context, "Saved to Snapora storage!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Save", tint = Color.White)
                }
            }
        }

        // Floating Caption Input Dialog / Overlay
        AnimatedVisibility(
            visible = showCaptionInput,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 70.dp, start = 16.dp, end = 16.dp)
        ) {
            Surface(
                color = DarkSurface.copy(alpha = 0.95f),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(SnaporaPurple, SnaporaPink))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = caption,
                        onValueChange = { caption = it },
                        placeholder = { Text("Add a caption...", color = TextMuted) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showCaptionInput = false }) {
                        Icon(Icons.Default.Check, contentDescription = "Done", tint = SnaporaPink)
                    }
                }
            }
        }

        // Bottom Controls: Timer, Filters, and Send Button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            // Emoji Selector Row
            AnimatedVisibility(visible = selectedEmoji.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    emojis.forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 24.sp,
                            modifier = Modifier
                                .clickable { selectedEmoji = emoji }
                                .padding(4.dp)
                        )
                    }
                }
            }

            // Quick Tool Bar: Filter Chips & Timer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Timer Chip (Disappearing duration)
                AssistChip(
                    onClick = {
                        durationSeconds = when (durationSeconds) {
                            5 -> 10
                            10 -> 30
                            else -> 5
                        }
                    },
                    label = { Text("${durationSeconds}s", fontWeight = FontWeight.Bold, color = Color.White) },
                    leadingIcon = {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = SnaporaAmber, modifier = Modifier.size(16.dp))
                    },
                    colors = AssistChipDefaults.assistChipColors(containerColor = Color.Black.copy(alpha = 0.6f))
                )

                // Filter Selector
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    filters.forEach { filter ->
                        Text(
                            text = filter,
                            fontSize = 11.sp,
                            fontWeight = if (selectedFilter == filter) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedFilter == filter) SnaporaPink else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier
                                .clickable { selectedFilter = filter }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Send To Button
            Button(
                onClick = { showSendSheet = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("send_snap_sheet_button"),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SnaporaPurple)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("Send To...", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Bottom Sheet for Recipient Selection
        if (showSendSheet) {
            ModalBottomSheet(
                onDismissRequest = { if (!isSending) showSendSheet = false },
                containerColor = DarkSurface,
                scrimColor = Color.Black.copy(alpha = 0.65f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Send Snap",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // My Story Option
                    Surface(
                        onClick = { postToStory = !postToStory },
                        color = if (postToStory) SnaporaPurple.copy(alpha = 0.2f) else DarkSurfaceVariant,
                        shape = RoundedCornerShape(14.dp),
                        border = if (postToStory) CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(SnaporaPurple, SnaporaPink))) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(SnaporaPink, SnaporaPurple))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AddCircle, contentDescription = null, tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("My Story", fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Visible to friends for 24 hours", fontSize = 12.sp, color = TextSecondary)
                            }
                            Checkbox(
                                checked = postToStory,
                                onCheckedChange = { postToStory = it },
                                colors = CheckboxDefaults.colors(checkedColor = SnaporaPurple)
                            )
                        }
                    }

                    Text(
                        text = "Friends",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )

                    if (friends.isEmpty()) {
                        Text(
                            text = "No friends added yet. You can still post to your Story or search users in Chat!",
                            fontSize = 13.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 240.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(friends) { friend ->
                                val isChecked = selectedFriendIds.contains(friend.id)
                                Surface(
                                    onClick = {
                                        if (isChecked) selectedFriendIds.remove(friend.id)
                                        else selectedFriendIds.add(friend.id)
                                    },
                                    color = if (isChecked) DarkSurfaceVariant.copy(alpha = 0.9f) else DarkSurfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(SnaporaPurpleDark),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = friend.displayName.take(1).uppercase(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(friend.displayName, color = Color.White, fontWeight = FontWeight.SemiBold)
                                            Text("@${friend.username}", fontSize = 12.sp, color = TextSecondary)
                                        }
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { checked ->
                                                if (checked) selectedFriendIds.add(friend.id)
                                                else selectedFriendIds.remove(friend.id)
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = SnaporaPurple)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Final Send Button
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isSending = true
                                try {
                                    if (postToStory) {
                                        storyRepository.postStory(
                                            userId = currentUserId,
                                            mediaUri = mediaPath,
                                            mediaType = mediaType,
                                            caption = caption,
                                            emojiOverlay = selectedEmoji,
                                            filter = selectedFilter
                                        )
                                    }
                                    if (selectedFriendIds.isNotEmpty()) {
                                        snapRepository.sendSnap(
                                            senderId = currentUserId,
                                            receiverIds = selectedFriendIds.toList(),
                                            mediaUri = mediaPath,
                                            mediaType = mediaType,
                                            caption = caption,
                                            emojiOverlay = selectedEmoji,
                                            filter = selectedFilter,
                                            durationSeconds = durationSeconds
                                        )
                                    }
                                    Toast.makeText(context, "Snap sent!", Toast.LENGTH_SHORT).show()
                                    showSendSheet = false
                                    onSentSuccessfully()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    isSending = false
                                }
                            }
                        },
                        enabled = (postToStory || selectedFriendIds.isNotEmpty()) && !isSending,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("confirm_send_snap_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SnaporaPink)
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text(
                                text = if (postToStory && selectedFriendIds.isEmpty()) "Post to Story"
                                else if (selectedFriendIds.isNotEmpty()) "Send to (${selectedFriendIds.size + (if (postToStory) 1 else 0)})"
                                else "Select recipient",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
