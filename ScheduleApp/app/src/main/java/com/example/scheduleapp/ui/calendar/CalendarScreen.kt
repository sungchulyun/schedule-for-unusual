package com.example.scheduleapp.ui.calendar

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

private const val CalendarCellAspectRatio = 0.52f
private val CalendarGridLineAlpha = 0.18f
private const val CalendarDayCellVisibleEventLimit = 3
private val CalendarDayHeaderHeight = 20.dp
private val CalendarEventSlotHeight = 15.dp
private val CalendarEventSlotGap = 1.dp
private val CalendarEventSlotsTopInset = 25.dp

private enum class CalendarScreenMode {
    MONTH,
    DAY_DETAIL,
    ENTRY_HOME,
    EVENT_ENTRY,
    BULK_SHIFT
}

private data class WeekEventSegment(
    val event: CalendarEvent,
    val startColumn: Int,
    val endColumn: Int,
    val continuesFromPreviousWeek: Boolean,
    val continuesToNextWeek: Boolean
)

private data class WeekEventBarLayout(
    val segment: WeekEventSegment,
    val slot: Int
)

private data class CalendarEventBuckets(
    val allByDate: Map<LocalDate, List<CalendarEvent>>,
    val singleDayByDate: Map<LocalDate, List<CalendarEvent>>,
    val multiDayByDate: Map<LocalDate, List<CalendarEvent>>
)

private data class BulkShiftOcrUiState(
    val isRecognizing: Boolean = false,
    val statusMessage: String? = null,
    val issues: List<String> = emptyList()
)

@Composable
fun CalendarScreen(
    requestedOpenDate: LocalDate? = null,
    onRequestedOpenDateConsumed: () -> Unit = {},
    hasPartnerConnected: Boolean = false,
    defaultShiftOwnerType: ShiftOwnerType = ShiftOwnerType.ME,
    onSessionChanged: () -> Unit = {},
    showPartnerInviteAction: Boolean = false,
    onInvitePartner: () -> Unit = {},
    onCalendarDataChanged: () -> Unit = {},
    onLogout: () -> Unit = {},
    calendarViewModel: CalendarViewModel = viewModel()
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var filters by remember { mutableStateOf(CalendarFilters()) }
    var screenMode by remember { mutableStateOf(CalendarScreenMode.MONTH) }
    var selectedShiftOwnerType by remember(defaultShiftOwnerType, hasPartnerConnected) {
        mutableStateOf(defaultShiftOwnerType.availableOrFallback(hasPartnerConnected))
    }

    val remoteState = calendarViewModel.uiState
    val eventEntries = remoteState.events
    val shiftEntries = remoteState.shifts

    LaunchedEffect(currentMonth, selectedShiftOwnerType) {
        calendarViewModel.loadMonth(currentMonth, selectedShiftOwnerType)
    }

    LaunchedEffect(requestedOpenDate) {
        val targetDate = requestedOpenDate ?: return@LaunchedEffect
        selectedDate = targetDate
        currentMonth = YearMonth.from(targetDate)
        screenMode = CalendarScreenMode.DAY_DETAIL
        onRequestedOpenDateConsumed()
    }

    val eventsByDate = remember(eventEntries) { buildEventsByDate(eventEntries) }
    val shiftsByDate = remember(shiftEntries) { shiftEntries.associateBy { it.date } }
    val filteredEventBuckets = remember(eventsByDate, filters) {
        buildCalendarEventBuckets(eventsByDate, filters)
    }
    val visibleShiftsByDate = remember(shiftsByDate, filters.showShift) {
        if (filters.showShift) shiftsByDate else emptyMap()
    }
    val selectedDateEvents = remember(selectedDate, filteredEventBuckets) {
        filteredEventBuckets.allByDate[selectedDate].orEmpty()
    }
    val selectedDateShift = remember(selectedDate, shiftsByDate) {
        shiftsByDate[selectedDate]
    }

    when (screenMode) {
        CalendarScreenMode.MONTH -> {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CalendarHeader(
                        currentMonth = currentMonth,
                        onPreviousMonth = { currentMonth = currentMonth.minusMonths(1) },
                        onNextMonth = { currentMonth = currentMonth.plusMonths(1) },
                        onLogout = onLogout
                    )

                    SyncStatusSection(
                        isLoading = remoteState.isLoading,
                        isSubmitting = remoteState.isSubmitting,
                        errorMessage = remoteState.errorMessage,
                        onDismissError = calendarViewModel::clearError
                    )

                    if (showPartnerInviteAction) {
                        PartnerInviteSection(onInvitePartner = onInvitePartner)
                    }

                    FilterSection(
                        filters = filters,
                        onFiltersChanged = { filters = it },
                        shiftOwnerType = selectedShiftOwnerType,
                        savedDefaultShiftOwnerType = defaultShiftOwnerType.availableOrFallback(hasPartnerConnected),
                        hasPartnerConnected = hasPartnerConnected,
                        isSavingDefault = remoteState.isSubmitting,
                        onShiftOwnerTypeChanged = { selectedShiftOwnerType = it },
                        onSaveDefaultShiftOwnerType = {
                            calendarViewModel.updateDefaultShiftOwnerType(selectedShiftOwnerType) {
                                onSessionChanged()
                            }
                        }
                    )

                    MonthCalendar(
                        modifier = Modifier.weight(1f),
                        month = currentMonth,
                        selectedDate = selectedDate,
                        allEventsByDate = filteredEventBuckets.allByDate,
                        shiftsByDate = visibleShiftsByDate,
                        onDateSelected = {
                            selectedDate = it
                            screenMode = CalendarScreenMode.DAY_DETAIL
                        }
                    )

                    EntryShortcutBar(
                        onOpenEntryHome = { screenMode = CalendarScreenMode.ENTRY_HOME }
                    )
                }
            }
        }

        CalendarScreenMode.DAY_DETAIL -> {
            DayDetailScreen(
                selectedDate = selectedDate,
                events = selectedDateEvents,
                shift = selectedDateShift,
                filters = filters,
                shiftOwnerType = selectedShiftOwnerType,
                isLoading = remoteState.isLoading,
                isSubmitting = remoteState.isSubmitting,
                errorMessage = remoteState.errorMessage,
                onDismissError = calendarViewModel::clearError,
                onBack = { screenMode = CalendarScreenMode.MONTH },
                onSaveEvent = { eventId, ownerType, startDate, endDate, startTime, endTime, title, note ->
                    val monthToRefresh = YearMonth.from(startDate)
                    val onSuccess = {
                        selectedDate = startDate
                        currentMonth = monthToRefresh
                        screenMode = CalendarScreenMode.MONTH
                        onCalendarDataChanged()
                    }
                    if (eventId == null) {
                        calendarViewModel.createEvent(
                            ownerType = ownerType,
                            startDate = startDate,
                            endDate = endDate,
                            startTime = startTime,
                            endTime = endTime,
                            title = title,
                            note = note,
                            monthToRefresh = monthToRefresh,
                            onSuccess = onSuccess
                        )
                    } else {
                        calendarViewModel.updateEvent(
                            eventId = eventId,
                            ownerType = ownerType,
                            startDate = startDate,
                            endDate = endDate,
                            startTime = startTime,
                            endTime = endTime,
                            title = title,
                            note = note,
                            monthToRefresh = monthToRefresh,
                            onSuccess = onSuccess
                        )
                    }
                },
                onDeleteEvent = { eventId ->
                    calendarViewModel.deleteEvent(
                        eventId = eventId,
                        monthToRefresh = currentMonth,
                        onSuccess = {
                            screenMode = CalendarScreenMode.MONTH
                            onCalendarDataChanged()
                        }
                    )
                },
                onSaveShift = { shiftType ->
                    calendarViewModel.upsertShift(
                        date = selectedDate,
                        shiftType = shiftType,
                        monthToRefresh = currentMonth,
                        onSuccess = onCalendarDataChanged
                    )
                },
                onDeleteShift = {
                    calendarViewModel.deleteShift(
                        date = selectedDate,
                        monthToRefresh = currentMonth,
                        onSuccess = onCalendarDataChanged
                    )
                }
            )
        }

        CalendarScreenMode.ENTRY_HOME -> {
            EntryHomeScreen(
                currentMonth = currentMonth,
                onBack = { screenMode = CalendarScreenMode.MONTH },
                onOpenEventEntry = { screenMode = CalendarScreenMode.EVENT_ENTRY },
                onOpenBulkShift = { screenMode = CalendarScreenMode.BULK_SHIFT }
            )
        }

        CalendarScreenMode.EVENT_ENTRY -> {
            EventEntryScreen(
                initialDate = selectedDate,
                isLoading = remoteState.isLoading,
                isSubmitting = remoteState.isSubmitting,
                errorMessage = remoteState.errorMessage,
                onDismissError = calendarViewModel::clearError,
                onBack = { screenMode = CalendarScreenMode.ENTRY_HOME },
                onSaveEvent = { ownerType, startDate, endDate, startTime, endTime, title, note ->
                    val targetMonth = YearMonth.from(startDate)
                    calendarViewModel.createEvent(
                        ownerType = ownerType,
                        startDate = startDate,
                        endDate = endDate,
                        startTime = startTime,
                        endTime = endTime,
                        title = title,
                        note = note,
                        monthToRefresh = targetMonth,
                        onSuccess = {
                            selectedDate = startDate
                            currentMonth = targetMonth
                            screenMode = CalendarScreenMode.MONTH
                            onCalendarDataChanged()
                        }
                    )
                }
            )
        }

        CalendarScreenMode.BULK_SHIFT -> {
            BulkShiftRegistrationScreen(
                month = currentMonth,
                existingShifts = shiftEntries,
                isLoading = remoteState.isLoading,
                isSubmitting = remoteState.isSubmitting,
                errorMessage = remoteState.errorMessage,
                onDismissError = calendarViewModel::clearError,
                onBack = { screenMode = CalendarScreenMode.ENTRY_HOME },
                onSaveBulkShifts = { updatedMonthShifts ->
                    calendarViewModel.saveMonthlyShifts(
                        month = currentMonth,
                        updatedMonthShifts = updatedMonthShifts,
                        onSuccess = {
                            screenMode = CalendarScreenMode.MONTH
                            onCalendarDataChanged()
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun PartnerInviteSection(
    onInvitePartner: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "파트너 연결이 아직 없습니다.",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "카카오톡으로 초대 링크를 보내 연결할 수 있습니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(onClick = onInvitePartner) {
                Text("파트너 초대")
            }
        }
    }
}

@Composable
private fun CalendarHeader(
    currentMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onLogout: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Text(text = "<", style = MaterialTheme.typography.titleLarge)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${currentMonth.year}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${currentMonth.monthValue}월",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            OutlinedButton(onClick = onLogout) {
                Text("로그아웃")
            }
            IconButton(onClick = onNextMonth) {
                Text(text = ">", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
private fun FilterSection(
    filters: CalendarFilters,
    onFiltersChanged: (CalendarFilters) -> Unit,
    shiftOwnerType: ShiftOwnerType,
    savedDefaultShiftOwnerType: ShiftOwnerType,
    hasPartnerConnected: Boolean,
    isSavingDefault: Boolean,
    onShiftOwnerTypeChanged: (ShiftOwnerType) -> Unit,
    onSaveDefaultShiftOwnerType: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "표시 필터",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filters.showMe,
                    onClick = { onFiltersChanged(filters.copy(showMe = !filters.showMe)) },
                    label = { Text("내 일정") }
                )
                FilterChip(
                    selected = filters.showUs,
                    onClick = { onFiltersChanged(filters.copy(showUs = !filters.showUs)) },
                    label = { Text("우리 일정") }
                )
                FilterChip(
                    selected = filters.showPartner,
                    onClick = { onFiltersChanged(filters.copy(showPartner = !filters.showPartner)) },
                    label = { Text("상대 일정") }
                )
                FilterChip(
                    selected = filters.showShift,
                    onClick = { onFiltersChanged(filters.copy(showShift = !filters.showShift)) },
                    label = { Text("근무 스케줄") }
                )
            }
        }

        if (filters.showShift) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "현재 표시 중인 근무: ${shiftOwnerType.label}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = shiftOwnerType == ShiftOwnerType.ME,
                            onClick = { onShiftOwnerTypeChanged(ShiftOwnerType.ME) },
                            label = { Text("내 근무") }
                        )
                        if (hasPartnerConnected) {
                            FilterChip(
                                selected = shiftOwnerType == ShiftOwnerType.PARTNER,
                                onClick = { onShiftOwnerTypeChanged(ShiftOwnerType.PARTNER) },
                                label = { Text("상대 근무") }
                            )
                        }
                    }
                    if (shiftOwnerType != savedDefaultShiftOwnerType) {
                        OutlinedButton(
                            onClick = onSaveDefaultShiftOwnerType,
                            enabled = !isSavingDefault
                        ) {
                            Text("현재 선택을 기본값으로 저장")
                        }
                    } else {
                        Text(
                            text = "기본 표시 대상: ${savedDefaultShiftOwnerType.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncStatusSection(
    isLoading: Boolean,
    isSubmitting: Boolean,
    errorMessage: String?,
    onDismissError: () -> Unit
) {
    if (!isLoading && !isSubmitting && errorMessage == null) {
        return
    }

    val message = when {
        errorMessage != null -> errorMessage
        isSubmitting -> "서버에 변경사항을 저장하는 중입니다."
        else -> "서버에서 일정을 불러오는 중입니다."
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (errorMessage == null) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.82f)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = if (errorMessage == null) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                }
            )
            if (errorMessage != null) {
                OutlinedButton(onClick = onDismissError) {
                    Text("닫기")
                }
            }
        }
    }
}

@Composable
private fun MonthCalendar(
    modifier: Modifier = Modifier,
    month: YearMonth,
    selectedDate: LocalDate,
    allEventsByDate: Map<LocalDate, List<CalendarEvent>>,
    shiftsByDate: Map<LocalDate, ShiftSchedule>,
    onDateSelected: (LocalDate) -> Unit
) {
    val days = remember(month) { buildCalendarDays(month) }
    val weeks = remember(days) { days.chunked(7) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                WeekdayLabels.forEach { label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                weeks.forEachIndexed { weekIndex, week ->
                    WeekCalendarRow(
                        modifier = Modifier.weight(1f),
                        week = week,
                        weekIndex = weekIndex,
                        month = month,
                        selectedDate = selectedDate,
                        allEventsByDate = allEventsByDate,
                        shiftsByDate = shiftsByDate,
                        onDateSelected = onDateSelected
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekCalendarRow(
    modifier: Modifier = Modifier,
    week: List<LocalDate?>,
    weekIndex: Int,
    month: YearMonth,
    selectedDate: LocalDate,
    allEventsByDate: Map<LocalDate, List<CalendarEvent>>,
    shiftsByDate: Map<LocalDate, ShiftSchedule>,
    onDateSelected: (LocalDate) -> Unit
) {
    val barLayouts = remember(week, allEventsByDate) {
        buildWeekEventBarLayouts(week, allEventsByDate)
    }
    val occupiedSlotsByColumn = remember(barLayouts) {
        buildMap<Int, Set<Int>> {
            repeat(7) { column ->
                put(
                    column,
                    barLayouts
                        .filter { column in it.segment.startColumn..it.segment.endColumn }
                        .map { it.slot }
                        .toSet()
                )
            }
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            week.forEachIndexed { columnIndex, date ->
                if (date == null) {
                    EmptyCalendarCell(
                        modifier = Modifier.weight(1f),
                        isCurrentMonth = false,
                        showTopDivider = weekIndex > 0,
                        showRightDivider = columnIndex < week.lastIndex
                    )
                } else {
                    val dayEvents = allEventsByDate[date].orEmpty()
                    val singleDayEvents = dayEvents.filterNot { it.isMultiDay() }
                    val inlineEventSlots = buildInlineEventSlots(
                        events = singleDayEvents,
                        occupiedSlots = occupiedSlotsByColumn[columnIndex].orEmpty()
                    )
                    CalendarDayCell(
                        modifier = Modifier.weight(1f),
                        showTopDivider = weekIndex > 0,
                        showRightDivider = columnIndex < week.lastIndex,
                        date = date,
                        isCurrentMonth = date.month == month.month,
                        isSelected = date == selectedDate,
                        inlineEventSlots = inlineEventSlots,
                        occupiedSlots = occupiedSlotsByColumn[columnIndex].orEmpty(),
                        totalEventCount = dayEvents.size,
                        visibleShift = shiftsByDate[date],
                        onClick = { onDateSelected(date) }
                    )
                }
            }
        }

        barLayouts.forEach { layout ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = CalendarEventSlotsTopInset +
                            (CalendarEventSlotHeight + CalendarEventSlotGap) * layout.slot
                    )
                    .height(CalendarEventSlotHeight)
            ) {
                WeekEventBar(segment = layout.segment)
            }
        }
    }
}

@Composable
private fun WeekEventBar(segment: WeekEventSegment) {
    val span = (segment.endColumn - segment.startColumn + 1).coerceAtLeast(1)
    val shape = RoundedCornerShape(
        topStart = if (segment.continuesFromPreviousWeek) 3.dp else 10.dp,
        bottomStart = if (segment.continuesFromPreviousWeek) 3.dp else 10.dp,
        topEnd = if (segment.continuesToNextWeek) 3.dp else 10.dp,
        bottomEnd = if (segment.continuesToNextWeek) 3.dp else 10.dp
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        repeat(segment.startColumn) {
            Spacer(modifier = Modifier.weight(1f))
        }

        Surface(
            modifier = Modifier
                .weight(span.toFloat())
                .fillMaxHeight(),
            shape = shape,
            color = segment.event.ownerType.color.copy(alpha = 0.22f),
            tonalElevation = 0.dp,
            border = BorderStroke(1.dp, segment.event.ownerType.color.copy(alpha = 0.45f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = segment.event.title.compactCalendarLabel(
                        maxVisibleChars = when {
                            span >= 3 -> 6
                            span == 2 -> 4
                            else -> 2
                        }
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    softWrap = false
                )
            }
        }

        repeat(6 - segment.endColumn) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun DayDetailScreen(
    selectedDate: LocalDate,
    events: List<CalendarEvent>,
    shift: ShiftSchedule?,
    filters: CalendarFilters,
    shiftOwnerType: ShiftOwnerType,
    isLoading: Boolean,
    isSubmitting: Boolean,
    errorMessage: String?,
    onDismissError: () -> Unit,
    onBack: () -> Unit,
    onSaveEvent: (String?, EventOwnerType, LocalDate, LocalDate, LocalTime, LocalTime, String, String?) -> Unit,
    onDeleteEvent: (String) -> Unit,
    onSaveShift: (ShiftType) -> Unit,
    onDeleteShift: () -> Unit
) {
    var selectedShiftType by remember(selectedDate, shift) { mutableStateOf(shift?.shiftType) }
    var editingEventId by remember(selectedDate, events) { mutableStateOf<String?>(null) }
    val editingEvent = remember(editingEventId, events) {
        events.firstOrNull { it.id == editingEventId }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Text(text = "<", style = MaterialTheme.typography.titleLarge)
                }
                Column {
                    Text(
                        text = "${selectedDate.monthValue}월 ${selectedDate.dayOfMonth}일",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "상세 및 스케줄 등록",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SyncStatusSection(
                isLoading = isLoading,
                isSubmitting = isSubmitting,
                errorMessage = errorMessage,
                onDismissError = onDismissError
            )

            SelectedDateSummary(
                selectedDate = selectedDate,
                events = events,
                shift = shift,
                filters = filters,
                shiftOwnerType = shiftOwnerType,
                editingEventId = editingEventId,
                onAddEvent = { editingEventId = NEW_EVENT_ID },
                onEditEvent = { editingEventId = it.id }
            )

            EventEditorSection(
                selectedDate = selectedDate,
                initialEvent = editingEvent,
                isCreating = editingEventId == NEW_EVENT_ID,
                onCancel = { editingEventId = null },
                onSave = { ownerType, startDate, endDate, startTime, endTime, title, note ->
                    onSaveEvent(
                        editingEvent?.id,
                        ownerType,
                        startDate,
                        endDate,
                        startTime,
                        endTime,
                        title,
                        note
                    )
                    editingEventId = null
                },
                onDelete = {
                    editingEvent?.let {
                        onDeleteEvent(it.id)
                        editingEventId = null
                    }
                }
            )

            ShiftRegistrationSection(
                selectedDate = selectedDate,
                hasExistingShift = shift != null,
                shiftOwnerType = shiftOwnerType,
                selectedShiftType = selectedShiftType,
                onShiftTypeSelected = { selectedShiftType = it },
                onSaveShift = {
                    val shiftType = selectedShiftType ?: return@ShiftRegistrationSection
                    onSaveShift(shiftType)
                },
                onDeleteShift = {
                    selectedShiftType = null
                    onDeleteShift()
                }
            )
        }
    }
}

private const val NEW_EVENT_ID = "__new__"

@Composable
private fun EntryShortcutBar(
    onOpenEntryHome: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 2.dp,
            modifier = Modifier.clickable(onClick = onOpenEntryHome)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "✎",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "일정 입력",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun EntryHomeScreen(
    currentMonth: YearMonth,
    onBack: () -> Unit,
    onOpenEventEntry: () -> Unit,
    onOpenBulkShift: () -> Unit
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Text(text = "<", style = MaterialTheme.typography.titleLarge)
                }
                Column {
                    Text(
                        text = "입력 메뉴",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${currentMonth.year}년 ${currentMonth.monthValue}월 기준 등록",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenEventEntry)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "일정 등록",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "나, 상대방, 우리 일정 중 하나를 선택해 날짜별 일정을 추가합니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenBulkShift)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "스케줄표 일괄 등록",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "스케줄표 이미지를 불러와 자동 입력하거나, 현재 달의 모든 날짜를 직접 검토하며 저장합니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EventEntryScreen(
    initialDate: LocalDate,
    isLoading: Boolean,
    isSubmitting: Boolean,
    errorMessage: String?,
    onDismissError: () -> Unit,
    onBack: () -> Unit,
    onSaveEvent: (EventOwnerType, LocalDate, LocalDate, LocalTime, LocalTime, String, String?) -> Unit
) {
    var selectedOwnerType by remember { mutableStateOf(EventOwnerType.ME) }
    var startDate by remember(initialDate) { mutableStateOf(initialDate) }
    var endDate by remember(initialDate) { mutableStateOf(initialDate) }
    var startTime by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var endTime by remember { mutableStateOf(LocalTime.of(10, 0)) }
    var title by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    var note by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Text(text = "<", style = MaterialTheme.typography.titleLarge)
                }
                Column {
                    Text(
                        text = "일정 등록",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "일정 유형과 날짜를 선택해 새 일정을 추가합니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SyncStatusSection(
                isLoading = isLoading,
                isSubmitting = isSubmitting,
                errorMessage = errorMessage,
                onDismissError = onDismissError
            )

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "일정 유형",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EventOwnerType.entries.forEach { ownerType ->
                            FilterChip(
                                selected = selectedOwnerType == ownerType,
                                onClick = { selectedOwnerType = ownerType },
                                label = { Text(ownerType.label) }
                            )
                        }
                    }

                    Text(
                        text = "시작일",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = {
                            startDate = startDate.minusDays(1)
                            if (endDate.isBefore(startDate)) endDate = startDate
                        }) {
                            Text(text = "<", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            text = "${startDate.year}.${startDate.monthValue}.${startDate.dayOfMonth}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = {
                            startDate = startDate.plusDays(1)
                            if (endDate.isBefore(startDate)) endDate = startDate
                            if (startDate == endDate && endTime.isBefore(startTime)) endTime = startTime
                        }) {
                            Text(text = ">", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    TimeStepperRow(
                        label = "시작 시간",
                        time = startTime,
                        onTimeChange = { time ->
                            startTime = time
                            if (startDate == endDate && endTime.isBefore(startTime)) {
                                endTime = startTime
                            }
                        }
                    )

                    Text(
                        text = "종료일",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = {
                            endDate = endDate.minusDays(1).takeIf { !it.isBefore(startDate) } ?: startDate
                            if (startDate == endDate && endTime.isBefore(startTime)) endTime = startTime
                        }) {
                            Text(text = "<", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            text = "${endDate.year}.${endDate.monthValue}.${endDate.dayOfMonth}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { endDate = endDate.plusDays(1) }) {
                            Text(text = ">", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    TimeStepperRow(
                        label = "종료 시간",
                        time = endTime,
                        onTimeChange = { time ->
                            endTime = if (startDate == endDate && time.isBefore(startTime)) startTime else time
                        }
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("일정 제목") },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("메모") },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done
                        )
                    )

                    OutlinedButton(
                        onClick = {
                            onSaveEvent(
                                selectedOwnerType,
                                startDate,
                                endDate,
                                startTime,
                                endTime,
                                title.text.trim(),
                                note.text.trim().ifBlank { null }
                            )
                        },
                        enabled = title.text.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("일정 저장")
                    }
                }
            }
        }
    }
}

@Composable
private fun EventEditorSection(
    selectedDate: LocalDate,
    initialEvent: CalendarEvent?,
    isCreating: Boolean,
    onCancel: () -> Unit,
    onSave: (EventOwnerType, LocalDate, LocalDate, LocalTime, LocalTime, String, String?) -> Unit,
    onDelete: () -> Unit
) {
    var selectedOwnerType by remember(initialEvent, isCreating) {
        mutableStateOf(initialEvent?.ownerType ?: EventOwnerType.ME)
    }
    var startDate by remember(initialEvent, selectedDate) {
        mutableStateOf(initialEvent?.startDate ?: selectedDate)
    }
    var endDate by remember(initialEvent, selectedDate) {
        mutableStateOf(initialEvent?.endDate ?: selectedDate)
    }
    var startTime by remember(initialEvent, selectedDate) {
        mutableStateOf(initialEvent?.startTime ?: LocalTime.of(9, 0))
    }
    var endTime by remember(initialEvent, selectedDate) {
        mutableStateOf(initialEvent?.endTime ?: LocalTime.of(10, 0))
    }
    var title by rememberSaveable(selectedDate, initialEvent?.id, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initialEvent?.title.orEmpty()))
    }
    var note by rememberSaveable(selectedDate, initialEvent?.id, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initialEvent?.note.orEmpty()))
    }

    if (!isCreating && initialEvent == null) {
        return
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isCreating) "일정 추가" else "일정 수정",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isCreating && initialEvent != null) {
                        OutlinedButton(onClick = onDelete) {
                            Text("삭제")
                        }
                    }
                    OutlinedButton(onClick = onCancel) {
                        Text("닫기")
                    }
                }
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EventOwnerType.entries.forEach { ownerType ->
                    FilterChip(
                        selected = selectedOwnerType == ownerType,
                        onClick = { selectedOwnerType = ownerType },
                        label = { Text(ownerType.label) }
                    )
                }
            }

            Text(
                text = "시작일",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = {
                    startDate = startDate.minusDays(1)
                    if (endDate.isBefore(startDate)) endDate = startDate
                }) {
                    Text(text = "<", style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    text = "${startDate.year}.${startDate.monthValue}.${startDate.dayOfMonth}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = {
                    startDate = startDate.plusDays(1)
                    if (endDate.isBefore(startDate)) endDate = startDate
                    if (startDate == endDate && endTime.isBefore(startTime)) endTime = startTime
                }) {
                    Text(text = ">", style = MaterialTheme.typography.titleMedium)
                }
            }
            TimeStepperRow(
                label = "시작 시간",
                time = startTime,
                onTimeChange = { time ->
                    startTime = time
                    if (startDate == endDate && endTime.isBefore(startTime)) {
                        endTime = startTime
                    }
                }
            )

            Text(
                text = "종료일",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = {
                    endDate = endDate.minusDays(1).takeIf { !it.isBefore(startDate) } ?: startDate
                    if (startDate == endDate && endTime.isBefore(startTime)) endTime = startTime
                }) {
                    Text(text = "<", style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    text = "${endDate.year}.${endDate.monthValue}.${endDate.dayOfMonth}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { endDate = endDate.plusDays(1) }) {
                    Text(text = ">", style = MaterialTheme.typography.titleMedium)
                }
            }
            TimeStepperRow(
                label = "종료 시간",
                time = endTime,
                onTimeChange = { time ->
                    endTime = if (startDate == endDate && time.isBefore(startTime)) startTime else time
                }
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("일정 제목") },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                singleLine = true
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("메모") },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                )
            )

            OutlinedButton(
                onClick = {
                    onSave(
                        selectedOwnerType,
                        startDate,
                        endDate,
                        startTime,
                        endTime,
                        title.text.trim(),
                        note.text.trim().ifBlank { null }
                    )
                },
                enabled = title.text.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isCreating) "일정 추가 저장" else "일정 수정 저장")
            }
        }
    }
}

@Composable
private fun TimeStepperRow(
    label: String,
    time: LocalTime,
    onTimeChange: (LocalTime) -> Unit
) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = { onTimeChange(time.minusMinutes(30)) }) {
            Text(text = "<", style = MaterialTheme.typography.titleMedium)
        }
        Text(
            text = time.displayText(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = { onTimeChange(time.plusMinutes(30)) }) {
            Text(text = ">", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun BulkShiftRegistrationScreen(
    month: YearMonth,
    existingShifts: List<ShiftSchedule>,
    isLoading: Boolean,
    isSubmitting: Boolean,
    errorMessage: String?,
    onDismissError: () -> Unit,
    onBack: () -> Unit,
    onSaveBulkShifts: (Map<LocalDate, ShiftType?>) -> Unit
) {
    val monthDates = remember(month) { (1..month.lengthOfMonth()).map { month.atDay(it) } }
    val initialSelections = remember(month, existingShifts) {
        monthDates.associateWith { date ->
            existingShifts.firstOrNull { it.date == date }?.shiftType
        }
    }
    var draftSelections by remember(month, existingShifts) { mutableStateOf(initialSelections) }
    var ocrUiState by remember(month) { mutableStateOf(BulkShiftOcrUiState()) }
    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        ocrUiState = BulkShiftOcrUiState(
            isRecognizing = true,
            statusMessage = "스케줄표를 인식하는 중입니다."
        )
        ShiftOcrImageRecognizer.recognize(
            context = context,
            uri = uri,
            onSuccess = { recognizedText ->
                val parseResult = parseShiftOcrText(recognizedText, month)
                draftSelections = monthDates.associateWith { date -> parseResult.items[date] }
                ocrUiState = ocrUiState.copy(
                    statusMessage = "${month.lengthOfMonth()}일 중 ${parseResult.recognizedCount}일을 자동 입력했습니다. 저장 전 날짜별 스케줄을 확인해 주세요.",
                    issues = parseResult.issues
                )
            },
            onFailure = { message ->
                ocrUiState = ocrUiState.copy(
                    statusMessage = message,
                    issues = emptyList()
                )
            },
            onComplete = {
                ocrUiState = ocrUiState.copy(isRecognizing = false)
            }
        )
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Text(text = "<", style = MaterialTheme.typography.titleLarge)
                }
                Column {
                    Text(
                        text = "스케줄표 일괄 등록",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${month.year}년 ${month.monthValue}월 스케줄 검토 및 저장",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SyncStatusSection(
                isLoading = isLoading,
                isSubmitting = isSubmitting,
                errorMessage = errorMessage,
                onDismissError = onDismissError
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "스케줄표 불러오기",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "본인 근무표 행 이미지를 선택하면 OCR로 근무 코드를 읽어 아래 날짜별 스케줄에 자동 입력합니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            enabled = !ocrUiState.isRecognizing && !isSubmitting
                        ) {
                            Text(if (ocrUiState.isRecognizing) "스케줄표 인식 중" else "스케줄표 불러오기")
                        }
                        OutlinedButton(
                            onClick = {
                                draftSelections = initialSelections
                                ocrUiState = BulkShiftOcrUiState()
                            },
                            enabled = !ocrUiState.isRecognizing && !isSubmitting
                        ) {
                            Text("기존 입력으로 되돌리기")
                        }
                    }
                    ocrUiState.statusMessage?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    ocrUiState.issues.forEach { issue ->
                        Text(
                            text = issue,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                monthDates.forEach { date ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${date.monthValue}월 ${date.dayOfMonth}일",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = draftSelections[date] == null,
                                    onClick = {
                                        draftSelections = draftSelections.toMutableMap().apply {
                                            this[date] = null
                                        }
                                    },
                                    label = { Text("없음") }
                                )
                                ShiftType.entries.forEach { shiftType ->
                                    InputChip(
                                        selected = draftSelections[date] == shiftType,
                                        onClick = {
                                            draftSelections = draftSelections.toMutableMap().apply {
                                                this[date] = shiftType
                                            }
                                        },
                                        label = { Text(shiftType.label) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = { onSaveBulkShifts(draftSelections) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("검토한 스케줄 저장")
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    modifier: Modifier = Modifier,
    showTopDivider: Boolean,
    showRightDivider: Boolean,
    date: LocalDate,
    isCurrentMonth: Boolean,
    isSelected: Boolean,
    inlineEventSlots: Map<Int, CalendarEvent>,
    occupiedSlots: Set<Int>,
    totalEventCount: Int,
    visibleShift: ShiftSchedule?,
    onClick: () -> Unit
) {
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = CalendarGridLineAlpha)
    val baseColor = when {
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        isCurrentMonth -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
    }
    val hasEvents = totalEventCount > 0
    val visibleEventCount = (inlineEventSlots.keys + occupiedSlots).distinct().size

    Column(
        modifier = modifier
            .aspectRatio(CalendarCellAspectRatio)
            .background(baseColor)
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                if (showTopDivider) {
                    drawLine(
                        color = dividerColor,
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                        strokeWidth = strokeWidth
                    )
                }
                if (showRightDivider) {
                    drawLine(
                        color = dividerColor,
                        start = androidx.compose.ui.geometry.Offset(size.width, 0f),
                        end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                        strokeWidth = strokeWidth
                    )
                }
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CalendarDayHeaderHeight),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isCurrentMonth) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )

                    if (hasEvents) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                visibleShift?.let { shift ->
                    Box(
                        modifier = Modifier
                            .size(width = 22.dp, height = 18.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(shift.shiftType.color.copy(alpha = 0.14f))
                            .border(
                                BorderStroke(1.dp, shift.shiftType.color.copy(alpha = 0.4f)),
                                RoundedCornerShape(6.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = shift.shiftType.calendarBadgeLabel(),
                            style = MaterialTheme.typography.labelSmall,
                            color = shift.shiftType.color,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(1.dp))

        repeat(CalendarDayCellVisibleEventLimit) { slot ->
            inlineEventSlots[slot]?.let { event ->
                EventTag(
                    event = event,
                    date = date,
                    modifier = Modifier.height(CalendarEventSlotHeight)
                )
            } ?: Spacer(modifier = Modifier.height(CalendarEventSlotHeight))

            if (slot < CalendarDayCellVisibleEventLimit - 1) {
                Spacer(modifier = Modifier.height(CalendarEventSlotGap))
            }
        }

        if (totalEventCount > visibleEventCount) {
            Text(
                text = "+${totalEventCount - visibleEventCount}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyCalendarCell(
    modifier: Modifier = Modifier,
    isCurrentMonth: Boolean,
    showTopDivider: Boolean,
    showRightDivider: Boolean
) {
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = CalendarGridLineAlpha)
    val baseColor = if (isCurrentMonth) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
    }

    Box(
        modifier = modifier
            .aspectRatio(CalendarCellAspectRatio)
            .background(baseColor)
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                if (showTopDivider) {
                    drawLine(
                        color = dividerColor,
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                        strokeWidth = strokeWidth
                    )
                }
                if (showRightDivider) {
                    drawLine(
                        color = dividerColor,
                        start = androidx.compose.ui.geometry.Offset(size.width, 0f),
                        end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                        strokeWidth = strokeWidth
                    )
                }
            }
    )
}

@Composable
private fun ShiftBadge(shiftType: ShiftType) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(shiftType.color.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = shiftType.calendarBadgeLabel(),
            style = MaterialTheme.typography.labelSmall,
            color = shiftType.color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EventTag(
    event: CalendarEvent,
    date: LocalDate,
    modifier: Modifier = Modifier
) {
    val containerColor = event.ownerType.color.copy(alpha = 0.18f)
    val shape = RoundedCornerShape(8.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(containerColor)
            .border(
                width = 1.dp,
                color = event.ownerType.color.copy(alpha = 0.32f),
                shape = shape
            )
            .padding(horizontal = 2.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(5.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(event.ownerType.color)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = event.title.compactCalendarLabel(maxVisibleChars = 2),
            maxLines = 1,
            overflow = TextOverflow.Clip,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = MaterialTheme.colorScheme.onSurface,
            softWrap = false
        )
    }
}

private fun String.compactCalendarLabel(maxVisibleChars: Int): String {
    val normalized = replace(" ", "")
    if (normalized.length <= maxVisibleChars) return normalized
    return normalized.take(maxVisibleChars) + "…"
}

private fun buildInlineEventSlots(
    events: List<CalendarEvent>,
    occupiedSlots: Set<Int>
): Map<Int, CalendarEvent> {
    val availableSlots = (0 until CalendarDayCellVisibleEventLimit).filterNot { it in occupiedSlots }
    return availableSlots
        .zip(events.take(availableSlots.size))
        .associate { (slot, event) -> slot to event }
}

private fun buildWeekEventBarLayouts(
    week: List<LocalDate?>,
    eventsByDate: Map<LocalDate, List<CalendarEvent>>
): List<WeekEventBarLayout> {
    val segments = buildWeekEventSegments(week, eventsByDate)
    val layouts = mutableListOf<WeekEventBarLayout>()

    segments.forEach { segment ->
        val slot = (0 until CalendarDayCellVisibleEventLimit).firstOrNull { candidate ->
            layouts.none { placed ->
                placed.slot == candidate &&
                    segment.startColumn <= placed.segment.endColumn &&
                    placed.segment.startColumn <= segment.endColumn
            }
        } ?: return@forEach

        layouts += WeekEventBarLayout(segment = segment, slot = slot)
    }

    return layouts
}

private fun buildWeekEventSegments(
    week: List<LocalDate?>,
    eventsByDate: Map<LocalDate, List<CalendarEvent>>
): List<WeekEventSegment> {
    val weekDates = week.filterNotNull()
    if (weekDates.isEmpty()) return emptyList()

    val weekStart = weekDates.first()
    val weekEnd = weekDates.last()

    return weekDates
        .flatMap { date -> eventsByDate[date].orEmpty() }
        .distinctBy { it.id }
        .filter { it.isMultiDay() }
        .mapNotNull { event ->
            val segmentStart = if (event.startDate.isAfter(weekStart)) event.startDate else weekStart
            val segmentEnd = if (event.endDate.isBefore(weekEnd)) event.endDate else weekEnd
            if (segmentEnd.isBefore(segmentStart)) {
                null
            } else {
                WeekEventSegment(
                    event = event,
                    startColumn = week.indexOf(segmentStart),
                    endColumn = week.indexOf(segmentEnd),
                    continuesFromPreviousWeek = event.startDate.isBefore(weekStart),
                    continuesToNextWeek = event.endDate.isAfter(weekEnd)
                )
            }
        }
        .sortedWith(
            compareBy<WeekEventSegment> { it.startColumn }
                .thenByDescending { it.endColumn - it.startColumn }
                .thenBy { it.event.title }
        )
}

private fun buildCalendarEventBuckets(
    eventsByDate: Map<LocalDate, List<CalendarEvent>>,
    filters: CalendarFilters
): CalendarEventBuckets {
    val filtered = eventsByDate.mapValues { (_, dayEvents) ->
        dayEvents.filter { filters.isVisible(it.ownerType) }
    }

    return CalendarEventBuckets(
        allByDate = filtered,
        singleDayByDate = filtered.mapValues { (_, dayEvents) ->
            dayEvents.filterNot { it.isMultiDay() }
        },
        multiDayByDate = filtered.mapValues { (_, dayEvents) ->
            dayEvents.filter { it.isMultiDay() }
        }
    )
}

@Composable
private fun SelectedDateSummary(
    selectedDate: LocalDate,
    events: List<CalendarEvent>,
    shift: ShiftSchedule?,
    filters: CalendarFilters,
    shiftOwnerType: ShiftOwnerType,
    editingEventId: String?,
    onAddEvent: () -> Unit,
    onEditEvent: (CalendarEvent) -> Unit
) {
    val visibleEvents = remember(events, filters) {
        events.filter { filters.isVisible(it.ownerType) }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${selectedDate.monthValue}월 ${selectedDate.dayOfMonth}일 상세",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(onClick = onAddEvent) {
                    Text("일정 추가")
                }
            }

            if (filters.showShift && shift != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = shiftOwnerType.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    ShiftBadge(shiftType = shift.shiftType)
                }
            }

            if (visibleEvents.isEmpty()) {
                Text(
                    text = "표시 중인 일정이 없습니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    visibleEvents.forEach { event ->
                        val isEditing = editingEventId == event.id
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isEditing) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            tonalElevation = 1.dp,
                            modifier = Modifier.clickable { onEditEvent(event) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(event.ownerType.color)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = event.title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = event.displayDateText() + " · " + event.ownerType.label + (event.note?.let { " · $it" } ?: ""),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = if (isEditing) "편집 중" else "수정",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShiftRegistrationSection(
    selectedDate: LocalDate,
    hasExistingShift: Boolean,
    shiftOwnerType: ShiftOwnerType,
    selectedShiftType: ShiftType?,
    onShiftTypeSelected: (ShiftType) -> Unit,
    onSaveShift: () -> Unit,
    onDeleteShift: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "${selectedDate.monthValue}월 ${selectedDate.dayOfMonth}일 스케줄 등록",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (shiftOwnerType == ShiftOwnerType.PARTNER) {
                    "상대 근무 보기에서는 수정할 수 없습니다. 내 근무 보기로 전환한 뒤 등록해 주세요."
                } else if (hasExistingShift) {
                    "근무 유형을 선택하면 기존 스케줄을 덮어쓰고, 삭제 버튼으로 비울 수 있습니다."
                } else {
                    "근무 유형을 선택하면 해당 날짜의 스케줄을 저장합니다."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShiftType.entries.forEach { shiftType ->
                    InputChip(
                        selected = selectedShiftType == shiftType,
                        onClick = { onShiftTypeSelected(shiftType) },
                        enabled = shiftOwnerType == ShiftOwnerType.ME,
                        label = { Text(shiftType.label) }
                    )
                }
            }
            OutlinedButton(
                onClick = onSaveShift,
                enabled = selectedShiftType != null && shiftOwnerType == ShiftOwnerType.ME
            ) {
                Text("스케줄 저장")
            }
            if (hasExistingShift && shiftOwnerType == ShiftOwnerType.ME) {
                OutlinedButton(
                    onClick = onDeleteShift,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("스케줄 삭제")
                }
            }
        }
    }
}
