package com.mybank.payment.controller;

import com.mybank.common.dto.ApiResponse;
import com.mybank.payment.dto.PaymentResponse;
import com.mybank.payment.dto.TransferRequest;
import com.mybank.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Payment REST controller
 */
@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<PaymentResponse>> transfer(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody TransferRequest request) {
        log.info("Transfer request from user: {}", userId);
        PaymentResponse response = paymentService.transfer(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(
            @PathVariable String paymentId) {
        log.info("Get payment: {}", paymentId);
        var payment = paymentService.getPaymentById(paymentId);
        PaymentResponse response = PaymentResponse.builder()
                .paymentId(payment.getId())
                .status(payment.getStatus().name())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .recipientName(payment.getRecipientName())
                .createdAt(payment.getCreatedAt())
                .completedAt(payment.getCompletedAt())
                .build();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Payment Service is healthy"));
    }
}
