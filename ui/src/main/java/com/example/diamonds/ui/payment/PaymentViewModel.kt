package com.example.diamonds.ui.payment

import androidx.lifecycle.viewModelScope
import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.domain.model.Booking
import com.example.diamonds.domain.model.Payment
import com.example.diamonds.domain.model.PaymentMethod
import com.example.diamonds.domain.model.PaymentStatus
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.repository.IAuthRepository
import com.example.diamonds.domain.repository.IBookingRepository
import com.example.diamonds.domain.repository.IPaymentRepository
import com.example.diamonds.domain.repository.IProviderRepository
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

data class PaymentUiState(
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    // Booking context
    val booking: Booking? = null,
    val serviceName: String = "",
    val providerName: String = "",
    val amount: Double = 0.0,
    // Card input fields
    val cardNumber: String = "",
    val cardHolder: String = "",
    val expiry: String = "",
    val cvv: String = "",
    // Field errors
    val cardNumberError: String? = null,
    val cardHolderError: String? = null,
    val expiryError: String? = null,
    val cvvError: String? = null,
    // Result
    val paymentSuccess: Boolean = false,
    val paymentId: String? = null,
    val paymentDeclined: Boolean = false
)

data class PaymentHistoryItem(
    val payment: Payment,
    val serviceName: String,
    val providerName: String
)

data class PaymentHistoryUiState(
    val isLoading: Boolean = false,
    val items: List<PaymentHistoryItem> = emptyList()
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val paymentRepository: IPaymentRepository,
    private val bookingRepository: IBookingRepository,
    private val providerRepository: IProviderRepository,
    private val serviceRepository: IServiceRepository,
    private val authRepository: IAuthRepository,
    connectivityObserver: ConnectivityObserver
) : BaseViewModel<Unit>(connectivityObserver, Unit) {

    private val _paymentState = MutableStateFlow(PaymentUiState())
    val paymentState: StateFlow<PaymentUiState> = _paymentState.asStateFlow()

    private val _historyState = MutableStateFlow(PaymentHistoryUiState())
    val historyState: StateFlow<PaymentHistoryUiState> = _historyState.asStateFlow()

    // ── Card input ────────────────────────────────────────────────────────────

    fun onCardNumberChange(v: String) {
        // Format as groups of 4: "1234 5678 9012 3456"
        val digits = v.filter { it.isDigit() }.take(16)
        val formatted = digits.chunked(4).joinToString(" ")
        _paymentState.value = _paymentState.value.copy(
            cardNumber = formatted, cardNumberError = null)
    }

    fun onCardHolderChange(v: String) {
        _paymentState.value = _paymentState.value.copy(
            cardHolder = v.uppercase(), cardHolderError = null)
    }

    fun onExpiryChange(v: String) {
        val digits = v.filter { it.isDigit() }.take(4)
        val formatted = if (digits.length >= 3) "${digits.take(2)}/${digits.drop(2)}" else digits
        _paymentState.value = _paymentState.value.copy(
            expiry = formatted, expiryError = null)
    }

    fun onCvvChange(v: String) {
        val digits = v.filter { it.isDigit() }.take(4)
        _paymentState.value = _paymentState.value.copy(cvv = digits, cvvError = null)
    }

    fun clearPaymentSuccess() {
        _paymentState.value = _paymentState.value.copy(paymentSuccess = false)
    }

    // ── Load booking context ──────────────────────────────────────────────────

    fun loadForBooking(bookingId: String) {
        viewModelScope.launch {
            _paymentState.value = PaymentUiState(isLoading = true)
            clearError()

            val booking  = (bookingRepository.getBooking(bookingId) as? Result.Success)?.data
            val provider = booking?.let { (providerRepository.getProvider(it.providerId) as? Result.Success)?.data }
            val service  = booking?.let { (serviceRepository.getService(it.serviceId) as? Result.Success)?.data }

            _paymentState.value = PaymentUiState(
                booking      = booking,
                serviceName  = service?.title    ?: "",
                providerName = provider?.name    ?: "",
                amount       = booking?.totalPrice ?: 0.0
            )
        }
    }

    // ── Process payment ───────────────────────────────────────────────────────

    fun processPayment(bookingId: String) {
        val s = _paymentState.value
        if (!validate(s)) return

        viewModelScope.launch {
            _paymentState.value = s.copy(isProcessing = true)
            clearError()

            val session = authRepository.getCurrentUserSession().first()
            val booking = s.booking ?: run {
                setError("Booking not loaded")
                _paymentState.value = _paymentState.value.copy(isProcessing = false)
                return@launch
            }

            val now = LocalDate.now().toString()
            val payment = Payment(
                id            = "",
                bookingId     = bookingId,
                clientId      = session?.userId ?: booking.clientId,
                providerId    = booking.providerId,
                amount        = booking.totalPrice,
                status        = PaymentStatus.PENDING,
                method        = PaymentMethod.CARD,
                createdAt     = now,
                updatedAt     = now
            )

            when (val r = paymentRepository.createPayment(payment)) {
                is Result.Success -> {
                    _paymentState.value = _paymentState.value.copy(
                        isProcessing    = false,
                        paymentSuccess  = r.data.status == PaymentStatus.SUCCEEDED,
                        paymentDeclined = r.data.status == PaymentStatus.FAILED,
                        paymentId       = r.data.id
                    )
                }
                is Result.Error -> {
                    _paymentState.value = _paymentState.value.copy(isProcessing = false)
                    setError(r.exception.message ?: "Payment failed. Please try again.")
                }
                is Result.Loading -> Unit
            }
        }
    }

    // ── Payment history ───────────────────────────────────────────────────────

    fun loadHistory() {
        viewModelScope.launch {
            _historyState.value = PaymentHistoryUiState(isLoading = true)
            clearError()

            val session  = authRepository.getCurrentUserSession().first()
            val clientId = session?.userId ?: return@launch

            when (val r = paymentRepository.getPaymentsForClient(clientId)) {
                is Result.Success -> {
                    val enriched = r.data.map { payment ->
                        val booking  = (bookingRepository.getBooking(payment.bookingId) as? Result.Success)?.data
                        val provider = booking?.let { (providerRepository.getProvider(it.providerId) as? Result.Success)?.data }
                        val service  = booking?.let { (serviceRepository.getService(it.serviceId) as? Result.Success)?.data }
                        PaymentHistoryItem(
                            payment      = payment,
                            serviceName  = service?.title  ?: payment.bookingId,
                            providerName = provider?.name  ?: ""
                        )
                    }
                    _historyState.value = PaymentHistoryUiState(items = enriched)
                }
                is Result.Error -> {
                    _historyState.value = PaymentHistoryUiState()
                    setError(r.exception.message ?: "Could not load payment history")
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun refreshHistory() {
        loadHistory()
    }

    private fun validate(s: PaymentUiState): Boolean {
        var valid = true
        var updated = s

        val digits = s.cardNumber.filter { it.isDigit() }
        if (digits.length < 16) {
            updated = updated.copy(cardNumberError = "Enter a valid 16-digit card number")
            valid = false
        }
        if (s.cardHolder.isBlank()) {
            updated = updated.copy(cardHolderError = "Enter the name on your card")
            valid = false
        }
        val expiryDigits = s.expiry.filter { it.isDigit() }
        if (expiryDigits.length < 4) {
            updated = updated.copy(expiryError = "Enter a valid expiry (MM/YY)")
            valid = false
        } else {
            val month = expiryDigits.take(2).toIntOrNull() ?: 0
            if (month < 1 || month > 12) {
                updated = updated.copy(expiryError = "Invalid month")
                valid = false
            }
        }
        if (s.cvv.length < 3) {
            updated = updated.copy(cvvError = "Enter your 3-4 digit CVV")
            valid = false
        }

        if (!valid) _paymentState.value = updated
        return valid
    }
}
