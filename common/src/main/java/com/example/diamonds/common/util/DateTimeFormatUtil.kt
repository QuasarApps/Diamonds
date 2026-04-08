@file:Suppress("NewApi") // java.time APIs are handled by core library desugaring

package com.example.diamonds.common.util

import android.content.Context
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Centralised date/time formatting that respects the device locale / user settings.
 *
 * **Storage format** (internal / domain model strings) is always ISO:
 *   - Date → `"yyyy-MM-dd"`
 *   - Time → `"HH:mm"`
 *
 * **Display format** adapts to the device locale via [java.time.format.FormatStyle.MEDIUM]
 * for dates (e.g. "Apr 8, 2026" in en-US, "8 Apr 2026" in en-GB) and the system 12h/24h
 * preference for times.
 */
object DateTimeFormatUtil {

    // ── ISO storage formats ────────────────────────────────────────────────────
    private const val ISO_DATE_PATTERN = "yyyy-MM-dd"

    // ── Display formatters (locale-aware) ─────────────────────────────────────

    /**
     * Format an ISO date string (`"yyyy-MM-dd"`) for display using the device locale.
     *
     * Example: `"2026-04-08"` → `"Apr 8, 2026"` (en-US) or `"8 avr. 2026"` (fr-FR).
     *
     * Returns the original string unchanged if parsing fails.
     */
    fun formatDateForDisplay(isoDate: String): String {
        return try {
            val date = LocalDate.parse(isoDate)
            val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(Locale.getDefault())
            date.format(formatter)
        } catch (_: Exception) {
            isoDate
        }
    }

    /**
     * Format an ISO time string (`"HH:mm"`) for display, respecting the system
     * 12-hour / 24-hour preference.
     *
     * When a [context] is supplied, `android.text.format.DateFormat.is24HourFormat`
     * is checked. Otherwise falls back to a locale-aware short time format.
     *
     * Example: `"14:30"` → `"2:30 PM"` (12h) or `"14:30"` (24h).
     */
    fun formatTimeForDisplay(isoTime: String, context: Context? = null): String {
        return try {
            val localTime = LocalTime.parse(isoTime)
            val is24h = context?.let { DateFormat.is24HourFormat(it) }

            if (is24h != null) {
                // Use the system 12h/24h setting explicitly
                val pattern = if (is24h) "HH:mm" else "h:mm a"
                val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
                localTime.format(formatter)
            } else {
                // No context – fall back to locale short time
                val formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                    .withLocale(Locale.getDefault())
                localTime.format(formatter)
            }
        } catch (_: Exception) {
            isoTime
        }
    }

    /**
     * Format a millisecond-epoch timestamp for display as a locale-aware date string.
     *
     * Useful for `createdAt` fields stored as epoch-millis strings.
     */
    fun formatTimestampAsDate(timestampMillis: Long): String {
        return try {
            val sdf = SimpleDateFormat(
                (DateFormat.getBestDateTimePattern(Locale.getDefault(), "yMMMd")),
                Locale.getDefault()
            )
            sdf.format(Date(timestampMillis))
        } catch (_: Exception) {
            timestampMillis.toString()
        }
    }

    /**
     * Format a millisecond-epoch timestamp for display as a locale-aware date + time string.
     */
    fun formatTimestampAsDateTime(timestampMillis: Long, context: Context? = null): String {
        return try {
            val is24h = context?.let { DateFormat.is24HourFormat(it) }
            val timeSkeleton = if (is24h == false) "yMMMd h:mm a" else "yMMMd HH:mm"
            val pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), timeSkeleton)
            val sdf = SimpleDateFormat(pattern, Locale.getDefault())
            sdf.format(Date(timestampMillis))
        } catch (_: Exception) {
            timestampMillis.toString()
        }
    }

    // ── Millis → ISO storage string ───────────────────────────────────────────

    /**
     * Convert DatePicker millis to an ISO date string for storage.
     * The DatePicker returns UTC midnight millis, so we use UTC timezone.
     */
    fun millisToIsoDate(millis: Long): String {
        val sdf = SimpleDateFormat(ISO_DATE_PATTERN, Locale.ROOT)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(millis))
    }

    /**
     * Convert hour + minute (from TimePicker) to an ISO time string for storage.
     */
    fun toIsoTime(hour: Int, minute: Int): String {
        return String.format(Locale.ROOT, "%02d:%02d", hour, minute)
    }

    /**
     * Convert DatePicker millis to a locale-aware display string.
     */
    fun millisToDisplayDate(millis: Long): String {
        return try {
            val sdf = SimpleDateFormat(ISO_DATE_PATTERN, Locale.ROOT)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val isoDate = sdf.format(Date(millis))
            formatDateForDisplay(isoDate)
        } catch (_: Exception) {
            millis.toString()
        }
    }

    /**
     * Convert hour + minute to a locale-aware display string.
     */
    fun toDisplayTime(hour: Int, minute: Int, context: Context? = null): String {
        val iso = toIsoTime(hour, minute)
        return formatTimeForDisplay(iso, context)
    }
}
