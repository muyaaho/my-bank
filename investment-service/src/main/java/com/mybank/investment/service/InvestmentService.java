package com.mybank.investment.service;

import com.mybank.investment.dto.InvestmentSummaryResponse;
import com.mybank.investment.model.Investment;
import com.mybank.investment.model.InvestmentAccount;
import com.mybank.investment.repository.InvestmentAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Investment service
 * DDD-compliant: Works with Aggregate Roots only
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvestmentService {

    private final InvestmentAccountRepository investmentAccountRepository;

    public InvestmentSummaryResponse getInvestmentSummary(String userId) {
        log.info("Fetching investment summary for user: {}", userId);

        // Get all investment accounts for the user
        List<InvestmentAccount> accounts = investmentAccountRepository
                .findByUserIdAndIsActive(userId, true);

        // Aggregate data from all accounts
        BigDecimal totalInvested = accounts.stream()
                .map(InvestmentAccount::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRoundedUp = accounts.stream()
                .filter(account -> account.getRoundUpConfig() != null)
                .map(account -> account.getRoundUpConfig().getTotalRoundedUp())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalRoundUpTransactions = accounts.stream()
                .filter(account -> account.getRoundUpConfig() != null)
                .mapToInt(account -> account.getRoundUpConfig().getTotalTransactions())
                .sum();

        // Get recent investments from all accounts
        List<InvestmentSummaryResponse.InvestmentDetail> recentInvestments = accounts.stream()
                .flatMap(account -> account.getInvestments().stream())
                .sorted((a, b) -> b.getInvestedAt().compareTo(a.getInvestedAt()))
                .limit(10)
                .map(investment -> InvestmentSummaryResponse.InvestmentDetail.builder()
                        .investmentId(investment.getId())
                        .productName(investment.getProductName())
                        .investmentType(investment.getInvestmentType().name())
                        .amount(investment.getAmount())
                        .investedAt(investment.getInvestedAt().toString())
                        .build())
                .collect(Collectors.toList());

        return InvestmentSummaryResponse.builder()
                .totalInvested(totalInvested)
                .totalRoundedUp(totalRoundedUp)
                .totalRoundUpTransactions(totalRoundUpTransactions)
                .recentInvestments(recentInvestments)
                .build();
    }

    public void enableRoundUp(String userId, String accountId, String sourceAccountId, BigDecimal roundUpUnit) {
        InvestmentAccount account = investmentAccountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Investment account not found"));

        if (!account.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to investment account");
        }

        // Delegate to Aggregate Root
        account.enableRoundUp(sourceAccountId, roundUpUnit);

        // Save the entire aggregate
        investmentAccountRepository.save(account);
    }

    public void disableRoundUp(String userId, String accountId) {
        InvestmentAccount account = investmentAccountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Investment account not found"));

        if (!account.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to investment account");
        }

        // Delegate to Aggregate Root
        account.disableRoundUp();

        // Save the entire aggregate
        investmentAccountRepository.save(account);
    }
}
