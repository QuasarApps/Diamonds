package com.example.diamonds.data.repository

import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.data.local.AppDatabase
import com.example.diamonds.data.mapper.toDomain
import com.example.diamonds.data.mapper.toDto
import com.example.diamonds.data.mapper.toEntity
import com.example.diamonds.data.remote.backend.IBackendService
import com.example.diamonds.domain.model.OfflineException
import com.example.diamonds.domain.model.Provider
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.model.ServiceCategory
import com.example.diamonds.domain.repository.IProviderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Provider repository with offline-first read pattern
 */
class ProviderRepository(
    db: AppDatabase,
    private val backendService: IBackendService,
    private val connectivityObserver: ConnectivityObserver
) : IProviderRepository {

    private val providerDao = db.providerDao()

    override suspend fun getProvider(providerId: String): Result<Provider> {
        // READ: Check cache first
        val cached = providerDao.getById(providerId)
        if (cached != null) {
            return Result.Success(cached.toDomain())
        }

        // Not cached, fetch if online
        if (!connectivityObserver.isOnline()) {
            return Result.Error(OfflineException("Provider not available offline"))
        }

        return try {
            val result = backendService.getProvider(providerId)
            when (result) {
                is Result.Success -> {
                    val provider = result.data.toDomain()
                    providerDao.upsert(provider.toEntity())
                    Result.Success(provider)
                }
                is Result.Error -> result
                is Result.Loading -> Result.Loading
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun searchProviders(
        latitude: Double,
        longitude: Double,
        radius: Int
    ): Result<List<Provider>> {
        // READ: Can work offline with cached data
        val cached = providerDao.getNearby(limit = 20)
        if (cached.isNotEmpty()) {
            return Result.Success(cached.map { it.toDomain() })
        }

        // Cache empty, fetch if online
        if (!connectivityObserver.isOnline()) {
            return Result.Error(OfflineException("Providers not available offline"))
        }

        return try {
            val result = backendService.searchProviders(latitude, longitude, radius)
            when (result) {
                is Result.Success -> {
                    val providers = result.data.map { it.toDomain() }
                    providers.forEach { providerDao.upsert(it.toEntity()) }
                    Result.Success(providers)
                }
                is Result.Error -> result
                is Result.Loading -> Result.Loading
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun searchProvidersByCategory(
        category: ServiceCategory,
        latitude: Double,
        longitude: Double,
        radius: Int
    ): Result<List<Provider>> {
        // READ: Can work offline with cached data
        val cached = providerDao.getNearby(limit = 20)
        if (cached.isNotEmpty()) {
            return Result.Success(cached.map { it.toDomain() })
        }

        // Cache empty, fetch if online
        if (!connectivityObserver.isOnline()) {
            return Result.Error(OfflineException("Providers not available offline"))
        }

        return try {
            val result = backendService.searchProviders(latitude, longitude, radius)
            when (result) {
                is Result.Success -> {
                    val providers = result.data.map { it.toDomain() }
                    providers.forEach { providerDao.upsert(it.toEntity()) }
                    Result.Success(providers)
                }
                is Result.Error -> result
                is Result.Loading -> Result.Loading
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateProvider(provider: Provider): Result<Provider> {
        // WRITE: Require online
        if (!connectivityObserver.isOnline()) {
            return Result.Error(OfflineException("Provider update requires internet connection"))
        }

        return try {
            when (val result = backendService.updateProvider(provider.toDto())) {
                is Result.Success -> {
                    val updated = result.data.toDomain()
                    providerDao.upsert(updated.toEntity())
                    Result.Success(updated)
                }
                is Result.Error -> result
                is Result.Loading -> Result.Loading
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun observeProviderRating(providerId: String): Flow<Pair<Float, Int>> {
        return providerDao.observeById(providerId).map { provider ->
            if (provider != null) {
                Pair(provider.rating, provider.reviewCount)
            } else {
                Pair(0f, 0)
            }
        }
    }
}
