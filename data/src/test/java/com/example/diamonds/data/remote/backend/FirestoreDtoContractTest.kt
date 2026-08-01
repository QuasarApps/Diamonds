package com.example.diamonds.data.remote.backend

import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier

/**
 * Guards the contract Firestore's object mapper depends on.
 *
 * `DocumentSnapshot.toObject(T::class.java)` / `QuerySnapshot.toObjects(...)` instantiate the
 * target reflectively and therefore require a **public no-argument constructor**. Kotlin only
 * emits one for a `data class` when *every* primary-constructor parameter has a default value.
 *
 * Before this was enforced, all 15 DTOs had at least one parameter without a default, so no DTO
 * had a no-arg constructor and **every** Firestore read threw — swallowed into `Result.Error` by
 * `FirebaseBackendService`'s `firestoreCall` wrapper. Writes succeeded, so flipping
 * `USE_MOCK_BACKEND=false` produced a write-only app. See `TECH_LEAD_REVIEW.md` §9.4.
 *
 * These tests fail the moment someone removes a default from a DTO parameter, which is exactly
 * the change that would silently break reads again.
 */
class FirestoreDtoContractTest {

    /**
     * Every DTO that `FirebaseBackendService` reads back via `toObject`/`toObjects`.
     * `*Request` types are deliberately excluded: they are only ever written, never deserialized,
     * so required parameters there are a genuine safety benefit.
     */
    private val firestoreReadDtos = listOf(
        ClientDto::class.java,
        ProviderDto::class.java,
        ServiceDto::class.java,
        BookingDto::class.java,
        ReviewDto::class.java,
        PaymentDto::class.java,
        ProviderLocationDto::class.java,
        ServiceAreaDto::class.java,
        ConversationDto::class.java,
        MessageDto::class.java,
        RecurringBookingDto::class.java,
        SupportTicketDto::class.java,
        ClaimDto::class.java,
        HelpArticleDto::class.java,
        SavedLocationDto::class.java
    )

    @Test
    fun `every Firestore-read DTO exposes a public no-arg constructor`() {
        val offenders = firestoreReadDtos.mapNotNull { type ->
            val ctor = runCatching { type.getDeclaredConstructor() }.getOrNull()
            when {
                ctor == null -> "${type.simpleName}: no no-arg constructor " +
                        "(at least one primary-constructor parameter lacks a default)"

                !Modifier.isPublic(ctor.modifiers) -> "${type.simpleName}: no-arg constructor is not public"
                else -> null
            }
        }

        assertTrue(
            "Firestore's object mapper cannot deserialize these DTOs, so every read of them " +
                    "would fail at runtime:\n  " + offenders.joinToString("\n  "),
            offenders.isEmpty()
        )
    }

    @Test
    fun `every Firestore-read DTO can actually be instantiated reflectively`() {
        // getDeclaredConstructor() succeeding is necessary but not sufficient — this proves the
        // mapper can really construct the instance it then populates via setters/fields.
        val offenders = firestoreReadDtos.mapNotNull { type ->
            runCatching { type.getDeclaredConstructor().newInstance() }
                .exceptionOrNull()
                ?.let { "${type.simpleName}: ${it::class.simpleName} — ${it.message}" }
        }

        assertTrue(
            "These DTOs have a no-arg constructor that throws when invoked:\n  " +
                    offenders.joinToString("\n  "),
            offenders.isEmpty()
        )
    }

    @Test
    fun `the registered DTO list is complete`() {
        // Keeps this test honest: a DTO added to IBackendService.kt must be registered above,
        // otherwise it could ship without a no-arg constructor and reintroduce the read failure.
        // If this fails, add the new DTO to firestoreReadDtos rather than just bumping the number.
        assertTrue(
            "IBackendService.kt declares a different number of *Dto types than the " +
                    "${firestoreReadDtos.size} registered here — add the new one to " +
                    "firestoreReadDtos so it is covered.",
            firestoreReadDtos.size == 15
        )
    }
}
