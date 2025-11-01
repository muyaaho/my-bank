package com.mybank.investment.service;

import com.mybank.investment.dto.InvestmentSummaryResponse;
import com.mybank.investment.model.Investment;
import com.mybank.investment.model.InvestmentType;
import com.mybank.investment.model.RoundUpConfig;
import com.mybank.investment.repository.InvestmentRepository;
import com.mybank.investment.repository.RoundUpConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InvestmentService Tests")
class InvestmentServiceTest {

    @Mock
    private InvestmentRepository investmentRepository;

    @Mock
    private RoundUpConfigRepository roundUpConfigRepository;

    @InjectMocks
    private InvestmentService investmentService;

    private List<Investment> testInvestments;
    private RoundUpConfig roundUpConfig;

    @BeforeEach
    void setUp() {
        testInvestments = Arrays.asList(
                createInvestment("inv-1", InvestmentType.ROUNDUP, new BigDecimal("500")),
                createInvestment("inv-2", InvestmentType.MANUAL, new BigDecimal("100000")),
                createInvestment("inv-3", InvestmentType.ROUNDUP, new BigDecimal("300"))
        );

        roundUpConfig = RoundUpConfig.builder()
                .id("config-1")
                .userId("user-123")
                .roundUpUnit(new BigDecimal("1000"))
                .totalRoundedUp(new BigDecimal("800"))
                .totalTransactions(2)
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("Should get investment summary successfully")
    void shouldGetInvestmentSummarySuccessfully() {
        // Given
        when(investmentRepository.findByUserId("user-123")).thenReturn(testInvestments);
        when(roundUpConfigRepository.findByUserId("user-123")).thenReturn(Optional.of(roundUpConfig));

        // When
        InvestmentSummaryResponse response = investmentService.getSummary("user-123");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTotalInvested()).isEqualByComparingTo(new BigDecimal("100800"));
        assertThat(response.getTotalRoundedUp()).isEqualByComparingTo(new BigDecimal("800"));
        assertThat(response.getTotalRoundUpTransactions()).isEqualTo(2);
        assertThat(response.getRecentInvestments()).hasSize(3);
    }

    @Test
    @DisplayName("Should calculate round-up amount correctly")
    void shouldCalculateRoundUpAmountCorrectly() {
        // Given
        BigDecimal paymentAmount = new BigDecimal("15300");
        BigDecimal roundUpUnit = new BigDecimal("1000");

        // When
        BigDecimal roundUpAmount = investmentService.calculateRoundUpAmount(paymentAmount, roundUpUnit);

        // Then
        // 15300 -> 16000, so round up is 700
        assertThat(roundUpAmount).isEqualByComparingTo(new BigDecimal("700"));
    }

    @Test
    @DisplayName("Should return zero for already rounded amount")
    void shouldReturnZeroForAlreadyRoundedAmount() {
        // Given
        BigDecimal paymentAmount = new BigDecimal("15000");
        BigDecimal roundUpUnit = new BigDecimal("1000");

        // When
        BigDecimal roundUpAmount = investmentService.calculateRoundUpAmount(paymentAmount, roundUpUnit);

        // Then
        assertThat(roundUpAmount).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private Investment createInvestment(String id, InvestmentType type, BigDecimal amount) {
        return Investment.builder()
                .id(id)
                .userId("user-123")
                .investmentType(type)
                .amount(amount)
                .productName("Test Product")
                .currency("KRW")
                .build();
    }
}
