package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.ChatDao
import com.example.data.dao.NotificationDao
import com.example.data.dao.ReportDao
import com.example.data.dao.SnapDao
import com.example.data.dao.SocialDao
import com.example.data.dao.StoryDao
import com.example.data.dao.UserDao
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

@Database(
    entities = [
        UserEntity::class,
        FriendRequestEntity::class,
        FriendshipEntity::class,
        BlockedUserEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        SnapEntity::class,
        StoryEntity::class,
        StoryViewEntity::class,
        NotificationEntity::class,
        ReportEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SnaporaDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun socialDao(): SocialDao
    abstract fun chatDao(): ChatDao
    abstract fun snapDao(): SnapDao
    abstract fun storyDao(): StoryDao
    abstract fun notificationDao(): NotificationDao
    abstract fun reportDao(): ReportDao

    companion object {
        @Volatile
        private var INSTANCE: SnaporaDatabase? = null

        fun getInstance(context: Context): SnaporaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SnaporaDatabase::class.java,
                    "snapora.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
