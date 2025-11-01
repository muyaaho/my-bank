package com.mybank.pfm.service;

import com.mybank.pfm.dto.AssetSummaryResponse;
import com.mybank.pfm.model.Asset;
import com.mybank.pfm.model.AssetType;
import com.mybank.pfm.repository.AssetRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssetService Tests")
class AssetServiceTest {

    @Mock
    private AssetRepository assetRepository;

    @InjectMocks
    private AssetService assetService;

    private List<Asset> testAssets;

    @BeforeEach
    void setUp() {
        testAssets = Arrays.asList(
                createAsset("asset-1", "user-123", AssetType.BANK, "KB Bank", new BigDecimal("1000000")),
                createAsset("asset-2", "user-123", AssetType.CARD, "Samsung Card", new BigDecimal("500000")),
                createAsset("asset-3", "user-123", AssetType.SECURITIES, "Mirae Asset", new BigDecimal("2000000"))
        );
    }

    @Test
    @DisplayName("Should get asset summary successfully")
    void shouldGetAssetSummarySuccessfully() {
        // Given
        when(assetRepository.findByUserId("user-123")).thenReturn(testAssets);

        // When
        AssetSummaryResponse response = assetService.getAssetSummary("user-123");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTotalBalance()).isEqualByComparingTo(new BigDecimal("3500000"));
        assertThat(response.getAssets()).hasSize(3);
        assertThat(response.getCategoryBreakdown()).hasSize(3);
    }

    @Test
    @DisplayName("Should calculate category breakdown correctly")
    void shouldCalculateCategoryBreakdownCorrectly() {
        // Given
        when(assetRepository.findByUserId("user-123")).thenReturn(testAssets);

        // When
        AssetSummaryResponse response = assetService.getAssetSummary("user-123");

        // Then
        assertThat(response.getCategoryBreakdown())
                .anyMatch(cb -> cb.getAssetType() == AssetType.BANK
                    && cb.getTotalValue().compareTo(new BigDecimal("1000000")) == 0)
                .anyMatch(cb -> cb.getAssetType() == AssetType.CARD
                    && cb.getTotalValue().compareTo(new BigDecimal("500000")) == 0)
                .anyMatch(cb -> cb.getAssetType() == AssetType.SECURITIES
                    && cb.getTotalValue().compareTo(new BigDecimal("2000000")) == 0);
    }

    private Asset createAsset(String id, String userId, AssetType type, String institution, BigDecimal balance) {
        return Asset.builder()
                .id(id)
                .userId(userId)
                .assetType(type)
                .institutionName(institution)
                .accountName("Account")
                .balance(balance)
                .currentValue(balance)
                .currency("KRW")
                .build();
    }
}
