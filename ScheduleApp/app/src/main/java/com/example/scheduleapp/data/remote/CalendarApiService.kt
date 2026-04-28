package com.example.scheduleapp.data.remote

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface CalendarApiService {
    @POST("api/v1/auth/kakao/mobile")
    suspend fun authenticateWithKakaoAccessToken(
        @Body request: KakaoMobileLoginRequest
    ): Response<ApiEnvelope<AuthResultResponse>>

    @POST("api/v1/auth/mobile/exchange")
    suspend fun exchangeMobileLogin(
        @Body request: MobileLoginExchangeRequest
    ): Response<ApiEnvelope<AuthResultResponse>>

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): Response<ApiEnvelope<AuthTokenDto>>

    @POST("api/v1/auth/logout")
    suspend fun logout(
        @Body request: LogoutRequest
    ): Response<ApiEnvelope<LogoutResponse>>

    @GET("api/v1/users/me")
    suspend fun getMyProfile(): Response<ApiEnvelope<UserProfileDto>>

    @PATCH("api/v1/users/me/settings")
    suspend fun updateMySettings(
        @Body request: UpdateUserSettingsRequest
    ): Response<ApiEnvelope<UpdateUserSettingsResponse>>

    @GET("api/v1/groups/me")
    suspend fun getMyGroup(): Response<ApiEnvelope<GroupMeResponse>>

    @POST("api/v1/groups/invites")
    suspend fun createInvite(
        @Body request: CreateInviteRequest
    ): Response<ApiEnvelope<CreateInviteResponse>>

    @GET("api/v1/groups/invites/{inviteToken}")
    suspend fun getInvite(
        @Path("inviteToken") inviteToken: String
    ): Response<ApiEnvelope<InviteLookupResponse>>

    @POST("api/v1/groups/invites/accept")
    suspend fun acceptInvite(
        @Body request: AcceptInviteRequest
    ): Response<ApiEnvelope<AcceptInviteResponse>>

    @GET("api/v1/calendar/month")
    suspend fun getCalendarMonth(
        @Query("year") year: Int,
        @Query("month") month: Int,
        @Query("shiftOwnerType") shiftOwnerType: String?,
        @Header("X-Group-Id") groupId: String?
    ): Response<ApiEnvelope<CalendarMonthResponse>>

    @GET("api/v1/calendar/date/{date}")
    suspend fun getCalendarDate(
        @Path("date") date: String,
        @Query("shiftOwnerType") shiftOwnerType: String?,
        @Header("X-Group-Id") groupId: String?
    ): Response<ApiEnvelope<CalendarDateResponse>>

    @POST("api/v1/events")
    suspend fun createEvent(
        @Header("X-Group-Id") groupId: String?,
        @Body request: CreateEventRequest
    ): Response<ApiEnvelope<EventDto>>

    @PATCH("api/v1/events/{eventId}")
    suspend fun updateEvent(
        @Path("eventId") eventId: String,
        @Header("X-Group-Id") groupId: String?,
        @Body request: UpdateEventRequest
    ): Response<ApiEnvelope<EventDto>>

    @DELETE("api/v1/events/{eventId}")
    suspend fun deleteEvent(
        @Path("eventId") eventId: String,
        @Header("X-Group-Id") groupId: String?
    ): Response<ApiEnvelope<Map<String, Any?>>>

    @PUT("api/v1/shifts/{date}")
    suspend fun upsertShift(
        @Path("date") date: String,
        @Header("X-Group-Id") groupId: String?,
        @Body request: UpsertShiftRequest
    ): Response<ApiEnvelope<ShiftDto>>

    @PUT("api/v1/shifts/monthly")
    suspend fun saveMonthlyShifts(
        @Query("year") year: Int,
        @Query("month") month: Int,
        @Header("X-Group-Id") groupId: String?,
        @Body request: MonthlyShiftRequest
    ): Response<ApiEnvelope<CalendarMonthResponse>>

    @DELETE("api/v1/shifts/{date}")
    suspend fun deleteShift(
        @Path("date") date: String,
        @Header("X-Group-Id") groupId: String?
    ): Response<ApiEnvelope<DeleteShiftResponse>>

    @POST("api/v1/auth/refresh")
    fun refreshTokenCall(
        @Body request: RefreshTokenRequest
    ): Call<ApiEnvelope<AuthTokenDto>>
}
