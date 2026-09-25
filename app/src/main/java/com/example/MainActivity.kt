package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.data.model.UserEntity
import com.example.ui.screens.MainScreen
import com.example.ui.screens.admin.AdminDashboardScreen
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.camera.SnapEditorPreviewScreen
import com.example.ui.screens.chat.ChatDetailScreen
import com.example.ui.screens.report.ReportDialog
import com.example.ui.screens.snaps.SnapViewerScreen
import com.example.ui.screens.stories.StoryViewerScreen
import com.example.ui.theme.DarkBg
import com.example.ui.theme.SnaporaPurple
import com.example.ui.theme.SnaporaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as SnaporaApplication

        setContent {
            SnaporaTheme(darkTheme = true) {
                var isInitialized by remember { mutableStateOf(false) }
                val currentUser by app.authRepository.currentUserState.collectAsState(initial = null)
                val coroutineScope = rememberCoroutineScope()

                // Active full-screen overlay state
                var activeSnapMedia by remember { mutableStateOf<Pair<String, String>?>(null) } // path, type
                var viewingSnapId by remember { mutableStateOf<Long?>(null) }
                var viewingStoryUserId by remember { mutableStateOf<Long?>(null) }
                var activeChatUserId by remember { mutableStateOf<Long?>(null) }
                var isViewingAdmin by remember { mutableStateOf(false) }
                var reportTarget by remember { mutableStateOf<Pair<String, Long>?>(null) }

                LaunchedEffect(Unit) {
                    app.authRepository.initialize()
                    isInitialized = true

                    // Periodically clean up expired snaps & stories
                    while (true) {
                        delay(60_000L)
                        app.snapRepository.cleanupExpiredSnaps()
                        app.storyRepository.cleanupExpiredStories()
                    }
                }

                if (!isInitialized) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DarkBg),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = SnaporaPurple)
                    }
                } else if (currentUser == null) {
                    AuthScreen(
                        authRepository = app.authRepository,
                        onAuthSuccess = {
                            coroutineScope.launch {
                                app.authRepository.refreshCurrentUser()
                            }
                        }
                    )
                } else {
                    val user = currentUser!!

                    // Navigation handling
                    Crossfade(
                        targetState = when {
                            isViewingAdmin -> "ADMIN"
                            viewingSnapId != null -> "SNAP_VIEWER"
                            viewingStoryUserId != null -> "STORY_VIEWER"
                            activeSnapMedia != null -> "SNAP_EDITOR"
                            activeChatUserId != null -> "CHAT_DETAIL"
                            else -> "MAIN"
                        },
                        label = "MainScreenNavigation"
                    ) { screen ->
                        when (screen) {
                            "ADMIN" -> {
                                BackHandler { isViewingAdmin = false }
                                AdminDashboardScreen(
                                    currentUserId = user.id,
                                    currentUserRole = user.role,
                                    adminRepository = app.adminRepository,
                                    onBack = { isViewingAdmin = false }
                                )
                            }
                            "SNAP_VIEWER" -> {
                                val snapId = viewingSnapId ?: return@Crossfade
                                BackHandler { viewingSnapId = null }
                                SnapViewerScreen(
                                    snapId = snapId,
                                    currentUserId = user.id,
                                    snapRepository = app.snapRepository,
                                    onClose = { viewingSnapId = null }
                                )
                            }
                            "STORY_VIEWER" -> {
                                val authorId = viewingStoryUserId ?: return@Crossfade
                                BackHandler { viewingStoryUserId = null }
                                StoryViewerScreen(
                                    authorUserId = authorId,
                                    currentUserId = user.id,
                                    storyRepository = app.storyRepository,
                                    onClose = { viewingStoryUserId = null }
                                )
                            }
                            "SNAP_EDITOR" -> {
                                val (mediaPath, mediaType) = activeSnapMedia ?: return@Crossfade
                                BackHandler { activeSnapMedia = null }
                                SnapEditorPreviewScreen(
                                    mediaPath = mediaPath,
                                    mediaType = mediaType,
                                    currentUserId = user.id,
                                    snapRepository = app.snapRepository,
                                    storyRepository = app.storyRepository,
                                    socialRepository = app.socialRepository,
                                    onRetake = { activeSnapMedia = null },
                                    onSentSuccessfully = { activeSnapMedia = null }
                                )
                            }
                            "CHAT_DETAIL" -> {
                                val otherId = activeChatUserId ?: return@Crossfade
                                BackHandler { activeChatUserId = null }
                                ChatDetailScreen(
                                    currentUserId = user.id,
                                    otherUserId = otherId,
                                    chatRepository = app.chatRepository,
                                    socialRepository = app.socialRepository,
                                    userDao = app.database.userDao(),
                                    mediaStorageManager = app.mediaStorageManager,
                                    onBack = { activeChatUserId = null },
                                    onOpenSnap = { snapId -> viewingSnapId = snapId },
                                    onOpenReport = { targetType, targetId ->
                                        reportTarget = Pair(targetType, targetId)
                                    },
                                    onLaunchCamera = {
                                        activeChatUserId = null
                                    }
                                )
                            }
                            else -> {
                                MainScreen(
                                    app = app,
                                    currentUser = user,
                                    onMediaCaptured = { path, type ->
                                        activeSnapMedia = Pair(path, type)
                                    },
                                    onOpenConversation = { otherId ->
                                        activeChatUserId = otherId
                                    },
                                    onOpenStoryViewer = { authorId ->
                                        viewingStoryUserId = authorId
                                    },
                                    onOpenAdmin = {
                                        isViewingAdmin = true
                                    },
                                    onLoggedOut = {
                                        // User logged out
                                    }
                                )
                            }
                        }
                    }

                    // Global Report Dialog if triggered
                    if (reportTarget != null) {
                        val (targetType, targetId) = reportTarget!!
                        ReportDialog(
                            reporterId = user.id,
                            targetType = targetType,
                            targetId = targetId,
                            reportRepository = app.reportRepository,
                            onDismiss = { reportTarget = null }
                        )
                    }
                }
            }
        }
    }
}
