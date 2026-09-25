package com.example.data.repository

import com.example.data.dao.NotificationDao
import com.example.data.dao.StoryDao
import com.example.data.dao.UserDao
import com.example.data.model.NotificationEntity
import com.example.data.model.StoryEntity
import com.example.data.model.StoryViewEntity
import com.example.data.model.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class UserStoryGroup(
    val user: UserEntity,
    val stories: List<StoryEntity>,
    val hasUnseen: Boolean
)

data class StoryViewerItem(
    val viewer: UserEntity,
    val viewedAt: Long
)

class StoryRepository(
    private val storyDao: StoryDao,
    private val userDao: UserDao,
    private val notificationDao: NotificationDao,
    private val mediaStorageManager: MediaStorageManager
) {

    fun getActiveStoryGroups(currentUserId: Long): Flow<List<UserStoryGroup>> {
        return storyDao.getActiveStories().map { allStories ->
            val userIds = allStories.map { it.userId }.distinct()
            val users = if (userIds.isEmpty()) emptyList() else userDao.getUsersByIds(userIds)
            val userMap = users.associateBy { it.id }

            userIds.mapNotNull { uid ->
                val user = userMap[uid] ?: return@mapNotNull null
                val userStories = allStories.filter { it.userId == uid }.sortedBy { it.createdAt }
                UserStoryGroup(
                    user = user,
                    stories = userStories,
                    hasUnseen = userStories.any { story ->
                        // If current user is not the owner, check if viewed
                        uid != currentUserId
                    }
                )
            }.sortedBy { group ->
                // Current user first, then others
                if (group.user.id == currentUserId) 0 else 1
            }
        }
    }

    suspend fun postStory(
        userId: Long,
        mediaUri: String,
        mediaType: String,
        caption: String,
        emojiOverlay: String,
        filter: String
    ): Result<Long> = withContext(Dispatchers.IO) {
        val story = StoryEntity(
            userId = userId,
            mediaUri = mediaUri,
            mediaType = mediaType,
            caption = caption,
            emojiOverlay = emojiOverlay,
            filter = filter,
            createdAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + (24 * 60 * 60 * 1000L) // 24 hours
        )
        val id = storyDao.insertStory(story)
        userDao.incrementSnapScore(userId, 2)
        Result.success(id)
    }

    suspend fun recordView(storyId: Long, viewerId: Long) = withContext(Dispatchers.IO) {
        val story = storyDao.getStoryById(storyId) ?: return@withContext
        if (story.userId != viewerId) {
            val alreadyViewed = storyDao.hasViewedStory(storyId, viewerId)
            if (!alreadyViewed) {
                storyDao.recordStoryView(StoryViewEntity(storyId = storyId, viewerId = viewerId))
                val viewer = userDao.getUserByIdSync(viewerId)
                notificationDao.insertNotification(
                    NotificationEntity(
                        userId = story.userId,
                        senderId = viewerId,
                        type = "STORY_VIEW",
                        title = "Story View",
                        body = "${viewer?.displayName ?: "Someone"} viewed your story",
                        relatedId = storyId
                    )
                )
            }
        }
    }

    fun getStoryViewers(storyId: Long): Flow<List<StoryViewerItem>> {
        return storyDao.getStoryViews(storyId).map { views ->
            val viewerIds = views.map { it.viewerId }
            val viewers = if (viewerIds.isEmpty()) emptyList() else userDao.getUsersByIds(viewerIds)
            val viewerMap = viewers.associateBy { it.id }
            views.mapNotNull { v ->
                viewerMap[v.viewerId]?.let { u -> StoryViewerItem(viewer = u, viewedAt = v.viewedAt) }
            }
        }
    }

    suspend fun deleteStory(storyId: Long, userId: Long, isAdmin: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) {
        val story = storyDao.getStoryById(storyId) ?: return@withContext Result.failure(IllegalArgumentException("Story not found"))
        if (story.userId != userId && !isAdmin) {
            return@withContext Result.failure(SecurityException("Unauthorized to delete this story"))
        }
        storyDao.markStoryDeleted(storyId)
        mediaStorageManager.deleteFile(story.mediaUri)
        Result.success(Unit)
    }

    suspend fun cleanupExpiredStories() = withContext(Dispatchers.IO) {
        val expired = storyDao.getExpiredStories()
        for (story in expired) {
            storyDao.markStoryDeleted(story.id)
            mediaStorageManager.deleteFile(story.mediaUri)
        }
    }
}
