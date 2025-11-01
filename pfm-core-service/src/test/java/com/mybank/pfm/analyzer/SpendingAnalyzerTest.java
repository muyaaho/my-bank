package com.mybank.pfm.analyzer;

import com.mybank.pfm.model.ConsumptionAnalysis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SpendingAnalyzer Tests")
class SpendingAnalyzerTest {

    private SpendingAnalyzer analyzer;
    private List<ConsumptionAnalysis> historicalData;

    @BeforeEach
    void setUp() {
        analyzer = new SpendingAnalyzer();

        // Historical data with normal spending patterns
        historicalData = Arrays.asList(
                createConsumption("1", "FOOD", new BigDecimal("10000")),
                createConsumption("2", "FOOD", new BigDecimal("12000")),
                createConsumption("3", "FOOD", new BigDecimal("11000")),
                createConsumption("4", "FOOD", new BigDecimal("9000")),
                createConsumption("5", "FOOD", new BigDecimal("10500"))
        );
    }

    @Test
    @DisplayName("Should detect anomalous high spending")
    void shouldDetectAnomalousHighSpending() {
        // Given
        ConsumptionAnalysis newTransaction = createConsumption("6", "FOOD", new BigDecimal("50000"));

        // When
        boolean isAnomalous = analyzer.isAnomalous(newTransaction, historicalData);

        // Then
        assertThat(isAnomalous).isTrue();
    }

    @Test
    @DisplayName("Should not detect normal spending as anomalous")
    void shouldNotDetectNormalSpendingAsAnomalous() {
        // Given
        ConsumptionAnalysis newTransaction = createConsumption("6", "FOOD", new BigDecimal("10500"));

        // When
        boolean isAnomalous = analyzer.isAnomalous(newTransaction, historicalData);

        // Then
        assertThat(isAnomalous).isFalse();
    }

    @Test
    @DisplayName("Should calculate average spending correctly")
    void shouldCalculateAverageSpendingCorrectly() {
        // Given
        String category = "FOOD";

        // When
        BigDecimal average = analyzer.calculateAverageSpending(historicalData, category);

        // Then
        // Average should be (10000 + 12000 + 11000 + 9000 + 10500) / 5 = 10500
        assertThat(average).isEqualByComparingTo(new BigDecimal("10500"));
    }

    private ConsumptionAnalysis createConsumption(String id, String category, BigDecimal amount) {
        return ConsumptionAnalysis.builder()
                .id(id)
                .userId("user-123")
                .transactionId("txn-" + id)
                .category(category)
                .amount(amount)
                .merchantName("Merchant")
                .transactionDate(LocalDateTime.now())
                .build();
    }
}
