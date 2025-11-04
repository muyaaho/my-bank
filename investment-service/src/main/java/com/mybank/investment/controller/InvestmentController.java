package com.mybank.investment.controller;

import com.mybank.common.dto.ApiResponse;
import com.mybank.investment.dto.InvestmentSummaryResponse;
import com.mybank.investment.service.InvestmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Investment REST controller
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

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Investment Service is healthy"));
    }
}
