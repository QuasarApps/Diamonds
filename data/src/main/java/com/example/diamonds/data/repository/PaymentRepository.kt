package com.example.diamonds.data.repository

import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.data.local.AppDatabase
import com.example.diamonds.data.mapper.toDomain
import com.example.diamonds.data.remote.backend.CreatePaymentRequest
import com.example.diamonds.data.remote.backend.IBackendService
import com.example.diamonds.domain.model.OfflineException
import com.example.diamonds.domain.model.Payment
import com.example.diamonds.domain.model.PaymentStatus
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.model.SyncStatus
import com.example.diamonds.domain.repository.IPaymentRepository

/**
 * Payment repository - reads are cached, writes require online
 */
class PaymentRepository(
    db: AppDatabase,
    private val backendService: IBackendService,
    private val connectivityObserver: ConnectivityObserver
) : IPaymentRepository {

    private val paymentDao = db.paymentDao()

    override suspend fun createPayment(payment: Payment): Result<Payment> {
        // WRITE: Require online
        if (!connectivityObserver.isOnline()) {
            return Result.Error(OfflineException("Payment processing requires internet connection"))
        }

        return try {
            val request = CreatePaymentRequest(
                bookingId = payment.bookingId,
                clientId = payment.clientId,
                providerId = payment.providerId,
                amount = payment.amount,
                method = payment.method.name
            )

            val result = backendService.createPayment(request)
            when (result) {
                is Result.Success -> {
                    val domainPayment = result.data.toDomain().copy(syncStatus = SyncStatus.SYNCED)
                    paymentDao.upsert(domainPayment.toEntity())
                    Result.Success(domainPayment)
                }
                is Result.Error -> result
                is Result.Loading -> Result.Loading
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getPayment(paymentId: String): Result<Payment> {
        // READ: Check cache first
        val cached = paymentDao.getById(paymentId)
        if (cached != null) {
            return Result.Success(cached.toDomain())
        }

        // Cache empty, fetch if online
        if (!connectivityObserver.isOnline()) {
            return Result.Error(OfflineException("Payment not available offline"))
        }

        return try {
            val result = backendService.getPayment(paymentId)
            when (result) {
                is Result.Success -> {
                    val payment = result.data.toDomain().copy(syncStatus = SyncStatus.SYNCED)
                    paymentDao.upsert(payment.toEntity())
                    Result.Success(payment)
                }
                is Result.Error -> result
                is Result.Loading -> Result.Loading
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getPaymentsForClient(clientId: String): Result<List<Payment>> {
        // READ: Check cache first
        val cached = paymentDao.getForClient(clientId)
        if (cached.isNotEmpty()) {
            return Result.Success(cached.map { it.toDomain() })
        }
        if (!connectivityObserver.isOnline()) {
            return Result.Error(OfflineException("Payments not available offline"))
        }
        return try {
            when (val r = backendService.getPaymentsForClient(clientId)) {
                is Result.Success -> {
                    val payments = r.data.map { it.toDomain().copy(syncStatus = SyncStatus.SYNCED) }
                    payments.forEach { paymentDao.upsert(it.toEntity()) }
                    Result.Success(payments)
                }
                is Result.Error   -> r
                is Result.Loading -> Result.Loading
            }
        } catch (e: Exception) { Result.Error(e) }
    }

    override suspend fun getPaymentsForProvider(providerId: String): Result<List<Payment>> {
        // READ: Check cache first
        val cached = paymentDao.getForProvider(providerId)
        if (cached.isNotEmpty()) {
            return Result.Success(cached.map { it.toDomain() })
        }

        // Cache empty, fetch if online
        if (!connectivityObserver.isOnline()) {
            return Result.Error(OfflineException("Payments not available offline"))
        }

        return try {
            Result.Error(Exception("Not implemented in backend service"))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updatePaymentStatus(paymentId: String, status: PaymentStatus): Result<Payment> {
        // WRITE: Require online
        if (!connectivityObserver.isOnline()) {
            return Result.Error(OfflineException("Payment update requires internet connection"))
        }

        // TODO: Implement in backend service
        return Result.Error(Exception("Not implemented in backend service"))
    }

    // Extension function for toEntity on Payment domain model
    private fun Payment.toEntity(): com.example.diamonds.data.local.entity.PaymentEntity {
        return com.example.diamonds.data.local.entity.PaymentEntity(
            id = id,
            bookingId = bookingId,
            clientId = clientId,
            providerId = providerId,
            amount = amount,
            status = status.name,
            method = method.name,
            transactionId = transactionId,
            syncStatus = syncStatus.name,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
