package com.example.diamonds.ui.booking

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.diamonds.ui.R

/**
 * Review / rating form for a completed booking.
 *
 * - Shows the provider + service being reviewed
 * - Interactive 5-star tap picker
 * - Optional comment text field
 * - Submits via [ReviewViewModel.submitReview]
 * - Navigates back on success
 */
@Composable
fun ReviewScreen(
    bookingId: String,
    providerId: String,
    onReviewSubmitted: () -> Unit,
    onFileClaim: (bookingId: String) -> Unit = {},
    viewModel: ReviewViewModel = hiltViewModel()
) {
    val state by viewModel.formState.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(bookingId) { viewModel.loadForBooking(bookingId) }
    LaunchedEffect(state.submitSuccess) {
        if (state.submitSuccess) {
            viewModel.clearSubmitSuccess()
            onReviewSubmitted()
        }
    }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Already reviewed — show read-only view
    if (state.existingReview != null && !state.submitSuccess) {
        ExistingReviewView(
            review       = state.existingReview!!,
            providerName = state.providerName,
            serviceName  = state.serviceName,
            onDone       = onReviewSubmitted
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))

        Text(stringResource(R.string.leave_a_review), style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(state.serviceName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                if (state.providerName.isNotBlank()) {
                    Text(stringResource(R.string.by_provider, state.providerName), fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // ── Star picker ───────────────────────────────────────────────────
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.rate_this_service_question),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            StarPicker(
                selected   = state.rating,
                onSelect   = viewModel::onRatingChange,
                starSizeSp = 44
            )
            Spacer(Modifier.height(6.dp))
            Text(
                starLabel(state.rating),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (state.rating > 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ── Comment ───────────────────────────────────────────────────────
        OutlinedTextField(
            value          = state.comment,
            onValueChange  = viewModel::onCommentChange,
            label          = { Text(stringResource(R.string.add_a_comment_optional)) },
            placeholder    = { Text(stringResource(R.string.review_comment_placeholder)) },
            minLines       = 3,
            maxLines       = 6,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                keyboardType   = KeyboardType.Text
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (error != null) {
            Text(error ?: "", color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp, textAlign = TextAlign.Center)
        }

        // "Having an issue?" link
        Text(
            stringResource(R.string.having_an_issue_file_claim),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { onFileClaim(bookingId) }
        )

        Button(
            onClick  = { viewModel.submitReview(bookingId, providerId) },
            enabled  = state.rating > 0 && !state.isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            if (state.isSubmitting) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color       = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(stringResource(R.string.submit_review), fontSize = 16.sp)
            }
        }
    }
}

// ── Already-reviewed read-only view ──────────────────────────────────────────

@Composable
private fun ExistingReviewView(
    review: com.example.diamonds.domain.model.Review,
    providerName: String,
    serviceName: String,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("✅", fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.review_submitted), style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.already_reviewed_this_booking), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(serviceName, fontWeight = FontWeight.SemiBold)
                if (providerName.isNotBlank())
                    Text(stringResource(R.string.by_provider, providerName), fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                StarPicker(selected = review.rating, onSelect = {}, enabled = false, starSizeSp = 28)
                review.comment?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.quoted_comment, it), fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.done)) }
    }
}

// ── Shared composables ────────────────────────────────────────────────────────

/**
 * Reusable interactive (or read-only) star row.
 */
@Composable
fun StarPicker(
    selected: Int,
    onSelect: (Int) -> Unit,
    enabled: Boolean = true,
    starSizeSp: Int = 36
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        (1..5).forEach { star ->
            val filled = star <= selected
            Text(
                text     = if (filled) "★" else "☆",
                fontSize = starSizeSp.sp,
                color    = if (filled) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.outlineVariant,
                modifier = if (enabled) Modifier.clickable { onSelect(star) } else Modifier
            )
        }
    }
}

private fun starLabel(rating: Int) = when (rating) {
    1 -> "Poor"
    2 -> "Fair"
    3 -> "Good"
    4 -> "Great"
    5 -> "Excellent!"
    else -> "Tap to rate"
}
