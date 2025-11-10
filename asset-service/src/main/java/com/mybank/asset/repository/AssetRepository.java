package com.mybank.asset.repository;

import com.mybank.asset.model.Asset;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Asset Repository
 */
@Repository
public interface AssetRepository extends MongoRepository<Asset, String> {

    List<Asset> findByUserIdAndIsActive(String userId, boolean isActive);

    List<Asset> findByUserIdAndAssetType(String userId, Asset.AssetType assetType);
}
