package com.example.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.data.model.UserEntity
import com.example.data.repository.AdminDashboardStats
import com.example.data.repository.AdminRepository
import com.example.data.repository.ReportWithDetails
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    currentUserId: Long,
    currentUserRole: String,
    adminRepository: AdminRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isAuthorized by remember { mutableStateOf(currentUserRole == "admin") }
    var stats by remember { mutableStateOf<AdminDashboardStats?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var userSearchQuery by remember { mutableStateOf("") }
    var allUsers by remember { mutableStateOf<List<UserEntity>>(emptyList()) }
    var reports by remember { mutableStateOf<List<ReportWithDetails>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Backend verification
    LaunchedEffect(currentUserId) {
        try {
            val loadedStats = adminRepository.getStats(currentUserId)
            stats = loadedStats
            isAuthorized = true

            adminRepository.getAllUsers(currentUserId).collect { users ->
                allUsers = users
            }
        } catch (e: Exception) {
            isAuthorized = false
            errorMessage = e.message ?: "Access Denied: You are not authorized as an administrator."
        }
    }

    LaunchedEffect(currentUserId, isAuthorized) {
        if (isAuthorized) {
            try {
                adminRepository.getReports(currentUserId).collect { list ->
                    reports = list
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    if (!isAuthorized) {
        // Strict Authorization Rejection Screen
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Admin Authorization") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
                )
            },
            containerColor = DarkBg
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.GppBad,
                        contentDescription = "Unauthorized",
                        tint = SnaporaRed,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Access Denied",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "You do not have permission to view or execute operations on the Snapora Admin Panel.",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = SnaporaPurple)
                    ) {
                        Text("Return to App")
                    }
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = SnaporaAmber, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Snapora Admin Center", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
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
            // Admin Tabs: Overview, Users, Reports
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkSurfaceVariant,
                contentColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = SnaporaAmber
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Overview", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Users (${allUsers.size})", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Reports (${reports.size})", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                )
            }

            when (selectedTab) {
                0 -> {
                    // Overview Dashboard
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Text(
                                text = "Platform Analytics",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        item {
                            // 2x2 Metric Grid
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    AdminMetricCard(
                                        title = "Total Users",
                                        value = "${stats?.totalUsers ?: allUsers.size}",
                                        icon = Icons.Default.People,
                                        iconTint = SnaporaPurpleLight,
                                        modifier = Modifier.weight(1f)
                                    )
                                    AdminMetricCard(
                                        title = "Suspended Accounts",
                                        value = "${stats?.suspendedUsers ?: allUsers.count { it.isSuspended }}",
                                        icon = Icons.Default.Block,
                                        iconTint = SnaporaRed,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    AdminMetricCard(
                                        title = "Active Stories",
                                        value = "${stats?.activeStories ?: 0}",
                                        icon = Icons.Default.AutoStories,
                                        iconTint = SnaporaCyan,
                                        modifier = Modifier.weight(1f)
                                    )
                                    AdminMetricCard(
                                        title = "Safety Reports",
                                        value = "${stats?.totalReports ?: reports.size}",
                                        icon = Icons.Default.Flag,
                                        iconTint = SnaporaAmber,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Security & Authorization Status", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("• Role check verified on DAO & repository layer", color = SnaporaGreen, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("• Expired snaps media auto-deleted", color = SnaporaGreen, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("• Password hashes protected via PBKDF2/SHA-256", color = SnaporaGreen, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("• Private owner account: snapora_owner", color = TextSecondary, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // User Management Tab
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        OutlinedTextField(
                            value = userSearchQuery,
                            onValueChange = { userSearchQuery = it },
                            placeholder = { Text("Filter users by name/username...", color = TextMuted) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SnaporaAmber) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface,
                                focusedBorderColor = SnaporaAmber,
                                unfocusedBorderColor = DarkCardBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        val filteredUsers = allUsers.filter {
                            it.username.contains(userSearchQuery, ignoreCase = true) ||
                                    it.displayName.contains(userSearchQuery, ignoreCase = true)
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredUsers) { user ->
                                Surface(
                                    color = DarkSurface,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(if (user.role == "admin") SnaporaAmber else SnaporaPurple),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = user.displayName.take(1).uppercase(),
                                                color = if (user.role == "admin") Color.Black else Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(user.displayName, color = Color.White, fontWeight = FontWeight.Bold)
                                                if (user.role == "admin") {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("ADMIN", color = SnaporaAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            Text("@${user.username} • ${user.email}", color = TextSecondary, fontSize = 11.sp)
                                            if (user.isSuspended) {
                                                Text("SUSPENDED", color = SnaporaRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        if (user.role != "admin") {
                                            if (user.isSuspended) {
                                                Button(
                                                    onClick = {
                                                        coroutineScope.launch {
                                                            adminRepository.restoreUser(currentUserId, user.id)
                                                            Toast.makeText(context, "Restored @${user.username}", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = SnaporaGreen),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("Restore", fontSize = 11.sp)
                                                }
                                            } else {
                                                OutlinedButton(
                                                    onClick = {
                                                        coroutineScope.launch {
                                                            adminRepository.suspendUser(currentUserId, user.id)
                                                            Toast.makeText(context, "Suspended @${user.username}", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SnaporaRed),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("Suspend", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Reports & Moderation Tab
                    if (reports.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = SnaporaGreen, modifier = Modifier.size(54.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("No safety reports pending", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text("The Snapora platform is safe and compliant.", color = TextSecondary, fontSize = 13.sp)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(reports) { item ->
                                val r = item.report
                                Surface(
                                    color = DarkSurface,
                                    shape = RoundedCornerShape(16.dp),
                                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkCardBorder)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                color = SnaporaPink.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = "REPORTED ${r.targetType}",
                                                    color = SnaporaPink,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                            Text(
                                                text = r.status,
                                                color = if (r.status == "RESOLVED") SnaporaGreen else SnaporaAmber,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(text = "Reason: ${r.reason}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        if (r.details.isNotBlank()) {
                                            Text(text = "Details: ${r.details}", color = TextSecondary, fontSize = 12.sp)
                                        }
                                        Text(text = "Reported by: @${item.reporter?.username ?: "user"}", color = TextMuted, fontSize = 11.sp)

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        adminRepository.removeReportedContent(currentUserId, r.id)
                                                        Toast.makeText(context, "Content removed and violator dealt with", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = SnaporaRed),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Remove Content", fontSize = 12.sp)
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        adminRepository.resolveReport(currentUserId, r.id, "DISMISSED")
                                                        Toast.makeText(context, "Report dismissed", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Dismiss", fontSize = 12.sp)
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
}

@Composable
fun AdminMetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = DarkSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text(text = title, fontSize = 12.sp, color = TextSecondary)
        }
    }
}
