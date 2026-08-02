package com.example.diamonds.data.remote.backend

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.lang.reflect.Modifier

/**
 * Guards the contract Firestore's object mapper depends on.
 *
 * `DocumentSnapshot.toObject(T::class.java)` / `QuerySnapshot.toObjects(...)` instantiate the
 * target reflectively and therefore require a **public no-argument constructor**. Kotlin only
 * emits one for a `data class` when *every* primary-constructor parameter has a default value.
 *
 * Before this was enforced, all 15 DTOs that existed then had at least one parameter without a
 * default, so no DTO had a no-arg constructor and **every** Firestore read threw — swallowed by
 * `FirebaseBackendService`'s `firestoreCall` wrapper. Writes succeeded, so flipping
 * `USE_MOCK_BACKEND=false` produced a write-only app. See `TECH_LEAD_REVIEW.md` §9.4.
 *
 * These tests fail the moment someone removes a default from a DTO parameter, which is exactly
 * the change that would silently break reads again.
 */
class FirestoreDtoContractTest {

    /**
     * Every `*Dto` declared in `IBackendService.kt`. `*Request` types are deliberately excluded:
     * they are only ever written, never deserialized, so required parameters there are a genuine
     * safety benefit.
     *
     * Most of these are read back via `toObject`/`toObjects` and would break at runtime without a
     * no-arg constructor. `FcmTokenDto` is write-only today, but it is held to the same contract:
     * defaults cost nothing, and the completeness check below is mechanical — it derives the
     * expected set from the source, so exempting a type would mean weakening the guard itself.
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
        SavedLocationDto::class.java,
        FcmTokenDto::class.java
    )

    @Test
    fun `every Firestore-read DTO exposes a public no-arg constructor`() {
        val offenders = firestoreReadDtos.mapNotNull { type ->
            val ctor = runCatching { type.getDeclaredConstructor() }.getOrNull()
            when {
                ctor == null -> "${type.simpleName}: is missing a no-arg constructor " +
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
    fun `every Dto declared in IBackendService is registered here`() {
        // Derived from the source rather than a hardcoded count: a size check would still pass if
        // someone added a 16th DTO and forgot to register it, which is precisely the case this
        // guard exists to catch.
        val declared = DTO_DECLARATION
            .findAll(backendServiceSource().readText())
            .map { it.groupValues[1] }
            .toSet()
        val registered = firestoreReadDtos.map { it.simpleName }.toSet()

        assertTrue(
            "These *Dto types are declared in IBackendService.kt but are not registered in " +
                    "firestoreReadDtos, so nothing verifies Firestore can deserialize them: " +
                    (declared - registered),
            (declared - registered).isEmpty()
        )
        assertTrue(
            "These types are registered in firestoreReadDtos but are no longer declared in " +
                    "IBackendService.kt: " + (registered - declared),
            (registered - declared).isEmpty()
        )
    }

    /**
     * Locates `IBackendService.kt` on disk. Gradle runs a module's tests with the module directory
     * as the working directory, but the candidates below also cover being run from the repo root.
     */
    private fun backendServiceSource(): File {
        val relative = "src/main/java/com/example/diamonds/data/remote/backend/IBackendService.kt"
        val candidates = listOf(File(relative), File("data/$relative"), File("../data/$relative"))
        return candidates.firstOrNull { it.isFile }
            ?: error(
                "Could not locate IBackendService.kt (working dir: ${File(".").absolutePath}). " +
                        "Tried: ${candidates.joinToString { it.path }}"
            )
    }

    private companion object {
        val DTO_DECLARATION = Regex("""^data class (\w+Dto)\(""", RegexOption.MULTILINE)
    }
}
