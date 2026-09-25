package com.example.data.repository

import com.example.data.dao.NotificationDao
import com.example.data.dao.ReportDao
import com.example.data.model.NotificationEntity
import com.example.data.model.ReportEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ReportRepository(private val reportDao: ReportDao) {
    suspend fun submitReport(
        reporterId: Long,
        targetType: String,
        targetId: Long,
        reason: String,
        details: String = ""
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val report = ReportEntity(
                reporterId = reporterId,
                targetType = targetType,
                targetId = targetId,
                reason = reason,
                details = details,
                status = "PENDING"
            )
            val id = reportDao.insertReport(report)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class NotificationRepository(private val notificationDao: NotificationDao) {
    fun getNotifications(userId: Long): Flow<List<NotificationEntity>> {
        return notificationDao.getNotificationsForUser(userId)
    }

    fun getUnreadCount(userId: Long): Flow<Int> {
        return notificationDao.getUnreadNotificationCount(userId)
    }

    suspend fun markAllAsRead(userId: Long) = withContext(Dispatchers.IO) {
        notificationDao.markAllAsRead(userId)
    }

    suspend fun markAsRead(notificationId: Long) = withContext(Dispatchers.IO) {
        notificationDao.markAsRead(notificationId)
    }
}
