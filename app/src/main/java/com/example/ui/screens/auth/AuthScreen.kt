package com.example.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AuthRepository
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    authRepository: AuthRepository,
    onAuthSuccess: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isSignUp by remember { mutableStateOf(false) }
    var isForgotPassword by remember { mutableStateOf(false) }

    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Snapora Logo & Title
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(SnaporaPurple, SnaporaPink, SnaporaCyan)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Snapora Logo",
                    tint = Color.White,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Snapora",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.sp
            )

            Text(
                text = if (isForgotPassword) "Reset Account Password"
                else if (isSignUp) "Join the snap community"
                else "Share real moments, real-time",
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Quick Demo User Switcher Chip Row
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Quick Sign-In / Switch Account:",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = {
                                coroutineScope.launch {
                                    isLoading = true
                                    errorMessage = null
                                    val res = authRepository.logIn("admin@snapora.com", "AdminPass123!")
                                    isLoading = false
                                    if (res.isSuccess) onAuthSuccess() else errorMessage = res.exceptionOrNull()?.message
                                }
                            },
                            label = { Text("Owner (Admin)", fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Security, contentDescription = null, tint = SnaporaAmber, modifier = Modifier.size(16.dp))
                            },
                            colors = AssistChipDefaults.assistChipColors(containerColor = DarkSurface)
                        )
                        AssistChip(
                            onClick = {
                                coroutineScope.launch {
                                    isLoading = true
                                    errorMessage = null
                                    val res = authRepository.logIn("chloe_vibe", "UserPass123!")
                                    isLoading = false
                                    if (res.isSuccess) onAuthSuccess() else errorMessage = res.exceptionOrNull()?.message
                                }
                            },
                            label = { Text("Chloe", fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = SnaporaPink, modifier = Modifier.size(16.dp))
                            },
                            colors = AssistChipDefaults.assistChipColors(containerColor = DarkSurface)
                        )
                        AssistChip(
                            onClick = {
                                coroutineScope.launch {
                                    isLoading = true
                                    errorMessage = null
                                    val res = authRepository.logIn("leo_lens", "UserPass123!")
                                    isLoading = false
                                    if (res.isSuccess) onAuthSuccess() else errorMessage = res.exceptionOrNull()?.message
                                }
                            },
                            label = { Text("Leo", fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Camera, contentDescription = null, tint = SnaporaCyan, modifier = Modifier.size(16.dp))
                            },
                            colors = AssistChipDefaults.assistChipColors(containerColor = DarkSurface)
                        )
                    }
                }
            }

            // Error or Success Banner
            AnimatedVisibility(visible = errorMessage != null) {
                Surface(
                    color = SnaporaRed.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(SnaporaRed, SnaporaRed))),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = SnaporaRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = errorMessage ?: "", color = Color.White, fontSize = 13.sp)
                    }
                }
            }

            AnimatedVisibility(visible = successMessage != null) {
                Surface(
                    color = SnaporaGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SnaporaGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = successMessage ?: "", color = Color.White, fontSize = 13.sp)
                    }
                }
            }

            // Input Fields
            if (isSignUp) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedBorderColor = SnaporaPurple,
                        unfocusedBorderColor = DarkCardBorder
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("display_name_input")
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedBorderColor = SnaporaPurple,
                        unfocusedBorderColor = DarkCardBorder
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("username_input")
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(if (isSignUp || isForgotPassword) "Email Address" else "Username or Email") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedBorderColor = SnaporaPurple,
                    unfocusedBorderColor = DarkCardBorder
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("email_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(if (isForgotPassword) "New Password" else "Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedBorderColor = SnaporaPurple,
                    unfocusedBorderColor = DarkCardBorder
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("password_input")
            )

            if (isSignUp) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio (Optional)") },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedBorderColor = SnaporaPurple,
                        unfocusedBorderColor = DarkCardBorder
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (!isSignUp && !isForgotPassword) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        isForgotPassword = true
                        errorMessage = null
                        successMessage = null
                    }) {
                        Text("Forgot password?", color = SnaporaPurpleLight, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Action Button
            Button(
                onClick = {
                    coroutineScope.launch {
                        isLoading = true
                        errorMessage = null
                        successMessage = null
                        if (isForgotPassword) {
                            val res = authRepository.resetPassword(email, password)
                            if (res.isSuccess) {
                                successMessage = "Password reset! You can now log in."
                                isForgotPassword = false
                            } else {
                                errorMessage = res.exceptionOrNull()?.message
                            }
                        } else if (isSignUp) {
                            val res = authRepository.signUp(
                                username = username,
                                email = email,
                                password = password,
                                displayName = displayName,
                                bio = bio
                            )
                            if (res.isSuccess) {
                                onAuthSuccess()
                            } else {
                                errorMessage = res.exceptionOrNull()?.message
                            }
                        } else {
                            val res = authRepository.logIn(email, password)
                            if (res.isSuccess) {
                                onAuthSuccess()
                            } else {
                                errorMessage = res.exceptionOrNull()?.message
                            }
                        }
                        isLoading = false
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("auth_submit_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SnaporaPurple
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (isForgotPassword) "Update Password"
                        else if (isSignUp) "Create Snapora Account"
                        else "Log In",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Switcher
            Row(
                modifier = Modifier.padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isForgotPassword) "Remember your password? "
                    else if (isSignUp) "Already have an account? "
                    else "Don't have an account? ",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                Text(
                    text = if (isForgotPassword) "Log In"
                    else if (isSignUp) "Log In"
                    else "Sign Up",
                    color = SnaporaPinkLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable {
                        errorMessage = null
                        successMessage = null
                        if (isForgotPassword) {
                            isForgotPassword = false
                        } else {
                            isSignUp = !isSignUp
                        }
                    }
                )
            }
        }
    }
}
