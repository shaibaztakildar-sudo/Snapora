package com.example.ui.screens.notifications

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.NotificationRepository
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSheet(
    currentUserId: Long,
    notificationRepository: NotificationRepository,
    onOpenChat: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val notifications by notificationRepository.getNotifications(currentUserId).collectAsState(initial = emptyList())
    val timeFormat = remember { SimpleDateFormat("h:mm a, MMM d", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        scrimColor = Color.Black.copy(alpha = 0.65f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Notifications",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                if (notifications.any { !it.isRead }) {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                notificationRepository.markAllAsRead(currentUserId)
                            }
                        }
                    ) {
                        Text("Mark all read", color = SnaporaPurpleLight, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.NotificationsNone,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No notifications yet", color = TextSecondary, fontSize = 15.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notifications) { item ->
                        Surface(
                            onClick = {
                                coroutineScope.launch {
                                    notificationRepository.markAsRead(item.id)
                                    if (item.senderId != null && (item.type == "MESSAGE" || item.type == "SNAP")) {
                                        onDismiss()
                                        onOpenChat(item.senderId)
                                    }
                                }
                            },
                            color = if (!item.isRead) DarkSurfaceVariant else DarkSurfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val (icon, tint) = when (item.type) {
                                    "SNAP" -> Pair(Icons.Default.CameraAlt, SnaporaPink)
                                    "MESSAGE" -> Pair(Icons.Default.Chat, SnaporaPurple)
                                    "FRIEND_REQ" -> Pair(Icons.Default.PersonAdd, SnaporaCyan)
                                    "FRIEND_ACC" -> Pair(Icons.Default.CheckCircle, SnaporaGreen)
                                    "STORY_VIEW" -> Pair(Icons.Default.Visibility, SnaporaAmber)
                                    else -> Pair(Icons.Default.Notifications, SnaporaPurpleLight)
                                }

                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(tint.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(item.body, color = TextSecondary, fontSize = 12.sp)
                                    Text(timeFormat.format(Date(item.createdAt)), color = TextMuted, fontSize = 10.sp)
                                }

                                if (!item.isRead) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(SnaporaPink)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
