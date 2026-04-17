package com.example.diamonds.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.diamonds.domain.model.Booking
import com.example.diamonds.domain.model.BookingStatus
import com.example.diamonds.domain.model.RecurringBooking
import com.example.diamonds.domain.model.RecurringFrequency
import com.example.diamonds.domain.model.Result
import com.example.diamonds.domain.model.SyncStatus
import com.example.diamonds.domain.repository.IBookingRepository
import com.example.diamonds.domain.repository.ISubscriptionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Periodic WorkManager job that checks all ACTIVE recurring bookings
 * and auto-creates booking entries when their nextBookingDate is today or past.
 */
@HiltWorker
class RecurringBookingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val subscriptionRepository: ISubscriptionRepository,
    private val bookingRepository: IBookingRepository
) : CoroutineWorker(context, params) {

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override suspend fun doWork(): Result {
        val today = dateFmt.format(Date())

        val dueResult = subscriptionRepository.getActiveRecurringBookingsDue(today)
        if (dueResult is com.example.diamonds.domain.model.Result.Error) {
            return Result.retry()
        }

        val dueBookings = (dueResult as com.example.diamonds.domain.model.Result.Success).data

        for (rb in dueBookings) {
            try {
                // Create a new booking from the recurring template
                val booking = Booking(
                    id = UUID.randomUUID().toString(),
                    clientId = rb.clientId,
                    providerId = rb.providerId,
                    serviceId = rb.serviceId,
                    status = BookingStatus.PENDING,
                    scheduledDate = rb.nextBookingDate,
                    scheduledTime = rb.preferredTime,
                    estimatedDuration = 120, // default 2 hours
                    totalPrice = rb.totalPrice,
                    notes = "Auto-generated from recurring booking ${rb.id}",
                    address = rb.address,
                    latitude = rb.latitude,
                    longitude = rb.longitude,
                    syncStatus = SyncStatus.PENDING,
                    createdAt = dateFmt.format(Date()),
                    updatedAt = dateFmt.format(Date())
                )
                bookingRepository.createBooking(booking)

                // Advance the next booking date
                val nextDate = calculateNextDate(rb)
                subscriptionRepository.advanceNextBookingDate(rb.id, nextDate)
            } catch (_: Exception) {
                // Continue with other bookings even if one fails
            }
        }

        return Result.success()
    }

    private fun calculateNextDate(rb: RecurringBooking): String {
        val cal = Calendar.getInstance()
        cal.time = dateFmt.parse(rb.nextBookingDate) ?: Date()

        when (rb.frequency) {
            RecurringFrequency.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
            RecurringFrequency.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            RecurringFrequency.FORTNIGHTLY -> cal.add(Calendar.WEEK_OF_YEAR, 2)
            RecurringFrequency.MONTHLY -> cal.add(Calendar.MONTH, 1)
        }
        return dateFmt.format(cal.time)
    }
}
