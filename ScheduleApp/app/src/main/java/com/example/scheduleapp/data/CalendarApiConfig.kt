package com.example.scheduleapp.data

object CalendarApiConfig {
    const val kakaoAppCallbackUri: String = "scheduleapp://auth/callback"

    val baseUrl: String = "https://api.link-together.site".ensureTrailingSlash()

    val groupId: String?
        get() = AuthSessionManager.getSession()?.groupId?.ifBlank { null }

    val currentUserId: String
        get() = AuthSessionManager.getSession()?.currentUserId ?: ""

    val partnerUserId: String?
        get() = AuthSessionManager.getSession()?.partnerUserId?.ifBlank { null }
}

private fun String.ensureTrailingSlash(): String {
    return if (endsWith("/")) this else "$this/"
}
