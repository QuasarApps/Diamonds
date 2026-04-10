package com.example.diamonds.common.util

/**
 * Connectivity state enum
 */
enum class ConnectivityState {
    ONLINE, OFFLINE, METERED
}

/**
 * Constants for the application
 */
object Constants {
    const val DEFAULT_SYNC_INTERVAL_MINUTES = 15
    const val MAX_RETRY_ATTEMPTS = 5
    const val INITIAL_BACKOFF_MINUTES = 1
    const val MAX_BACKOFF_MINUTES = 60
    const val PROVIDER_SEARCH_RADIUS_KM = 10
    const val CACHE_VALIDITY_HOURS = 1
}
