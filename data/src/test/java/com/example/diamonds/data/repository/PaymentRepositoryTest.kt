package com.example.diamonds.data.repository

import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.data.local.AppDatabase
import com.example.diamonds.data.local.dao.PaymentDao
import com.example.diamonds.data.remote.backend.IBackendService
import com.example.diamonds.data.remote.backend.PaymentDto
import com.example.diamonds.domain.model.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PaymentRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var paymentDao: PaymentDao
    private lateinit var backendService: IBackendService
    private lateinit var connectivityObserver: ConnectivityObserver
    private lateinit var repository: PaymentRepository

    private fun makePaymentDto(id: String = "pay1", clientId: String = "c1") = PaymentDto(
        id = id, bookingId = "b1", clientId = clientId, providerId = "p1",
        amount = 79.0, status = "SUCCEEDED", method = "CARD",
        transactionId = "txn_001", createdAt = "2026-04-07", updatedAt = "2026-04-07"
    )

    @Before
    fun setUp() {
        paymentDao = mockk(relaxed = true)
        db = mockk { every { paymentDao() } returns paymentDao }
        backendService = mockk()
        connectivityObserver = mockk { every { isOnline() } returns true }
        repository = PaymentRepository(db, backendService, connectivityObserver)
    }

    @Test
    fun `createPayment fails when offline`() = runTest {
        every { connectivityObserver.isOnline() } returns false

        val payment = Payment(id = "", bookingId = "b1", clientId = "c1", providerId = "p1",
            amount = 79.0, createdAt = "2026-04-07", updatedAt = "2026-04-07")
        val result = repository.createPayment(payment)
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is OfflineException)
    }

    @Test
    fun `createPayment delegates to backend and caches result`() = runTest {
        coEvery { backendService.createPayment(any()) } returns Result.Success(makePaymentDto())

        val payment = Payment(id = "", bookingId = "b1", clientId = "c1", providerId = "p1",
            amount = 79.0, createdAt = "2026-04-07", updatedAt = "2026-04-07")
        val result = repository.createPayment(payment) as Result.Success
        assertEquals("pay1", result.data.id)
        assertEquals(SyncStatus.SYNCED, result.data.syncStatus)
        coVerify { paymentDao.upsert(any()) }
    }

    @Test
    fun `getPayment returns cache hit without network call`() = runTest {
        val entity = com.example.diamonds.data.local.entity.PaymentEntity(
            id = "pay1", bookingId = "b1", clientId = "c1", providerId = "p1",
            amount = 79.0, status = "SUCCEEDED", method = "CARD",
            transactionId = "txn_001", syncStatus = SyncStatus.SYNCED.name,
            createdAt = "2026-04-07", updatedAt = "2026-04-07"
        )
        coEvery { paymentDao.getById("pay1") } returns entity

        val result = repository.getPayment("pay1") as Result.Success
        assertEquals("pay1", result.data.id)
        coVerify(exactly = 0) { backendService.getPayment(any()) }
    }

    @Test
    fun `getPayment fetches from backend on cache miss`() = runTest {
        coEvery { paymentDao.getById("pay1") } returns null
        coEvery { backendService.getPayment("pay1") } returns Result.Success(makePaymentDto())

        val result = repository.getPayment("pay1") as Result.Success
        assertEquals("pay1", result.data.id)
        coVerify { paymentDao.upsert(any()) }
    }

    @Test
    fun `getPaymentsForClient returns cached payments`() = runTest {
        val entity = com.example.diamonds.data.local.entity.PaymentEntity(
            id = "pay1", bookingId = "b1", clientId = "c1", providerId = "p1",
            amount = 79.0, status = "SUCCEEDED", method = "CARD",
            transactionId = null, syncStatus = SyncStatus.SYNCED.name,
            createdAt = "2026-04-07", updatedAt = "2026-04-07"
        )
        coEvery { paymentDao.getForClient("c1") } returns listOf(entity)

        val result = repository.getPaymentsForClient("c1") as Result.Success
        assertEquals(1, result.data.size)
        coVerify(exactly = 0) { backendService.getPaymentsForClient(any()) }
    }

    @Test
    fun `getPaymentsForClient fetches from backend when cache empty`() = runTest {
        coEvery { paymentDao.getForClient("c1") } returns emptyList()
        coEvery { backendService.getPaymentsForClient("c1") } returns
            Result.Success(listOf(makePaymentDto()))

        val result = repository.getPaymentsForClient("c1") as Result.Success
        assertEquals(1, result.data.size)
        coVerify { paymentDao.upsert(any()) }
    }

    @Test
    fun `getPaymentsForClient offline empty cache returns OfflineException`() = runTest {
        coEvery { paymentDao.getForClient("c1") } returns emptyList()
        every { connectivityObserver.isOnline() } returns false

        val result = repository.getPaymentsForClient("c1")
        assertTrue((result as Result.Error).exception is OfflineException)
    }
}
