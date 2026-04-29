package com.example.scheduleapp.ui.calendar

import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class CalendarFilters(
    val showMe: Boolean = true,
    val showUs: Boolean = true,
    val showPartner: Boolean = true,
    val showShift: Boolean = true
) {
    fun isVisible(ownerType: EventOwnerType): Boolean {
        return when (ownerType) {
            EventOwnerType.ME -> showMe
            EventOwnerType.US -> showUs
            EventOwnerType.PARTNER -> showPartner
        }
    }
}

data class CalendarEvent(
    val id: String,
    val title: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val ownerType: EventOwnerType,
    val note: String?
) {
    fun spans(date: LocalDate): Boolean = !date.isBefore(startDate) && !date.isAfter(endDate)

    fun isMultiDay(): Boolean = startDate != endDate

    fun displayDateText(): String {
        val start = "${startDate.monthValue}월 ${startDate.dayOfMonth}일 ${startTime.displayText()}"
        val end = "${endDate.monthValue}월 ${endDate.dayOfMonth}일 ${endTime.displayText()}"
        return "$start - $end"
    }
}

private val EventTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun LocalTime.displayText(): String = format(EventTimeFormatter)

enum class EventOwnerType(val label: String, val color: Color) {
    ME("내 일정", Color(0xFF326BFF)),
    US("우리 일정", Color(0xFFE85D75)),
    PARTNER("상대 일정", Color(0xFF00A67E))
}

data class ShiftSchedule(
    val id: String,
    val date: LocalDate,
    val shiftType: ShiftType
)

enum class ShiftType(val label: String, val color: Color) {
    DAY("Day", Color(0xFFFFA23A)),
    NIGHT("Night", Color(0xFF5B6CFF)),
    MID("Mid", Color(0xFF7A52CC)),
    EVENING("Evening", Color(0xFF009688)),
    OFF("Off", Color(0xFF6B7280)),
    VACATION("휴가", Color(0xFFE25555))
}

enum class ShiftOwnerType(val label: String) {
    ME("내 근무"),
    PARTNER("상대 근무");

    fun availableOrFallback(hasPartnerConnected: Boolean): ShiftOwnerType {
        return if (this == PARTNER && !hasPartnerConnected) ME else this
    }
}

fun ShiftType.calendarBadgeLabel(): String {
    return when (this) {
        ShiftType.VACATION -> label
        else -> label.first().toString()
    }
}

fun ShiftType.widgetShortLabel(): String {
    return when (this) {
        ShiftType.DAY -> "D"
        ShiftType.NIGHT -> "N"
        ShiftType.MID -> "M"
        ShiftType.EVENING -> "E"
        ShiftType.OFF -> "O"
        ShiftType.VACATION -> "V"
    }
}
