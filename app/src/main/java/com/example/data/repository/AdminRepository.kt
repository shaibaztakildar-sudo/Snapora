package com.example.data.repository

import com.example.data.dao.ChatDao
import com.example.data.dao.ReportDao
import com.example.data.dao.SnapDao
import com.example.data.dao.StoryDao
import com.example.data.dao.UserDao
import com.example.data.model.ReportEntity
import com.example.data.model.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class AdminDashboardStats(
    val totalUsers: Int,
    val suspendedUsers: Int,
    val totalConversations: Int,
    val activeStories: Int,
    val totalReports: Int
)

data class ReportWithDetails(
    val report: ReportEntity,
    val reporter: UserEntity?
)

class AdminRepository(
    private val userDao: UserDao,
    private val reportDao: ReportDao,
    private val storyDao: StoryDao,
    private val snapDao: SnapDao,
    private val chatDao: ChatDao,
    private val mediaStorageManager: MediaStorageManager
) {

    private suspend fun verifyAdminAuthorization(adminUserId: Long) {
        val admin = userDao.getUserByIdSync(adminUserId)
            ?: throw SecurityException("Unauthorized: Invalid user credentials")
        if (admin.role != "admin") {
            throw SecurityException("Access Denied: You are not authorized as an administrator.")
        }
        if (admin.isSuspended) {
            throw SecurityException("Access Denied: Admin account suspended.")
        }
    }

    suspend fun getStats(adminUserId: Long): AdminDashboardStats = withContext(Dispatchers.IO) {
        verifyAdminAuthorization(adminUserId)
        AdminDashboardStats(
            totalUsers = reportDao.getTotalUsersCount(),
            suspendedUsers = reportDao.getSuspendedUsersCount(),
            totalConversations = reportDao.getTotalConversationsCount(),
            activeStories = reportDao.getActiveStoriesCount(),
            totalReports = reportDao.getTotalReportsCount()
        )
    }

    suspend fun getAllUsers(adminUserId: Long): Flow<List<UserEntity>> = withContext(Dispatchers.IO) {
        verifyAdminAuthorization(adminUserId)
        userDao.getAllUsers()
    }

    suspend fun suspendUser(adminUserId: Long, targetUserId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            verifyAdminAuthorization(adminUserId)
            if (adminUserId == targetUserId) {
                return@withContext Result.failure(IllegalArgumentException("Cannot suspend your own admin account"))
            }
            userDao.setSuspended(targetUserId, true)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreUser(adminUserId: Long, targetUserId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            verifyAdminAuthorization(adminUserId)
            userDao.setSuspended(targetUserId, false)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getReports(adminUserId: Long): Flow<List<ReportWithDetails>> = withContext(Dispatchers.IO) {
        verifyAdminAuthorization(adminUserId)
        reportDao.getAllReports().map { reports ->
            val reporterIds = reports.map { it.reporterId }.distinct()
            val reporters = if (reporterIds.isEmpty()) emptyList() else userDao.getUsersByIds(reporterIds)
            val map = reporters.associateBy { it.id }
            reports.map { r ->
                ReportWithDetails(report = r, reporter = map[r.reporterId])
            }
        }
    }

    suspend fun resolveReport(adminUserId: Long, reportId: Long, status: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            verifyAdminAuthorization(adminUserId)
            val report = reportDao.getReportById(reportId)
                ?: return@withContext Result.failure(IllegalArgumentException("Report not found"))
            reportDao.updateReport(report.copy(status = status))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeReportedContent(adminUserId: Long, reportId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            verifyAdminAuthorization(adminUserId)
            val report = reportDao.getReportById(reportId)
                ?: return@withContext Result.failure(IllegalArgumentException("Report not found"))

            when (report.targetType) {
                "STORY" -> {
                    val story = storyDao.getStoryById(report.targetId)
                    if (story != null) {
                        storyDao.markStoryDeleted(story.id)
                        mediaStorageManager.deleteFile(story.mediaUri)
                    }
                }
                "SNAP" -> {
                    val snap = snapDao.getSnapById(report.targetId)
                    if (snap != null) {
                        snapDao.markExpired(snap.id)
                        mediaStorageManager.deleteFile(snap.mediaUri)
                    }
                }
                "MESSAGE" -> {
                    // Soft delete message
                    chatDao.softDeleteMessage(report.targetId, 0L)
                }
                "USER" -> {
                    userDao.setSuspended(report.targetId, true)
                }
            }

            reportDao.updateReport(report.copy(status = "RESOLVED"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
