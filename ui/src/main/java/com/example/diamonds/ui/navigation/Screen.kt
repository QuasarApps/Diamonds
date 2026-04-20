package com.example.diamonds.ui.navigation

/**
 * Every navigable destination in the app, organised into three groups:
 *
 *  1. **Top-level** – Splash, Auth flow
 *  2. **Customer tabs** – the bottom-nav destinations a *customer* sees
 *  3. **Cleaner tabs**  – the bottom-nav destinations a *cleaner* sees
 *
 * Both customer and cleaner enter through the same [Splash] → [Login] flow;
 * after auth the [AppShell] reads the session role and renders the correct
 * set of tabs.
 */
sealed class Screen(val route: String) {

    // ── Top-level (role-agnostic) ──────────────────────────────────────────
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Signup : Screen("signup")

    /** Single post-auth entry point – role drives the content inside. */
    data object AppShell : Screen("app_shell")

    // ── Customer tab destinations ──────────────────────────────────────────
    data object CustomerHome : Screen("customer/home")
    data object CustomerBookings : Screen("customer/bookings")
    data object CustomerProfile : Screen("customer/profile")

    // ── Customer booking flow (pushed onto the inner back-stack) ───────────
    data object ProviderSearch : Screen("customer/search")
    data class  ServiceList(val providerId: String = "{providerId}") : Screen("customer/services/{providerId}") {
        fun route(id: String) = "customer/services/$id"
    }
    data class  BookingForm(val serviceId: String = "{serviceId}", val providerId: String = "{providerId}") : Screen("customer/book/{providerId}/{serviceId}") {
        fun route(pid: String, sid: String) = "customer/book/$pid/$sid"
    }
    data class  BookingConfirmation(val bookingId: String = "{bookingId}") : Screen("customer/confirm/{bookingId}") {
        fun route(id: String) = "customer/confirm/$id"
    }
    data class  BookingDetail(val bookingId: String = "{bookingId}") : Screen("customer/booking/{bookingId}") {
        fun route(id: String) = "customer/booking/$id"
    }
    data class  ReviewBooking(
        val bookingId: String  = "{bookingId}",
        val providerId: String = "{providerId}"
    ) : Screen("customer/review/{bookingId}/{providerId}") {
        fun route(bid: String, pid: String) = "customer/review/$bid/$pid"
    }
    data class  ProviderRatings(val providerId: String = "{providerId}") : Screen("customer/ratings/{providerId}") {
        fun route(id: String) = "customer/ratings/$id"
    }
    data class  Payment(val bookingId: String = "{bookingId}") : Screen("customer/pay/{bookingId}") {
        fun route(id: String) = "customer/pay/$id"
    }
    data class  PaymentSuccess(val paymentId: String = "{paymentId}") : Screen("customer/pay/success/{paymentId}") {
        fun route(id: String) = "customer/pay/success/$id"
    }
    data object PaymentHistory : Screen("customer/payments")

    // ── Subscription / Recurring Bookings ──────────────────────────────────
    data class RecurringBookingSetup(
        val providerId: String = "{providerId}",
        val serviceId: String = "{serviceId}"
    ) : Screen("customer/recurring/{providerId}/{serviceId}") {
        fun route(pid: String, sid: String) = "customer/recurring/$pid/$sid"
    }

    data object SubscriptionManagement : Screen("customer/subscriptions")

    // ── Cleaner tab destinations ───────────────────────────────────────────
    data object CleanerDashboard       : Screen("cleaner/dashboard")
    data object CleanerBookings : Screen("cleaner/bookings")
    data object CleanerEarnings        : Screen("cleaner/earnings")
    data object CleanerProfile         : Screen("cleaner/profile")
    data object CleanerServiceManage   : Screen("cleaner/services")
    data class  CleanerServiceEdit(val serviceId: String = "{serviceId}") : Screen("cleaner/services/edit/{serviceId}") {
        fun route(id: String) = "cleaner/services/edit/$id"
    }

    /** Cleaner reviews a client after a completed booking. */
    data class LeaveClientReview(
        val bookingId: String = "{bookingId}",
        val clientId: String = "{clientId}"
    ) : Screen("cleaner/review-client/{bookingId}/{clientId}") {
        fun route(bid: String, cid: String) = "cleaner/review-client/$bid/$cid"
    }

    /** View a client's ratings from all providers. */
    data class ClientRatings(val clientId: String = "{clientId}") :
        Screen("cleaner/client-ratings/{clientId}") {
        fun route(id: String) = "cleaner/client-ratings/$id"
    }

    // ── Company tab destinations ───────────────────────────────────────────
    data object CompanyDashboard  : Screen("company/dashboard")
    data object CompanyBookings   : Screen("company/bookings")
    data object CompanyTeam       : Screen("company/team")
    data object CompanyEarnings   : Screen("company/earnings")
    data object CompanyProfile    : Screen("company/profile")
    data object CompanyServiceManage : Screen("company/services")
    data class  CompanyServiceEdit(val serviceId: String = "{serviceId}") : Screen("company/services/edit/{serviceId}") {
        fun route(id: String) = "company/services/edit/$id"
    }

    // ── Shared (all roles) ─────────────────────────────────────────────────
    data object Notifications : Screen("notifications")
    data object NotificationPreferences : Screen("notifications/preferences")
    data object SyncStatus : Screen("sync_status")
    data object LanguageSelector : Screen("settings/language")

    // ── Map & Location ─────────────────────────────────────────────────────
    /** Pick an address on a map during the booking flow. */
    data object BookingMap : Screen("customer/map")

    /** Real-time tracking of a cleaner for an active booking. */
    data class ProviderTracking(val bookingId: String = "{bookingId}") :
        Screen("customer/tracking/{bookingId}") {
        fun route(id: String) = "customer/tracking/$id"
    }

    // ── Chat & Messaging ───────────────────────────────────────────────────
    /** List of all conversations for the current user. */
    data object ConversationList : Screen("chat/conversations")

    /** Open a specific chat thread. Requires conversationId as a nav arg. */
    data class Chat(val conversationId: String = "{conversationId}") :
        Screen("chat/{conversationId}") {
        fun route(id: String) = "chat/$id"
    }

    // ── Help, Support & Claims ─────────────────────────────────────────────
    data object HelpCenter : Screen("support/help")

    data class ContextualHelp(val bookingId: String = "{bookingId}") :
        Screen("support/help/{bookingId}") {
        fun route(id: String) = "support/help/$id"
    }

    data class FileClaim(val bookingId: String = "{bookingId}") :
        Screen("support/claim/{bookingId}") {
        fun route(id: String) = "support/claim/$id"
    }

    data class ClaimDetail(val claimId: String = "{claimId}") :
        Screen("support/claim/detail/{claimId}") {
        fun route(id: String) = "support/claim/detail/$id"
    }

    data class CancelBooking(val bookingId: String = "{bookingId}") :
        Screen("support/cancel/{bookingId}") {
        fun route(id: String) = "support/cancel/$id"
    }

    data class EditBooking(val bookingId: String = "{bookingId}") :
        Screen("support/edit/{bookingId}") {
        fun route(id: String) = "support/edit/$id"
    }

    // ── Profile & Locations ────────────────────────────────────────────────
    data object SavedLocations : Screen("customer/locations")
}
