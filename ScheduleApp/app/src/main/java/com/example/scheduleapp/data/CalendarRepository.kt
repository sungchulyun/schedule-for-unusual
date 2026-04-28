package com.example.scheduleapp.data

import com.example.scheduleapp.data.remote.ApiEnvelope
import com.example.scheduleapp.data.remote.ApiErrorEnvelope
import com.example.scheduleapp.data.remote.AcceptInviteRequest
import com.example.scheduleapp.data.remote.AcceptInviteResponse
import com.example.scheduleapp.data.remote.AuthResultResponse
import com.example.scheduleapp.data.remote.AuthTokenDto
import com.example.scheduleapp.data.remote.GroupMeResponse
import com.example.scheduleapp.data.remote.KakaoMobileLoginRequest
import com.example.scheduleapp.data.remote.CalendarApiService
import com.example.scheduleapp.data.remote.CalendarMonthResponse
import com.example.scheduleapp.data.remote.CreateInviteRequest
import com.example.scheduleapp.data.remote.CreateInviteResponse
import com.example.scheduleapp.data.remote.CreateEventRequest
import com.example.scheduleapp.data.remote.DeleteShiftResponse
import com.example.scheduleapp.data.remote.EventDto
import com.example.scheduleapp.data.remote.FcmTokenRequest
import com.example.scheduleapp.data.remote.InviteLookupResponse
import com.example.scheduleapp.data.remote.LogoutRequest
import com.example.scheduleapp.data.remote.MobileLoginExchangeRequest
import com.example.scheduleapp.data.remote.MonthlyShiftItemRequest
import com.example.scheduleapp.data.remote.MonthlyShiftRequest
import com.example.scheduleapp.data.remote.RefreshTokenRequest
import com.example.scheduleapp.data.remote.ShiftDto
import com.example.scheduleapp.data.remote.UpdateUserSettingsRequest
import com.example.scheduleapp.data.remote.UpdateEventRequest
import com.example.scheduleapp.data.remote.UpsertShiftRequest
import com.example.scheduleapp.ui.calendar.CalendarEvent
import com.example.scheduleapp.ui.calendar.EventOwnerType
import com.example.scheduleapp.ui.calendar.ShiftSchedule
import com.example.scheduleapp.ui.calendar.ShiftOwnerType
import com.example.scheduleapp.ui.calendar.ShiftType
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response as OkHttpResponse
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.time.LocalDate
import java.time.YearMonth
import java.util.concurrent.TimeUnit

class CalendarRepository(
    private val service: CalendarApiService = createService()
) {
    private val errorParser = ErrorParser()

    suspend fun authenticateWithKakaoAccessToken(accessToken: String): AuthSession {
        val result = unwrap(service.authenticateWithKakaoAccessToken(KakaoMobileLoginRequest(accessToken)))
        return result.toAuthSession()
    }

    suspend fun exchangeMobileLogin(loginCode: String): AuthSession {
        val result = unwrap(service.exchangeMobileLogin(MobileLoginExchangeRequest(loginCode)))
        return result.toAuthSession()
    }

    suspend fun refreshSession(): AuthSession {
        val current = AuthSessionManager.getSession()
            ?: throw IOException("로그인 세션이 없습니다.")
        val tokens = unwrap(service.refreshToken(RefreshTokenRequest(current.refreshToken)))
        val updated = current.copy(
            accessToken = tokens.accessToken,
            refreshToken = tokens.refreshToken,
            tokenType = tokens.tokenType,
            accessTokenExpiresAtEpochMillis = tokens.accessTokenExpiresAtEpochMillis(),
            refreshTokenExpiresAtEpochMillis = tokens.refreshTokenExpiresAtEpochMillis()
        )
        AuthSessionManager.saveSession(updated)
        return updated
    }

    suspend fun logout() {
        val current = AuthSessionManager.getSession() ?: return
        runCatching {
            unwrap(service.logout(LogoutRequest(current.refreshToken)))
        }
        AuthSessionManager.clearSession()
        AuthSessionManager.clearPendingInviteToken()
    }

    suspend fun registerFcmToken(token: String) {
        if (token.isBlank() || AuthSessionManager.getSession() == null) {
            return
        }
        unwrap(service.registerFcmToken(FcmTokenRequest(token = token)))
    }

    suspend fun unregisterFcmToken(token: String) {
        if (token.isBlank() || AuthSessionManager.getSession() == null) {
            return
        }
        unwrapNullable(service.unregisterFcmToken(FcmTokenRequest(token = token, platform = null)))
    }

    suspend fun createInvite(): CreateInviteResponse {
        return unwrap(
            service.createInvite(
                CreateInviteRequest(channel = "KAKAO_TALK_SHARE")
            )
        )
    }

    suspend fun getInvite(inviteToken: String): InviteLookupResponse {
        return unwrap(service.getInvite(inviteToken))
    }

    suspend fun acceptInvite(inviteToken: String): AcceptInviteResponse {
        val response = unwrap(service.acceptInvite(AcceptInviteRequest(inviteToken = inviteToken)))
        response.tokens?.let { tokens ->
            val current = AuthSessionManager.getSession()
            if (current != null) {
                AuthSessionManager.saveSession(
                    current.copy(
                        accessToken = tokens.accessToken,
                        refreshToken = tokens.refreshToken,
                        tokenType = tokens.tokenType,
                        accessTokenExpiresAtEpochMillis = tokens.accessTokenExpiresAtEpochMillis(),
                        refreshTokenExpiresAtEpochMillis = tokens.refreshTokenExpiresAtEpochMillis(),
                        groupId = response.groupId,
                        partnerUserId = response.partnerUserId(current.currentUserId)
                    )
                )
            }
        }
        return response
    }

    suspend fun getMonth(month: YearMonth, shiftOwnerType: ShiftOwnerType): CalendarMonthData {
        val data = unwrap(
            service.getCalendarMonth(
                month.year,
                month.monthValue,
                shiftOwnerType.name,
                CalendarApiConfig.groupId
            )
        )
        return data.toDomain()
    }

    suspend fun refreshSessionContext() {
        val current = AuthSessionManager.getSession() ?: return
        val profile = unwrap(service.getMyProfile())
        val group = unwrap(service.getMyGroup())
        val partnerUserId = group.members
            .firstOrNull { it.userId != current.currentUserId }
            ?.userId
        AuthSessionManager.saveSession(
            current.copy(
                groupId = profile.groupId.ifBlank { current.groupId },
                nickname = profile.nickname,
                profileImageUrl = profile.profileImageUrl,
                defaultShiftOwnerType = profile.defaultShiftOwnerType.toShiftOwnerType(),
                partnerUserId = partnerUserId
            )
        )
    }

    suspend fun updateDefaultShiftOwnerType(shiftOwnerType: ShiftOwnerType): ShiftOwnerType {
        val result = unwrap(
            service.updateMySettings(
                UpdateUserSettingsRequest(defaultShiftOwnerType = shiftOwnerType.name)
            )
        )
        val updated = result.defaultShiftOwnerType.toShiftOwnerType()
        AuthSessionManager.updateDefaultShiftOwnerType(updated)
        return updated
    }

    suspend fun createEvent(
        ownerType: EventOwnerType,
        startDate: LocalDate,
        endDate: LocalDate,
        title: String,
        note: String?
    ) {
        val request = buildEventRequest(ownerType, startDate, endDate, title, note)
        unwrap(
            service.createEvent(
                groupId = CalendarApiConfig.groupId,
                request = request
            )
        )
    }

    suspend fun updateEvent(
        eventId: String,
        ownerType: EventOwnerType,
        startDate: LocalDate,
        endDate: LocalDate,
        title: String,
        note: String?
    ) {
        val request = buildEventRequest(ownerType, startDate, endDate, title, note)
        unwrap(
            service.updateEvent(
                eventId = eventId,
                groupId = CalendarApiConfig.groupId,
                request = request.toUpdateRequest()
            )
        )
    }

    suspend fun deleteEvent(eventId: String) {
        unwrap(service.deleteEvent(eventId, CalendarApiConfig.groupId))
    }

    suspend fun upsertShift(date: LocalDate, shiftType: ShiftType) {
        unwrap(
            service.upsertShift(
                date = date.toString(),
                groupId = CalendarApiConfig.groupId,
                request = UpsertShiftRequest(shiftType = shiftType.name)
            )
        )
    }

    suspend fun saveMonthlyShifts(
        month: YearMonth,
        updatedMonthShifts: Map<LocalDate, ShiftType?>
    ) {
        val items = updatedMonthShifts
            .toSortedMap()
            .mapNotNull { (date, shiftType) ->
                shiftType?.let {
                    MonthlyShiftItemRequest(
                        date = date.toString(),
                        shiftType = it.name
                    )
                }
            }
        unwrap(
            service.saveMonthlyShifts(
                year = month.year,
                month = month.monthValue,
                groupId = CalendarApiConfig.groupId,
                request = MonthlyShiftRequest(items)
            )
        )
    }

    suspend fun deleteShift(date: LocalDate): DeleteShiftResponse {
        return unwrap(service.deleteShift(date.toString(), CalendarApiConfig.groupId))
    }

    private suspend fun <T> unwrap(response: Response<ApiEnvelope<T>>): T {
        val body = response.body()
        if (response.isSuccessful && body?.success == true && body.data != null) {
            return body.data
        }

        val message = when {
            body?.error?.message != null -> body.error.message
            else -> errorParser.parse(response) ?: "서버 요청에 실패했습니다."
        }
        throw IOException(message)
    }

    private suspend fun <T> unwrapNullable(response: Response<ApiEnvelope<T>>): T? {
        val body = response.body()
        if (response.isSuccessful && body?.success == true) {
            return body.data
        }

        val message = when {
            body?.error?.message != null -> body.error.message
            else -> errorParser.parse(response) ?: "서버 요청에 실패했습니다."
        }
        throw IOException(message)
    }

    private fun buildEventRequest(
        ownerType: EventOwnerType,
        startDate: LocalDate,
        endDate: LocalDate,
        title: String,
        note: String?
    ): CreateEventRequest {
        val payload = ownerType.toEventPayload()
        return CreateEventRequest(
            title = title,
            startDate = startDate.toString(),
            endDate = endDate.toString(),
            subjectType = payload.subjectType,
            ownerUserId = payload.ownerUserId,
            note = note
        )
    }

    private fun CalendarMonthResponse.toDomain(): CalendarMonthData {
        return CalendarMonthData(
            events = events.mapNotNull { it.toDomain() },
            shifts = shifts.mapNotNull { it.toDomain() }
        )
    }

    private fun AuthResultResponse.toAuthSession(): AuthSession {
        return AuthSession(
            accessToken = tokens.accessToken,
            refreshToken = tokens.refreshToken,
            tokenType = tokens.tokenType,
            accessTokenExpiresAtEpochMillis = tokens.accessTokenExpiresAtEpochMillis(),
            refreshTokenExpiresAtEpochMillis = tokens.refreshTokenExpiresAtEpochMillis(),
            currentUserId = user.id,
            groupId = user.groupId,
            nickname = user.nickname,
            profileImageUrl = user.profileImageUrl,
            defaultShiftOwnerType = user.defaultShiftOwnerType.toShiftOwnerType()
        )
    }

    private fun AcceptInviteResponse.partnerUserId(currentUserId: String): String? {
        return members.firstOrNull { it.userId != currentUserId }?.userId
    }

    private fun EventDto.toDomain(): CalendarEvent? {
        val ownerTypeValue = ownerType
        val mappedOwnerType = EventOwnerType.entries.firstOrNull { it.name == ownerTypeValue }
            ?: return null
        return CalendarEvent(
            id = id,
            title = title,
            startDate = LocalDate.parse(startDate),
            endDate = LocalDate.parse(endDate),
            ownerType = mappedOwnerType,
            note = note
        )
    }

    private fun ShiftDto.toDomain(): ShiftSchedule? {
        val shiftTypeValue = shiftType
        val mappedShiftType = ShiftType.entries.firstOrNull { it.name == shiftTypeValue }
            ?: return null
        return ShiftSchedule(
            id = id,
            date = LocalDate.parse(date),
            shiftType = mappedShiftType
        )
    }

    private fun EventOwnerType.toEventPayload(): EventPayload {
        return when (this) {
            EventOwnerType.ME -> EventPayload(
                subjectType = "PERSONAL",
                ownerUserId = CalendarApiConfig.currentUserId
            )
            EventOwnerType.US -> EventPayload(
                subjectType = "SHARED",
                ownerUserId = null
            )
            EventOwnerType.PARTNER -> EventPayload(
                subjectType = "PERSONAL",
                ownerUserId = CalendarApiConfig.partnerUserId
                    ?: throw IOException("파트너 연결이 완료된 뒤 상대 일정으로 저장할 수 있습니다.")
            )
        }
    }

    private fun CreateEventRequest.toUpdateRequest(): UpdateEventRequest {
        return UpdateEventRequest(
            title = title,
            startDate = startDate,
            endDate = endDate,
            subjectType = subjectType,
            ownerUserId = ownerUserId,
            note = note
        )
    }

    private fun String?.toShiftOwnerType(): ShiftOwnerType {
        return ShiftOwnerType.entries.firstOrNull { it.name == this } ?: ShiftOwnerType.ME
    }

    private data class EventPayload(
        val subjectType: String,
        val ownerUserId: String?
    )

    data class CalendarMonthData(
        val events: List<CalendarEvent>,
        val shifts: List<ShiftSchedule>
    )

    private class ErrorParser {
        private val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        private val adapter = moshi.adapter(ApiErrorEnvelope::class.java)

        fun parse(response: Response<*>): String? {
            return try {
                response.errorBody()?.string()?.takeIf { it.isNotBlank() }?.let { raw ->
                    adapter.fromJson(raw)?.error?.message ?: raw
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    companion object {
        private val refreshLock = Any()
        private val refreshBeforeExpiryMillis = TimeUnit.MINUTES.toMillis(1)

        private fun createService(): CalendarApiService {
            val logger = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()
            val authService = createRetrofit(moshi)
                .create(CalendarApiService::class.java)
            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val originalRequest = chain.request()
                    val session = if (originalRequest.isRefreshRequest()) {
                        AuthSessionManager.getSession()
                    } else {
                        refreshSessionIfNeeded(authService)
                    }
                    val requestBuilder: Request.Builder = chain.request().newBuilder()

                    session?.accessToken?.takeIf { it.isNotBlank() }?.let {
                        requestBuilder.header("Authorization", "${session.tokenType} $it")
                    }
                    session?.groupId?.takeIf { it.isNotBlank() }?.let {
                        requestBuilder.header("X-Group-Id", it)
                    }

                    chain.proceed(requestBuilder.build())
                }
                .authenticator(SessionRefreshAuthenticator(authService))
                .addInterceptor(logger)
                .build()

            return createRetrofit(moshi, okHttpClient)
                .create(CalendarApiService::class.java)
        }

        private fun createRetrofit(
            moshi: Moshi,
            okHttpClient: OkHttpClient? = null
        ): Retrofit {
            return Retrofit.Builder()
                .baseUrl(CalendarApiConfig.baseUrl)
                .apply {
                    if (okHttpClient != null) {
                        client(okHttpClient)
                    }
                }
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
        }

        private fun AuthTokenDto.applyTo(session: AuthSession): AuthSession {
            return session.copy(
                accessToken = accessToken,
                refreshToken = refreshToken,
                tokenType = tokenType,
                accessTokenExpiresAtEpochMillis = accessTokenExpiresAtEpochMillis(),
                refreshTokenExpiresAtEpochMillis = refreshTokenExpiresAtEpochMillis()
            )
        }

        private fun AuthTokenDto.accessTokenExpiresAtEpochMillis(): Long {
            return System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(expiresIn.coerceAtLeast(0L))
        }

        private fun AuthTokenDto.refreshTokenExpiresAtEpochMillis(): Long {
            return System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(refreshTokenExpiresIn.coerceAtLeast(0L))
        }

        private fun refreshSessionIfNeeded(authService: CalendarApiService): AuthSession? {
            val session = AuthSessionManager.getSession() ?: return null
            if (!session.shouldRefreshAccessToken()) {
                return session
            }

            synchronized(refreshLock) {
                val latestSession = AuthSessionManager.getSession() ?: return null
                if (!latestSession.shouldRefreshAccessToken()) {
                    return latestSession
                }

                val refreshResponse = runCatching {
                    authService.refreshTokenCall(
                        RefreshTokenRequest(latestSession.refreshToken)
                    ).execute()
                }.getOrNull() ?: return latestSession

                val body = refreshResponse.body()
                val refreshedTokens = if (refreshResponse.isSuccessful && body?.success == true) {
                    body.data
                } else {
                    null
                } ?: return latestSession

                return refreshedTokens.applyTo(latestSession)
                    .also(AuthSessionManager::saveSession)
            }
        }

        private fun AuthSession.shouldRefreshAccessToken(): Boolean {
            val expiresAt = accessTokenExpiresAtEpochMillis
            return expiresAt <= 0L || expiresAt - System.currentTimeMillis() <= refreshBeforeExpiryMillis
        }

        private fun Request.isRefreshRequest(): Boolean {
            return url.encodedPath.endsWith("/api/v1/auth/refresh")
        }
    }

    private class SessionRefreshAuthenticator(
        private val authService: CalendarApiService
    ) : Authenticator {
        override fun authenticate(route: Route?, response: OkHttpResponse): Request? {
            if (responseCount(response) >= 2) {
                return null
            }

            val bearerToken = response.request.header("Authorization")

            synchronized(refreshLock) {
                val latestSession = AuthSessionManager.getSession() ?: return null
                val latestHeader = "${latestSession.tokenType} ${latestSession.accessToken}"

                if (bearerToken != null && bearerToken != latestHeader) {
                    return response.request.newBuilder()
                        .header("Authorization", latestHeader)
                        .build()
                }

                val refreshResponse = runCatching {
                    authService.refreshTokenCall(
                        RefreshTokenRequest(latestSession.refreshToken)
                    ).execute()
                }.getOrNull() ?: return null

                val body = refreshResponse.body()
                val refreshedTokens = if (refreshResponse.isSuccessful && body?.success == true) {
                    body.data
                } else {
                    null
                } ?: run {
                    AuthSessionManager.clearSession()
                    AuthSessionManager.clearPendingInviteToken()
                    return null
                }

                val updatedSession = refreshedTokens.applyTo(latestSession)
                AuthSessionManager.saveSession(updatedSession)
                return response.request.newBuilder()
                    .header("Authorization", "${updatedSession.tokenType} ${updatedSession.accessToken}")
                    .build()
            }
        }

        private fun responseCount(response: OkHttpResponse): Int {
            var current: OkHttpResponse? = response
            var count = 1
            while (current?.priorResponse != null) {
                count += 1
                current = current.priorResponse
            }
            return count
        }
    }
}
