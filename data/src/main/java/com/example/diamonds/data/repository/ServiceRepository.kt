package com.example.diamonds.data.repository

import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.data.local.AppDatabase
import com.example.diamonds.data.mapper.toDomain
import com.example.diamonds.data.mapper.toEntity
import com.example.diamonds.data.remote.backend.IBackendService
import com.example.diamonds.domain.model.*
import com.example.diamonds.domain.repository.IServiceRepository

/**
 * Service repository - reads are cached, writes require online
 */
class ServiceRepository(
    private val db: AppDatabase,
    private val backendService: IBackendService,
    private val connectivityObserver: ConnectivityObserver
) : IServiceRepository {

    private val serviceDao = db.serviceDao()

    override suspend fun getService(serviceId: String): Result<Service> {
        // READ: Check cache first
        val cached = serviceDao.getById(serviceId)
        if (cached != null) {
            return Result.Success(cached.toDomain())
        }

        // Cache empty, fetch if online
        if (!connectivityObserver.isOnline()) {
            return Result.Error(OfflineException("Service not available offline"))
        }

        return try {
            val result = backendService.getService(serviceId)
            when (result) {
                is Result.Success -> {
                    val service = result.data.toDomain()
                    serviceDao.upsert(service.toEntity())
                    Result.Success(service)
                }
                is Result.Error -> result
                is Result.Loading -> Result.Loading
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getServicesForProvider(providerId: String): Result<List<Service>> {
        // READ: Check cache first
        val cached = serviceDao.getForProvider(providerId)
        if (cached.isNotEmpty()) {
            return Result.Success(cached.map { it.toDomain() })
        }

        // Cache empty, fetch if online
        if (!connectivityObserver.isOnline()) {
            return Result.Error(OfflineException("Services not available offline"))
        }

        return try {
            val result = backendService.getServicesForProvider(providerId)
            when (result) {
                is Result.Success -> {
                    val services = result.data.map { it.toDomain() }
                    services.forEach { serviceDao.upsert(it.toEntity()) }
                    Result.Success(services)
                }
                is Result.Error -> result
                is Result.Loading -> Result.Loading
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun searchServicesByCategory(category: ServiceCategory): Result<List<Service>> {
        // READ: Check cache first
        val cached = serviceDao.getByCategory(category.name)
        if (cached.isNotEmpty()) {
            return Result.Success(cached.map { it.toDomain() })
        }

        // Cache empty, fetch if online
        if (!connectivityObserver.isOnline()) {
            return Result.Error(OfflineException("Services not available offline"))
        }

        return try {
            val result = backendService.searchServicesByCategory(category.name)
            when (result) {
                is Result.Success -> {
                    val services = result.data.map { it.toDomain() }
                    services.forEach { serviceDao.upsert(it.toEntity()) }
                    Result.Success(services)
                }
                is Result.Error -> result
                is Result.Loading -> Result.Loading
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun createService(service: Service): Result<Service> {
        // WRITE: Require online
        if (!connectivityObserver.isOnline()) {
            return Result.Error(OfflineException("Service creation requires internet connection"))
        }

        return try {
            val dto = com.example.diamonds.data.remote.backend.ServiceDto(
                id = service.id,
                providerId = service.providerId,
                title = service.title,
                description = service.description,
                basePrice = service.basePrice,
                duration = service.duration,
                category = service.category.name,
                imageUrl = service.imageUrl,
                isActive = service.isActive,
                createdAt = service.createdAt,
                updatedAt = service.updatedAt
            )
            val result = backendService.createService(dto)
            when (result) {
                is Result.Success -> {
                    val createdService = result.data.toDomain()
                    serviceDao.upsert(createdService.toEntity())
                    Result.Success(createdService)
                }
                is Result.Error -> result
                is Result.Loading -> Result.Loading
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateService(service: Service): Result<Service> {
        // WRITE: Require online
        if (!connectivityObserver.isOnline()) {
            return Result.Error(OfflineException("Service update requires internet connection"))
        }

        // TODO: Implement in backend service
        return Result.Error(Exception("Not implemented in backend service"))
    }
}
