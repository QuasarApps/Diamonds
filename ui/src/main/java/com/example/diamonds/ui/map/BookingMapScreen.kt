package com.example.diamonds.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.diamonds.ui.R
import com.example.diamonds.ui.components.RequireLocationPermission
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

/**
 * Full-screen map screen for picking a booking address.
 * The user can tap the map or use "My Location" to select a point,
 * then confirm to pass the coordinates back to the booking form.
 */
@Composable
fun BookingMapScreen(
    providerId: String? = null,
    onLocationConfirmed: (lat: Double, lng: Double, address: String) -> Unit,
    onCancel: () -> Unit,
    viewModel: MapViewModel = hiltViewModel()
) {
    val state by viewModel.mapState.collectAsState()
    val error by viewModel.error.collectAsState()
    val tapToSelect = stringResource(R.string.tap_to_select)

    LaunchedEffect(providerId) {
        viewModel.initMap(providerId)
    }

    RequireLocationPermission(
        onPermissionDenied = { /* proceed without device location — user can still tap map */ }
    ) {
        // Re-trigger location fetch once permission granted
        LaunchedEffect(Unit) {
            if (state.deviceLocation == null) viewModel.initMap(providerId)
        }
    }

    val defaultLatLng = LatLng(
        state.selectedLocation?.latitude ?: state.deviceLocation?.latitude ?: 40.7128,
        state.selectedLocation?.longitude ?: state.deviceLocation?.longitude ?: -74.0060
    )

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLatLng, 14f)
    }

    // Animate camera when selected location changes
    LaunchedEffect(state.selectedLocation) {
        state.selectedLocation?.let { loc ->
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 15f)
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            onMapClick = { latLng ->
                viewModel.onMapClick(latLng.latitude, latLng.longitude)
            }
        ) {
            // Selected location marker
            state.selectedLocation?.let { loc ->
                Marker(
                    state = MarkerState(position = LatLng(loc.latitude, loc.longitude)),
                    title = stringResource(R.string.booking_location),
                    snippet = state.selectedAddress.ifBlank { tapToSelect }
                )
            }

            // Service area circle overlay
            state.serviceArea?.let { area ->
                Circle(
                    center = LatLng(area.center.latitude, area.center.longitude),
                    radius = area.radiusKm * 1000, // metres
                    fillColor = androidx.compose.ui.graphics.Color(0x220066FF),
                    strokeColor = androidx.compose.ui.graphics.Color(0x880066FF),
                    strokeWidth = 2f
                )
            }
        }

        // Bottom card overlay with address input and buttons
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.select_location), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = state.selectedAddress,
                    onValueChange = viewModel::onAddressEntered,
                    label = { Text(stringResource(R.string.address)) },
                    placeholder = { Text(stringResource(R.string.tap_map_or_type_address)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (state.selectedLocation != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "📍 ${
                            String.format(
                                "%.5f",
                                state.selectedLocation!!.latitude
                            )
                        }, ${String.format("%.5f", state.selectedLocation!!.longitude)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (error != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(error ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                Spacer(Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { viewModel.useMyLocation() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.my_location_with_icon))
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val (loc, addr) = viewModel.confirmLocation()
                            if (loc != null) {
                                onLocationConfirmed(loc.latitude, loc.longitude, addr)
                            }
                        },
                        enabled = state.selectedLocation != null,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(stringResource(R.string.confirm))
                        }
                    }
                }
            }
        }
    }
}
