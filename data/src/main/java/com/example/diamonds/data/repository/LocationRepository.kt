package com.example.diamonds.data.repository

import android.annotation.SuppressLint
import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.data.local.AppDatabase
import com.example.diamonds.data.mapper.toDomain
import com.example.diamonds.data.mapper.toEntity
import com.example.diamonds.data.remote.backend.IBackendService
import com.example.diamonds.data.remote.backend.ProviderLocationDto
import com.example.diamonds.domain.model.GeoLocation
import com.example.diamonds.domain.model.ProviderLocation
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.model.ServiceArea
import com.example.diamonds.domain.repository.ILocationRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Offline-first [ILocationRepository] implementation.
 *
 * - Device location via [FusedLocationProviderClient]
 * - Provider locations cached in Room, fetched from backend when online
 * - Haversine distance and simple ETA estimation
 */
class LocationRepository(
    private val db: AppDatabase,
    private val backendService: IBackendService,
    private val connectivityObserver: ConnectivityObserver,
    private val fusedLocationClient: FusedLocationProviderClient
) : ILocationRepository {

    private val locationDao = db.providerLocationDao()

    // ── Device location ─────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): Result<GeoLocation> {
        return try {
            val cancellationToken = CancellationTokenSource()
            val location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationToken.token
            ).await()
            if (location != null) {
                Result.Success(GeoLocation(location.latitude, location.longitude))
            } else {
                Result.Error(Exception("Could not determine current location"))
            }
        } catch (e: Exception) {
            Result.Error(Exception("Location access failed: ${e.message}"))
        }
    }

    // ── Provider live location ──────────────────────────────────────────────

    override fun observeProviderLocation(providerId: String): Flow<ProviderLocation> {
        // Kick off a background fetch to keep the cache warm
        // The Flow below will emit the latest cached value reactively
        return locationDao.observeByProviderId(providerId)
            .filterNotNull()
            .map { it.toDomain() }
    }

    override suspend fun updateProviderLocation(providerLocation: ProviderLocation): Result<Unit> {
        // Always persist locally first
        locationDao.upsert(providerLocation.toEntity())
        return try {
            val dto = ProviderLocationDto(
                providerId = providerLocation.providerId,
                latitude = providerLocation.location.latitude,
                longitude = providerLocation.location.longitude,
                heading = providerLocation.heading,
                updatedAt = providerLocation.updatedAt
            )
            backendService.updateProviderLocation(dto)
        } catch (e: Exception) {
            // Offline — local update still succeeded
            Result.Success(Unit)
        }
    }

    // ── Service area ────────────────────────────────────────────────────────

    override suspend fun getServiceArea(providerId: String): Result<ServiceArea> {
        return when (val r = backendService.getServiceArea(providerId)) {
            is Result.Success -> {
                val dto = r.data
                Result.Success(
                    ServiceArea(
                        providerId = dto.providerId,
                        center = GeoLocation(dto.centerLatitude, dto.centerLongitude),
                        radiusKm = dto.radiusKm
                    )
                )
            }

            is Result.Error -> r
            is Result.Loading -> r
        }
    }

    // ── ETA / Distance ──────────────────────────────────────────────────────

    /**
     * Simple ETA estimation assuming average speed of 30 km/h (city driving).
     * In production, use the Google Directions API for real routing.
     */
    override suspend fun calculateEta(from: GeoLocation, to: GeoLocation): Result<Double> {
        val distKm = calculateDistance(from, to)
        val avgSpeedKmH = 30.0
        val etaMinutes = (distKm / avgSpeedKmH) * 60.0
        return Result.Success(etaMinutes)
    }

    /**
     * Haversine formula — returns great-circle distance in km.
     */
    override fun calculateDistance(from: GeoLocation, to: GeoLocation): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(to.latitude - from.latitude)
        val dLon = Math.toRadians(to.longitude - from.longitude)
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                sin(dLon / 2) * sin(dLon / 2) * cos(lat1) * cos(lat2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }

    /**
     * Fetch the provider's latest location from the backend and cache it.
     * Call this on a periodic basis or when tracking screen opens.
     */
    suspend fun refreshProviderLocation(providerId: String) {
        when (val r = backendService.getProviderLocation(providerId)) {
            is Result.Success -> {
                val dto = r.data
                val entity = com.example.diamonds.data.local.entity.ProviderLocationEntity(
                    providerId = dto.providerId,
                    latitude = dto.latitude,
                    longitude = dto.longitude,
                    heading = dto.heading,
                    updatedAt = dto.updatedAt
                )
                locationDao.upsert(entity)
            }

            else -> { /* offline / error — stale cache is fine */
            }
        }
    }
}
