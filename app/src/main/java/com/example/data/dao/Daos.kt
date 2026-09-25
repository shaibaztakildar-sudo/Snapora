package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BlockedUserEntity
import com.example.data.model.ConversationEntity
import com.example.data.model.FriendRequestEntity
import com.example.data.model.FriendshipEntity
import com.example.data.model.MessageEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.ReportEntity
import com.example.data.model.SnapEntity
import com.example.data.model.StoryEntity
import com.example.data.model.StoryViewEntity
import com.example.data.model.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserById(id: Long): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserByIdSync(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE username LIKE '%' || :query || '%' OR displayName LIKE '%' || :query || '%'")
    fun searchUsers(query: String): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id IN (:ids)")
    suspend fun getUsersByIds(ids: List<Long>): List<UserEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET snapScore = snapScore + :amount WHERE id = :userId")
    suspend fun incrementSnapScore(userId: Long, amount: Int = 1)

    @Query("UPDATE users SET isSuspended = :isSuspended WHERE id = :userId")
    suspend fun setSuspended(userId: Long, isSuspended: Boolean)
}

@Dao
interface SocialDao {
    @Query("SELECT * FROM friendships WHERE userId = :userId")
    fun getFriendships(userId: Long): Flow<List<FriendshipEntity>>

    @Query("SELECT * FROM friendships WHERE userId = :userId")
    suspend fun getFriendshipsSync(userId: Long): List<FriendshipEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriendship(friendship: FriendshipEntity): Long

    @Query("DELETE FROM friendships WHERE (userId = :userId AND friendId = :friendId) OR (userId = :friendId AND friendId = :userId)")
    suspend fun deleteFriendship(userId: Long, friendId: Long)

    @Query("SELECT * FROM friend_requests WHERE receiverId = :receiverId AND status = 'PENDING' ORDER BY createdAt DESC")
    fun getPendingRequestsForReceiver(receiverId: Long): Flow<List<FriendRequestEntity>>

    @Query("SELECT * FROM friend_requests WHERE senderId = :senderId ORDER BY createdAt DESC")
    fun getSentRequests(senderId: Long): Flow<List<FriendRequestEntity>>

    @Query("SELECT * FROM friend_requests WHERE (senderId = :senderId AND receiverId = :receiverId) OR (senderId = :receiverId AND receiverId = :senderId) LIMIT 1")
    suspend fun getFriendRequest(senderId: Long, receiverId: Long): FriendRequestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriendRequest(req: FriendRequestEntity): Long

    @Update
    suspend fun updateFriendRequest(req: FriendRequestEntity)

    @Query("DELETE FROM friend_requests WHERE id = :id")
    suspend fun deleteFriendRequestById(id: Long)

    @Query("DELETE FROM friend_requests WHERE (senderId = :userA AND receiverId = :userB) OR (senderId = :userB AND receiverId = :userA)")
    suspend fun deleteFriendRequestBetween(userA: Long, userB: Long)

    @Query("SELECT * FROM blocked_users WHERE blockerId = :blockerId")
    fun getBlockedUsers(blockerId: Long): Flow<List<BlockedUserEntity>>

    @Query("SELECT COUNT(*) > 0 FROM blocked_users WHERE (blockerId = :userA AND blockedId = :userB) OR (blockerId = :userB AND blockedId = :userA)")
    suspend fun isBlocked(userA: Long, userB: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlocked(block: BlockedUserEntity): Long

    @Query("DELETE FROM blocked_users WHERE blockerId = :blockerId AND blockedId = :blockedId")
    suspend fun deleteBlocked(blockerId: Long, blockedId: Long)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM conversations WHERE user1Id = :userId OR user2Id = :userId ORDER BY lastMessageTime DESC")
    fun getConversationsForUser(userId: Long): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE (user1Id = :userA AND user2Id = :userB) OR (user1Id = :userB AND user2Id = :userA) LIMIT 1")
    suspend fun getConversationBetween(userA: Long, userB: Long): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversationById(id: Long): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity): Long

    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessages(conversationId: Long): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("UPDATE messages SET isDeleted = 1 WHERE id = :messageId AND senderId = :userId")
    suspend fun softDeleteMessage(messageId: Long, userId: Long)

    @Query("UPDATE messages SET status = 'READ' WHERE conversationId = :conversationId AND receiverId = :userId AND status != 'READ'")
    suspend fun markMessagesAsRead(conversationId: Long, userId: Long)
}

@Dao
interface SnapDao {
    @Query("SELECT * FROM snaps WHERE receiverId = :receiverId AND isExpired = 0 ORDER BY createdAt DESC")
    fun getInboxSnaps(receiverId: Long): Flow<List<SnapEntity>>

    @Query("SELECT * FROM snaps WHERE senderId = :senderId ORDER BY createdAt DESC")
    fun getSentSnaps(senderId: Long): Flow<List<SnapEntity>>

    @Query("SELECT * FROM snaps WHERE id = :snapId")
    suspend fun getSnapById(snapId: Long): SnapEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnap(snap: SnapEntity): Long

    @Update
    suspend fun updateSnap(snap: SnapEntity)

    @Query("DELETE FROM snaps WHERE id = :snapId")
    suspend fun deleteSnap(snapId: Long)

    @Query("SELECT * FROM snaps WHERE isExpired = 1 OR (openedAt IS NOT NULL AND (:now - openedAt) > (durationSeconds * 1000))")
    suspend fun getExpiredSnaps(now: Long = System.currentTimeMillis()): List<SnapEntity>

    @Query("UPDATE snaps SET isExpired = 1 WHERE id = :snapId")
    suspend fun markExpired(snapId: Long)
}

@Dao
interface StoryDao {
    @Query("SELECT * FROM stories WHERE expiresAt > :now AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getActiveStories(now: Long = System.currentTimeMillis()): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories WHERE userId = :userId AND expiresAt > :now AND isDeleted = 0 ORDER BY createdAt ASC")
    fun getActiveStoriesForUser(userId: Long, now: Long = System.currentTimeMillis()): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories WHERE id = :id AND isDeleted = 0")
    suspend fun getStoryById(id: Long): StoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: StoryEntity): Long

    @Query("UPDATE stories SET isDeleted = 1 WHERE id = :id")
    suspend fun markStoryDeleted(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun recordStoryView(view: StoryViewEntity)

    @Query("SELECT * FROM story_views WHERE storyId = :storyId ORDER BY viewedAt DESC")
    fun getStoryViews(storyId: Long): Flow<List<StoryViewEntity>>

    @Query("SELECT COUNT(*) > 0 FROM story_views WHERE storyId = :storyId AND viewerId = :viewerId")
    suspend fun hasViewedStory(storyId: Long, viewerId: Long): Boolean

    @Query("SELECT * FROM stories WHERE expiresAt <= :now OR isDeleted = 1")
    suspend fun getExpiredStories(now: Long = System.currentTimeMillis()): List<StoryEntity>
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY createdAt DESC")
    fun getNotificationsForUser(userId: Long): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :userId AND isRead = 0")
    fun getUnreadNotificationCount(userId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllAsRead(userId: Long)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :notificationId")
    suspend fun markAsRead(notificationId: Long)
}

@Dao
interface ReportDao {
    @Query("SELECT * FROM reports ORDER BY createdAt DESC")
    fun getAllReports(): Flow<List<ReportEntity>>

    @Query("SELECT * FROM reports WHERE status = 'PENDING' ORDER BY createdAt DESC")
    fun getPendingReports(): Flow<List<ReportEntity>>

    @Query("SELECT * FROM reports WHERE id = :id")
    suspend fun getReportById(id: Long): ReportEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity): Long

    @Update
    suspend fun updateReport(report: ReportEntity)

    @Query("SELECT COUNT(*) FROM reports")
    suspend fun getTotalReportsCount(): Int

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getTotalUsersCount(): Int

    @Query("SELECT COUNT(*) FROM users WHERE isSuspended = 1")
    suspend fun getSuspendedUsersCount(): Int

    @Query("SELECT COUNT(*) FROM conversations")
    suspend fun getTotalConversationsCount(): Int

    @Query("SELECT COUNT(*) FROM stories WHERE expiresAt > :now AND isDeleted = 0")
    suspend fun getActiveStoriesCount(now: Long = System.currentTimeMillis()): Int
}
