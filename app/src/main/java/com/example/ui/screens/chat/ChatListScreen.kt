package com.example.ui.screens.chat

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.ChatRepository
import com.example.data.repository.ConversationItem
import com.example.data.repository.SocialRepository
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatListScreen(
    currentUserId: Long,
    chatRepository: ChatRepository,
    socialRepository: SocialRepository,
    onOpenConversation: (otherUserId: Long) -> Unit,
    onOpenUserSearch: () -> Unit,
    onOpenFriendRequests: () -> Unit
) {
    val conversations by chatRepository.getConversations(currentUserId).collectAsState(initial = emptyList())
    val pendingRequests by socialRepository.getPendingRequests(currentUserId).collectAsState(initial = emptyList())

    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }

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
                    text = "Chat",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "Real-time messaging & snaps",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Friend Requests Button (with badge if pending)
                BadgedBox(
                    badge = {
                        if (pendingRequests.isNotEmpty()) {
                            Badge(containerColor = SnaporaPink) {
                                Text("${pendingRequests.size}")
                            }
                        }
                    }
                ) {
                    IconButton(
                        onClick = onOpenFriendRequests,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant)
                    ) {
                        Icon(Icons.Default.GroupAdd, contentDescription = "Friend Requests", tint = Color.White)
                    }
                }

                // Search Users / Start Chat Button
                IconButton(
                    onClick = onOpenUserSearch,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(SnaporaPurple)
                        .testTag("chat_search_button")
                ) {
                    Icon(Icons.Default.PersonSearch, contentDescription = "Search Users", tint = Color.White)
                }
            }
        }

        if (conversations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            tint = SnaporaPurpleLight,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No conversations yet",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Connect with friends or search usernames to start sharing snaps and messages!",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onOpenUserSearch,
                        colors = ButtonDefaults.buttonColors(containerColor = SnaporaPurple),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Search Friends & Users")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(conversations) { item ->
                    val other = item.otherUser
                    val isSnap = item.conversation.lastMessageText.contains("Snap")
                    val isUnread = item.unreadCount > 0

                    Surface(
                        onClick = { onOpenConversation(other.id) },
                        color = if (isUnread) DarkSurfaceVariant else DarkSurface,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("conversation_item_${other.username}")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(SnaporaPurpleDark),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = other.displayName.take(1).uppercase(),
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            // Names & Last message
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = other.displayName,
                                        color = Color.White,
                                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                                        fontSize = 16.sp
                                    )
                                    val date = Date(item.conversation.lastMessageTime)
                                    Text(
                                        text = timeFormat.format(date),
                                        color = if (isUnread) SnaporaPink else TextMuted,
                                        fontSize = 11.sp,
                                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isSnap) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = null,
                                            tint = SnaporaPink,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = item.conversation.lastMessageText,
                                        color = if (isUnread) Color.White else TextSecondary,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal,
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (isUnread) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(SnaporaPink),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${item.unreadCount}",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
