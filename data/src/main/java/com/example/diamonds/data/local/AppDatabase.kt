package com.example.diamonds.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.diamonds.data.local.dao.BookingDao
import com.example.diamonds.data.local.dao.ClientDao
import com.example.diamonds.data.local.dao.NotificationDao
import com.example.diamonds.data.local.dao.PaymentDao
import com.example.diamonds.data.local.dao.ProviderDao
import com.example.diamonds.data.local.dao.ProviderLocationDao
import com.example.diamonds.data.local.dao.ReviewDao
import com.example.diamonds.data.local.dao.ServiceDao
import com.example.diamonds.data.local.dao.SyncQueueDao
import com.example.diamonds.data.local.entity.BookingEntity
import com.example.diamonds.data.local.entity.ClientEntity
import com.example.diamonds.data.local.entity.NotificationEntity
import com.example.diamonds.data.local.entity.PaymentEntity
import com.example.diamonds.data.local.entity.ProviderEntity
import com.example.diamonds.data.local.entity.ProviderLocationEntity
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
        ProviderLocationEntity::class
    ],
    version = 4,
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

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "diamonds_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
