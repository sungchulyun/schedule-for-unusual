package com.example.scheduleapp.data.remote

data class ApiEnvelope<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ApiErrorBody? = null
)

data class ApiErrorEnvelope(
    val success: Boolean,
    val error: ApiErrorBody? = null
)

data class ApiErrorBody(
    val code: String? = null,
    val message: String? = null
)

data class AuthResultResponse(
    val user: UserProfileDto,
    val tokens: AuthTokenDto,
    val isNewUser: Boolean
)

data class UserProfileDto(
    val id: String,
    val oauthProvider: String,
    val oauthProviderUserId: String,
    val nickname: String,
    val profileImageUrl: String? = null,
    val groupId: String,
    val defaultShiftOwnerType: String? = null,
    val createdAt: String,
    val updatedAt: String
)

data class AuthTokenDto(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Long,
    val refreshTokenExpiresIn: Long
)

data class CalendarMonthResponse(
    val year: Int,
    val month: Int,
    val events: List<EventDto> = emptyList(),
    val shifts: List<ShiftDto> = emptyList()
)

data class CalendarDateResponse(
    val date: String,
    val events: List<EventDto> = emptyList(),
    val shift: ShiftDto? = null
)

data class EventDto(
    val id: String,
    val groupId: String? = null,
    val title: String,
    val startDate: String,
    val endDate: String,
    val subjectType: String? = null,
    val ownerUserId: String? = null,
    val ownerType: String,
    val note: String? = null
)

data class ShiftDto(
    val id: String,
    val groupId: String? = null,
    val date: String,
    val ownerType: String? = null,
    val shiftType: String
)

data class CreateEventRequest(
    val title: String,
    val startDate: String,
    val endDate: String,
    val subjectType: String,
    val ownerUserId: String?,
    val note: String?
)

data class UpdateEventRequest(
    val title: String,
    val startDate: String,
    val endDate: String,
    val subjectType: String,
    val ownerUserId: String?,
    val note: String?
)

data class UpsertShiftRequest(
    val shiftType: String
)

data class MonthlyShiftItemRequest(
    val date: String,
    val shiftType: String
)

data class MonthlyShiftRequest(
    val items: List<MonthlyShiftItemRequest>
)

data class DeleteShiftResponse(
    val date: String,
    val deleted: Boolean,
    val deletedAt: String? = null
)

data class GroupMeResponse(
    val groupId: String,
    val members: List<GroupMemberDto> = emptyList()
)

data class GroupMemberDto(
    val userId: String,
    val role: String,
    val partnerStatus: String
)

data class GroupPermissionsDto(
    val canReadAllEvents: Boolean,
    val canEditAllEvents: Boolean,
    val canEditAllShifts: Boolean = false,
    val canEditOwnShifts: Boolean = false,
    val canReadAllShifts: Boolean = false
)

data class CreateInviteRequest(
    val channel: String
)

data class CreateInviteResponse(
    val inviteId: String,
    val groupId: String,
    val inviteToken: String,
    val shareUrl: String,
    val deepLink: String,
    val status: String,
    val expiresAt: String
)

data class InviteLookupResponse(
    val inviteId: String,
    val groupId: String,
    val inviter: InviteInviterDto,
    val status: String,
    val requiresAuth: Boolean,
    val expiresAt: String
)

data class InviteInviterDto(
    val userId: String,
    val nickname: String
)

data class AcceptInviteRequest(
    val inviteToken: String
)

data class AcceptInviteResponse(
    val groupId: String,
    val inviteId: String,
    val accepted: Boolean,
    val members: List<GroupMemberDto>,
    val permissions: GroupPermissionsDto,
    val tokens: AuthTokenDto? = null
)

data class RefreshTokenRequest(
    val refreshToken: String
)

data class LogoutRequest(
    val refreshToken: String
)

data class LogoutResponse(
    val loggedOut: Boolean
)

data class UpdateUserSettingsRequest(
    val defaultShiftOwnerType: String
)

data class UpdateUserSettingsResponse(
    val defaultShiftOwnerType: String
)

data class MobileLoginExchangeRequest(
    val loginCode: String
)

data class KakaoMobileLoginRequest(
    val accessToken: String
)
