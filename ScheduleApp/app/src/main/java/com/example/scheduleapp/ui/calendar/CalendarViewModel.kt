package com.example.scheduleapp.ui.calendar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scheduleapp.data.CalendarRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class CalendarRemoteState(
    val loadedMonth: YearMonth? = null,
    val events: List<CalendarEvent> = emptyList(),
    val shifts: List<ShiftSchedule> = emptyList(),
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

class CalendarViewModel(
    private val repository: CalendarRepository = CalendarRepository()
) : ViewModel() {
    var uiState by mutableStateOf(CalendarRemoteState())
        private set

    fun loadMonth(month: YearMonth, force: Boolean = false) {
        if (
            !force &&
            uiState.loadedMonth == month &&
            !uiState.isLoading &&
            uiState.errorMessage == null
        ) {
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(
                loadedMonth = month,
                isLoading = true,
                errorMessage = null
            )
            runCatching { repository.getMonth(month) }
                .onSuccess { data ->
                    uiState = uiState.copy(
                        loadedMonth = month,
                        events = data.events,
                        shifts = data.shifts,
                        isLoading = false
                    )
                }
                .onFailure { throwable ->
                    uiState = uiState.copy(
                        loadedMonth = month,
                        isLoading = false,
                        errorMessage = throwable.message ?: "월간 데이터를 불러오지 못했습니다."
                    )
                }
        }
    }

    fun createEvent(
        ownerType: EventOwnerType,
        startDate: LocalDate,
        endDate: LocalDate,
        title: String,
        note: String?,
        monthToRefresh: YearMonth,
        onSuccess: () -> Unit
    ) {
        submit(monthToRefresh, onSuccess) {
            repository.createEvent(ownerType, startDate, endDate, title, note)
        }
    }

    fun updateEvent(
        eventId: String,
        ownerType: EventOwnerType,
        startDate: LocalDate,
        endDate: LocalDate,
        title: String,
        note: String?,
        monthToRefresh: YearMonth,
        onSuccess: () -> Unit
    ) {
        submit(monthToRefresh, onSuccess) {
            repository.updateEvent(eventId, ownerType, startDate, endDate, title, note)
        }
    }

    fun deleteEvent(
        eventId: String,
        monthToRefresh: YearMonth,
        onSuccess: () -> Unit
    ) {
        submit(monthToRefresh, onSuccess) {
            repository.deleteEvent(eventId)
        }
    }

    fun upsertShift(
        date: LocalDate,
        shiftType: ShiftType,
        monthToRefresh: YearMonth,
        onSuccess: () -> Unit = {}
    ) {
        submit(monthToRefresh, onSuccess) {
            repository.upsertShift(date, shiftType)
        }
    }

    fun saveMonthlyShifts(
        month: YearMonth,
        updatedMonthShifts: Map<LocalDate, ShiftType?>,
        onSuccess: () -> Unit
    ) {
        submit(month, onSuccess) {
            repository.saveMonthlyShifts(month, updatedMonthShifts)
        }
    }

    fun deleteShift(
        date: LocalDate,
        monthToRefresh: YearMonth,
        onSuccess: () -> Unit = {}
    ) {
        submit(monthToRefresh, onSuccess) {
            repository.deleteShift(date)
        }
    }

    fun clearError() {
        uiState = uiState.copy(errorMessage = null)
    }

    private fun submit(
        monthToRefresh: YearMonth,
        onSuccess: () -> Unit,
        action: suspend () -> Unit
    ) {
        viewModelScope.launch {
            uiState = uiState.copy(isSubmitting = true, errorMessage = null)
            runCatching { action() }
                .onSuccess {
                    uiState = uiState.copy(isSubmitting = false)
                    loadMonth(monthToRefresh, force = true)
                    onSuccess()
                }
                .onFailure { throwable ->
                    uiState = uiState.copy(
                        isSubmitting = false,
                        errorMessage = throwable.message ?: "서버 요청에 실패했습니다."
                    )
                }
        }
    }
}
