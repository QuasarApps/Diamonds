package com.example.diamonds.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.diamonds.common.util.DateTimeFormatUtil
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

/**
 * Full-screen real-time tracking screen showing the cleaner's
 * live position moving toward the booking address.
 */
@Composable
fun ProviderTrackingScreen(
    bookingId: String,
    viewModel: MapViewModel = hiltViewModel()
) {
    val state by viewModel.trackingState.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(bookingId) {
        viewModel.startTracking(bookingId)
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopTracking() }
    }

    if (state.isLoading) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text("Loading tracking…")
        }
        return
    }

    if (error != null) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("⚠️", fontSize = 40.sp)
            Spacer(Modifier.height(12.dp))
            Text(error ?: "Something went wrong", color = MaterialTheme.colorScheme.error)
        }
        return
    }

    val providerLoc = state.trackingState.providerLocation
    val jobLoc = state.trackingState.jobLocation

    // Default camera: NYC
    val defaultPos = LatLng(40.7128, -74.0060)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultPos, 13f)
    }

    // Animate camera to show both markers
    LaunchedEffect(providerLoc, jobLoc) {
        if (providerLoc != null && jobLoc != null) {
            val provLatLng = LatLng(providerLoc.location.latitude, providerLoc.location.longitude)
            val jobLatLng = LatLng(jobLoc.latitude, jobLoc.longitude)
            val bounds = LatLngBounds.builder()
                .include(provLatLng)
                .include(jobLatLng)
                .build()
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngBounds(bounds, 120)
            )
        } else if (providerLoc != null) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(providerLoc.location.latitude, providerLoc.location.longitude),
                    15f
                )
            )
        } else if (jobLoc != null) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(jobLoc.latitude, jobLoc.longitude),
                    15f
                )
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            // Provider marker (cleaner's live position)
            providerLoc?.let { loc ->
                Marker(
                    state = MarkerState(
                        position = LatLng(
                            loc.location.latitude,
                            loc.location.longitude
                        )
                    ),
                    title = state.provider?.name ?: "Cleaner",
                    snippet = "En route"
                )
            }

            // Job location marker
            jobLoc?.let { loc ->
                Marker(
                    state = MarkerState(position = LatLng(loc.latitude, loc.longitude)),
                    title = "Service Location",
                    snippet = state.booking?.address ?: ""
                )
            }

            // Line between provider and job
            if (providerLoc != null && jobLoc != null) {
                Polyline(
                    points = listOf(
                        LatLng(providerLoc.location.latitude, providerLoc.location.longitude),
                        LatLng(jobLoc.latitude, jobLoc.longitude)
                    ),
                    color = Color(0xFF4285F4),
                    width = 6f
                )
            }
        }

        // ETA / Distance info card
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    state.provider?.name ?: "Cleaner",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "is on the way",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // ETA badge
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🕐", fontSize = 20.sp)
                        Spacer(Modifier.height(2.dp))
                        val etaText = state.trackingState.etaMinutes?.let {
                            "${it.toInt()} min"
                        } ?: "—"
                        Text(etaText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "ETA",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    // Distance badge
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📏", fontSize = 20.sp)
                        Spacer(Modifier.height(2.dp))
                        val distText = state.trackingState.distanceKm?.let {
                            String.format("%.1f km", it)
                        } ?: "—"
                        Text(distText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "Distance",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Booking info bar at bottom
        state.booking?.let { booking ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Booking #${booking.id.takeLast(6)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(booking.address, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            DateTimeFormatUtil.formatTimeForDisplay(booking.scheduledTime),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            DateTimeFormatUtil.formatDateForDisplay(booking.scheduledDate),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
