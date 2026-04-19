package com.example.diamonds.data.repository

import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.data.local.AppDatabase
import com.example.diamonds.data.mapper.toDomain
import com.example.diamonds.data.remote.backend.CreateReviewRequest
import com.example.diamonds.data.remote.backend.IBackendService
import com.example.diamonds.domain.model.OfflineException
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.model.Review
import com.example.diamonds.domain.model.ReviewDirection
import com.example.diamonds.domain.model.SyncStatus
import com.example.diamonds.domain.repository.IReviewRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Review repository - reads are cached, writes require online.
 * Supports bidirectional reviews (CLIENT_REVIEWS_PROVIDER and PROVIDER_REVIEWS_CLIENT).
 */
class ReviewRepository(
    db: AppDatabase,
    private val backendService: IBackendService,
    private val connectivityObserver: ConnectivityObserver
) : IReviewRepository {

    private val reviewDao = db.reviewDao()

    override suspend fun createReview(review: Review): Result<Review> {
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
                imageUrls = review.imageUrls,
                direction = review.direction.name,
                locationTags = review.locationTags
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
        val cached = reviewDao.getForProvider(providerId)
        if (cached.isNotEmpty()) {
            return Result.Success(cached.map { it.toDomain() })
        }

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
        val cached = reviewDao.getForBooking(bookingId)
        if (cached != null) {
            return Result.Success(cached.toDomain())
        }

        if (!connectivityObserver.isOnline()) {
            return Result.Success(null)
        }

        return Result.Success(null)
    }

    override suspend fun getReviewsForClient(clientId: String): Result<List<Review>> {
        val cached = reviewDao.getForClient(clientId)
        if (cached.isNotEmpty()) {
            return Result.Success(cached.map { it.toDomain() })
        }

        if (!connectivityObserver.isOnline()) {
            return Result.Error(OfflineException("Reviews not available offline"))
        }

        return try {
            val result = backendService.getReviewsForClient(clientId)
            when (result) {
                is Result.Success -> {
                    val reviews =
                        result.data.map { it.toDomain().copy(syncStatus = SyncStatus.SYNCED) }
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

    override suspend fun getReviewForBookingByDirection(
        bookingId: String,
        direction: ReviewDirection
    ): Result<Review?> {
        val cached = reviewDao.getForBookingByDirection(bookingId, direction.name)
        if (cached != null) {
            return Result.Success(cached.toDomain())
        }

        if (!connectivityObserver.isOnline()) {
            return Result.Success(null)
        }

        return try {
            val result = backendService.getReviewForBookingByDirection(bookingId, direction.name)
            when (result) {
                is Result.Success -> {
                    result.data?.let { dto ->
                        val review = dto.toDomain().copy(syncStatus = SyncStatus.SYNCED)
                        reviewDao.upsert(review.toEntity())
                        Result.Success(review)
                    } ?: Result.Success(null)
                }

                is Result.Error -> result
                is Result.Loading -> Result.Loading
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun observeReviewsForProvider(providerId: String): Flow<List<Review>> =
        reviewDao.observeForProvider(providerId).map { reviews ->
            reviews.map { it.toDomain() }
        }

    override fun observeReviewsForClient(clientId: String): Flow<List<Review>> =
        reviewDao.observeForClient(clientId).map { reviews ->
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
                if (imageUrls.isEmpty()) "" else "[${imageUrls.joinToString(",") { "\"$it\"" }}]"
            } catch (e: Exception) {
                ""
            },
            reviewDirection = direction.name,
            locationTags = try {
                if (locationTags.isEmpty()) "" else "[${locationTags.joinToString(",") { "\"$it\"" }}]"
            } catch (e: Exception) {
                ""
            },
            syncStatus = syncStatus.name,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
