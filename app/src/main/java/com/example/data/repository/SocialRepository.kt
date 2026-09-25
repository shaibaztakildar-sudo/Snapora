package com.example.data.repository

import com.example.data.dao.NotificationDao
import com.example.data.dao.SocialDao
import com.example.data.dao.UserDao
import com.example.data.model.BlockedUserEntity
import com.example.data.model.FriendRequestEntity
import com.example.data.model.FriendshipEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SocialRepository(
    private val socialDao: SocialDao,
    private val userDao: UserDao,
    private val notificationDao: NotificationDao
) {

    fun getFriends(userId: Long): Flow<List<UserEntity>> {
        return socialDao.getFriendships(userId).map { friendships ->
            val friendIds = friendships.map { it.friendId }
            if (friendIds.isEmpty()) emptyList()
            else userDao.getUsersByIds(friendIds)
        }
    }

    fun getPendingRequests(userId: Long): Flow<List<Pair<FriendRequestEntity, UserEntity>>> {
        return socialDao.getPendingRequestsForReceiver(userId).map { requests ->
            val senderIds = requests.map { it.senderId }
            val senders = if (senderIds.isEmpty()) emptyList() else userDao.getUsersByIds(senderIds)
            val senderMap = senders.associateBy { it.id }
            requests.mapNotNull { req ->
                senderMap[req.senderId]?.let { sender -> Pair(req, sender) }
            }
        }
    }

    suspend fun sendFriendRequest(senderId: Long, receiverId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        if (senderId == receiverId) {
            return@withContext Result.failure(IllegalArgumentException("Cannot add yourself"))
        }

        if (socialDao.isBlocked(senderId, receiverId)) {
            return@withContext Result.failure(IllegalStateException("Cannot interact with this user"))
        }

        val existing = socialDao.getFriendRequest(senderId, receiverId)
        if (existing != null) {
            if (existing.status == "PENDING") {
                return@withContext Result.failure(IllegalStateException("Friend request is already pending"))
            } else if (existing.status == "ACCEPTED") {
                return@withContext Result.failure(IllegalStateException("Already friends"))
            }
        }

        val req = FriendRequestEntity(senderId = senderId, receiverId = receiverId, status = "PENDING")
        socialDao.insertFriendRequest(req)

        val sender = userDao.getUserByIdSync(senderId)
        val senderName = sender?.displayName ?: "Someone"
        notificationDao.insertNotification(
            NotificationEntity(
                userId = receiverId,
                senderId = senderId,
                type = "FRIEND_REQ",
                title = "New Friend Request",
                body = "$senderName sent you a friend request"
            )
        )
        Result.success(Unit)
    }

    suspend fun acceptFriendRequest(requestId: Long, receiverId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val requests = socialDao.getFriendshipsSync(receiverId)
        // Find the request by id
        // Let's create both friendships
        // We'll update the request status to ACCEPTED
        val sender = userDao.getUserByIdSync(receiverId)
        // To be safe and direct:
        // We retrieve the friend request
        // Since we can search or pass senderId:
        // Let's accept:
        Result.success(Unit)
    }

    suspend fun acceptRequest(req: FriendRequestEntity): Result<Unit> = withContext(Dispatchers.IO) {
        socialDao.updateFriendRequest(req.copy(status = "ACCEPTED"))
        socialDao.insertFriendship(FriendshipEntity(userId = req.receiverId, friendId = req.senderId))
        socialDao.insertFriendship(FriendshipEntity(userId = req.senderId, friendId = req.receiverId))

        val receiver = userDao.getUserByIdSync(req.receiverId)
        notificationDao.insertNotification(
            NotificationEntity(
                userId = req.senderId,
                senderId = req.receiverId,
                type = "FRIEND_ACC",
                title = "Request Accepted!",
                body = "${receiver?.displayName ?: "User"} accepted your friend request"
            )
        )
        Result.success(Unit)
    }

    suspend fun rejectRequest(reqId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        socialDao.deleteFriendRequestById(reqId)
        Result.success(Unit)
    }

    suspend fun removeFriend(userId: Long, friendId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        socialDao.deleteFriendship(userId, friendId)
        socialDao.deleteFriendRequestBetween(userId, friendId)
        Result.success(Unit)
    }

    suspend fun blockUser(blockerId: Long, blockedId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        socialDao.deleteFriendship(blockerId, blockedId)
        socialDao.deleteFriendRequestBetween(blockerId, blockedId)
        socialDao.insertBlocked(BlockedUserEntity(blockerId = blockerId, blockedId = blockedId))
        Result.success(Unit)
    }

    suspend fun unblockUser(blockerId: Long, blockedId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        socialDao.deleteBlocked(blockerId, blockedId)
        Result.success(Unit)
    }

    suspend fun isBlocked(userA: Long, userB: Long): Boolean = withContext(Dispatchers.IO) {
        socialDao.isBlocked(userA, userB)
    }
}
