package com.example

import android.app.Application
import com.example.data.local.SnaporaDatabase
import com.example.data.repository.AdminRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.MediaStorageManager
import com.example.data.repository.NotificationRepository
import com.example.data.repository.ReportRepository
import com.example.data.repository.SnapRepository
import com.example.data.repository.SocialRepository
import com.example.data.repository.StoryRepository

class SnaporaApplication : Application() {

    lateinit var database: SnaporaDatabase
        private set
    lateinit var mediaStorageManager: MediaStorageManager
        private set
    lateinit var authRepository: AuthRepository
        private set
    lateinit var socialRepository: SocialRepository
        private set
    lateinit var chatRepository: ChatRepository
        private set
    lateinit var snapRepository: SnapRepository
        private set
    lateinit var storyRepository: StoryRepository
        private set
    lateinit var adminRepository: AdminRepository
        private set
    lateinit var reportRepository: ReportRepository
        private set
    lateinit var notificationRepository: NotificationRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = SnaporaDatabase.getInstance(this)
        mediaStorageManager = MediaStorageManager(this)

        authRepository = AuthRepository(database.userDao(), this)
        notificationRepository = NotificationRepository(database.notificationDao())
        socialRepository = SocialRepository(database.socialDao(), database.userDao(), database.notificationDao())
        chatRepository = ChatRepository(database.chatDao(), database.userDao(), database.socialDao(), database.notificationDao())
        snapRepository = SnapRepository(database.snapDao(), database.userDao(), chatRepository, mediaStorageManager)
        storyRepository = StoryRepository(database.storyDao(), database.userDao(), database.notificationDao(), mediaStorageManager)
        adminRepository = AdminRepository(database.userDao(), database.reportDao(), database.storyDao(), database.snapDao(), database.chatDao(), mediaStorageManager)
        reportRepository = ReportRepository(database.reportDao())
    }
}
