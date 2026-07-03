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

    /**
     * One degree along a great circle. A pure latitude delta (or an equatorial longitude
     * delta) reduces the Haversine formula exactly to `R · Δ`, so this is the expected
     * distance for both — derived from the same constants the implementation uses (≈ 111.19 km).
     */
    private val oneDegreeKm = 6371.0 * Math.toRadians(1.0)

    // ── calculateDistance ─────────────────────────────────────────────────────

    @Test
    fun `distance between identical points is zero`() {
        val point = GeoLocation(40.7128, -74.0060)
        assertEquals(0.0, repository.calculateDistance(point, point), 1e-9)
    }

    @Test
    fun `one degree of latitude equals R times one radian-degree`() {
        // A pure latitude delta reduces the Haversine formula exactly to R · Δφ.
        val distance = repository.calculateDistance(GeoLocation(0.0, 0.0), GeoLocation(1.0, 0.0))
        assertEquals(oneDegreeKm, distance, 1e-6)
    }

    @Test
    fun `one degree of longitude at the equator equals R times one radian-degree`() {
        // At the equator cos(lat) = 1, so a longitude degree reduces to R · Δλ, same as latitude.
        val distance = repository.calculateDistance(GeoLocation(0.0, 0.0), GeoLocation(0.0, 1.0))
        assertEquals(oneDegreeKm, distance, 1e-6)
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

    @Test
    fun `distance matches a known city pair`() {
        // London → Paris ≈ 343.56 km. The only case with both latitude and longitude
        // deltas non-zero at non-zero latitude, so it exercises all four Haversine terms
        // and rejects the structural bugs the pure-axis cases can't: a lat/lon swap
        // (→ 403.6 km) or a dropped cos(lat) factor (→ 403.7 km). The tight ±0.1 km
        // tolerance also pins the Earth-radius constant near 6371 km — enough to reject
        // an equatorial (6378 → 343.94) or polar (6357 → 342.79) substitution, while
        // the deterministic path clears 343.556 by ~6e-5 km so it's never flaky.
        val london = GeoLocation(51.5074, -0.1278)
        val paris = GeoLocation(48.8566, 2.3522)
        assertEquals(343.556, repository.calculateDistance(london, paris), 0.1)
    }

    // ── calculateEta ──────────────────────────────────────────────────────────

    @Test
    fun `eta assumes 30 km per hour`() = runTest {
        val from = GeoLocation(0.0, 0.0)
        val to = GeoLocation(1.0, 0.0)

        val result = repository.calculateEta(from, to)

        assertTrue(result is Result.Success)
        val etaMinutes = (result as Result.Success).data
        // 30 km/h ⇒ 2 min/km. The expected value is derived from R · Δφ rather than by
        // calling calculateDistance, so the assertion checks the ETA formula instead of
        // just restating the implementation. (calculateEta still uses calculateDistance
        // internally — that path is covered by the calculateDistance tests above.)
        val expectedEtaMinutes = oneDegreeKm / 30.0 * 60.0
        assertEquals(expectedEtaMinutes, etaMinutes, 1e-6)
    }

    @Test
    fun `eta of zero distance is zero`() = runTest {
        val point = GeoLocation(34.0522, -118.2437)
        val result = repository.calculateEta(point, point)
        assertTrue(result is Result.Success)
        assertEquals(0.0, (result as Result.Success).data, 1e-9)
    }
}
