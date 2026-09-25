package com.example.ui.screens.chat

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.dao.UserDao
import com.example.data.model.MessageEntity
import com.example.data.model.UserEntity
import com.example.data.repository.ChatRepository
import com.example.data.repository.MediaStorageManager
import com.example.data.repository.SocialRepository
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatDetailScreen(
    currentUserId: Long,
    otherUserId: Long,
    chatRepository: ChatRepository,
    socialRepository: SocialRepository,
    userDao: UserDao,
    mediaStorageManager: MediaStorageManager,
    onBack: () -> Unit,
    onOpenSnap: (snapId: Long) -> Unit,
    onOpenReport: (targetType: String, targetId: Long) -> Unit,
    onLaunchCamera: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var otherUser by remember { mutableStateOf<UserEntity?>(null) }
    var conversationId by remember { mutableLongStateOf(0L) }
    var messageText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var selectedMessageForAction by remember { mutableStateOf<MessageEntity?>(null) }

    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    // Gallery picker for sending photos in chat
    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val path = mediaStorageManager.copyUriToInternal(uri, "chat")
                if (path != null) {
                    chatRepository.sendMessage(
                        senderId = currentUserId,
                        receiverId = otherUserId,
                        text = "",
                        mediaUri = path,
                        mediaType = "IMAGE"
                    )
                }
            }
        }
    }

    LaunchedEffect(otherUserId) {
        otherUser = userDao.getUserByIdSync(otherUserId)
        val conv = chatRepository.getOrCreateConversation(currentUserId, otherUserId)
        conversationId = conv.id
        chatRepository.markAsRead(conv.id, currentUserId)
    }

    val messages by (if (conversationId != 0L) chatRepository.getMessages(conversationId)
    else kotlinx.coroutines.flow.flowOf(emptyList())).collectAsState(initial = emptyList())

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(SnaporaPurple),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (otherUser?.displayName ?: "U").take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = otherUser?.displayName ?: "Chat",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "@${otherUser?.username ?: ""}",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onLaunchCamera) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Snap", tint = SnaporaPink)
                    }
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.White)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        containerColor = DarkSurface
                    ) {
                        DropdownMenuItem(
                            text = { Text("Report User", color = SnaporaRed) },
                            onClick = {
                                showMenu = false
                                onOpenReport("USER", otherUserId)
                            },
                            leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null, tint = SnaporaRed) }
                        )
                        DropdownMenuItem(
                            text = { Text("Block User", color = SnaporaRed) },
                            onClick = {
                                showMenu = false
                                coroutineScope.launch {
                                    socialRepository.blockUser(currentUserId, otherUserId)
                                    Toast.makeText(context, "User blocked", Toast.LENGTH_SHORT).show()
                                    onBack()
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.Block, contentDescription = null, tint = SnaporaRed) }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Message List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { msg ->
                    val isMine = msg.senderId == currentUserId

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
                    ) {
                        if (msg.isDeleted) {
                            Surface(
                                color = DarkSurfaceVariant,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "This message was deleted",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        } else if (msg.mediaType == "SNAP" && msg.snapId != null) {
                            // Disappearing Snap Card in Chat
                            Surface(
                                onClick = { onOpenSnap(msg.snapId) },
                                color = if (isMine) SnaporaPurpleDark else DarkSurfaceVariant,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .widthIn(max = 260.dp)
                                    .combinedClickable(
                                        onClick = { onOpenSnap(msg.snapId) },
                                        onLongClick = { selectedMessageForAction = msg }
                                    )
                                    .testTag("snap_message_card")
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(SnaporaPink),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = if (isMine) "Sent Snap" else "New Snap! 🔥",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = if (isMine) "Opened when viewed" else "Tap to open",
                                            fontSize = 12.sp,
                                            color = if (isMine) TextSecondary else SnaporaPinkLight
                                        )
                                    }
                                }
                            }
                        } else if (msg.mediaType == "IMAGE" && msg.mediaUri != null) {
                            // Photo message
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isMine) SnaporaPurple else DarkSurface,
                                modifier = Modifier
                                    .widthIn(max = 240.dp)
                                    .combinedClickable(
                                        onClick = {},
                                        onLongClick = { selectedMessageForAction = msg }
                                    )
                            ) {
                                Column {
                                    AsyncImage(
                                        model = File(msg.mediaUri),
                                        contentDescription = "Photo message",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                    )
                                    if (msg.text.isNotBlank()) {
                                        Text(
                                            text = msg.text,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            // Standard Text Message
                            Surface(
                                color = if (isMine) SnaporaPurple else DarkSurface,
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isMine) 16.dp else 4.dp,
                                    bottomEnd = if (isMine) 4.dp else 16.dp
                                ),
                                modifier = Modifier
                                    .widthIn(max = 280.dp)
                                    .combinedClickable(
                                        onClick = {},
                                        onLongClick = { selectedMessageForAction = msg }
                                    )
                            ) {
                                Text(
                                    text = msg.text,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                        }

                        // Timestamp & Read Receipt
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
                        ) {
                            Text(
                                text = timeFormat.format(Date(msg.timestamp)),
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                            if (isMine && !msg.isDeleted) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (msg.status == "READ") Icons.Default.DoneAll else Icons.Default.Done,
                                    contentDescription = msg.status,
                                    tint = if (msg.status == "READ") SnaporaCyan else TextMuted,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Input Bar
            Surface(
                color = DarkSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onLaunchCamera,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = SnaporaPink)
                    }

                    IconButton(
                        onClick = {
                            galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", tint = SnaporaPurpleLight)
                    }

                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Send a chat...", color = TextMuted, fontSize = 14.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSurfaceVariant,
                            unfocusedContainerColor = DarkSurfaceVariant,
                            focusedBorderColor = SnaporaPurple,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                            .testTag("chat_input_field"),
                        maxLines = 4
                    )

                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                val text = messageText.trim()
                                messageText = ""
                                coroutineScope.launch {
                                    chatRepository.sendMessage(
                                        senderId = currentUserId,
                                        receiverId = otherUserId,
                                        text = text
                                    )
                                }
                            }
                        },
                        enabled = messageText.isNotBlank(),
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (messageText.isNotBlank()) SnaporaPurple else DarkSurfaceVariant)
                            .testTag("chat_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (messageText.isNotBlank()) Color.White else TextMuted
                        )
                    }
                }
            }
        }

        // Long Press Message Action Dialog (Delete / Report)
        if (selectedMessageForAction != null) {
            val msg = selectedMessageForAction!!
            AlertDialog(
                onDismissRequest = { selectedMessageForAction = null },
                title = { Text("Message Options") },
                text = { Text("Choose an action for this message") },
                confirmButton = {
                    if (msg.senderId == currentUserId && !msg.isDeleted) {
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    chatRepository.deleteMessage(msg.id, currentUserId)
                                    selectedMessageForAction = null
                                    Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text("Delete Message", color = SnaporaRed)
                        }
                    } else if (msg.senderId != currentUserId) {
                        TextButton(
                            onClick = {
                                onOpenReport("MESSAGE", msg.id)
                                selectedMessageForAction = null
                            }
                        ) {
                            Text("Report Message", color = SnaporaRed)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedMessageForAction = null }) {
                        Text("Cancel")
                    }
                },
                containerColor = DarkSurface
            )
        }
    }
}
