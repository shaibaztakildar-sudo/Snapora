package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["username"], unique = true),
        Index(value = ["email"], unique = true)
    ]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val username: String,
    val email: String,
    val passwordHash: String,
    val salt: String,
    val displayName: String,
    val bio: String = "",
    val avatarUri: String? = null,
    val role: String = "user", // "user" or "admin"
    val isSuspended: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis(),
    val snapScore: Int = 0
)

@Entity(
    tableName = "friend_requests",
    indices = [
        Index(value = ["senderId", "receiverId"], unique = true),
        Index(value = ["receiverId"])
    ]
)
data class FriendRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val senderId: Long,
    val receiverId: Long,
    val status: String = "PENDING", // PENDING, ACCEPTED, REJECTED
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "friendships",
    indices = [
        Index(value = ["userId", "friendId"], unique = true),
        Index(value = ["userId"])
    ]
)
data class FriendshipEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val userId: Long,
    val friendId: Long,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "blocked_users",
    indices = [
        Index(value = ["blockerId", "blockedId"], unique = true),
        Index(value = ["blockerId"])
    ]
)
data class BlockedUserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val blockerId: Long,
    val blockedId: Long,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "conversations",
    indices = [
        Index(value = ["user1Id", "user2Id"], unique = true)
    ]
)
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val user1Id: Long,
    val user2Id: Long,
    val lastMessageText: String = "",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val lastSenderId: Long = 0L,
    val unreadCountUser1: Int = 0,
    val unreadCountUser2: Int = 0
)

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["conversationId"]),
        Index(value = ["timestamp"])
    ]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val conversationId: Long,
    val senderId: Long,
    val receiverId: Long,
    val text: String = "",
    val mediaUri: String? = null,
    val mediaType: String = "TEXT", // TEXT, IMAGE, VIDEO, SNAP
    val snapId: Long? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "SENT", // SENT, DELIVERED, READ
    val isDeleted: Boolean = false
)

@Entity(
    tableName = "snaps",
    indices = [
        Index(value = ["receiverId"]),
        Index(value = ["senderId"])
    ]
)
data class SnapEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val senderId: Long,
    val receiverId: Long,
    val mediaUri: String,
    val mediaType: String = "IMAGE", // IMAGE, VIDEO
    val caption: String = "",
    val emojiOverlay: String = "",
    val filter: String = "Normal",
    val durationSeconds: Int = 10,
    val createdAt: Long = System.currentTimeMillis(),
    val openedAt: Long? = null,
    val isOpened: Boolean = false,
    val isExpired: Boolean = false
)

@Entity(
    tableName = "stories",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["expiresAt"])
    ]
)
data class StoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val userId: Long,
    val mediaUri: String,
    val mediaType: String = "IMAGE", // IMAGE, VIDEO
    val caption: String = "",
    val emojiOverlay: String = "",
    val filter: String = "Normal",
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (24 * 60 * 60 * 1000L),
    val isDeleted: Boolean = false
)

@Entity(
    tableName = "story_views",
    indices = [
        Index(value = ["storyId", "viewerId"], unique = true)
    ]
)
data class StoryViewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val storyId: Long,
    val viewerId: Long,
    val viewedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["createdAt"])
    ]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val userId: Long,
    val senderId: Long? = null,
    val type: String, // MESSAGE, SNAP, FRIEND_REQ, FRIEND_ACC, STORY_VIEW
    val title: String,
    val body: String,
    val relatedId: Long? = null,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "reports",
    indices = [
        Index(value = ["status"]),
        Index(value = ["createdAt"])
    ]
)
data class ReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val reporterId: Long,
    val targetType: String, // USER, MESSAGE, STORY, SNAP
    val targetId: Long,
    val reason: String,
    val details: String = "",
    val status: String = "PENDING", // PENDING, RESOLVED, DISMISSED
    val createdAt: Long = System.currentTimeMillis()
)
