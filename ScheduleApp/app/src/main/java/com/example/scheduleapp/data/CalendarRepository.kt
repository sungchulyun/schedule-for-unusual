package com.example.scheduleapp.data

import com.example.scheduleapp.data.remote.ApiEnvelope
import com.example.scheduleapp.data.remote.ApiErrorEnvelope
import com.example.scheduleapp.data.remote.AuthResultResponse
import com.example.scheduleapp.data.remote.GroupMeResponse
import com.example.scheduleapp.data.remote.KakaoMobileLoginRequest
import com.example.scheduleapp.data.remote.CalendarApiService
import com.example.scheduleapp.data.remote.CalendarMonthResponse
import com.example.scheduleapp.data.remote.CreateEventRequest
import com.example.scheduleapp.data.remote.EventDto
import com.example.scheduleapp.data.remote.MobileLoginExchangeRequest
import com.example.scheduleapp.data.remote.MonthlyShiftItemRequest
import com.example.scheduleapp.data.remote.MonthlyShiftRequest
import com.example.scheduleapp.data.remote.ShiftDto
import com.example.scheduleapp.data.remote.UpdateEventRequest
import com.example.scheduleapp.data.remote.UpsertShiftRequest
import com.example.scheduleapp.ui.calendar.CalendarEvent
import com.example.scheduleapp.ui.calendar.EventOwnerType
import com.example.scheduleapp.ui.calendar.ShiftSchedule
import com.example.scheduleapp.ui.calendar.ShiftType
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.time.LocalDate
import java.time.YearMonth

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

    suspend fun getMonth(month: YearMonth): CalendarMonthData {
        val data = unwrap(service.getCalendarMonth(month.year, month.monthValue, CalendarApiConfig.groupId))
        return data.toDomain()
    }

    suspend fun refreshPartnerUserId() {
        val group = unwrap(service.getMyGroup())
        val partnerUserId = group.members
            .firstOrNull { it.userId != CalendarApiConfig.currentUserId }
            ?.userId
        AuthSessionManager.updatePartnerUserId(partnerUserId)
    }

    suspend fun createEvent(
        ownerType: EventOwnerType,
        startDate: LocalDate,
        endDate: LocalDate,
        title: String,
        note: String?
    ) {
        val payload = ownerType.toEventPayload()
        unwrap(
            service.createEvent(
                groupId = CalendarApiConfig.groupId,
                request = CreateEventRequest(
                    title = title,
                    startDate = startDate.toString(),
                    endDate = endDate.toString(),
                    subjectType = payload.subjectType,
                    ownerUserId = payload.ownerUserId,
                    note = note
                )
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
        val payload = ownerType.toEventPayload()
        unwrap(
            service.updateEvent(
                eventId = eventId,
                groupId = CalendarApiConfig.groupId,
                request = UpdateEventRequest(
                    title = title,
                    startDate = startDate.toString(),
                    endDate = endDate.toString(),
                    subjectType = payload.subjectType,
                    ownerUserId = payload.ownerUserId,
                    note = note
                )
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
            currentUserId = user.id,
            groupId = user.groupId,
            nickname = user.nickname,
            profileImageUrl = user.profileImageUrl
        )
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
                    ?: throw IOException("PARTNER 일정을 저장하려면 API_PARTNER_USER_ID 설정이 필요합니다.")
            )
        }
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
        private fun createService(): CalendarApiService {
            val logger = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()
            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val session = AuthSessionManager.getSession()
                    val requestBuilder: Request.Builder = chain.request().newBuilder()

                    session?.accessToken?.takeIf { it.isNotBlank() }?.let {
                        requestBuilder.header("Authorization", "Bearer $it")
                    }
                    session?.groupId?.takeIf { it.isNotBlank() }?.let {
                        requestBuilder.header("X-Group-Id", it)
                    }

                    chain.proceed(requestBuilder.build())
                }
                .addInterceptor(logger)
                .build()

            return Retrofit.Builder()
                .baseUrl(CalendarApiConfig.baseUrl)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(CalendarApiService::class.java)
        }
    }
}
