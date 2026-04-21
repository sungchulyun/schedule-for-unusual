package com.example.scheduleapp.data

object CalendarApiConfig {
    val baseUrl: String = "http://10.0.2.2:8080/".ensureTrailingSlash()
    val groupId: String? = "grp_01J8ZP3TQ4X".ifBlank { null }
    val currentUserId: String = "usr_me"
    val partnerUserId: String? = "usr_partner".ifBlank { null }
}

private fun String.ensureTrailingSlash(): String {
    return if (endsWith("/")) this else "$this/"
}
