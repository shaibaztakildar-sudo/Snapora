package com.example.ui.screens.friends

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.dao.UserDao
import com.example.data.model.UserEntity
import com.example.data.repository.SocialRepository
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchAndFriendsSheet(
    currentUserId: Long,
    userDao: UserDao,
    socialRepository: SocialRepository,
    onStartChat: (otherUserId: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    val allSearchResults by userDao.searchUsers(searchQuery).collectAsState(initial = emptyList())
    val searchResults = allSearchResults.filter { it.id != currentUserId }

    val friends by socialRepository.getFriends(currentUserId).collectAsState(initial = emptyList())
    val friendIds = remember(friends) { friends.map { it.id }.toSet() }

    val pendingRequests by socialRepository.getPendingRequests(currentUserId).collectAsState(initial = emptyList())

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
            Text(
                text = "Friends & Community",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tabs: Search, Friends, Requests
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = DarkSurfaceVariant,
                contentColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = SnaporaPurple
                    )
                },
                modifier = Modifier.clip(RoundedCornerShape(14.dp))
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Find Users", fontSize = 13.sp) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Friends (${friends.size})", fontSize = 13.sp) }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("Requests (${pendingRequests.size})", fontSize = 13.sp) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTabIndex) {
                0 -> {
                    // Search Tab
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by @username or name...", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SnaporaPurpleLight) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSurfaceVariant,
                            unfocusedContainerColor = DarkSurfaceVariant,
                            focusedBorderColor = SnaporaPurple,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("user_search_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (searchResults.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isBlank()) "Type a username to discover creators" else "No users found matching '$searchQuery'",
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 350.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(searchResults) { user ->
                                val isFriend = friendIds.contains(user.id)
                                Surface(
                                    color = DarkSurfaceVariant,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(SnaporaPurple),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = user.displayName.take(1).uppercase(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(user.displayName, color = Color.White, fontWeight = FontWeight.Bold)
                                            Text("@${user.username}", color = TextSecondary, fontSize = 12.sp)
                                        }

                                        if (isFriend) {
                                            FilledTonalButton(
                                                onClick = {
                                                    onDismiss()
                                                    onStartChat(user.id)
                                                },
                                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = SnaporaPurple.copy(alpha = 0.3f))
                                            ) {
                                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Chat")
                                            }
                                        } else {
                                            Button(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        val res = socialRepository.sendFriendRequest(currentUserId, user.id)
                                                        if (res.isSuccess) {
                                                            Toast.makeText(context, "Friend request sent to @${user.username}!", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            Toast.makeText(context, res.exceptionOrNull()?.message ?: "Error sending request", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = SnaporaPink),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Add")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Friends Tab
                    if (friends.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("You haven't added any friends yet.", color = TextMuted, fontSize = 14.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 350.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(friends) { friend ->
                                Surface(
                                    color = DarkSurfaceVariant,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(SnaporaPink),
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
                                            Text(friend.displayName, color = Color.White, fontWeight = FontWeight.Bold)
                                            Text("@${friend.username} • Score: ${friend.snapScore}", color = TextSecondary, fontSize = 12.sp)
                                        }

                                        IconButton(
                                            onClick = {
                                                onDismiss()
                                                onStartChat(friend.id)
                                            }
                                        ) {
                                            Icon(Icons.Default.Chat, contentDescription = "Chat", tint = SnaporaCyan)
                                        }

                                        IconButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    socialRepository.removeFriend(currentUserId, friend.id)
                                                    Toast.makeText(context, "Removed friend", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.PersonRemove, contentDescription = "Remove", tint = TextMuted)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Requests Tab
                    if (pendingRequests.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No pending friend requests", color = TextMuted, fontSize = 14.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 350.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(pendingRequests) { (req, sender) ->
                                Surface(
                                    color = DarkSurfaceVariant,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(SnaporaPurple),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = sender.displayName.take(1).uppercase(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(sender.displayName, color = Color.White, fontWeight = FontWeight.Bold)
                                            Text("@${sender.username}", color = TextSecondary, fontSize = 12.sp)
                                        }

                                        IconButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    socialRepository.acceptRequest(req)
                                                    Toast.makeText(context, "Accepted friend request!", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = "Accept", tint = SnaporaGreen)
                                        }

                                        IconButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    socialRepository.rejectRequest(req.id)
                                                    Toast.makeText(context, "Declined request", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Reject", tint = SnaporaRed)
                                        }
                                    }
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
