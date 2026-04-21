package com.example.scheduleapp.ui.calendar

import java.time.LocalDate
import java.time.YearMonth

val WeekdayLabels = listOf("일", "월", "화", "수", "목", "금", "토")

fun buildCalendarDays(month: YearMonth): List<LocalDate?> {
    val firstDay = month.atDay(1)
    val startOffset = firstDay.dayOfWeek.value % 7
    val dates = MutableList<LocalDate?>(startOffset) { null }

    repeat(month.lengthOfMonth()) { index ->
        dates += month.atDay(index + 1)
    }

    while (dates.size % 7 != 0) {
        dates += null
    }

    return dates
}

fun buildEventsByDate(events: List<CalendarEvent>): Map<LocalDate, List<CalendarEvent>> {
    return events
        .flatMap { event ->
            generateSequence(event.startDate) { current ->
                current.plusDays(1).takeIf { !it.isAfter(event.endDate) }
            }.map { date -> date to event }.toList()
        }
        .groupBy(keySelector = { it.first }, valueTransform = { it.second })
}
