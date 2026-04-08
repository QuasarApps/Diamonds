package com.example.diamonds.data.remote.backend

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Provides real-time Firestore snapshot listeners for bookings.
 *
 * These return [Flow]s that emit whenever the underlying Firestore document(s)
 * change, enabling live booking status updates in the UI.
 *
 * Only active when Firebase backend is enabled (`USE_MOCK_BACKEND = false`).
 */
class FirestoreBookingListener {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    /**
     * Observe a single booking document in real-time.
     * Emits the latest [BookingDto] whenever the document changes.
     */
    fun observeBooking(bookingId: String): Flow<BookingDto?> = callbackFlow {
        val registration: ListenerRegistration = db.collection("bookings")
            .document(bookingId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val dto = snapshot?.toObject(BookingDto::class.java)
                trySend(dto)
            }

        awaitClose { registration.remove() }
    }

    /**
     * Observe all bookings for a given provider in real-time.
     * Emits the full list whenever any booking in the query set changes.
     */
    fun observeProviderBookings(providerId: String): Flow<List<BookingDto>> = callbackFlow {
        val registration: ListenerRegistration = db.collection("bookings")
            .whereEqualTo("providerId", providerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val bookings = snapshot?.toObjects(BookingDto::class.java) ?: emptyList()
                trySend(bookings)
            }

        awaitClose { registration.remove() }
    }

    /**
     * Observe all bookings for a given client in real-time.
     * Emits the full list whenever any booking in the query set changes.
     */
    fun observeClientBookings(clientId: String): Flow<List<BookingDto>> = callbackFlow {
        val registration: ListenerRegistration = db.collection("bookings")
            .whereEqualTo("clientId", clientId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val bookings = snapshot?.toObjects(BookingDto::class.java) ?: emptyList()
                trySend(bookings)
            }

        awaitClose { registration.remove() }
    }
}
