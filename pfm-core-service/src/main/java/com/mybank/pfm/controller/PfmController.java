package com.mybank.pfm.controller;

import com.mybank.common.dto.ApiResponse;
import com.mybank.pfm.dto.AssetSummaryResponse;
import com.mybank.pfm.dto.SpendingAnalysisResponse;
import com.mybank.pfm.service.AssetService;
import com.mybank.pfm.service.SpendingAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * PFM REST controller
 */
@Slf4j
@RestController
@RequestMapping("/api/pfm")
@RequiredArgsConstructor
public class PfmController {

    private final AssetService assetService;
    private final SpendingAnalysisService spendingAnalysisService;

    @GetMapping("/assets")
    public ResponseEntity<ApiResponse<AssetSummaryResponse>> getAssets(
            @RequestHeader("X-User-Id") String userId) {
        log.info("Get assets request for user: {}", userId);
        AssetSummaryResponse response = assetService.getAssetSummary(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/spending/analysis")
    public ResponseEntity<ApiResponse<SpendingAnalysisResponse>> getSpendingAnalysis(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "30") int daysBack) {
        log.info("Get spending analysis for user: {}, days: {}", userId, daysBack);
        SpendingAnalysisResponse response = spendingAnalysisService.getSpendingAnalysis(userId, daysBack);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("PFM Core Service is healthy"));
    }
}
