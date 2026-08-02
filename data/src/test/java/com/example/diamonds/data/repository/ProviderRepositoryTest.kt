package com.example.diamonds.data.repository

import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.data.local.AppDatabase
import com.example.diamonds.data.local.dao.ProviderDao
import com.example.diamonds.data.remote.backend.IBackendService
import com.example.diamonds.data.remote.backend.ProviderDto
import com.example.diamonds.domain.model.CleanerType
import com.example.diamonds.domain.model.CleaningType
import com.example.diamonds.domain.model.OfflineException
import com.example.diamonds.domain.model.Provider
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.model.VerificationStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Covers `updateProvider`, which until now returned `Result.Error("Not implemented in backend
 * service")` regardless of input — cleaners could edit their profile in the UI and the save
 * always failed.
 */
class ProviderRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var providerDao: ProviderDao
    private lateinit var backendService: IBackendService
    private lateinit var connectivityObserver: ConnectivityObserver
    private lateinit var repository: ProviderRepository

    private val provider = Provider(
        id = "p1",
        name = "Ada Cleaners",
        email = "ada@example.com",
        phoneNumber = "+15550100",
        bio = "Ten years of deep cleans",
        rating = 4.8f,
        reviewCount = 42,
        verificationStatus = VerificationStatus.APPROVED,
        serviceRadius = 25,
        cleanerType = CleanerType.EMPLOYED,
        employerId = "co1",
        employerName = "Sparkle Co",
        specializations = listOf(CleaningType.DEEP_CLEAN, CleaningType.MOVE_IN_MOVE_OUT),
        createdAt = "2026-04-07",
        updatedAt = "2026-04-07"
    )

    @Before
    fun setUp() {
        providerDao = mockk(relaxed = true)
        db = mockk { every { providerDao() } returns providerDao }
        backendService = mockk()
        connectivityObserver = mockk { every { isOnline() } returns true }
        repository = ProviderRepository(db, backendService, connectivityObserver)
    }

    @Test
    fun `updateProvider fails offline`() = runTest {
        every { connectivityObserver.isOnline() } returns false

        val result = repository.updateProvider(provider)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is OfflineException)
    }

    @Test
    fun `updateProvider sends every field to the backend`() = runTest {
        val dto = slot<ProviderDto>()
        coEvery { backendService.updateProvider(capture(dto)) } answers { Result.Success(dto.captured) }

        val result = repository.updateProvider(provider)

        assertTrue(result is Result.Success)
        assertEquals("p1", dto.captured.id)
        assertEquals("Ten years of deep cleans", dto.captured.bio)
        assertEquals(25, dto.captured.serviceRadius)
        // Enums travel as their `name`, which is what ProviderDto.toDomain() parses back.
        assertEquals(VerificationStatus.APPROVED.name, dto.captured.verificationStatus)
        assertEquals(CleanerType.EMPLOYED.name, dto.captured.cleanerType)
        assertEquals(listOf("DEEP_CLEAN", "MOVE_IN_MOVE_OUT"), dto.captured.specializations)
        assertEquals("co1", dto.captured.employerId)
    }

    @Test
    fun `updateProvider caches the backend response`() = runTest {
        coEvery { backendService.updateProvider(any()) } answers { Result.Success(firstArg<ProviderDto>()) }

        val result = repository.updateProvider(provider) as Result.Success

        assertEquals(provider, result.data)
        coVerify { providerDao.upsert(any()) }
    }

    @Test
    fun `updateProvider propagates backend error without caching`() = runTest {
        coEvery { backendService.updateProvider(any()) } returns Result.Error(Exception("server error"))

        val result = repository.updateProvider(provider)

        assertTrue(result is Result.Error)
        assertEquals("server error", (result as Result.Error).exception.message)
        coVerify(exactly = 0) { providerDao.upsert(any()) }
    }

    @Test
    fun `updateProvider wraps a thrown exception`() = runTest {
        coEvery { backendService.updateProvider(any()) } throws RuntimeException("boom")

        val result = repository.updateProvider(provider)

        assertTrue(result is Result.Error)
        assertEquals("boom", (result as Result.Error).exception.message)
    }
}
