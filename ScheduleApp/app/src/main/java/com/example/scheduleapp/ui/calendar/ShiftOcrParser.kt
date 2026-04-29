package com.example.scheduleapp.ui.calendar

import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs

data class ShiftOcrParseResult(
    val items: Map<LocalDate, ShiftType>,
    val recognizedCount: Int,
    val issues: List<String>
)

fun parseShiftOcrText(
    text: String,
    month: YearMonth
): ShiftOcrParseResult {
    val daysInMonth = month.lengthOfMonth()
    val codes = text.extractScheduleCodes(daysInMonth)
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

private const val ScheduleCellStartIndex = 2
private val CellSeparator = Regex("\\s+")

private data class ShiftOcrRowCandidate(
    val codes: List<ShiftType>
)

private fun String.extractScheduleCodes(daysInMonth: Int): List<ShiftType> {
    val rowCandidates = lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .mapNotNull { line ->
            val cells = line.split(CellSeparator)
            if (cells.size <= ScheduleCellStartIndex) {
                null
            } else {
                val codes = cells
                    .drop(ScheduleCellStartIndex)
                    .flatMap { cell -> cell.asShiftTypes() }
                if (codes.isEmpty()) null else ShiftOcrRowCandidate(codes)
            }
        }
        .toList()

    return rowCandidates
        .minWithOrNull(
            compareBy<ShiftOcrRowCandidate> { abs(it.codes.size - daysInMonth) }
                .thenByDescending { it.codes.size }
        )
        ?.codes
        ?: asShiftTypes()
}

private fun String.asShiftTypes(): List<ShiftType> {
    return asSequence()
        .mapNotNull { it.toShiftTypeOrNull() }
        .toList()
}

private fun Char.toShiftTypeOrNull(): ShiftType? {
    if (this == '/') return ShiftType.OFF

    val normalized = uppercaseChar().toString()
    return ShiftType.entries.firstOrNull { it.widgetShortLabel() == normalized }
}
