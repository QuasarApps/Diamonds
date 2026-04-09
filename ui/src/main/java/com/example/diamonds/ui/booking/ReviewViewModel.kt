package com.example.diamonds.ui.booking

import androidx.lifecycle.viewModelScope
import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.domain.model.Provider
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.model.Review
import com.example.diamonds.domain.repository.IAuthRepository
import com.example.diamonds.domain.repository.IBookingRepository
import com.example.diamonds.domain.repository.IProviderRepository
import com.example.diamonds.domain.repository.IReviewRepository
import com.example.diamonds.domain.repository.IServiceRepository
import com.example.diamonds.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

// ── UI State ──────────────────────────────────────────────────────────────────

data class ReviewFormUiState(
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val rating: Int = 0,
    val comment: String = "",
    val providerName: String = "",
    val serviceName: String = "",
    val existingReview: Review? = null,
    val submitSuccess: Boolean = false
)

data class ProviderRatingsUiState(
    val isLoading: Boolean = false,
    val provider: Provider? = null,
    val reviews: List<ReviewWithClientName> = emptyList(),
    /** Count per star (index 0 = 1-star, index 4 = 5-star) */
    val starCounts: List<Int> = List(5) { 0 },
    val averageRating: Float = 0f,
    val totalCount: Int = 0
)

data class ReviewWithClientName(
    val review: Review,
    val clientName: String
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val reviewRepository: IReviewRepository,
    private val bookingRepository: IBookingRepository,
    private val providerRepository: IProviderRepository,
    private val serviceRepository: IServiceRepository,
    private val authRepository: IAuthRepository,
    connectivityObserver: ConnectivityObserver
) : BaseViewModel<Unit>(connectivityObserver, Unit) {

    private val _formState = MutableStateFlow(ReviewFormUiState())
    val formState: StateFlow<ReviewFormUiState> = _formState.asStateFlow()

    private val _ratingsState = MutableStateFlow(ProviderRatingsUiState())
    val ratingsState: StateFlow<ProviderRatingsUiState> = _ratingsState.asStateFlow()

    // ── Form input ────────────────────────────────────────────────────────────

    fun onRatingChange(r: Int)   { _formState.value = _formState.value.copy(rating = r) }
    fun onCommentChange(c: String) { _formState.value = _formState.value.copy(comment = c) }
    fun clearSubmitSuccess()     { _formState.value = _formState.value.copy(submitSuccess = false) }

    // ── Load existing review + booking context ────────────────────────────────

    fun loadForBooking(bookingId: String) {
        viewModelScope.launch {
            _formState.value = ReviewFormUiState(isLoading = true)
            clearError()

            val booking = (bookingRepository.getBooking(bookingId) as? Result.Success)?.data
            val provider = booking?.let {
                (providerRepository.getProvider(it.providerId) as? Result.Success)?.data
            }
            val service = booking?.let {
                (serviceRepository.getService(it.serviceId) as? Result.Success)?.data
            }
            val existing = (reviewRepository.getReviewsForBooking(bookingId) as? Result.Success)?.data

            _formState.value = ReviewFormUiState(
                providerName   = provider?.name    ?: "",
                serviceName    = service?.title    ?: "",
                existingReview = existing,
                rating         = existing?.rating  ?: 0,
                comment        = existing?.comment ?: ""
            )
        }
    }

    // ── Submit review ─────────────────────────────────────────────────────────

    fun submitReview(bookingId: String, providerId: String) {
        val rating = _formState.value.rating
        if (rating == 0) {
            setError("Please select a star rating")
            return
        }

        viewModelScope.launch {
            _formState.value = _formState.value.copy(isSubmitting = true)
            clearError()

            val session = authRepository.getCurrentUserSession().first()
            val clientId = session?.userId ?: run {
                setError("Not signed in")
                _formState.value = _formState.value.copy(isSubmitting = false)
                return@launch
            }

            val now = LocalDate.now().toString()
            val review = Review(
                id         = "",          // assigned by backend
                bookingId  = bookingId,
                clientId   = clientId,
                providerId = providerId,
                rating     = rating,
                comment    = _formState.value.comment.trim().ifBlank { null },
                createdAt  = now,
                updatedAt  = now
            )

            when (val r = reviewRepository.createReview(review)) {
                is Result.Success -> {
                    _formState.value = _formState.value.copy(
                        isSubmitting  = false,
                        submitSuccess = true,
                        existingReview = r.data
                    )
                }
                is Result.Error -> {
                    _formState.value = _formState.value.copy(isSubmitting = false)
                    setError(r.exception.message ?: "Failed to submit review")
                }
                is Result.Loading -> Unit
            }
        }
    }

    // ── Load all reviews for a provider ──────────────────────────────────────

    fun loadProviderRatings(providerId: String) {
        viewModelScope.launch {
            _ratingsState.value = ProviderRatingsUiState(isLoading = true)
            clearError()

            val provider = (providerRepository.getProvider(providerId) as? Result.Success)?.data
            val reviews  = (reviewRepository.getReviewsForProvider(providerId) as? Result.Success)?.data
                ?: emptyList()

            // Enrich with client names — use clientId as fallback
            val enriched = reviews.sortedByDescending { it.createdAt }.map { rev ->
                val name = rev.clientId.let { cid ->
                    // Map known demo client IDs to display names
                    when (cid) {
                        "demo_customer" -> "Demo Customer"
                        "client_alice"  -> "Alice J."
                        "client_bob"    -> "Bob W."
                        "client_carol"  -> "Carol M."
                        "client_dave"   -> "Dave T."
                        "client_eve"    -> "Eve R."
                        "client_frank"  -> "Frank L."
                        "client_grace"  -> "Grace K."
                        else            -> cid.replaceFirstChar { it.uppercase() }
                    }
                }
                ReviewWithClientName(rev, name)
            }

            val starCounts = List(5) { star -> reviews.count { it.rating == star + 1 } }
            val avg = if (reviews.isEmpty()) 0f
            else reviews.sumOf { it.rating }.toFloat() / reviews.size

            _ratingsState.value = ProviderRatingsUiState(
                provider      = provider,
                reviews       = enriched,
                starCounts    = starCounts,
                averageRating = avg,
                totalCount    = reviews.size
            )
        }
    }

    fun refreshProviderRatings() {
        _ratingsState.value.provider?.id?.let { loadProviderRatings(it) }
    }
}
