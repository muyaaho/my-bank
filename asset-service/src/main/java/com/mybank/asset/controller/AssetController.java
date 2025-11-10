package com.mybank.asset.controller;

import com.mybank.common.dto.ApiResponse;
import com.mybank.asset.dto.AssetSummaryResponse;
import com.mybank.asset.service.AssetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Asset REST Controller
 * Bounded Context: Asset Management
 */
@Slf4j
@RestController
@RequestMapping("/api/asset")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AssetSummaryResponse>> getAssetSummary(
            @RequestHeader("X-User-Id") String userId) {
        log.info("Get asset summary request for user: {}", userId);
        AssetSummaryResponse response = assetService.getAssetSummary(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Asset Service is healthy"));
    }
}
