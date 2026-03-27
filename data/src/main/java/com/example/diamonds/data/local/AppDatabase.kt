package com.example.diamonds.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.diamonds.data.local.dao.*
import com.example.diamonds.data.local.entity.*

@Database(
    entities = [
        ClientEntity::class,
        ProviderEntity::class,
        ServiceEntity::class,
        BookingEntity::class,
        ReviewEntity::class,
        PaymentEntity::class,
        SyncQueueEntity::class
    ],
    version = 1,
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

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "diamonds_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
