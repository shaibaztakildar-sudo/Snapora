package com.example.ui.screens.stories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.UserEntity
import com.example.data.repository.StoryRepository
import com.example.data.repository.UserStoryGroup
import com.example.ui.theme.*
import java.io.File

@Composable
fun StoriesScreen(
    currentUserId: Long,
    storyRepository: StoryRepository,
    onOpenStoryViewer: (userId: Long) -> Unit,
    onLaunchCamera: () -> Unit
) {
    val storyGroups by storyRepository.getActiveStoryGroups(currentUserId).collectAsState(initial = emptyList())
    val myGroup = storyGroups.find { it.user.id == currentUserId }
    val friendGroups = storyGroups.filter { it.user.id != currentUserId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Stories",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "24h daily moments",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            IconButton(
                onClick = onLaunchCamera,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(SnaporaPurple)
            ) {
                Icon(Icons.Default.AddAPhoto, contentDescription = "Add Story", tint = Color.White)
            }
        }

        // Horizontal Story Circles Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // My Story item
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        if (myGroup != null && myGroup.stories.isNotEmpty()) {
                            onOpenStoryViewer(currentUserId)
                        } else {
                            onLaunchCamera()
                        }
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .then(
                                if (myGroup != null && myGroup.stories.isNotEmpty()) {
                                    Modifier.border(
                                        3.dp,
                                        Brush.sweepGradient(listOf(SnaporaPurple, SnaporaPink, SnaporaCyan, SnaporaPurple)),
                                        CircleShape
                                    )
                                } else {
                                    Modifier.border(2.dp, DarkCardBorder, CircleShape)
                                }
                            )
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (myGroup != null && myGroup.stories.isNotEmpty()) {
                            AsyncImage(
                                model = File(myGroup.stories.last().mediaUri),
                                contentDescription = "My Story",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(Icons.Default.Add, contentDescription = null, tint = SnaporaPurpleLight, modifier = Modifier.size(28.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (myGroup != null && myGroup.stories.isNotEmpty()) "My Story" else "Add Story",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Friends' Stories
            items(friendGroups) { group ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        onOpenStoryViewer(group.user.id)
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .border(
                                3.dp,
                                Brush.sweepGradient(listOf(SnaporaPink, SnaporaPurple, SnaporaCyan, SnaporaPink)),
                                CircleShape
                            )
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (group.stories.isNotEmpty()) {
                            AsyncImage(
                                model = File(group.stories.last().mediaUri),
                                contentDescription = group.user.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = group.user.displayName.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = group.user.displayName.split(" ").firstOrNull() ?: group.user.displayName,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recent Stories Feed Cards
        Text(
            text = "Active Stories (${storyGroups.sumOf { it.stories.size }})",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        if (storyGroups.isEmpty() || storyGroups.all { it.stories.isEmpty() }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No active stories yet", color = TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Capture a photo or video snap and post to My Story!", color = TextMuted, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onLaunchCamera,
                        colors = ButtonDefaults.buttonColors(containerColor = SnaporaPurple),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Open Camera")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(storyGroups) { group ->
                    if (group.stories.isNotEmpty()) {
                        val latestStory = group.stories.last()
                        Surface(
                            onClick = { onOpenStoryViewer(group.user.id) },
                            color = DarkSurface,
                            shape = RoundedCornerShape(18.dp),
                            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(DarkCardBorder, DarkCardBorder))),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(DarkSurfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = File(latestStory.mediaUri),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = group.user.displayName,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = if (latestStory.caption.isNotBlank()) latestStory.caption else "${group.stories.size} snaps posted",
                                        color = TextSecondary,
                                        fontSize = 13.sp,
                                        maxLines = 1
                                    )
                                    val hoursRemaining = ((latestStory.expiresAt - System.currentTimeMillis()) / (1000 * 3600)).coerceAtLeast(0)
                                    Text(
                                        text = "Expires in ${hoursRemaining}h",
                                        color = SnaporaPinkLight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
                            }
                        }
                    }
                }
            }
        }
    }
}
