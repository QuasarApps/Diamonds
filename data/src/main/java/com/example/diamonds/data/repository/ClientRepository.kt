package com.example.diamonds.data.repository

import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.data.local.AppDatabase
import com.example.diamonds.data.mapper.toDomain
import com.example.diamonds.data.mapper.toEntity
import com.example.diamonds.data.remote.backend.IBackendService
import com.example.diamonds.domain.model.Client
import com.example.diamonds.domain.model.OfflineException
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.repository.IClientRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Client repository with offline-first read pattern
 */
class ClientRepository(
    db: AppDatabase,
    private val backendService: IBackendService,
    private val connectivityObserver: ConnectivityObserver
) : IClientRepository {

    private val clientDao = db.clientDao()

    override suspend fun getClient(clientId: String): Result<Client> {
        // READ: Check cache first
        val cached = clientDao.getById(clientId)
        if (cached != null) {
            return Result.Success(cached.toDomain())
        }

        // Not cached, fetch if online
        if (!connectivityObserver.isOnline()) {
            return Result.Error(OfflineException("Client not available offline"))
        }

        return try {
            val result = backendService.getClient(clientId)
            when (result) {
                is Result.Success -> {
                    val client = result.data.toDomain()
                    clientDao.upsert(client.toEntity())
                    Result.Success(client)
                }
                is Result.Error -> result
                is Result.Loading -> Result.Loading
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateClient(client: Client): Result<Client> {
        // WRITE: Require online
        if (!connectivityObserver.isOnline()) {
            return Result.Error(OfflineException("Client update requires internet connection"))
        }

        return try {
            val dto = com.example.diamonds.data.remote.backend.ClientDto(
                id = client.id,
                name = client.name,
                email = client.email,
                phoneNumber = client.phoneNumber,
                profileImageUrl = client.profileImageUrl,
                createdAt = client.createdAt,
                updatedAt = client.updatedAt
            )
            val result = backendService.updateClient(dto)
            when (result) {
                is Result.Success -> {
                    val updatedClient = result.data.toDomain()
                    clientDao.upsert(updatedClient.toEntity())
                    Result.Success(updatedClient)
                }
                is Result.Error -> result
                is Result.Loading -> Result.Loading
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getCurrentClient(): Result<Client> {
        // TODO: Get current user ID from session
        return Result.Error(Exception("Not implemented"))
    }

    override fun observeCurrentClient(): Flow<Client?> {
        // TODO: Observe current client from session
        return clientDao.observeById("current_user").map { it?.toDomain() }
    }
}
