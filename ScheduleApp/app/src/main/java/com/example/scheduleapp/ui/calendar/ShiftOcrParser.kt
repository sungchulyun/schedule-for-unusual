package com.example.scheduleapp.ui.calendar

import java.time.LocalDate
import java.time.YearMonth

data class ShiftOcrParseResult(
    val items: Map<LocalDate, ShiftType>,
    val recognizedCount: Int,
    val issues: List<String>
)

fun parseShiftOcrText(
    text: String,
    month: YearMonth
): ShiftOcrParseResult {
    val codes = text
        .asSequence()
        .mapNotNull { it.toShiftTypeOrNull() }
        .toList()
    val daysInMonth = month.lengthOfMonth()
    val items = codes
        .take(daysInMonth)
        .mapIndexed { index, shiftType -> month.atDay(index + 1) to shiftType }
        .toMap()
    val issues = buildList {
        if (codes.size < daysInMonth) {
            add("${daysInMonth - codes.size}일치 코드가 누락되었습니다.")
        }
        if (codes.size > daysInMonth) {
            add("${codes.size - daysInMonth}개 코드가 월 일수보다 많아 제외되었습니다.")
        }
    }

    return ShiftOcrParseResult(
        items = items,
        recognizedCount = codes.size.coerceAtMost(daysInMonth),
        issues = issues
    )
}

private fun Char.toShiftTypeOrNull(): ShiftType? {
    return when (this) {
        '/' -> ShiftType.OFF
        'D', 'd' -> ShiftType.DAY
        'E', 'e' -> ShiftType.EVENING
        'N', 'n' -> ShiftType.NIGHT
        'M', 'm' -> ShiftType.MID
        else -> null
    }
}
