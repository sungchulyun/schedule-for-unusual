package com.schedule.api.common.exception;

import com.schedule.api.common.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/test/common")
class TestCommonController {

    @GetMapping("/success")
    ApiResponse<Map<String, String>> success() {
        return ApiResponse.success(Map.of("message", "ok"));
    }

    @GetMapping("/business")
    ApiResponse<Void> business() {
        throw new BusinessException(ErrorCode.GROUP_NOT_FOUND);
    }

    @PostMapping("/validation")
    ApiResponse<Map<String, String>> validation(@Valid @RequestBody TestRequest request) {
        return ApiResponse.success(Map.of("name", request.name()));
    }

    @GetMapping("/constraint")
    ApiResponse<Map<String, String>> constraint(@RequestParam @NotBlank String name) {
        return ApiResponse.success(Map.of("name", name));
    }

    @GetMapping("/unexpected")
    ApiResponse<Void> unexpected() {
        throw new IllegalStateException("boom");
    }

    record TestRequest(
            @NotBlank String name
    ) {
    }
}
