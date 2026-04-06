package com.example.diamonds.data.repository

import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.data.local.AppDatabase
import com.example.diamonds.data.mapper.toDomain
import com.example.diamonds.data.remote.backend.CreateReviewRequest
import com.example.diamonds.data.remote.backend.IBackendService
import com.example.diamonds.domain.model.*
import com.example.diamonds.domain.repository.IReviewRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Review repository - reads are cached, writes require online
 */
class ReviewRepository(
    private val db: AppDatabase,
    private val backendService: IBackendService,
    private val connectivityObserver: ConnectivityObserver
) : IReviewRepository {

    private val reviewDao = db.reviewDao()

    override suspend fun createReview(review: Review): Result<Review> {
        // WRITE: Require online
        if (!connectivityObserver.isOnline()) {
            return Result.Error(OfflineException("Review submission requires internet connection"))
        }

        return try {
            val request = CreateReviewRequest(
                bookingId = review.bookingId,
                clientId = review.clientId,
                providerId = review.providerId,
                rating = review.rating,
                comment = review.comment,
                imageUrls = review.imageUrls
            )

            val result = backendService.createReview(request)
            when (result) {
                is Result.Success -> {
                    val domainReview = result.data.toDomain().copy(syncStatus = SyncStatus.SYNCED)
                    reviewDao.upsert(domainReview.toEntity())
                    Result.Success(domainReview)
                }
                is Result.Error -> result
                is Result.Loading -> Result.Loading
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getReviewsForProvider(providerId: String): Result<List<Review>> {
        // READ: Check cache first
        val cached = reviewDao.getForProvider(providerId)
        if (cached.isNotEmpty()) {
            return Result.Success(cached.map { it.toDomain() })
        }

        // Cache empty, fetch if online
        if (!connectivityObserver.isOnline()) {
            return Result.Error(OfflineException("Reviews not available offline"))
        }

        return try {
            val result = backendService.getReviewsForProvider(providerId)
            when (result) {
                is Result.Success -> {
                    val reviews = result.data.map { it.toDomain().copy(syncStatus = SyncStatus.SYNCED) }
                    reviews.forEach { reviewDao.upsert(it.toEntity()) }
                    Result.Success(reviews)
                }
                is Result.Error -> result
                is Result.Loading -> Result.Loading
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getReviewsForBooking(bookingId: String): Result<Review?> {
        // READ: Check cache first
        val cached = reviewDao.getForBooking(bookingId)
        if (cached != null) {
            return Result.Success(cached.toDomain())
        }

        // Cache empty, fetch if online
        if (!connectivityObserver.isOnline()) {
            return Result.Success(null) // No review yet is not an error
        }

        // TODO: Implement fetch from backend
        return Result.Success(null)
    }

    override fun observeReviewsForProvider(providerId: String): Flow<List<Review>> =
        reviewDao.observeForProvider(providerId).map { reviews ->
            reviews.map { it.toDomain() }
        }

    // Extension function for toEntity on Review domain model
    private fun Review.toEntity(): com.example.diamonds.data.local.entity.ReviewEntity {
        return com.example.diamonds.data.local.entity.ReviewEntity(
            id = id,
            bookingId = bookingId,
            clientId = clientId,
            providerId = providerId,
            rating = rating,
            comment = comment,
            imageUrls = try {
                if (imageUrls.isEmpty()) "" else imageUrls.joinToString(",", "[\"", "\"]") { "\"$it\"" }
            } catch (e: Exception) {
                ""
            },
            syncStatus = syncStatus.name,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
