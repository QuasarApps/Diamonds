package com.example.diamonds.data.repository

import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.data.local.AppDatabase
import com.example.diamonds.data.local.dao.ProviderLocationDao
import com.example.diamonds.data.remote.backend.IBackendService
import com.example.diamonds.domain.model.GeoLocation
import com.example.diamonds.domain.model.Result
import com.google.android.gms.location.FusedLocationProviderClient
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [LocationRepository]'s pure Haversine distance and ETA math.
 *
 * These functions ignore every injected dependency, so the collaborators are
 * mocked only to satisfy construction. The expected values are derived from the
 * Haversine formula itself (great-circle distance on a sphere of radius 6371 km):
 * a pure latitude delta reduces exactly to `R · Δφ`, and a longitude delta scales
 * with `cos(latitude)`.
 */
class LocationRepositoryTest {

    private lateinit var repository: LocationRepository

    @Before
    fun setUp() {
        val locationDao = mockk<ProviderLocationDao>(relaxed = true)
        val db = mockk<AppDatabase> { every { providerLocationDao() } returns locationDao }
        val backendService = mockk<IBackendService>(relaxed = true)
        val connectivityObserver = mockk<ConnectivityObserver>(relaxed = true)
        val fusedLocationClient = mockk<FusedLocationProviderClient>(relaxed = true)
        repository = LocationRepository(db, backendService, connectivityObserver, fusedLocationClient)
    }

    // ── calculateDistance ─────────────────────────────────────────────────────

    @Test
    fun `distance between identical points is zero`() {
        val point = GeoLocation(40.7128, -74.0060)
        assertEquals(0.0, repository.calculateDistance(point, point), 1e-9)
    }

    @Test
    fun `one degree of latitude is about 111 km`() {
        // R * toRadians(1) = 6371 * 0.0174533 ≈ 111.19 km
        val distance = repository.calculateDistance(GeoLocation(0.0, 0.0), GeoLocation(1.0, 0.0))
        assertEquals(111.19, distance, 0.5)
    }

    @Test
    fun `one degree of longitude at the equator is about 111 km`() {
        // At the equator cos(lat) = 1, so a longitude degree spans the same as a latitude degree.
        val distance = repository.calculateDistance(GeoLocation(0.0, 0.0), GeoLocation(0.0, 1.0))
        assertEquals(111.19, distance, 0.5)
    }

    @Test
    fun `longitude distance shrinks with latitude by cosine`() {
        // A longitude degree at 60°N should be ~cos(60°) = 0.5 of one at the equator.
        val atEquator = repository.calculateDistance(GeoLocation(0.0, 0.0), GeoLocation(0.0, 1.0))
        val atSixty = repository.calculateDistance(GeoLocation(60.0, 0.0), GeoLocation(60.0, 1.0))
        assertEquals(atEquator * 0.5, atSixty, 0.2)
    }

    @Test
    fun `distance is symmetric`() {
        val a = GeoLocation(51.5074, -0.1278)   // London
        val b = GeoLocation(48.8566, 2.3522)     // Paris
        assertEquals(
            repository.calculateDistance(a, b),
            repository.calculateDistance(b, a),
            1e-9
        )
    }

    // ── calculateEta ──────────────────────────────────────────────────────────

    @Test
    fun `eta assumes 30 km per hour`() = runTest {
        val from = GeoLocation(0.0, 0.0)
        val to = GeoLocation(1.0, 0.0)
        val distanceKm = repository.calculateDistance(from, to)

        val result = repository.calculateEta(from, to)

        assertTrue(result is Result.Success)
        val etaMinutes = (result as Result.Success).data
        // eta = distance / 30 km/h * 60 min  (i.e. 2 minutes per km)
        assertEquals(distanceKm / 30.0 * 60.0, etaMinutes, 1e-6)
        assertEquals(222.39, etaMinutes, 1.0)
    }

    @Test
    fun `eta of zero distance is zero`() = runTest {
        val point = GeoLocation(34.0522, -118.2437)
        val result = repository.calculateEta(point, point)
        assertTrue(result is Result.Success)
        assertEquals(0.0, (result as Result.Success).data, 1e-9)
    }
}
