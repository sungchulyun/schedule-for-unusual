package com.schedule.api.event.controller;

import com.schedule.api.common.context.RequestContextProvider;
import com.schedule.api.common.response.ApiResponse;
import com.schedule.api.event.dto.CreateEventRequest;
import com.schedule.api.event.dto.DeleteEventResponse;
import com.schedule.api.event.dto.EventDateResponse;
import com.schedule.api.event.dto.EventMonthResponse;
import com.schedule.api.event.dto.EventResponse;
import com.schedule.api.event.dto.UpdateEventRequest;
import com.schedule.api.event.service.EventService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final EventService eventService;
    private final RequestContextProvider requestContextProvider;

    public EventController(EventService eventService, RequestContextProvider requestContextProvider) {
        this.eventService = eventService;
        this.requestContextProvider = requestContextProvider;
    }

    @GetMapping
    public ApiResponse<EventMonthResponse> getMonthlyEvents(
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ApiResponse.success(eventService.getMonthlyEvents(requestContextProvider.getRequiredContext(), year, month));
    }

    @GetMapping("/date/{date}")
    public ApiResponse<EventDateResponse> getDateEvents(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ApiResponse.success(eventService.getDateEvents(requestContextProvider.getRequiredContext(), date));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(@Valid @RequestBody CreateEventRequest request) {
        EventResponse response = eventService.createEvent(requestContextProvider.getRequiredContext(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PatchMapping("/{eventId}")
    public ApiResponse<EventResponse> updateEvent(
            @PathVariable String eventId,
            @RequestBody UpdateEventRequest request
    ) {
        return ApiResponse.success(eventService.updateEvent(requestContextProvider.getRequiredContext(), eventId, request));
    }

    @DeleteMapping("/{eventId}")
    public ApiResponse<DeleteEventResponse> deleteEvent(@PathVariable String eventId) {
        return ApiResponse.success(eventService.deleteEvent(requestContextProvider.getRequiredContext(), eventId));
    }
}
