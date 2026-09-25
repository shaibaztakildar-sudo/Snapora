package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.SnaporaApplication
import com.example.data.model.UserEntity
import com.example.ui.navigation.MainTab
import com.example.ui.navigation.SnaporaBottomBar
import com.example.ui.screens.camera.CameraScreen
import com.example.ui.screens.chat.ChatListScreen
import com.example.ui.screens.friends.UserSearchAndFriendsSheet
import com.example.ui.screens.notifications.NotificationsSheet
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.report.ReportDialog
import com.example.ui.screens.stories.StoriesScreen
import com.example.ui.theme.*

@Composable
fun MainScreen(
    app: SnaporaApplication,
    currentUser: UserEntity,
    onMediaCaptured: (mediaPath: String, mediaType: String) -> Unit,
    onOpenConversation: (otherUserId: Long) -> Unit,
    onOpenStoryViewer: (authorUserId: Long) -> Unit,
    onOpenAdmin: () -> Unit,
    onLoggedOut: () -> Unit
) {
    var currentTab by remember { mutableStateOf(MainTab.CAMERA) }

    var showSearchSheet by remember { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }
    var activeReportTarget by remember { mutableStateOf<Pair<String, Long>?>(null) }

    val unreadNotifications by app.notificationRepository.getUnreadCount(currentUser.id).collectAsState(initial = 0)
    val conversations by app.chatRepository.getConversations(currentUser.id).collectAsState(initial = emptyList())
    val totalUnreadChats = remember(conversations) { conversations.sumOf { it.unreadCount } }

    Scaffold(
        topBar = {
            // Top App Bar (visible on Chat, Stories, and Profile; hidden on full-screen Camera)
            if (currentTab != MainTab.CAMERA) {
                Surface(
                    color = DarkBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Snapora Logo & Title
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(SnaporaPurple, SnaporaPink))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Snapora",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Right Top Action Icons: Admin Shield, Search, Notifications
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (currentUser.role == "admin") {
                                IconButton(
                                    onClick = onOpenAdmin,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(SnaporaAmber.copy(alpha = 0.2f))
                                        .testTag("admin_top_action_button")
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = "Admin Panel", tint = SnaporaAmber, modifier = Modifier.size(20.dp))
                                }
                            }

                            IconButton(
                                onClick = { showSearchSheet = true },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(DarkSurfaceVariant)
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White, modifier = Modifier.size(20.dp))
                            }

                            BadgedBox(
                                badge = {
                                    if (unreadNotifications > 0) {
                                        Badge(containerColor = SnaporaPink) {
                                            Text("$unreadNotifications")
                                        }
                                    }
                                }
                            ) {
                                IconButton(
                                    onClick = { showNotificationsSheet = true },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(DarkSurfaceVariant)
                                        .testTag("notifications_button")
                                ) {
                                    Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            SnaporaBottomBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it },
                unreadChatCount = totalUnreadChats
            )
        },
        containerColor = DarkBg
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (currentTab == MainTab.CAMERA) 0.dp else paddingValues.calculateBottomPadding())
        ) {
            Crossfade(targetState = currentTab, label = "TabSwitch") { tab ->
                when (tab) {
                    MainTab.CHAT -> {
                        ChatListScreen(
                            currentUserId = currentUser.id,
                            chatRepository = app.chatRepository,
                            socialRepository = app.socialRepository,
                            onOpenConversation = onOpenConversation,
                            onOpenUserSearch = { showSearchSheet = true },
                            onOpenFriendRequests = { showSearchSheet = true }
                        )
                    }
                    MainTab.CAMERA -> {
                        CameraScreen(
                            mediaStorageManager = app.mediaStorageManager,
                            onMediaCaptured = onMediaCaptured
                        )
                    }
                    MainTab.STORIES -> {
                        StoriesScreen(
                            currentUserId = currentUser.id,
                            storyRepository = app.storyRepository,
                            onOpenStoryViewer = onOpenStoryViewer,
                            onLaunchCamera = { currentTab = MainTab.CAMERA }
                        )
                    }
                    MainTab.PROFILE -> {
                        ProfileScreen(
                            currentUser = currentUser,
                            authRepository = app.authRepository,
                            socialRepository = app.socialRepository,
                            storyRepository = app.storyRepository,
                            mediaStorageManager = app.mediaStorageManager,
                            onOpenAdmin = onOpenAdmin,
                            onOpenUserSearch = { showSearchSheet = true },
                            onLoggedOut = onLoggedOut
                        )
                    }
                }
            }
        }
    }

    // Search & Friend Management Sheet
    if (showSearchSheet) {
        UserSearchAndFriendsSheet(
            currentUserId = currentUser.id,
            userDao = app.database.userDao(),
            socialRepository = app.socialRepository,
            onStartChat = { otherId ->
                showSearchSheet = false
                onOpenConversation(otherId)
            },
            onDismiss = { showSearchSheet = false }
        )
    }

    // Notifications Sheet
    if (showNotificationsSheet) {
        NotificationsSheet(
            currentUserId = currentUser.id,
            notificationRepository = app.notificationRepository,
            onOpenChat = { senderId ->
                showNotificationsSheet = false
                onOpenConversation(senderId)
            },
            onDismiss = { showNotificationsSheet = false }
        )
    }

    // Report Dialog
    if (activeReportTarget != null) {
        val (targetType, targetId) = activeReportTarget!!
        ReportDialog(
            reporterId = currentUser.id,
            targetType = targetType,
            targetId = targetId,
            reportRepository = app.reportRepository,
            onDismiss = { activeReportTarget = null }
        )
    }
}
