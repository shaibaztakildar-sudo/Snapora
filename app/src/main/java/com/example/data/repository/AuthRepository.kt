package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.dao.UserDao
import com.example.data.model.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class AuthRepository(
    private val userDao: UserDao,
    private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("snapora_auth_prefs", Context.MODE_PRIVATE)

    private val _currentUserState = MutableStateFlow<UserEntity?>(null)
    val currentUserState: Flow<UserEntity?> = _currentUserState.asStateFlow()

    suspend fun initialize() = withContext(Dispatchers.IO) {
        seedInitialAccountsIfNeeded()
        val savedUserId = prefs.getLong("logged_in_user_id", -1L)
        if (savedUserId != -1L) {
            val user = userDao.getUserByIdSync(savedUserId)
            if (user != null && !user.isSuspended) {
                _currentUserState.value = user
            } else {
                prefs.edit().remove("logged_in_user_id").apply()
                _currentUserState.value = null
            }
        }
    }

    private suspend fun seedInitialAccountsIfNeeded() {
        val existingAdmin = userDao.getUserByUsername("snapora_owner")
        if (existingAdmin == null) {
            val adminSalt = PasswordSecurity.generateSalt()
            val adminHash = PasswordSecurity.hashPassword("AdminPass123!", adminSalt)
            userDao.insertUser(
                UserEntity(
                    username = "snapora_owner",
                    email = "admin@snapora.com",
                    passwordHash = adminHash,
                    salt = adminSalt,
                    displayName = "Snapora Admin (Owner)",
                    bio = "Official Snapora Platform Administration & Safety",
                    role = "admin",
                    snapScore = 9999
                )
            )

            // Seed a few lively social accounts so users have friends, stories, and chats immediately
            val user1Salt = PasswordSecurity.generateSalt()
            userDao.insertUser(
                UserEntity(
                    username = "chloe_vibe",
                    email = "chloe@snapora.internal",
                    passwordHash = PasswordSecurity.hashPassword("UserPass123!", user1Salt),
                    salt = user1Salt,
                    displayName = "Chloe Vance",
                    bio = "Living for golden hour moments 🌅 | Snap daily 📸",
                    role = "user",
                    snapScore = 1420
                )
            )

            val user2Salt = PasswordSecurity.generateSalt()
            userDao.insertUser(
                UserEntity(
                    username = "leo_lens",
                    email = "leo@snapora.internal",
                    passwordHash = PasswordSecurity.hashPassword("UserPass123!", user2Salt),
                    salt = user2Salt,
                    displayName = "Leo Walker",
                    bio = "Visual creator & camera enthusiast 🎨✨",
                    role = "user",
                    snapScore = 850
                )
            )
        }
    }

    suspend fun signUp(
        username: String,
        email: String,
        password: String,
        displayName: String,
        bio: String = ""
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        val cleanUsername = username.trim().lowercase().removePrefix("@")
        val cleanEmail = email.trim().lowercase()

        if (cleanUsername.length < 3) {
            return@withContext Result.failure(IllegalArgumentException("Username must be at least 3 characters"))
        }
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address"))
        }
        if (password.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("Password must be at least 6 characters"))
        }

        val existingUser = userDao.getUserByUsername(cleanUsername)
        if (existingUser != null) {
            return@withContext Result.failure(IllegalArgumentException("Username @$cleanUsername is already taken"))
        }
        val existingEmail = userDao.getUserByEmail(cleanEmail)
        if (existingEmail != null) {
            return@withContext Result.failure(IllegalArgumentException("An account with this email already exists"))
        }

        val salt = PasswordSecurity.generateSalt()
        val hash = PasswordSecurity.hashPassword(password, salt)

        val newUser = UserEntity(
            username = cleanUsername,
            email = cleanEmail,
            passwordHash = hash,
            salt = salt,
            displayName = displayName.trim().ifBlank { cleanUsername },
            bio = bio.trim(),
            role = "user",
            snapScore = 10
        )

        try {
            val id = userDao.insertUser(newUser)
            val createdUser = newUser.copy(id = id)
            prefs.edit().putLong("logged_in_user_id", id).apply()
            _currentUserState.value = createdUser
            Result.success(createdUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logIn(usernameOrEmail: String, password: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        val query = usernameOrEmail.trim().lowercase().removePrefix("@")
        val user = if (query.contains("@")) {
            userDao.getUserByEmail(query)
        } else {
            userDao.getUserByUsername(query)
        }

        if (user == null) {
            return@withContext Result.failure(IllegalArgumentException("No account found with this username/email"))
        }

        if (user.isSuspended) {
            return@withContext Result.failure(SecurityException("Your account has been suspended by administration."))
        }

        val isValid = PasswordSecurity.verifyPassword(password, user.salt, user.passwordHash)
        if (!isValid) {
            return@withContext Result.failure(IllegalArgumentException("Incorrect password"))
        }

        val updated = user.copy(lastActiveAt = System.currentTimeMillis())
        userDao.updateUser(updated)
        prefs.edit().putLong("logged_in_user_id", user.id).apply()
        _currentUserState.value = updated
        Result.success(updated)
    }

    suspend fun switchUserDirect(userId: Long): Result<UserEntity> = withContext(Dispatchers.IO) {
        val user = userDao.getUserByIdSync(userId) ?: return@withContext Result.failure(IllegalArgumentException("User not found"))
        if (user.isSuspended) {
            return@withContext Result.failure(SecurityException("Account is suspended"))
        }
        prefs.edit().putLong("logged_in_user_id", user.id).apply()
        _currentUserState.value = user
        Result.success(user)
    }

    suspend fun resetPassword(email: String, newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val user = userDao.getUserByEmail(cleanEmail)
            ?: return@withContext Result.failure(IllegalArgumentException("No account registered with this email"))

        if (newPassword.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("New password must be at least 6 characters"))
        }

        val salt = PasswordSecurity.generateSalt()
        val hash = PasswordSecurity.hashPassword(newPassword, salt)
        val updated = user.copy(passwordHash = hash, salt = salt)
        userDao.updateUser(updated)
        Result.success(Unit)
    }

    suspend fun updateProfile(
        userId: Long,
        displayName: String,
        bio: String,
        avatarUri: String?
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        val user = userDao.getUserByIdSync(userId) ?: return@withContext Result.failure(IllegalArgumentException("User not found"))
        val updated = user.copy(
            displayName = displayName.trim().ifBlank { user.displayName },
            bio = bio.trim(),
            avatarUri = avatarUri ?: user.avatarUri
        )
        userDao.updateUser(updated)
        if (_currentUserState.value?.id == userId) {
            _currentUserState.value = updated
        }
        Result.success(updated)
    }

    suspend fun refreshCurrentUser() = withContext(Dispatchers.IO) {
        val currentId = _currentUserState.value?.id ?: prefs.getLong("logged_in_user_id", -1L)
        if (currentId != -1L) {
            val user = userDao.getUserByIdSync(currentId)
            if (user != null && !user.isSuspended) {
                _currentUserState.value = user
            } else {
                logOut()
            }
        }
    }

    fun logOut() {
        prefs.edit().remove("logged_in_user_id").apply()
        _currentUserState.value = null
    }

    suspend fun getAllUsersList(): List<UserEntity> = withContext(Dispatchers.IO) {
        userDao.getUsersByIds(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
    }
}
