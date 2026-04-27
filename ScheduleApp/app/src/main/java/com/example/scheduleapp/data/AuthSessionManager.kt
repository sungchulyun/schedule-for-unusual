package com.example.scheduleapp.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val accessTokenExpiresAtEpochMillis: Long,
    val refreshTokenExpiresAtEpochMillis: Long,
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
    private const val KeyAccessTokenExpiresAt = "access_token_expires_at"
    private const val KeyRefreshTokenExpiresAt = "refresh_token_expires_at"
    private const val KeyCurrentUserId = "current_user_id"
    private const val KeyGroupId = "group_id"
    private const val KeyNickname = "nickname"
    private const val KeyProfileImageUrl = "profile_image_url"
    private const val KeyPartnerUserId = "partner_user_id"
    private const val KeyPendingInviteToken = "pending_invite_token"
    private var appContext: Context? = null
    @Volatile
    private var currentSession: AuthSession? = null
    @Volatile
    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        preferences = createPreferences(requireNotNull(appContext))
        currentSession = readSession()
    }

    fun getSession(): AuthSession? {
        return currentSession ?: readSession()?.also { currentSession = it }
    }

    fun saveSession(session: AuthSession) {
        val context = requireNotNull(appContext) { "AuthSessionManager is not initialized." }
        prefs(context)
            .edit()
            .putString(KeyAccessToken, session.accessToken)
            .putString(KeyRefreshToken, session.refreshToken)
            .putString(KeyTokenType, session.tokenType)
            .putLong(KeyAccessTokenExpiresAt, session.accessTokenExpiresAtEpochMillis)
            .putLong(KeyRefreshTokenExpiresAt, session.refreshTokenExpiresAtEpochMillis)
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
        prefs(context)
            .edit()
            .clear()
            .apply()
        currentSession = null
    }

    fun getPendingInviteToken(): String? {
        val context = appContext ?: return null
        return prefs(context)
            .getString(KeyPendingInviteToken, null)
            ?.trim()
            ?.ifBlank { null }
    }

    fun savePendingInviteToken(inviteToken: String) {
        val context = requireNotNull(appContext) { "AuthSessionManager is not initialized." }
        prefs(context)
            .edit()
            .putString(KeyPendingInviteToken, inviteToken.trim())
            .apply()
    }

    fun clearPendingInviteToken() {
        val context = appContext ?: return
        prefs(context)
            .edit()
            .remove(KeyPendingInviteToken)
            .apply()
    }

    private fun readSession(): AuthSession? {
        val context = appContext ?: return null
        val preferences = prefs(context)
        val accessToken = preferences.getString(KeyAccessToken, null)?.trim().orEmpty()
        val refreshToken = preferences.getString(KeyRefreshToken, null)?.trim().orEmpty()
        val tokenType = preferences.getString(KeyTokenType, null)?.trim().orEmpty()
        val accessTokenExpiresAt = preferences.getLong(KeyAccessTokenExpiresAt, 0L)
        val refreshTokenExpiresAt = preferences.getLong(KeyRefreshTokenExpiresAt, 0L)
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
            accessTokenExpiresAtEpochMillis = accessTokenExpiresAt,
            refreshTokenExpiresAtEpochMillis = refreshTokenExpiresAt,
            currentUserId = currentUserId,
            groupId = groupId,
            nickname = nickname,
            profileImageUrl = profileImageUrl,
            partnerUserId = partnerUserId
        )
    }

    private fun prefs(context: Context): SharedPreferences {
        return preferences ?: synchronized(this) {
            preferences ?: createPreferences(context.applicationContext).also { preferences = it }
        }
    }

    private fun createPreferences(context: Context): SharedPreferences {
        return runCatching {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                PreferencesName,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }.getOrElse {
            context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        }
    }
}
