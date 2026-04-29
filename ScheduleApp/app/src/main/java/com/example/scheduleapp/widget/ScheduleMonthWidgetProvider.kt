package com.example.scheduleapp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import com.example.scheduleapp.MainActivity
import com.example.scheduleapp.R
import com.example.scheduleapp.data.AuthSessionManager
import com.example.scheduleapp.data.CalendarRepository
import com.example.scheduleapp.ui.calendar.CalendarEvent
import com.example.scheduleapp.ui.calendar.EventOwnerType
import com.example.scheduleapp.ui.calendar.ShiftSchedule
import com.example.scheduleapp.ui.calendar.ShiftOwnerType
import com.example.scheduleapp.ui.calendar.ShiftType
import com.example.scheduleapp.ui.calendar.buildCalendarDays
import com.example.scheduleapp.ui.calendar.buildEventsByDate
import com.example.scheduleapp.ui.calendar.widgetShortLabel
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class ScheduleMonthWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateAllWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ScheduleMonthWidgetContract.ActionRefresh,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED,
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> requestRefresh(context)
        }
    }

    companion object {
        private const val WIDGET_ROWS = 6
        private const val WIDGET_COLUMNS = 7

        fun requestRefresh(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, ScheduleMonthWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            updateAllWidgets(context, appWidgetManager, appWidgetIds)
        }

        private fun updateAllWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            if (appWidgetIds.isEmpty()) return

            Thread {
                val payload = loadMonthPayload(context)
                appWidgetIds.forEach { appWidgetId ->
                    updateAppWidget(context, appWidgetManager, appWidgetId, payload)
                }
            }.start()
        }

        private fun loadMonthPayload(context: Context): WidgetMonthPayload {
            val month = YearMonth.now()
            val visibleDays = buildCalendarDays(month)
                .toMutableList()
                .apply {
                    while (size < WIDGET_ROWS * WIDGET_COLUMNS) {
                        add(null)
                    }
                }

            AuthSessionManager.initialize(context)
            val session = AuthSessionManager.getSession()
            if (session == null) {
                return WidgetMonthPayload(
                    month = month,
                    days = visibleDays,
                    eventsByDate = emptyMap(),
                    shiftsByDate = emptyMap()
                )
            }

            val monthData = runCatching {
                val shiftOwnerType = session.defaultShiftOwnerType
                    .availableOrFallback(hasPartnerConnected = session.partnerUserId != null)
                runBlocking { CalendarRepository().getMonth(month, shiftOwnerType) }
            }.getOrNull()

            return WidgetMonthPayload(
                month = month,
                days = visibleDays,
                eventsByDate = monthData?.events?.let(::buildEventsByDate).orEmpty(),
                shiftsByDate = monthData?.shifts?.associateBy { it.date }.orEmpty()
            )
        }

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            payload: WidgetMonthPayload
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_month_calendar).apply {
                setTextViewText(
                    R.id.widget_month_title,
                    payload.month.format(
                        DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREAN)
                    )
                )

                val openAppIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId + 10_000,
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setOnClickPendingIntent(R.id.widget_header_root, openAppIntent)

                val refreshIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId + 20_000,
                    Intent(context, ScheduleMonthWidgetProvider::class.java).apply {
                        action = ScheduleMonthWidgetContract.ActionRefresh
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setOnClickPendingIntent(R.id.widget_refresh_button, refreshIntent)

                repeat(WIDGET_ROWS) { rowIndex ->
                    val rowId = weekRowId(rowIndex)
                    removeAllViews(rowId)

                    repeat(WIDGET_COLUMNS) { columnIndex ->
                        val position = (rowIndex * WIDGET_COLUMNS) + columnIndex
                        val date = payload.days.getOrNull(position)
                        addView(
                            rowId,
                            buildDayView(
                                context = context,
                                appWidgetId = appWidgetId,
                                position = position,
                                date = date,
                                events = date?.let { payload.eventsByDate[it] }.orEmpty(),
                                shift = date?.let { payload.shiftsByDate[it] }
                            )
                        )
                    }
                }
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun buildDayView(
            context: Context,
            appWidgetId: Int,
            position: Int,
            date: LocalDate?,
            events: List<CalendarEvent>,
            shift: ShiftSchedule?
        ): RemoteViews {
            if (date == null) {
                return RemoteViews(context.packageName, R.layout.widget_month_day_empty)
            }

            return RemoteViews(context.packageName, R.layout.widget_month_day).apply {
                val isToday = date == LocalDate.now()
                setInt(
                    R.id.widget_day_root,
                    "setBackgroundResource",
                    if (isToday) R.drawable.widget_day_background_today else R.drawable.widget_day_background
                )
                setTextViewText(R.id.widget_day_number, date.dayOfMonth.toString())
                setTextColor(R.id.widget_day_number, resolveDayNumberColor(position, isToday))

                if (shift != null) {
                    setViewVisibility(R.id.widget_shift_badge, View.VISIBLE)
                    setTextViewText(R.id.widget_shift_badge, shift.shiftType.widgetShortLabel())
                    setInt(
                        R.id.widget_shift_badge,
                        "setBackgroundResource",
                        shift.shiftType.widgetBadgeBackgroundRes()
                    )
                } else {
                    setViewVisibility(R.id.widget_shift_badge, View.GONE)
                }

                bindEventLine(this, R.id.widget_event_1, events.getOrNull(0))
                bindEventLine(this, R.id.widget_event_2, events.getOrNull(1))
                setViewVisibility(
                    R.id.widget_overflow_dot,
                    if (events.size > 2) View.VISIBLE else View.INVISIBLE
                )

                val openDateIntent = PendingIntent.getActivity(
                    context,
                    (appWidgetId * 100) + position,
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra(ScheduleMonthWidgetContract.ExtraDate, date.toString())
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setOnClickPendingIntent(R.id.widget_day_root, openDateIntent)
            }
        }

        private fun bindEventLine(views: RemoteViews, lineId: Int, event: CalendarEvent?) {
            if (event == null) {
                views.setTextViewText(lineId, "")
                views.setViewVisibility(lineId, View.INVISIBLE)
                return
            }

            views.setViewVisibility(lineId, View.VISIBLE)
            views.setTextViewText(lineId, event.title.compactWidgetLabel(2))
            views.setTextColor(lineId, Color.parseColor("#1F2937"))
            views.setInt(
                lineId,
                "setBackgroundResource",
                event.ownerType.widgetTagBackgroundRes()
            )
        }

        private fun weekRowId(rowIndex: Int): Int {
            return when (rowIndex) {
                0 -> R.id.widget_week_row_0
                1 -> R.id.widget_week_row_1
                2 -> R.id.widget_week_row_2
                3 -> R.id.widget_week_row_3
                4 -> R.id.widget_week_row_4
                else -> R.id.widget_week_row_5
            }
        }

        private fun resolveDayNumberColor(position: Int, isToday: Boolean): Int {
            if (isToday) return Color.WHITE
            return when (position % WIDGET_COLUMNS) {
                0 -> Color.parseColor("#D15353")
                WIDGET_COLUMNS - 1 -> Color.parseColor("#4A67D6")
                else -> Color.parseColor("#1F2937")
            }
        }
    }
}

private data class WidgetMonthPayload(
    val month: YearMonth,
    val days: List<LocalDate?>,
    val eventsByDate: Map<LocalDate, List<CalendarEvent>>,
    val shiftsByDate: Map<LocalDate, ShiftSchedule>
)

private fun String.compactWidgetLabel(maxVisibleChars: Int): String {
    val normalized = replace(" ", "")
    if (normalized.length <= maxVisibleChars) return normalized
    return normalized.take(maxVisibleChars) + "…"
}

private fun EventOwnerType.widgetTagBackgroundRes(): Int {
    return when (this) {
        EventOwnerType.ME -> R.drawable.widget_event_tag_me
        EventOwnerType.US -> R.drawable.widget_event_tag_us
        EventOwnerType.PARTNER -> R.drawable.widget_event_tag_partner
    }
}

private fun ShiftType.widgetBadgeBackgroundRes(): Int {
    return when (this) {
        ShiftType.DAY -> R.drawable.widget_shift_badge_day
        ShiftType.NIGHT -> R.drawable.widget_shift_badge_night
        ShiftType.MID -> R.drawable.widget_shift_badge_mid
        ShiftType.EVENING -> R.drawable.widget_shift_badge_evening
        ShiftType.OFF -> R.drawable.widget_shift_badge_off
        ShiftType.VACATION -> R.drawable.widget_shift_badge_vacation
    }
}
