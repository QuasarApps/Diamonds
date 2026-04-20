package com.example.diamonds.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.diamonds.data.local.dao.BookingDao
import com.example.diamonds.data.local.dao.ClaimDao
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
import com.example.diamonds.data.local.dao.SupportTicketDao
import com.example.diamonds.data.local.dao.SyncQueueDao
import com.example.diamonds.data.local.dao.SavedLocationDao
import com.example.diamonds.data.local.entity.BookingEntity
import com.example.diamonds.data.local.entity.ClaimEntity
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
import com.example.diamonds.data.local.entity.SupportTicketEntity
import com.example.diamonds.data.local.entity.SyncQueueEntity
import com.example.diamonds.data.local.entity.SavedLocationEntity

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
        RecurringBookingEntity::class,
        SupportTicketEntity::class,
        ClaimEntity::class,
        SavedLocationEntity::class
    ],
    version = 10,
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
    abstract fun supportTicketDao(): SupportTicketDao
    abstract fun claimDao(): ClaimDao
    abstract fun savedLocationDao(): SavedLocationDao

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

        /** v7 → v8: add reviewDirection and locationTags columns to reviews table for bidirectional reviews. */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reviews ADD COLUMN reviewDirection TEXT NOT NULL DEFAULT 'CLIENT_REVIEWS_PROVIDER'")
                db.execSQL("ALTER TABLE reviews ADD COLUMN locationTags TEXT NOT NULL DEFAULT ''")
            }
        }

        /** v8 → v9: add support_tickets and claims tables for help & claims system. */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS support_tickets (
                        id TEXT NOT NULL PRIMARY KEY,
                        userId TEXT NOT NULL,
                        userRole TEXT NOT NULL,
                        bookingId TEXT,
                        type TEXT NOT NULL,
                        status TEXT NOT NULL,
                        subject TEXT NOT NULL,
                        description TEXT NOT NULL,
                        conversationId TEXT,
                        createdAt TEXT NOT NULL,
                        updatedAt TEXT NOT NULL
                    )
                """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS claims (
                        id TEXT NOT NULL PRIMARY KEY,
                        bookingId TEXT NOT NULL,
                        filedByUserId TEXT NOT NULL,
                        filedByRole TEXT NOT NULL,
                        claimType TEXT NOT NULL,
                        status TEXT NOT NULL,
                        description TEXT NOT NULL,
                        evidenceImageUrls TEXT NOT NULL DEFAULT '',
                        resolutionNotes TEXT,
                        refundAmount REAL,
                        createdAt TEXT NOT NULL,
                        updatedAt TEXT NOT NULL
                    )
                """.trimIndent()
                )
            }
        }

        /** v9 → v10: add specializations to providers, cleaningType/locationType to bookings, saved_locations table. */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE providers ADD COLUMN specializations TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE bookings ADD COLUMN cleaningType TEXT")
                db.execSQL("ALTER TABLE bookings ADD COLUMN locationType TEXT")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS saved_locations (
                        id TEXT NOT NULL PRIMARY KEY,
                        clientId TEXT NOT NULL,
                        label TEXT NOT NULL,
                        address TEXT NOT NULL,
                        latitude REAL,
                        longitude REAL,
                        locationType TEXT NOT NULL DEFAULT 'HOUSE',
                        roomCount INTEGER NOT NULL DEFAULT 1,
                        bathroomCount INTEGER NOT NULL DEFAULT 1,
                        sqFootage INTEGER,
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
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
