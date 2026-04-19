package com.example.diamonds.ui.review

import androidx.lifecycle.viewModelScope
import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.domain.model.Client
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.model.Review
import com.example.diamonds.domain.model.ReviewDirection
import com.example.diamonds.domain.repository.IAuthRepository
import com.example.diamonds.domain.repository.IBookingRepository
import com.example.diamonds.domain.repository.IClientRepository
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

data class ClientReviewFormUiState(
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val rating: Int = 0,
    val comment: String = "",
    val clientName: String = "",
    val serviceName: String = "",
    val selectedTags: Set<String> = emptySet(),
    val existingReview: Review? = null,
    val submitSuccess: Boolean = false
)

data class ClientRatingsUiState(
    val isLoading: Boolean = false,
    val client: Client? = null,
    val reviews: List<ReviewWithProviderName> = emptyList(),
    val starCounts: List<Int> = List(5) { 0 },
    val averageRating: Float = 0f,
    val totalCount: Int = 0
)

data class ReviewWithProviderName(
    val review: Review,
    val providerName: String
)

/** Standard location tags a cleaner can apply to a client review. */
val LOCATION_TAGS = listOf(
    "Easy parking",
    "Tight parking",
    "Clear instructions",
    "Pet-friendly",
    "Well-stocked supplies",
    "Good access",
    "Friendly client",
    "Key handover smooth"
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class ClientReviewViewModel @Inject constructor(
    private val reviewRepository: IReviewRepository,
    private val bookingRepository: IBookingRepository,
    private val clientRepository: IClientRepository,
    private val serviceRepository: IServiceRepository,
    private val authRepository: IAuthRepository,
    connectivityObserver: ConnectivityObserver
) : BaseViewModel<Unit>(connectivityObserver, Unit) {

    private val _formState = MutableStateFlow(ClientReviewFormUiState())
    val formState: StateFlow<ClientReviewFormUiState> = _formState.asStateFlow()

    private val _ratingsState = MutableStateFlow(ClientRatingsUiState())
    val ratingsState: StateFlow<ClientRatingsUiState> = _ratingsState.asStateFlow()

    // ── Form input ────────────────────────────────────────────────────────────

    fun onRatingChange(r: Int) {
        _formState.value = _formState.value.copy(rating = r)
    }

    fun onCommentChange(c: String) {
        _formState.value = _formState.value.copy(comment = c)
    }

    fun clearSubmitSuccess() {
        _formState.value = _formState.value.copy(submitSuccess = false)
    }

    fun toggleTag(tag: String) {
        val current = _formState.value.selectedTags
        _formState.value = _formState.value.copy(
            selectedTags = if (tag in current) current - tag else current + tag
        )
    }

    // ── Load existing reverse review + booking context ────────────────────────

    fun loadForBooking(bookingId: String, clientId: String) {
        viewModelScope.launch {
            _formState.value = ClientReviewFormUiState(isLoading = true)
            clearError()

            val booking = (bookingRepository.getBooking(bookingId) as? Result.Success)?.data
            val client = (clientRepository.getClient(clientId) as? Result.Success)?.data
            val service = booking?.let {
                (serviceRepository.getService(it.serviceId) as? Result.Success)?.data
            }
            val existing = (reviewRepository.getReviewForBookingByDirection(
                bookingId, ReviewDirection.PROVIDER_REVIEWS_CLIENT
            ) as? Result.Success)?.data

            _formState.value = ClientReviewFormUiState(
                clientName = client?.name ?: clientId.replaceFirstChar { it.uppercase() },
                serviceName = service?.title ?: "",
                existingReview = existing,
                rating = existing?.rating ?: 0,
                comment = existing?.comment ?: "",
                selectedTags = existing?.locationTags?.toSet() ?: emptySet()
            )
        }
    }

    // ── Submit reverse review ─────────────────────────────────────────────────

    fun submitReview(bookingId: String, clientId: String) {
        val rating = _formState.value.rating
        if (rating == 0) {
            setError("Please select a star rating")
            return
        }

        viewModelScope.launch {
            _formState.value = _formState.value.copy(isSubmitting = true)
            clearError()

            val session = authRepository.getCurrentUserSession().first()
            val providerId = session?.userId ?: run {
                setError("Not signed in")
                _formState.value = _formState.value.copy(isSubmitting = false)
                return@launch
            }

            val now = LocalDate.now().toString()
            val review = Review(
                id = "",
                bookingId = bookingId,
                clientId = clientId,
                providerId = providerId,
                rating = rating,
                comment = _formState.value.comment.trim().ifBlank { null },
                direction = ReviewDirection.PROVIDER_REVIEWS_CLIENT,
                locationTags = _formState.value.selectedTags.toList(),
                createdAt = now,
                updatedAt = now
            )

            when (val r = reviewRepository.createReview(review)) {
                is Result.Success -> {
                    _formState.value = _formState.value.copy(
                        isSubmitting = false,
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

    // ── Load all reverse reviews for a client ────────────────────────────────

    fun loadClientRatings(clientId: String) {
        viewModelScope.launch {
            _ratingsState.value = ClientRatingsUiState(isLoading = true)
            clearError()

            val client = (clientRepository.getClient(clientId) as? Result.Success)?.data
            val reviews = (reviewRepository.getReviewsForClient(clientId) as? Result.Success)?.data
                ?: emptyList()

            val enriched = reviews.sortedByDescending { it.createdAt }.map { rev ->
                val name = when (rev.providerId) {
                    "p1" -> "Maria Garcia"
                    "p2" -> "James Okafor"
                    "p3" -> "Sofia Petrov"
                    "p4" -> "Daniel Choi"
                    "p5" -> "Amara Diop"
                    else -> rev.providerId.replaceFirstChar { it.uppercase() }
                }
                ReviewWithProviderName(rev, name)
            }

            val starCounts = List(5) { star -> reviews.count { it.rating == star + 1 } }
            val avg = if (reviews.isEmpty()) 0f
            else reviews.sumOf { it.rating }.toFloat() / reviews.size

            _ratingsState.value = ClientRatingsUiState(
                client = client,
                reviews = enriched,
                starCounts = starCounts,
                averageRating = avg,
                totalCount = reviews.size
            )
        }
    }
}
