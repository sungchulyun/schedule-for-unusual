package com.example.scheduleapp

import com.example.scheduleapp.ui.calendar.CalendarEvent
import com.example.scheduleapp.ui.calendar.EventOwnerType
import com.example.scheduleapp.ui.calendar.ShiftOwnerType
import com.example.scheduleapp.ui.calendar.ShiftType
import com.example.scheduleapp.ui.calendar.buildCalendarDays
import com.example.scheduleapp.ui.calendar.buildEventsByDate
import com.example.scheduleapp.ui.calendar.parseShiftOcrText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

class CalendarDataTest {

    @Test
    fun `calendar days adds leading blanks for sunday-first grid`() {
        val days = buildCalendarDays(YearMonth.of(2026, 4))

        assertEquals(35, days.size)
        assertNull(days[0])
        assertNull(days[1])
        assertNull(days[2])
        assertEquals(1, days[3]?.dayOfMonth)
    }

    @Test
    fun `calendar days pads trailing blanks to full weeks`() {
        val days = buildCalendarDays(YearMonth.of(2026, 4))

        assertEquals(30, days.filterNotNull().size)
        assertEquals(30, days[32]?.dayOfMonth)
        assertNull(days.last())
    }

    @Test
    fun `build events by date expands multi day event`() {
        val event = CalendarEvent(
            id = "evt_01",
            title = "출장",
            startDate = LocalDate.of(2026, 4, 18),
            endDate = LocalDate.of(2026, 4, 20),
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(18, 0),
            ownerType = EventOwnerType.ME,
            note = null
        )

        val eventsByDate = buildEventsByDate(listOf(event))

        assertEquals(3, eventsByDate.size)
        assertTrue(eventsByDate.keys.contains(LocalDate.of(2026, 4, 18)))
        assertTrue(eventsByDate.keys.contains(LocalDate.of(2026, 4, 19)))
        assertTrue(eventsByDate.keys.contains(LocalDate.of(2026, 4, 20)))
    }

    @Test
    fun `build events by date keeps multiple events on same day`() {
        val date = LocalDate.of(2026, 4, 18)
        val startTime = LocalTime.of(9, 0)
        val endTime = LocalTime.of(10, 0)
        val events = listOf(
            CalendarEvent("evt_01", "데이트", date, date, startTime, endTime, EventOwnerType.US, null),
            CalendarEvent("evt_02", "야근", date, date, startTime, endTime, EventOwnerType.ME, "확정 전")
        )

        val eventsByDate = buildEventsByDate(events)

        assertEquals(2, eventsByDate[date]?.size)
    }

    @Test
    fun `event display text includes start and end date times`() {
        val date = LocalDate.of(2026, 4, 18)
        val event = CalendarEvent(
            id = "evt_01",
            title = "데이트",
            startDate = date,
            endDate = date,
            startTime = LocalTime.of(19, 0),
            endTime = LocalTime.of(21, 0),
            ownerType = EventOwnerType.US,
            note = null
        )

        assertEquals("4월 18일 19:00 - 4월 18일 21:00", event.displayDateText())
    }

    @Test
    fun `partner shift owner falls back to me when partner is not connected`() {
        assertEquals(
            ShiftOwnerType.ME,
            ShiftOwnerType.PARTNER.availableOrFallback(hasPartnerConnected = false)
        )
        assertEquals(
            ShiftOwnerType.PARTNER,
            ShiftOwnerType.PARTNER.availableOrFallback(hasPartnerConnected = true)
        )
    }

    @Test
    fun `parse shift ocr text maps supported schedule codes by date`() {
        val result = parseShiftOcrText("1 / D e n M x", YearMonth.of(2026, 4))

        assertEquals(5, result.recognizedCount)
        assertEquals(ShiftType.OFF, result.items[LocalDate.of(2026, 4, 1)])
        assertEquals(ShiftType.DAY, result.items[LocalDate.of(2026, 4, 2)])
        assertEquals(ShiftType.EVENING, result.items[LocalDate.of(2026, 4, 3)])
        assertEquals(ShiftType.NIGHT, result.items[LocalDate.of(2026, 4, 4)])
        assertEquals(ShiftType.MID, result.items[LocalDate.of(2026, 4, 5)])
        assertTrue(result.issues.first().contains("누락"))
    }

    @Test
    fun `parse shift ocr text trims extra codes beyond month length`() {
        val result = parseShiftOcrText("/".repeat(35), YearMonth.of(2026, 4))

        assertEquals(30, result.recognizedCount)
        assertEquals(30, result.items.size)
        assertTrue(result.issues.first().contains("많아"))
    }
}
