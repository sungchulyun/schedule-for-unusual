package com.schedule.api.calendar.controller;

import com.schedule.api.calendar.dto.CalendarDateResponse;
import com.schedule.api.calendar.dto.CalendarMonthResponse;
import com.schedule.api.calendar.service.CalendarQueryService;
import com.schedule.api.common.context.RequestContextProvider;
import com.schedule.api.common.response.ApiResponse;
import com.schedule.api.event.domain.EventOwnerType;
import com.schedule.api.event.support.EventOwnerTypeFilterParser;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/calendar")
public class CalendarController {

    private final CalendarQueryService calendarQueryService;
    private final RequestContextProvider requestContextProvider;

    public CalendarController(
            CalendarQueryService calendarQueryService,
            RequestContextProvider requestContextProvider
    ) {
        this.calendarQueryService = calendarQueryService;
        this.requestContextProvider = requestContextProvider;
    }

    @GetMapping("/month")
    public ApiResponse<CalendarMonthResponse> getCalendarMonth(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) String ownerTypes,
            @RequestParam(defaultValue = "true") boolean includeShifts,
            @RequestParam(required = false) EventOwnerType shiftOwnerType
    ) {
        return ApiResponse.success(
                calendarQueryService.getMonthlyCalendar(
                        requestContextProvider.getRequiredContext(),
                        year,
                        month,
                        EventOwnerTypeFilterParser.parse(ownerTypes),
                        includeShifts,
                        shiftOwnerType
                )
        );
    }

    @GetMapping("/date/{date}")
    public ApiResponse<CalendarDateResponse> getCalendarDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String ownerTypes,
            @RequestParam(defaultValue = "true") boolean includeShifts,
            @RequestParam(required = false) EventOwnerType shiftOwnerType
    ) {
        return ApiResponse.success(calendarQueryService.getDateCalendar(
                requestContextProvider.getRequiredContext(),
                date,
                EventOwnerTypeFilterParser.parse(ownerTypes),
                includeShifts,
                shiftOwnerType
        ));
    }
}
