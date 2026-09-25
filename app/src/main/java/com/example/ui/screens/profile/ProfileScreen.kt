package com.example.ui.screens.profile

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.data.repository.AuthRepository
import com.example.data.repository.MediaStorageManager
import com.example.data.repository.SocialRepository
import com.example.data.repository.StoryRepository
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    currentUser: UserEntity,
    authRepository: AuthRepository,
    socialRepository: SocialRepository,
    storyRepository: StoryRepository,
    mediaStorageManager: MediaStorageManager,
    onOpenAdmin: () -> Unit,
    onOpenUserSearch: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val friends by socialRepository.getFriends(currentUser.id).collectAsState(initial = emptyList())
    val myStories by storyRepository.getActiveStoryGroups(currentUser.id).collectAsState(initial = emptyList())
    val activeStoryCount = myStories.find { it.user.id == currentUser.id }?.stories?.size ?: 0

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var editDisplayName by remember { mutableStateOf(currentUser.displayName) }
    var editBio by remember { mutableStateOf(currentUser.bio) }

    // Avatar Photo Picker
    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val savedPath = mediaStorageManager.copyUriToInternal(uri, "avatar")
                if (savedPath != null) {
                    authRepository.updateProfile(
                        userId = currentUser.id,
                        displayName = currentUser.displayName,
                        bio = currentUser.bio,
                        avatarUri = savedPath
                    )
                    Toast.makeText(context, "Profile photo updated!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Profile",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // If user is Admin, show Admin Shield Icon
                if (currentUser.role == "admin") {
                    IconButton(
                        onClick = onOpenAdmin,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(SnaporaAmber)
                            .testTag("admin_panel_icon_button")
                    ) {
                        Icon(Icons.Default.Security, contentDescription = "Admin Panel", tint = Color.Black)
                    }
                }

                IconButton(
                    onClick = {
                        authRepository.logOut()
                        onLoggedOut()
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = "Logout", tint = SnaporaRed)
                }
            }
        }

        // Profile Avatar, Display Name, Username, Bio
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .clip(CircleShape)
                    .border(
                        3.dp,
                        Brush.sweepGradient(listOf(SnaporaPurple, SnaporaPink, SnaporaCyan, SnaporaPurple)),
                        CircleShape
                    )
                    .padding(5.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
                    .clickable {
                        avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                contentAlignment = Alignment.Center
            ) {
                if (currentUser.avatarUri != null) {
                    AsyncImage(
                        model = File(currentUser.avatarUri),
                        contentDescription = "Profile Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = currentUser.displayName.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Camera badge icon on bottom right
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(SnaporaPink),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Change photo", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = currentUser.displayName,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "@${currentUser.username}",
                    fontSize = 14.sp,
                    color = SnaporaPurpleLight,
                    fontWeight = FontWeight.SemiBold
                )
                if (currentUser.role == "admin") {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = SnaporaAmber.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "OWNER/ADMIN",
                            color = SnaporaAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (currentUser.bio.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentUser.bio,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    editDisplayName = currentUser.displayName
                    editBio = currentUser.bio
                    showEditProfileDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Edit Profile", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Stats Cards: Snap Score, Friends, Active Stories
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                color = DarkSurface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🔥 ${currentUser.snapScore}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Snap Score", color = TextSecondary, fontSize = 11.sp)
                }
            }

            Surface(
                color = DarkSurface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpenUserSearch() }
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "👥 ${friends.size}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Friends", color = TextSecondary, fontSize = 11.sp)
                }
            }

            Surface(
                color = DarkSurface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "📸 $activeStoryCount", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Active Stories", color = TextSecondary, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Admin Access Card (if user is admin)
        if (currentUser.role == "admin") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clickable { onOpenAdmin() }
                    .testTag("admin_panel_card"),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(SnaporaAmber, SnaporaPurple))),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(SnaporaAmber.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = SnaporaAmber)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Private Admin Dashboard", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Platform metrics, user suspension, report moderation", color = TextSecondary, fontSize = 12.sp)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SnaporaAmber)
                }
            }
        }

        // Account Switcher for Testing (Chloe, Leo, Admin)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Switch Account (Instant Multi-User Test):",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = {
                            coroutineScope.launch {
                                authRepository.logIn("admin@snapora.com", "AdminPass123!")
                                authRepository.refreshCurrentUser()
                            }
                        },
                        label = { Text("Owner (Admin)", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Security, contentDescription = null, tint = SnaporaAmber, modifier = Modifier.size(14.dp)) }
                    )
                    AssistChip(
                        onClick = {
                            coroutineScope.launch {
                                authRepository.logIn("chloe_vibe", "UserPass123!")
                                authRepository.refreshCurrentUser()
                            }
                        },
                        label = { Text("Chloe", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = SnaporaPink, modifier = Modifier.size(14.dp)) }
                    )
                    AssistChip(
                        onClick = {
                            coroutineScope.launch {
                                authRepository.logIn("leo_lens", "UserPass123!")
                                authRepository.refreshCurrentUser()
                            }
                        },
                        label = { Text("Leo", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Camera, contentDescription = null, tint = SnaporaCyan, modifier = Modifier.size(14.dp)) }
                    )
                }
            }
        }

        // Settings / Account Info Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Account Security & Info", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Email", color = TextSecondary, fontSize = 13.sp)
                    Text(currentUser.email, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Role", color = TextSecondary, fontSize = 13.sp)
                    Text(currentUser.role.uppercase(), color = if (currentUser.role == "admin") SnaporaAmber else SnaporaPurpleLight, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Password Encryption", color = TextSecondary, fontSize = 13.sp)
                    Text("SHA-256 + Salted", color = SnaporaGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Profile", color = Color.White) },
            text = {
                Column {
                    OutlinedTextField(
                        value = editDisplayName,
                        onValueChange = { editDisplayName = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSurfaceVariant,
                            unfocusedContainerColor = DarkSurfaceVariant,
                            focusedBorderColor = SnaporaPurple,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editBio,
                        onValueChange = { editBio = it },
                        label = { Text("Bio") },
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSurfaceVariant,
                            unfocusedContainerColor = DarkSurfaceVariant,
                            focusedBorderColor = SnaporaPurple,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            authRepository.updateProfile(
                                userId = currentUser.id,
                                displayName = editDisplayName,
                                bio = editBio,
                                avatarUri = currentUser.avatarUri
                            )
                            showEditProfileDialog = false
                            Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SnaporaPurple)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = DarkSurface
        )
    }
}
