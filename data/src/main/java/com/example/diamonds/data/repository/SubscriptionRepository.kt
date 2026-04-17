package com.example.diamonds.data.repository

import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.data.local.AppDatabase
import com.example.diamonds.data.mapper.toDomain
import com.example.diamonds.data.mapper.toEntity
import com.example.diamonds.data.remote.backend.CreateRecurringBookingRequest
import com.example.diamonds.data.remote.backend.IBackendService
import com.example.diamonds.domain.model.RecurringBooking
import com.example.diamonds.domain.model.RecurringBookingStatus
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.repository.ISubscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class SubscriptionRepository @Inject constructor(
    private val db: AppDatabase,
    private val backendService: IBackendService,
    private val connectivityObserver: ConnectivityObserver
) : ISubscriptionRepository {

    private val dao = db.recurringBookingDao()
    private fun now() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    override suspend fun createRecurringBooking(recurringBooking: RecurringBooking): Result<RecurringBooking> {
        return try {
            if (!connectivityObserver.isOnline()) {
                return Result.Error(Exception("No internet connection"))
            }
            val request = CreateRecurringBookingRequest(
                clientId = recurringBooking.clientId,
                providerId = recurringBooking.providerId,
                providerName = recurringBooking.providerName,
                serviceId = recurringBooking.serviceId,
                serviceName = recurringBooking.serviceName,
                frequency = recurringBooking.frequency.name,
                preferredDay = recurringBooking.preferredDay,
                preferredTime = recurringBooking.preferredTime,
                address = recurringBooking.address,
                latitude = recurringBooking.latitude,
                longitude = recurringBooking.longitude,
                totalPrice = recurringBooking.totalPrice
            )
            when (val result = backendService.createRecurringBooking(request)) {
                is Result.Success -> {
                    val domain = result.data.toDomain()
                    dao.upsert(domain.toEntity())
                    Result.Success(domain)
                }

                is Result.Error -> result
                else -> Result.Error(Exception("Unexpected result"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getRecurringBooking(id: String): Result<RecurringBooking> {
        val cached = dao.getById(id)
        if (cached != null) return Result.Success(cached.toDomain())

        return try {
            if (!connectivityObserver.isOnline()) return Result.Error(Exception("Not found"))
            when (val result = backendService.getRecurringBooking(id)) {
                is Result.Success -> {
                    val domain = result.data.toDomain()
                    dao.upsert(domain.toEntity())
                    Result.Success(domain)
                }

                is Result.Error -> result
                else -> Result.Error(Exception("Unexpected result"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getRecurringBookingsForClient(clientId: String): Result<List<RecurringBooking>> {
        // Refresh from backend if online
        try {
            if (connectivityObserver.isOnline()) {
                when (val result = backendService.getRecurringBookingsForClient(clientId)) {
                    is Result.Success -> {
                        result.data.forEach { dto -> dao.upsert(dto.toDomain().toEntity()) }
                    }

                    else -> { /* ignore, use cache */
                    }
                }
            }
        } catch (_: Exception) {
        }
        return Result.Success(dao.getForClient(clientId).map { it.toDomain() })
    }

    override suspend fun updateRecurringBookingStatus(
        id: String,
        status: RecurringBookingStatus
    ): Result<RecurringBooking> {
        return try {
            if (!connectivityObserver.isOnline()) {
                return Result.Error(Exception("No internet connection"))
            }
            when (val result = backendService.updateRecurringBookingStatus(id, status.name)) {
                is Result.Success -> {
                    val domain = result.data.toDomain()
                    dao.upsert(domain.toEntity())
                    Result.Success(domain)
                }

                is Result.Error -> result
                else -> Result.Error(Exception("Unexpected result"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateSchedule(
        id: String,
        preferredDay: Int,
        preferredTime: String
    ): Result<RecurringBooking> {
        return try {
            if (!connectivityObserver.isOnline()) {
                return Result.Error(Exception("No internet connection"))
            }
            when (val result =
                backendService.updateRecurringBookingSchedule(id, preferredDay, preferredTime)) {
                is Result.Success -> {
                    val domain = result.data.toDomain()
                    dao.upsert(domain.toEntity())
                    Result.Success(domain)
                }

                is Result.Error -> result
                else -> Result.Error(Exception("Unexpected result"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun observeRecurringBookingsForClient(clientId: String): Flow<List<RecurringBooking>> {
        return dao.observeForClient(clientId).map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getActiveRecurringBookingsDue(todayIso: String): Result<List<RecurringBooking>> {
        return Result.Success(dao.getActiveDue(todayIso).map { it.toDomain() })
    }

    override suspend fun advanceNextBookingDate(id: String, newDate: String): Result<Unit> {
        dao.advanceNextBookingDate(id, newDate, now())
        // Best-effort remote sync
        try {
            if (connectivityObserver.isOnline()) {
                backendService.advanceRecurringBookingDate(id, newDate)
            }
        } catch (_: Exception) {
        }
        return Result.Success(Unit)
    }
}
