package com.example.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun SnaporaBottomBar(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    unreadChatCount: Int = 0
) {
    Surface(
        color = DarkSurface.copy(alpha = 0.95f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(DarkCardBorder, Color.Transparent))),
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Chat Tab
            NavTabItem(
                label = "Chat",
                selected = currentTab == MainTab.CHAT,
                selectedIcon = Icons.Filled.ChatBubble,
                unselectedIcon = Icons.Outlined.ChatBubbleOutline,
                badgeCount = unreadChatCount,
                testTag = "nav_tab_chat",
                onClick = { onTabSelected(MainTab.CHAT) }
            )

            // Central Camera Action Button
            Box(
                modifier = Modifier
                    .offset(y = (-14).dp)
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            listOf(SnaporaPurple, SnaporaPink, SnaporaCyan, SnaporaPurple)
                        )
                    )
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(if (currentTab == MainTab.CAMERA) SnaporaPink else Color.Black)
                    .clickable { onTabSelected(MainTab.CAMERA) }
                    .testTag("nav_tab_camera"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Camera",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }

            // Stories Tab
            NavTabItem(
                label = "Stories",
                selected = currentTab == MainTab.STORIES,
                selectedIcon = Icons.Filled.AutoStories,
                unselectedIcon = Icons.Outlined.AutoStories,
                testTag = "nav_tab_stories",
                onClick = { onTabSelected(MainTab.STORIES) }
            )

            // Profile Tab
            NavTabItem(
                label = "Profile",
                selected = currentTab == MainTab.PROFILE,
                selectedIcon = Icons.Filled.Person,
                unselectedIcon = Icons.Outlined.Person,
                testTag = "nav_tab_profile",
                onClick = { onTabSelected(MainTab.PROFILE) }
            )
        }
    }
}

@Composable
private fun NavTabItem(
    label: String,
    selected: Boolean,
    selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    badgeCount: Int = 0,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp)
            .testTag(testTag)
    ) {
        BadgedBox(
            badge = {
                if (badgeCount > 0) {
                    Badge(containerColor = SnaporaPink) {
                        Text("$badgeCount", fontSize = 10.sp)
                    }
                }
            }
        ) {
            Icon(
                imageVector = if (selected) selectedIcon else unselectedIcon,
                contentDescription = label,
                tint = if (selected) SnaporaPink else TextSecondary,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else TextSecondary
        )
    }
}
