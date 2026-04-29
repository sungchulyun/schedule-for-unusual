package com.schedule.api.shift.controller;

import com.schedule.api.common.context.RequestContextProvider;
import com.schedule.api.common.response.ApiResponse;
import com.schedule.api.shift.dto.DeleteShiftResponse;
import com.schedule.api.shift.dto.MonthlyShiftResponse;
import com.schedule.api.shift.dto.MonthlyShiftUpsertRequest;
import com.schedule.api.shift.dto.ShiftImagePreviewResponse;
import com.schedule.api.shift.dto.ShiftImagePreviewTextRequest;
import com.schedule.api.shift.dto.ShiftDateResponse;
import com.schedule.api.shift.dto.ShiftMonthResponse;
import com.schedule.api.shift.dto.ShiftResponse;
import com.schedule.api.shift.dto.UpsertShiftRequest;
import com.schedule.api.shift.service.ShiftImagePreviewService;
import com.schedule.api.shift.service.ShiftService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shifts")
public class ShiftController {

    private final ShiftService shiftService;
    private final ShiftImagePreviewService shiftImagePreviewService;
    private final RequestContextProvider requestContextProvider;

    public ShiftController(
            ShiftService shiftService,
            ShiftImagePreviewService shiftImagePreviewService,
            RequestContextProvider requestContextProvider
    ) {
        this.shiftService = shiftService;
        this.shiftImagePreviewService = shiftImagePreviewService;
        this.requestContextProvider = requestContextProvider;
    }

    @GetMapping
    public ApiResponse<ShiftMonthResponse> getMonthlyShifts(
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ApiResponse.success(shiftService.getMonthlyShifts(requestContextProvider.getRequiredContext(), year, month));
    }

    @GetMapping("/date/{date}")
    public ApiResponse<ShiftDateResponse> getDateShift(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ApiResponse.success(shiftService.getDateShift(requestContextProvider.getRequiredContext(), date));
    }

    @PutMapping("/{date}")
    public ApiResponse<ShiftResponse> upsertShift(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody UpsertShiftRequest request
    ) {
        return ApiResponse.success(shiftService.upsertShift(requestContextProvider.getRequiredContext(), date, request));
    }

    @PutMapping("/monthly")
    public ApiResponse<MonthlyShiftResponse> replaceMonthlyShifts(
            @RequestParam int year,
            @RequestParam int month,
            @Valid @RequestBody MonthlyShiftUpsertRequest request
    ) {
        return ApiResponse.success(
                shiftService.replaceMonthlyShifts(requestContextProvider.getRequiredContext(), year, month, request)
        );
    }

    @PostMapping("/monthly/preview-from-text")
    public ApiResponse<ShiftImagePreviewResponse> previewMonthlyShiftsFromText(
            @RequestParam int year,
            @RequestParam int month,
            @Valid @RequestBody ShiftImagePreviewTextRequest request
    ) {
        return ApiResponse.success(shiftImagePreviewService.preview(year, month, request.scheduleText()));
    }

    @DeleteMapping("/{date}")
    public ApiResponse<DeleteShiftResponse> deleteShift(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ApiResponse.success(shiftService.deleteShift(requestContextProvider.getRequiredContext(), date));
    }
}
