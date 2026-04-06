package com.example.diamonds.data.local.dao

import androidx.room.*
import com.example.diamonds.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(client: ClientEntity)

    @Query("SELECT * FROM clients WHERE id = :id")
    suspend fun getById(id: String): ClientEntity?

    @Query("SELECT * FROM clients WHERE id = :id")
    fun observeById(id: String): Flow<ClientEntity?>

    @Query("DELETE FROM clients WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface ProviderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(provider: ProviderEntity)

    @Query("SELECT * FROM providers WHERE id = :id")
    suspend fun getById(id: String): ProviderEntity?

    @Query("SELECT * FROM providers WHERE id = :id")
    fun observeById(id: String): Flow<ProviderEntity?>

    @Query("SELECT * FROM providers LIMIT :limit")
    suspend fun getNearby(limit: Int): List<ProviderEntity>


    @Delete
    suspend fun delete(provider: ProviderEntity)
}

@Dao
interface ServiceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(service: ServiceEntity)

    @Query("SELECT * FROM services WHERE id = :id")
    suspend fun getById(id: String): ServiceEntity?

    @Query("SELECT * FROM services WHERE providerId = :providerId")
    suspend fun getForProvider(providerId: String): List<ServiceEntity>

    @Query("SELECT * FROM services WHERE category = :category")
    suspend fun getByCategory(category: String): List<ServiceEntity>

    @Query("DELETE FROM services WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface BookingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(booking: BookingEntity)

    @Query("SELECT * FROM bookings WHERE id = :id")
    suspend fun getById(id: String): BookingEntity?

    @Query("SELECT * FROM bookings WHERE id = :id")
    fun observeById(id: String): Flow<BookingEntity?>

    @Query("SELECT * FROM bookings WHERE clientId = :clientId ORDER BY createdAt DESC")
    suspend fun getForClient(clientId: String): List<BookingEntity>

    @Query("SELECT * FROM bookings WHERE clientId = :clientId ORDER BY createdAt DESC")
    fun observeForClient(clientId: String): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE providerId = :providerId ORDER BY createdAt DESC")
    suspend fun getForProvider(providerId: String): List<BookingEntity>

    @Query("SELECT * FROM bookings WHERE providerId = :providerId ORDER BY createdAt DESC")
    fun observeForProvider(providerId: String): Flow<List<BookingEntity>>

    @Query("DELETE FROM bookings WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface ReviewDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(review: ReviewEntity)

    @Query("SELECT * FROM reviews WHERE id = :id")
    suspend fun getById(id: String): ReviewEntity?

    @Query("SELECT * FROM reviews WHERE providerId = :providerId ORDER BY createdAt DESC")
    suspend fun getForProvider(providerId: String): List<ReviewEntity>

    @Query("SELECT * FROM reviews WHERE providerId = :providerId ORDER BY createdAt DESC")
    fun observeForProvider(providerId: String): Flow<List<ReviewEntity>>

    @Query("SELECT * FROM reviews WHERE bookingId = :bookingId LIMIT 1")
    suspend fun getForBooking(bookingId: String): ReviewEntity?

    @Query("DELETE FROM reviews WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface PaymentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(payment: PaymentEntity)

    @Query("SELECT * FROM payments WHERE id = :id")
    suspend fun getById(id: String): PaymentEntity?

    @Query("SELECT * FROM payments WHERE clientId = :clientId ORDER BY createdAt DESC")
    suspend fun getForClient(clientId: String): List<PaymentEntity>

    @Query("SELECT * FROM payments WHERE providerId = :providerId ORDER BY createdAt DESC")
    suspend fun getForProvider(providerId: String): List<PaymentEntity>

    @Query("DELETE FROM payments WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface SyncQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(operation: SyncQueueEntity)

    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingOperations(): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue WHERE status = 'FAILED' ORDER BY lastAttemptAt ASC")
    suspend fun getFailedOperations(): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue WHERE status IN ('PENDING', 'FAILED') ORDER BY createdAt ASC")
    suspend fun getQueuedOperations(): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue WHERE status IN ('PENDING', 'FAILED') ORDER BY createdAt ASC")
    fun observeQueuedOperations(): Flow<List<SyncQueueEntity>>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status IN ('PENDING', 'FAILED')")
    fun observePendingCount(): Flow<Int>

    @Query("UPDATE sync_queue SET status = :newStatus WHERE id = :id")
    suspend fun updateStatus(id: String, newStatus: String)

    @Query("UPDATE sync_queue SET status = :newStatus, retryCount = retryCount + 1, lastAttemptAt = :timestamp, error = :error WHERE id = :id")
    suspend fun updateAfterRetry(id: String, newStatus: String, timestamp: String, error: String?)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM sync_queue WHERE status = 'CANCELLED'")
    suspend fun purgeCancelledOperations()
}
