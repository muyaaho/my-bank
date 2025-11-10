package com.mybank.asset.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Asset summary response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetSummaryResponse {

    private BigDecimal totalBalance;
    private String currency;
    private List<AssetDetail> assets;
    private List<CategorySummary> categoryBreakdown;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssetDetail {
        private String id;
        private String assetType;
        private String institutionName;
        private String accountName;
        private BigDecimal balance;
        private BigDecimal currentValue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySummary {
        private String assetType;
        private BigDecimal totalValue;
        private int count;
    }
}
