package com.mybank.payment.service;

import com.mybank.common.exception.BusinessException;
import com.mybank.payment.dto.PaymentResponse;
import com.mybank.payment.dto.TransferRequest;
import com.mybank.payment.model.Payment;
import com.mybank.payment.model.PaymentStatus;
import com.mybank.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Tests")
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private PaymentService paymentService;

    private TransferRequest transferRequest;
    private Payment payment;

    @BeforeEach
    void setUp() {
        transferRequest = TransferRequest.builder()
                .fromAccountId("account-123")
                .toAccountId("account-456")
                .recipientName("John Doe")
                .amount(new BigDecimal("100000"))
                .description("Test transfer")
                .build();

        payment = Payment.builder()
                .id("payment-123")
                .userId("user-123")
                .fromAccountId("account-123")
                .toAccountId("account-456")
                .recipientName("John Doe")
                .amount(new BigDecimal("100000"))
                .currency("KRW")
                .status(PaymentStatus.COMPLETED)
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Should transfer money successfully")
    void shouldTransferMoneySuccessfully() {
        // Given
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        // When
        PaymentResponse response = paymentService.transfer("user-123", transferRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getPaymentId()).isEqualTo("payment-123");
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("100000"));

        verify(paymentRepository).save(any(Payment.class));
        verify(kafkaTemplate, times(2)).send(anyString(), any());
        verify(redisTemplate).delete(anyString());
    }

    @Test
    @DisplayName("Should throw exception when payment already in progress")
    void shouldThrowExceptionWhenPaymentInProgress() {
        // Given
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> paymentService.transfer("user-123", transferRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "PAYMENT_IN_PROGRESS");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get payment by ID")
    void shouldGetPaymentById() {
        // Given
        when(paymentRepository.findById("payment-123")).thenReturn(Optional.of(payment));

        // When
        PaymentResponse response = paymentService.getPayment("payment-123");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getPaymentId()).isEqualTo("payment-123");
    }

    @Test
    @DisplayName("Should throw exception when payment not found")
    void shouldThrowExceptionWhenPaymentNotFound() {
        // Given
        when(paymentRepository.findById("payment-999")).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> paymentService.getPayment("payment-999"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "PAYMENT_NOT_FOUND");
    }
}
