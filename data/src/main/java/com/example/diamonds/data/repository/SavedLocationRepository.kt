package com.example.diamonds.data.repository

import com.example.diamonds.data.local.AppDatabase
import com.example.diamonds.data.local.entity.SavedLocationEntity
import com.example.diamonds.data.mapper.toDomain
import com.example.diamonds.data.mapper.toDto
import com.example.diamonds.data.mapper.toEntity
import com.example.diamonds.data.remote.backend.IBackendService
import com.example.diamonds.data.remote.backend.SavedLocationDto
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.model.SavedLocation
import com.example.diamonds.domain.repository.ISavedLocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SavedLocationRepository(
    private val db: AppDatabase,
    private val backend: IBackendService
) : ISavedLocationRepository {

    override suspend fun getSavedLocations(clientId: String): Result<List<SavedLocation>> {
        val cached: List<SavedLocationEntity> = db.savedLocationDao().getForClient(clientId)
        if (cached.isNotEmpty()) return Result.Success(cached.map { it.toDomain() })
        return when (val r = backend.getSavedLocations(clientId)) {
            is Result.Success -> {
                val domains: List<SavedLocation> =
                    r.data.map { dto: SavedLocationDto -> dto.toDomain() }
                domains.forEach { db.savedLocationDao().upsert(it.toEntity()) }
                Result.Success(domains)
            }

            is Result.Error -> Result.Success(emptyList()) // fall back to empty list gracefully
            is Result.Loading -> Result.Loading
        }
    }

    override fun observeSavedLocations(clientId: String): Flow<List<SavedLocation>> =
        db.savedLocationDao().observeForClient(clientId)
            .map { list: List<SavedLocationEntity> -> list.map { entity: SavedLocationEntity -> entity.toDomain() } }

    override suspend fun upsertSavedLocation(location: SavedLocation): Result<SavedLocation> {
        db.savedLocationDao().upsert(location.toEntity())
        return when (val r = backend.upsertSavedLocation(location.toDto())) {
            is Result.Success -> Result.Success(r.data.toDomain())
            is Result.Error -> Result.Success(location) // persist locally even if remote fails
            is Result.Loading -> Result.Loading
        }
    }

    override suspend fun deleteSavedLocation(locationId: String): Result<Unit> {
        db.savedLocationDao().delete(locationId)
        backend.deleteSavedLocation(locationId)
        return Result.Success(Unit)
    }
}
