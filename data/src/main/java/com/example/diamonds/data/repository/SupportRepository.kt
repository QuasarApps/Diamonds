package com.example.diamonds.data.repository

import com.example.diamonds.data.local.AppDatabase
import com.example.diamonds.data.mapper.toDomain
import com.example.diamonds.data.mapper.toEntity
import com.example.diamonds.data.remote.backend.CancelBookingWithReasonRequest
import com.example.diamonds.data.remote.backend.CreateSupportTicketRequest
import com.example.diamonds.data.remote.backend.EditBookingRequest
import com.example.diamonds.data.remote.backend.FileClaimRequest
import com.example.diamonds.data.remote.backend.IBackendService
import com.example.diamonds.domain.model.Booking
import com.example.diamonds.domain.model.CancellationReason
import com.example.diamonds.domain.model.Claim
import com.example.diamonds.domain.model.HelpArticle
import com.example.diamonds.domain.model.Payment
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.model.SupportTicket
import com.example.diamonds.domain.repository.ISupportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SupportRepository(
    private val db: AppDatabase,
    private val backend: IBackendService
) : ISupportRepository {

    // ── Support Tickets ────────────────────────────────────────────────────

    override suspend fun createSupportTicket(ticket: SupportTicket): Result<SupportTicket> {
        val request = CreateSupportTicketRequest(
            userId = ticket.userId, userRole = ticket.userRole, bookingId = ticket.bookingId,
            type = ticket.type.name, subject = ticket.subject, description = ticket.description
        )
        return when (val r = backend.createSupportTicket(request)) {
            is Result.Success -> {
                val domain = r.data.toDomain()
                db.supportTicketDao().upsert(domain.toEntity())
                Result.Success(domain)
            }

            is Result.Error -> r
            is Result.Loading -> Result.Loading
        }
    }

    override suspend fun getSupportTicket(ticketId: String): Result<SupportTicket> {
        db.supportTicketDao().getById(ticketId)?.let { return Result.Success(it.toDomain()) }
        return when (val r = backend.getSupportTicket(ticketId)) {
            is Result.Success -> {
                val domain = r.data.toDomain()
                db.supportTicketDao().upsert(domain.toEntity())
                Result.Success(domain)
            }

            is Result.Error -> r
            is Result.Loading -> Result.Loading
        }
    }

    override suspend fun getTicketsForUser(userId: String): Result<List<SupportTicket>> {
        return when (val r = backend.getTicketsForUser(userId)) {
            is Result.Success -> {
                val list = r.data.map { it.toDomain() }
                list.forEach { db.supportTicketDao().upsert(it.toEntity()) }
                Result.Success(list)
            }

            is Result.Error -> {
                val cached = db.supportTicketDao().getForUser(userId).map { it.toDomain() }
                if (cached.isNotEmpty()) Result.Success(cached) else r
            }

            is Result.Loading -> Result.Loading
        }
    }

    override fun observeTicketsForUser(userId: String): Flow<List<SupportTicket>> =
        db.supportTicketDao().observeForUser(userId).map { list -> list.map { it.toDomain() } }

    // ── Claims ─────────────────────────────────────────────────────────────

    override suspend fun fileClaim(claim: Claim): Result<Claim> {
        val request = FileClaimRequest(
            bookingId = claim.bookingId, filedByUserId = claim.filedByUserId,
            filedByRole = claim.filedByRole, claimType = claim.claimType.name,
            description = claim.description, evidenceImageUrls = claim.evidenceImageUrls
        )
        return when (val r = backend.fileClaim(request)) {
            is Result.Success -> {
                val domain = r.data.toDomain()
                db.claimDao().upsert(domain.toEntity())
                Result.Success(domain)
            }

            is Result.Error -> r
            is Result.Loading -> Result.Loading
        }
    }

    override suspend fun getClaim(claimId: String): Result<Claim> {
        db.claimDao().getById(claimId)?.let { return Result.Success(it.toDomain()) }
        return when (val r = backend.getClaim(claimId)) {
            is Result.Success -> {
                val domain = r.data.toDomain()
                db.claimDao().upsert(domain.toEntity())
                Result.Success(domain)
            }

            is Result.Error -> r
            is Result.Loading -> Result.Loading
        }
    }

    override suspend fun getClaimsForUser(userId: String): Result<List<Claim>> {
        return when (val r = backend.getClaimsForUser(userId)) {
            is Result.Success -> {
                val list = r.data.map { it.toDomain() }
                list.forEach { db.claimDao().upsert(it.toEntity()) }
                Result.Success(list)
            }

            is Result.Error -> {
                val cached = db.claimDao().getForUser(userId).map { it.toDomain() }
                if (cached.isNotEmpty()) Result.Success(cached) else r
            }

            is Result.Loading -> Result.Loading
        }
    }

    override suspend fun getClaimForBooking(bookingId: String): Result<Claim?> {
        db.claimDao().getByBookingId(bookingId)?.let { return Result.Success(it.toDomain()) }
        return when (val r = backend.getClaimForBooking(bookingId)) {
            is Result.Success -> {
                r.data?.let { dto ->
                    val domain = dto.toDomain()
                    db.claimDao().upsert(domain.toEntity())
                    Result.Success(domain)
                } ?: Result.Success(null)
            }

            is Result.Error -> r
            is Result.Loading -> Result.Loading
        }
    }

    override fun observeClaimsForUser(userId: String): Flow<List<Claim>> =
        db.claimDao().observeForUser(userId).map { list -> list.map { it.toDomain() } }

    // ── Booking actions ────────────────────────────────────────────────────

    override suspend fun cancelBookingWithReason(
        bookingId: String,
        reason: CancellationReason,
        notes: String?
    ): Result<Booking> {
        val request = CancelBookingWithReasonRequest(bookingId, reason.name, notes)
        return when (val r = backend.cancelBookingWithReason(request)) {
            is Result.Success -> {
                val domain = r.data.toDomain()
                db.bookingDao().upsert(domain.toEntity())
                Result.Success(domain)
            }

            is Result.Error -> r
            is Result.Loading -> Result.Loading
        }
    }

    override suspend fun editBooking(
        bookingId: String,
        newAddress: String?,
        newServiceId: String?
    ): Result<Booking> {
        val request = EditBookingRequest(bookingId, newAddress, newServiceId)
        return when (val r = backend.editBooking(request)) {
            is Result.Success -> {
                val domain = r.data.toDomain()
                db.bookingDao().upsert(domain.toEntity())
                Result.Success(domain)
            }

            is Result.Error -> r
            is Result.Loading -> Result.Loading
        }
    }

    override suspend fun requestRefund(bookingId: String): Result<Payment> {
        return when (val r = backend.requestRefund(bookingId)) {
            is Result.Success -> Result.Success(r.data.toDomain())
            is Result.Error -> r
            is Result.Loading -> Result.Loading
        }
    }

    // ── Help Articles ──────────────────────────────────────────────────────

    override suspend fun getHelpArticles(): Result<List<HelpArticle>> {
        return when (val r = backend.getHelpArticles()) {
            is Result.Success -> Result.Success(r.data.map { it.toDomain() })
            is Result.Error -> r
            is Result.Loading -> Result.Loading
        }
    }
}
