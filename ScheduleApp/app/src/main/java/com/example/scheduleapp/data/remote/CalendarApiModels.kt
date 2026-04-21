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
