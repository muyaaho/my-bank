package com.mybank.analytics.repository;

import com.mybank.analytics.model.SpendingAnalysis;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spending Analysis Repository
 */
@Repository
public interface SpendingAnalysisRepository extends MongoRepository<SpendingAnalysis, String> {

    List<SpendingAnalysis> findByUserIdAndTransactionDateBetween(
            String userId, LocalDateTime startDate, LocalDateTime endDate);

    List<SpendingAnalysis> findByUserIdAndIsAnomalous(String userId, boolean isAnomalous);

    List<SpendingAnalysis> findByUserIdAndCategory(String userId, String category);
}
