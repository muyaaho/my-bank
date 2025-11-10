package com.mybank.asset.service;

import com.mybank.asset.dto.AssetSummaryResponse;
import com.mybank.asset.model.Asset;
import com.mybank.asset.repository.AssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Asset Service implementing Cache-Aside pattern with Redis
 * Bounded Context: Asset Management
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetRepository assetRepository;

    @Cacheable(value = "assets", key = "#userId")
    public AssetSummaryResponse getAssetSummary(String userId) {
        log.info("Fetching asset summary for user: {}", userId);

        List<Asset> assets = assetRepository.findByUserIdAndIsActive(userId, true);

        BigDecimal totalBalance = assets.stream()
                .map(asset -> asset.getBalance() != null ? asset.getBalance() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<AssetSummaryResponse.AssetDetail> assetDetails = assets.stream()
                .map(asset -> AssetSummaryResponse.AssetDetail.builder()
                        .id(asset.getId())
                        .assetType(asset.getAssetType().name())
                        .institutionName(asset.getInstitutionName())
                        .accountName(asset.getAccountName())
                        .balance(asset.getBalance())
                        .currentValue(asset.getCurrentValue())
                        .build())
                .collect(Collectors.toList());

        Map<Asset.AssetType, List<Asset>> assetsByType = assets.stream()
                .collect(Collectors.groupingBy(Asset::getAssetType));

        List<AssetSummaryResponse.CategorySummary> categoryBreakdown = assetsByType.entrySet().stream()
                .map(entry -> {
                    BigDecimal categoryTotal = entry.getValue().stream()
                            .map(asset -> asset.getBalance() != null ? asset.getBalance() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return AssetSummaryResponse.CategorySummary.builder()
                            .assetType(entry.getKey().name())
                            .totalValue(categoryTotal)
                            .count(entry.getValue().size())
                            .build();
                })
                .collect(Collectors.toList());

        return AssetSummaryResponse.builder()
                .totalBalance(totalBalance)
                .currency("KRW")
                .assets(assetDetails)
                .categoryBreakdown(categoryBreakdown)
                .build();
    }

    @CacheEvict(value = "assets", key = "#userId")
    public void invalidateAssetCache(String userId) {
        log.info("Invalidating asset cache for user: {}", userId);
    }

    public Asset saveAsset(Asset asset) {
        Asset saved = assetRepository.save(asset);
        invalidateAssetCache(asset.getUserId());
        return saved;
    }
}
