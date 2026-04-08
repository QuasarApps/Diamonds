package com.example.diamonds.data.repository

import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.data.local.AppDatabase
import com.example.diamonds.data.mapper.toDomain
import com.example.diamonds.data.mapper.toEntity
import com.example.diamonds.data.remote.backend.CreateBookingRequest
import com.example.diamonds.data.remote.backend.IBackendService
import com.example.diamonds.domain.model.Booking
import com.example.diamonds.domain.model.BookingStatus
import com.example.diamonds.domain.model.OfflineException
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.model.SyncStatus
import com.example.diamonds.domain.repository.IBookingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Booking repository with offline-first, read-only offline pattern
 * - READ operations: cached first, fetch from server if stale/empty
 * - WRITE operations: require ONLINE state, return OfflineException if offline
 */
class BookingRepository(
    db: AppDatabase,
    private val backendService: IBackendService,
    private val connectivityObserver: ConnectivityObserver
) : IBookingRepository {

    private val bookingDao = db.bookingDao()

    override suspend fun createBooking(booking: Booking): Result<Booking> {
        // WRITE operation: require online
        if (!connectivityObserver.isOnline()) {
            return Result.Error(OfflineException("Booking creation requires internet connection"))
        }

        return try {
            // Send to backend immediately
            val request = CreateBookingRequest(
                clientId = booking.clientId,
                providerId = booking.providerId,
                serviceId = booking.serviceId,
                scheduledDate = booking.scheduledDate,
                scheduledTime = booking.scheduledTime,
                address = booking.address,
                latitude = booking.latitude,
                longitude = booking.longitude,
                notes = booking.notes
            )

            val backendResult = backendService.createBooking(request)
            when (backendResult) {
                is Result.Success -> {
                    // Update local cache only after server confirms
                    val domainBooking = backendResult.data.toDomain().copy(syncStatus = SyncStatus.SYNCED)
                    bookingDao.upsert(domainBooking.toEntity())
                    Result.Success(domainBooking)
                }
                is Result.Error -> {
                    // Network error during creation - don't queue, return error
                    Result.Error(backendResult.exception)
                }
                is Result.Loading -> Result.Loading
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getBooking(bookingId: String): Result<Booking> {
        // READ operation: cached first if available
        val cached = bookingDao.getById(bookingId)
        if (cached != null) {
            return Result.Success(cached.toDomain())
        }

        // Not in cache, fetch from backend if online
        if (!connectivityObserver.isOnline()) {
            return Result.Error(OfflineException("Booking not available offline"))
        }

        return try {
            val result = backendService.getBooking(bookingId)
            when (result) {
                is Result.Success -> {
                    val domainBooking = result.data.toDomain().copy(syncStatus = SyncStatus.SYNCED)
                    bookingDao.upsert(domainBooking.toEntity())
                    Result.Success(domainBooking)
                }
                is Result.Error -> result
                is Result.Loading -> Result.Loading
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getClientBookings(clientId: String): Result<List<Booking>> {
        // READ operation: return cached bookings immediately
        val cached = bookingDao.getForClient(clientId)
        if (cached.isNotEmpty()) {
            return Result.Success(cached.map { it.toDomain() })
        }

        // Cache empty, fetch from backend if online
        if (!connectivityObserver.isOnline()) {
            return Result.Error(OfflineException("Bookings not available offline"))
        }

        return try {
            val result = backendService.getClientBookings(clientId)
            when (result) {
                is Result.Success -> {
                    val bookings = result.data.map { it.toDomain().copy(syncStatus = SyncStatus.SYNCED) }
                    bookings.forEach { bookingDao.upsert(it.toEntity()) }
                    Result.Success(bookings)
                }
                is Result.Error -> result
                is Result.Loading -> Result.Loading
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getProviderBookings(providerId: String): Result<List<Booking>> {
        // READ operation: return cached bookings immediately
        val cached = bookingDao.getForProvider(providerId)
        if (cached.isNotEmpty()) {
            return Result.Success(cached.map { it.toDomain() })
        }

        // Cache empty, fetch from backend if online
        if (!connectivityObserver.isOnline()) {
            return Result.Error(OfflineException("Bookings not available offline"))
        }

        return try {
            val result = backendService.getProviderBookings(providerId)
            when (result) {
                is Result.Success -> {
                    val bookings = result.data.map { it.toDomain().copy(syncStatus = SyncStatus.SYNCED) }
                    bookings.forEach { bookingDao.upsert(it.toEntity()) }
                    Result.Success(bookings)
                }
                is Result.Error -> result
                is Result.Loading -> Result.Loading
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateBookingStatus(bookingId: String, status: BookingStatus): Result<Booking> {
        // WRITE operation: require online
        if (!connectivityObserver.isOnline()) {
            return Result.Error(OfflineException("Booking update requires internet connection"))
        }

        return try {
            val result = backendService.updateBookingStatus(bookingId, status.name)
            when (result) {
                is Result.Success -> {
                    val domainBooking = result.data.toDomain().copy(syncStatus = SyncStatus.SYNCED)
                    bookingDao.upsert(domainBooking.toEntity())
                    Result.Success(domainBooking)
                }
                is Result.Error -> result
                is Result.Loading -> Result.Loading
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun cancelBooking(bookingId: String): Result<Booking> {
        // WRITE operation: require online
        if (!connectivityObserver.isOnline()) {
            return Result.Error(OfflineException("Booking cancellation requires internet connection"))
        }

        return try {
            val result = backendService.cancelBooking(bookingId)
            when (result) {
                is Result.Success -> {
                    val domainBooking = result.data.toDomain().copy(syncStatus = SyncStatus.SYNCED)
                    bookingDao.upsert(domainBooking.toEntity())
                    Result.Success(domainBooking)
                }
                is Result.Error -> result
                is Result.Loading -> Result.Loading
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun observeBooking(bookingId: String): Flow<Booking?> =
        bookingDao.observeById(bookingId).map { it?.toDomain() }

    override fun observeClientBookings(clientId: String): Flow<List<Booking>> =
        bookingDao.observeForClient(clientId).map { bookings -> bookings.map { it.toDomain() } }

    override fun observeProviderBookings(providerId: String): Flow<List<Booking>> =
        bookingDao.observeForProvider(providerId).map { bookings -> bookings.map { it.toDomain() } }
}
