package com.example.diamonds.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.diamonds.data.local.dao.BookingDao
import com.example.diamonds.data.local.dao.ClientDao
import com.example.diamonds.data.local.dao.ConversationDao
import com.example.diamonds.data.local.dao.MessageDao
import com.example.diamonds.data.local.dao.NotificationDao
import com.example.diamonds.data.local.dao.PaymentDao
import com.example.diamonds.data.local.dao.ProviderDao
import com.example.diamonds.data.local.dao.ProviderLocationDao
import com.example.diamonds.data.local.dao.RecurringBookingDao
import com.example.diamonds.data.local.dao.ReviewDao
import com.example.diamonds.data.local.dao.ServiceDao
import com.example.diamonds.data.local.dao.SyncQueueDao
import com.example.diamonds.data.local.entity.BookingEntity
import com.example.diamonds.data.local.entity.ClientEntity
import com.example.diamonds.data.local.entity.ConversationEntity
import com.example.diamonds.data.local.entity.MessageEntity
import com.example.diamonds.data.local.entity.NotificationEntity
import com.example.diamonds.data.local.entity.PaymentEntity
import com.example.diamonds.data.local.entity.ProviderEntity
import com.example.diamonds.data.local.entity.ProviderLocationEntity
import com.example.diamonds.data.local.entity.RecurringBookingEntity
import com.example.diamonds.data.local.entity.ReviewEntity
import com.example.diamonds.data.local.entity.ServiceEntity
import com.example.diamonds.data.local.entity.SyncQueueEntity

@Database(
    entities = [
        ClientEntity::class,
        ProviderEntity::class,
        ServiceEntity::class,
        BookingEntity::class,
        ReviewEntity::class,
        PaymentEntity::class,
        SyncQueueEntity::class,
        NotificationEntity::class,
        ProviderLocationEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        RecurringBookingEntity::class
    ],
    version = 7,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clientDao(): ClientDao
    abstract fun providerDao(): ProviderDao
    abstract fun serviceDao(): ServiceDao
    abstract fun bookingDao(): BookingDao
    abstract fun reviewDao(): ReviewDao
    abstract fun paymentDao(): PaymentDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun notificationDao(): NotificationDao
    abstract fun providerLocationDao(): ProviderLocationDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun recurringBookingDao(): RecurringBookingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** v1 → v2: add cleanerType, employerId, employerName to providers table. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE providers ADD COLUMN cleanerType TEXT NOT NULL DEFAULT 'INDEPENDENT'")
                db.execSQL("ALTER TABLE providers ADD COLUMN employerId TEXT")
                db.execSQL("ALTER TABLE providers ADD COLUMN employerName TEXT")
            }
        }

        /** v2 → v3: add notifications table for push / in-app notifications. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS notifications (
                        id TEXT NOT NULL PRIMARY KEY,
                        userId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        body TEXT NOT NULL,
                        type TEXT NOT NULL,
                        referenceId TEXT,
                        isRead INTEGER NOT NULL DEFAULT 0,
                        createdAt TEXT NOT NULL
                    )
                """.trimIndent()
                )
            }
        }

        /** v3 → v4: add provider_locations table for maps / live tracking. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS provider_locations (
                        providerId TEXT NOT NULL PRIMARY KEY,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        heading REAL NOT NULL DEFAULT 0,
                        updatedAt TEXT NOT NULL
                    )
                """.trimIndent()
                )
            }
        }

        /** v4 → v5: add serverPayload column to sync_queue for conflict resolution. */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sync_queue ADD COLUMN serverPayload TEXT")
            }
        }

        /** v5 → v6: add conversations and messages tables for in-app chat. */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS conversations (
                        id TEXT NOT NULL PRIMARY KEY,
                        bookingId TEXT NOT NULL,
                        clientId TEXT NOT NULL,
                        clientName TEXT NOT NULL,
                        providerId TEXT NOT NULL,
                        providerName TEXT NOT NULL,
                        lastMessage TEXT NOT NULL DEFAULT '',
                        lastMessageAt TEXT NOT NULL DEFAULT '',
                        unreadCount INTEGER NOT NULL DEFAULT 0,
                        updatedAt TEXT NOT NULL
                    )
                """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS messages (
                        id TEXT NOT NULL PRIMARY KEY,
                        conversationId TEXT NOT NULL,
                        senderId TEXT NOT NULL,
                        senderName TEXT NOT NULL,
                        body TEXT NOT NULL,
                        isRead INTEGER NOT NULL DEFAULT 0,
                        createdAt TEXT NOT NULL
                    )
                """.trimIndent()
                )
            }
        }

        /** v6 → v7: add recurring_bookings table for subscription / recurring booking support. */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS recurring_bookings (
                        id TEXT NOT NULL PRIMARY KEY,
                        clientId TEXT NOT NULL,
                        providerId TEXT NOT NULL,
                        providerName TEXT NOT NULL,
                        serviceId TEXT NOT NULL,
                        serviceName TEXT NOT NULL,
                        frequency TEXT NOT NULL,
                        preferredDay INTEGER NOT NULL,
                        preferredTime TEXT NOT NULL,
                        address TEXT NOT NULL,
                        latitude REAL,
                        longitude REAL,
                        totalPrice REAL NOT NULL,
                        status TEXT NOT NULL,
                        nextBookingDate TEXT NOT NULL,
                        createdAt TEXT NOT NULL,
                        updatedAt TEXT NOT NULL
                    )
                """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "diamonds_database"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
