package com.example.scheduleapp

import com.example.scheduleapp.ui.calendar.CalendarEvent
import com.example.scheduleapp.ui.calendar.EventOwnerType
import com.example.scheduleapp.ui.calendar.ShiftOwnerType
import com.example.scheduleapp.ui.calendar.buildCalendarDays
import com.example.scheduleapp.ui.calendar.buildEventsByDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
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
        val events = listOf(
            CalendarEvent("evt_01", "데이트", date, date, EventOwnerType.US, null),
            CalendarEvent("evt_02", "야근", date, date, EventOwnerType.ME, "확정 전")
        )

        val eventsByDate = buildEventsByDate(events)

        assertEquals(2, eventsByDate[date]?.size)
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
}
