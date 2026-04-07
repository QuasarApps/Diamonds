package com.example.diamonds.data.repository

import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.data.local.AppDatabase
import com.example.diamonds.data.local.dao.ReviewDao
import com.example.diamonds.data.remote.backend.IBackendService
import com.example.diamonds.data.remote.backend.ReviewDto
import com.example.diamonds.domain.model.*
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ReviewRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var reviewDao: ReviewDao
    private lateinit var backendService: IBackendService
    private lateinit var connectivityObserver: ConnectivityObserver
    private lateinit var repository: ReviewRepository

    private fun makeReviewDto(id: String = "rv1", bookingId: String = "b1") = ReviewDto(
        id = id, bookingId = bookingId, clientId = "c1", providerId = "p1",
        rating = 5, comment = "Great!", createdAt = "2026-04-07", updatedAt = "2026-04-07"
    )

    @Before
    fun setUp() {
        reviewDao = mockk(relaxed = true)
        db = mockk { every { reviewDao() } returns reviewDao }
        backendService = mockk()
        connectivityObserver = mockk { every { isOnline() } returns true }
        repository = ReviewRepository(db, backendService, connectivityObserver)
    }

    @Test
    fun `createReview fails offline`() = runTest {
        every { connectivityObserver.isOnline() } returns false

        val review = Review(
            id = "", bookingId = "b1", clientId = "c1", providerId = "p1",
            rating = 5, createdAt = "2026-04-07", updatedAt = "2026-04-07"
        )
        val result = repository.createReview(review)
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is OfflineException)
    }

    @Test
    fun `createReview online calls backend and caches result`() = runTest {
        coEvery { backendService.createReview(any()) } returns Result.Success(makeReviewDto())

        val review = Review(
            id = "", bookingId = "b1", clientId = "c1", providerId = "p1",
            rating = 5, createdAt = "2026-04-07", updatedAt = "2026-04-07"
        )
        val result = repository.createReview(review) as Result.Success
        assertEquals("rv1", result.data.id)
        assertEquals(SyncStatus.SYNCED, result.data.syncStatus)
        coVerify { reviewDao.upsert(any()) }
    }

    @Test
    fun `createReview propagates backend error`() = runTest {
        coEvery { backendService.createReview(any()) } returns Result.Error(Exception("server error"))

        val review = Review(id = "", bookingId = "b1", clientId = "c1", providerId = "p1",
            rating = 4, createdAt = "2026-04-07", updatedAt = "2026-04-07")
        val result = repository.createReview(review)
        assertTrue(result is Result.Error)
    }

    @Test
    fun `getReviewsForProvider returns cache when non-empty`() = runTest {
        val entity = com.example.diamonds.data.local.entity.ReviewEntity(
            id = "rv1", bookingId = "b1", clientId = "c1", providerId = "p1",
            rating = 5, comment = "Good", imageUrls = "[]",
            syncStatus = SyncStatus.SYNCED.name, createdAt = "2026-04-07", updatedAt = "2026-04-07"
        )
        coEvery { reviewDao.getForProvider("p1") } returns listOf(entity)

        val result = repository.getReviewsForProvider("p1") as Result.Success
        assertEquals(1, result.data.size)
        coVerify(exactly = 0) { backendService.getReviewsForProvider(any()) }
    }

    @Test
    fun `getReviewsForProvider fetches from backend when cache empty`() = runTest {
        coEvery { reviewDao.getForProvider("p1") } returns emptyList()
        coEvery { backendService.getReviewsForProvider("p1") } returns
            Result.Success(listOf(makeReviewDto()))

        val result = repository.getReviewsForProvider("p1") as Result.Success
        assertEquals(1, result.data.size)
        coVerify { reviewDao.upsert(any()) }
    }

    @Test
    fun `getReviewsForProvider offline with empty cache returns OfflineException`() = runTest {
        coEvery { reviewDao.getForProvider("p1") } returns emptyList()
        every { connectivityObserver.isOnline() } returns false

        val result = repository.getReviewsForProvider("p1")
        assertTrue((result as Result.Error).exception is OfflineException)
    }

    @Test
    fun `getReviewsForBooking returns cached review when present`() = runTest {
        val entity = com.example.diamonds.data.local.entity.ReviewEntity(
            id = "rv1", bookingId = "b1", clientId = "c1", providerId = "p1",
            rating = 4, comment = null, imageUrls = "[]",
            syncStatus = SyncStatus.SYNCED.name, createdAt = "2026-04-07", updatedAt = "2026-04-07"
        )
        coEvery { reviewDao.getForBooking("b1") } returns entity

        val result = repository.getReviewsForBooking("b1") as Result.Success
        assertNotNull(result.data)
        assertEquals("rv1", result.data!!.id)
    }

    @Test
    fun `getReviewsForBooking returns null when no review exists`() = runTest {
        coEvery { reviewDao.getForBooking("b_none") } returns null
        every { connectivityObserver.isOnline() } returns false

        val result = repository.getReviewsForBooking("b_none") as Result.Success
        assertNull(result.data)
    }

    @Test
    fun `observeReviewsForProvider emits mapped domain objects`() = runTest {
        val entity = com.example.diamonds.data.local.entity.ReviewEntity(
            id = "rv2", bookingId = "b2", clientId = "c2", providerId = "p2",
            rating = 3, comment = "OK", imageUrls = "[]",
            syncStatus = SyncStatus.SYNCED.name, createdAt = "2026-04-07", updatedAt = "2026-04-07"
        )
        every { reviewDao.observeForProvider("p2") } returns flowOf(listOf(entity))

        var received: List<Review>? = null
        repository.observeReviewsForProvider("p2").collect { received = it }

        assertEquals(1, received?.size)
        assertEquals("rv2", received?.first()?.id)
    }
}
