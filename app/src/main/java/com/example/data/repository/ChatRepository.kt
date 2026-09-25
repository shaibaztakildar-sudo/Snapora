package com.example.data.repository

import com.example.data.dao.ChatDao
import com.example.data.dao.NotificationDao
import com.example.data.dao.SocialDao
import com.example.data.dao.UserDao
import com.example.data.model.ConversationEntity
import com.example.data.model.MessageEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class ConversationItem(
    val conversation: ConversationEntity,
    val otherUser: UserEntity,
    val unreadCount: Int
)

class ChatRepository(
    private val chatDao: ChatDao,
    private val userDao: UserDao,
    private val socialDao: SocialDao,
    private val notificationDao: NotificationDao
) {

    fun getConversations(currentUserId: Long): Flow<List<ConversationItem>> {
        return chatDao.getConversationsForUser(currentUserId).map { conversations ->
            val otherUserIds = conversations.map {
                if (it.user1Id == currentUserId) it.user2Id else it.user1Id
            }.distinct()

            val users = if (otherUserIds.isEmpty()) emptyList() else userDao.getUsersByIds(otherUserIds)
            val userMap = users.associateBy { it.id }

            conversations.mapNotNull { conv ->
                val otherId = if (conv.user1Id == currentUserId) conv.user2Id else conv.user1Id
                val other = userMap[otherId] ?: return@mapNotNull null
                val unread = if (conv.user1Id == currentUserId) conv.unreadCountUser1 else conv.unreadCountUser2
                ConversationItem(conversation = conv, otherUser = other, unreadCount = unread)
            }
        }
    }

    suspend fun getOrCreateConversation(userA: Long, userB: Long): ConversationEntity = withContext(Dispatchers.IO) {
        val existing = chatDao.getConversationBetween(userA, userB)
        if (existing != null) return@withContext existing

        val newConv = ConversationEntity(
            user1Id = minOf(userA, userB),
            user2Id = maxOf(userA, userB),
            lastMessageText = "Started a conversation",
            lastMessageTime = System.currentTimeMillis()
        )
        val id = chatDao.insertConversation(newConv)
        newConv.copy(id = id)
    }

    fun getMessages(conversationId: Long): Flow<List<MessageEntity>> {
        return chatDao.getMessages(conversationId)
    }

    suspend fun sendMessage(
        senderId: Long,
        receiverId: Long,
        text: String,
        mediaUri: String? = null,
        mediaType: String = "TEXT",
        snapId: Long? = null
    ): Result<Long> = withContext(Dispatchers.IO) {
        if (socialDao.isBlocked(senderId, receiverId)) {
            return@withContext Result.failure(IllegalStateException("Cannot send message: user is blocked"))
        }

        val conv = getOrCreateConversation(senderId, receiverId)

        val message = MessageEntity(
            conversationId = conv.id,
            senderId = senderId,
            receiverId = receiverId,
            text = text,
            mediaUri = mediaUri,
            mediaType = mediaType,
            snapId = snapId,
            timestamp = System.currentTimeMillis(),
            status = "SENT"
        )
        val messageId = chatDao.insertMessage(message)

        val previewText = when (mediaType) {
            "SNAP" -> "Sent a Snap 📸"
            "IMAGE" -> "Sent a photo 📷"
            "VIDEO" -> "Sent a video 🎥"
            else -> text
        }

        val updatedConv = conv.copy(
            lastMessageText = previewText,
            lastMessageTime = System.currentTimeMillis(),
            lastSenderId = senderId,
            unreadCountUser1 = if (conv.user1Id == receiverId) conv.unreadCountUser1 + 1 else conv.unreadCountUser1,
            unreadCountUser2 = if (conv.user2Id == receiverId) conv.unreadCountUser2 + 1 else conv.unreadCountUser2
        )
        chatDao.updateConversation(updatedConv)

        val sender = userDao.getUserByIdSync(senderId)
        val senderName = sender?.displayName ?: "Friend"

        notificationDao.insertNotification(
            NotificationEntity(
                userId = receiverId,
                senderId = senderId,
                type = if (mediaType == "SNAP") "SNAP" else "MESSAGE",
                title = if (mediaType == "SNAP") "New Snap from $senderName" else senderName,
                body = previewText,
                relatedId = conv.id
            )
        )

        userDao.incrementSnapScore(senderId, 1)

        Result.success(messageId)
    }

    suspend fun deleteMessage(messageId: Long, currentUserId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        chatDao.softDeleteMessage(messageId, currentUserId)
        Result.success(Unit)
    }

    suspend fun markAsRead(conversationId: Long, currentUserId: Long) = withContext(Dispatchers.IO) {
        val conv = chatDao.getConversationById(conversationId) ?: return@withContext
        val updated = if (conv.user1Id == currentUserId) {
            conv.copy(unreadCountUser1 = 0)
        } else {
            conv.copy(unreadCountUser2 = 0)
        }
        chatDao.updateConversation(updated)
        chatDao.markMessagesAsRead(conversationId, currentUserId)
    }
}
