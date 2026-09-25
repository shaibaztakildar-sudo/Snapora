package com.example.data.repository

import com.example.data.dao.SnapDao
import com.example.data.dao.UserDao
import com.example.data.model.SnapEntity
import com.example.data.model.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class SnapWithSender(
    val snap: SnapEntity,
    val sender: UserEntity
)

class SnapRepository(
    private val snapDao: SnapDao,
    private val userDao: UserDao,
    private val chatRepository: ChatRepository,
    private val mediaStorageManager: MediaStorageManager
) {

    fun getInboxSnaps(receiverId: Long): Flow<List<SnapWithSender>> {
        return snapDao.getInboxSnaps(receiverId).map { snaps ->
            val senderIds = snaps.map { it.senderId }.distinct()
            val senders = if (senderIds.isEmpty()) emptyList() else userDao.getUsersByIds(senderIds)
            val senderMap = senders.associateBy { it.id }
            snaps.mapNotNull { snap ->
                val sender = senderMap[snap.senderId] ?: return@mapNotNull null
                SnapWithSender(snap = snap, sender = sender)
            }
        }
    }

    suspend fun sendSnap(
        senderId: Long,
        receiverIds: List<Long>,
        mediaUri: String,
        mediaType: String,
        caption: String,
        emojiOverlay: String,
        filter: String,
        durationSeconds: Int
    ): Result<List<Long>> = withContext(Dispatchers.IO) {
        val createdIds = mutableListOf<Long>()
        for (receiverId in receiverIds) {
            val snap = SnapEntity(
                senderId = senderId,
                receiverId = receiverId,
                mediaUri = mediaUri,
                mediaType = mediaType,
                caption = caption,
                emojiOverlay = emojiOverlay,
                filter = filter,
                durationSeconds = durationSeconds
            )
            val snapId = snapDao.insertSnap(snap)
            createdIds.add(snapId)

            // Also post message entry to conversation
            chatRepository.sendMessage(
                senderId = senderId,
                receiverId = receiverId,
                text = "Sent a Snap ($durationSeconds s)",
                mediaUri = mediaUri,
                mediaType = "SNAP",
                snapId = snapId
            )
        }
        Result.success(createdIds)
    }

    suspend fun openSnap(snapId: Long, viewerId: Long): Result<SnapEntity> = withContext(Dispatchers.IO) {
        val snap = snapDao.getSnapById(snapId)
            ?: return@withContext Result.failure(IllegalArgumentException("Snap not found"))

        if (snap.receiverId != viewerId) {
            return@withContext Result.failure(SecurityException("Unauthorized to view this snap"))
        }

        if (snap.isExpired) {
            return@withContext Result.failure(IllegalStateException("This snap has expired and is no longer available."))
        }

        val openedSnap = if (!snap.isOpened) {
            val updated = snap.copy(
                isOpened = true,
                openedAt = System.currentTimeMillis()
            )
            snapDao.updateSnap(updated)
            updated
        } else {
            // Check if time expired
            val elapsed = System.currentTimeMillis() - (snap.openedAt ?: 0L)
            if (elapsed > snap.durationSeconds * 1000L) {
                expireSnap(snapId)
                return@withContext Result.failure(IllegalStateException("This snap has expired."))
            }
            snap
        }

        Result.success(openedSnap)
    }

    suspend fun expireSnap(snapId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val snap = snapDao.getSnapById(snapId) ?: return@withContext Result.success(Unit)
        snapDao.markExpired(snapId)
        // Securely delete the media file from storage
        mediaStorageManager.deleteFile(snap.mediaUri)
        Result.success(Unit)
    }

    suspend fun cleanupExpiredSnaps() = withContext(Dispatchers.IO) {
        val expired = snapDao.getExpiredSnaps()
        for (snap in expired) {
            snapDao.markExpired(snap.id)
            mediaStorageManager.deleteFile(snap.mediaUri)
        }
    }
}
