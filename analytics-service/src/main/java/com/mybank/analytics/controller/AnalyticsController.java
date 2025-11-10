package com.mybank.analytics.controller;

import com.mybank.common.dto.ApiResponse;
import com.mybank.analytics.dto.SpendingAnalysisResponse;
import com.mybank.analytics.service.SpendingAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Analytics REST Controller
 * Bounded Context: Spending Analytics
 */
@Slf4j
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final SpendingAnalysisService spendingAnalysisService;

    @GetMapping("/spending")
    public ResponseEntity<ApiResponse<SpendingAnalysisResponse>> getSpendingAnalysis(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "30") int daysBack) {
        log.info("Get spending analysis for user: {}, days: {}", userId, daysBack);
        SpendingAnalysisResponse response = spendingAnalysisService.getSpendingAnalysis(userId, daysBack);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Analytics Service is healthy"));
    }
}
