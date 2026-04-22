package com.example.scheduleapp.data

import android.content.Context

data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val currentUserId: String,
    val groupId: String,
    val nickname: String?,
    val profileImageUrl: String?,
    val partnerUserId: String? = null
)

object AuthSessionManager {
    private const val PreferencesName = "auth_session"
    private const val KeyAccessToken = "access_token"
    private const val KeyRefreshToken = "refresh_token"
    private const val KeyTokenType = "token_type"
    private const val KeyCurrentUserId = "current_user_id"
    private const val KeyGroupId = "group_id"
    private const val KeyNickname = "nickname"
    private const val KeyProfileImageUrl = "profile_image_url"
    private const val KeyPartnerUserId = "partner_user_id"
    private var appContext: Context? = null
    @Volatile
    private var currentSession: AuthSession? = null

    fun initialize(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        currentSession = readSession()
    }

    fun getSession(): AuthSession? {
        return currentSession ?: readSession()?.also { currentSession = it }
    }

    fun saveSession(session: AuthSession) {
        val context = requireNotNull(appContext) { "AuthSessionManager is not initialized." }
        context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
            .edit()
            .putString(KeyAccessToken, session.accessToken)
            .putString(KeyRefreshToken, session.refreshToken)
            .putString(KeyTokenType, session.tokenType)
            .putString(KeyCurrentUserId, session.currentUserId)
            .putString(KeyGroupId, session.groupId)
            .putString(KeyNickname, session.nickname)
            .putString(KeyProfileImageUrl, session.profileImageUrl)
            .putString(KeyPartnerUserId, session.partnerUserId)
            .apply()
        currentSession = session
    }

    fun updatePartnerUserId(partnerUserId: String?) {
        val existing = getSession() ?: return
        saveSession(existing.copy(partnerUserId = partnerUserId))
    }

    fun clearSession() {
        val context = appContext ?: return
        context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
        currentSession = null
    }

    private fun readSession(): AuthSession? {
        val context = appContext ?: return null
        val preferences = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        val accessToken = preferences.getString(KeyAccessToken, null)?.trim().orEmpty()
        val refreshToken = preferences.getString(KeyRefreshToken, null)?.trim().orEmpty()
        val tokenType = preferences.getString(KeyTokenType, null)?.trim().orEmpty()
        val currentUserId = preferences.getString(KeyCurrentUserId, null)?.trim().orEmpty()
        val groupId = preferences.getString(KeyGroupId, null)?.trim().orEmpty()
        val nickname = preferences.getString(KeyNickname, null)?.trim()?.ifBlank { null }
        val profileImageUrl = preferences.getString(KeyProfileImageUrl, null)?.trim()?.ifBlank { null }
        val partnerUserId = preferences.getString(KeyPartnerUserId, null)?.trim()?.ifBlank { null }

        if (accessToken.isBlank() || refreshToken.isBlank() || tokenType.isBlank() || currentUserId.isBlank() || groupId.isBlank()) {
            return null
        }

        return AuthSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            tokenType = tokenType,
            currentUserId = currentUserId,
            groupId = groupId,
            nickname = nickname,
            profileImageUrl = profileImageUrl,
            partnerUserId = partnerUserId
        )
    }
}
