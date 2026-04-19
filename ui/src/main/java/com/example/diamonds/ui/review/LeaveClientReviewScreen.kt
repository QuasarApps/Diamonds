package com.example.diamonds.ui.review

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Screen for a cleaner to review a client after a completed booking.
 * Includes star rating, comment field, and optional location/client tags.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LeaveClientReviewScreen(
    bookingId: String,
    clientId: String,
    onReviewSubmitted: () -> Unit,
    viewModel: ClientReviewViewModel = hiltViewModel()
) {
    val state by viewModel.formState.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(bookingId, clientId) { viewModel.loadForBooking(bookingId, clientId) }
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

    // Read-only if already reviewed
    val readOnly = state.existingReview != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            if (readOnly) "Your Review" else "Review Client",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        if (state.clientName.isNotBlank()) {
            Text(
                state.clientName,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (state.serviceName.isNotBlank()) {
            Text(
                state.serviceName,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(24.dp))

        // Star picker
        Text(
            when (state.rating) {
                1 -> "Poor"
                2 -> "Below Average"
                3 -> "Average"
                4 -> "Good"
                5 -> "Excellent"
                else -> "Tap a star to rate"
            },
            fontWeight = FontWeight.Medium,
            color = if (state.rating > 0) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            for (i in 1..5) {
                Text(
                    text = if (i <= state.rating) "★" else "☆",
                    fontSize = 40.sp,
                    color = if (i <= state.rating) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .then(if (!readOnly) Modifier.clickable { viewModel.onRatingChange(i) } else Modifier)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Location tags
        Text(
            "Location & Client Tags (optional)",
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            LOCATION_TAGS.forEach { tag ->
                FilterChip(
                    selected = tag in state.selectedTags,
                    onClick = { if (!readOnly) viewModel.toggleTag(tag) },
                    label = { Text(tag, fontSize = 13.sp) }
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Comment
        OutlinedTextField(
            value = state.comment,
            onValueChange = { if (!readOnly) viewModel.onCommentChange(it) },
            label = { Text("Feedback (optional)") },
            placeholder = { Text("How was the client and their location?") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 6,
            readOnly = readOnly,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                keyboardType = KeyboardType.Text
            )
        )

        // Error
        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    error ?: "",
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        if (!readOnly) {
            Button(
                onClick = { viewModel.submitReview(bookingId, clientId) },
                enabled = state.rating > 0 && !state.isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Submit Review")
                }
            }
        }
    }
}
