package com.mybank.investment.controller;

import com.mybank.common.dto.ApiResponse;
import com.mybank.investment.dto.InvestmentSummaryResponse;
import com.mybank.investment.service.InvestmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * Investment REST controller
 * DDD-compliant: All operations go through Aggregate Root
 */
@Slf4j
@RestController
@RequestMapping("/api/invest")
@RequiredArgsConstructor
public class InvestmentController {

    private final InvestmentService investmentService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<InvestmentSummaryResponse>> getInvestmentSummary(
            @RequestHeader("X-User-Id") String userId) {
        log.info("Get investment summary for user: {}", userId);
        InvestmentSummaryResponse response = investmentService.getInvestmentSummary(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/roundup/enable/{accountId}")
    public ResponseEntity<ApiResponse<String>> enableRoundUp(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String accountId,
            @RequestParam String sourceAccountId,
            @RequestParam(defaultValue = "1000") BigDecimal roundUpUnit) {
        log.info("Enable round-up for user: {}, account: {}", userId, accountId);
        investmentService.enableRoundUp(userId, accountId, sourceAccountId, roundUpUnit);
        return ResponseEntity.ok(ApiResponse.success("Round-up enabled successfully"));
    }

    @PostMapping("/roundup/disable/{accountId}")
    public ResponseEntity<ApiResponse<String>> disableRoundUp(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String accountId) {
        log.info("Disable round-up for user: {}, account: {}", userId, accountId);
        investmentService.disableRoundUp(userId, accountId);
        return ResponseEntity.ok(ApiResponse.success("Round-up disabled successfully"));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Investment Service is healthy"));
    }
}
