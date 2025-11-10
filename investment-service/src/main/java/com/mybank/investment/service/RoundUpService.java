package com.mybank.investment.service;

import com.mybank.common.event.PaymentCompletedEvent;
import com.mybank.investment.model.Investment;
import com.mybank.investment.model.InvestmentAccount;
import com.mybank.investment.repository.InvestmentAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Round-up investing service implementing EDA pattern
 * Automatically invests spare change from payments
 *
 * DDD-compliant: Delegates business logic to Aggregate Root
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoundUpService {

    private final InvestmentAccountRepository investmentAccountRepository;

    public void processRoundUp(PaymentCompletedEvent event) {
        log.info("Processing round-up for payment: {}", event.getPaymentId());

        // Find investment accounts with round-up enabled for this payment source
        List<InvestmentAccount> accounts = investmentAccountRepository
                .findByUserIdAndIsActive(event.getUserId(), true);

        for (InvestmentAccount account : accounts) {
            try {
                // Check if this account has round-up enabled for the payment account
                if (account.getRoundUpConfig() == null || !account.getRoundUpConfig().isEnabled()) {
                    continue;
                }

                if (!event.getAccountId().equals(account.getRoundUpConfig().getSourceAccountId())) {
                    continue;
                }

                log.info("Processing round-up for account: {}", account.getId());

                // Delegate to Aggregate Root
                Investment investment = account.processRoundUp(
                        event.getPaymentId(),
                        event.getAmount(),
                        event.getCurrency()
                );

                if (investment != null) {
                    // Save the entire aggregate (single transaction)
                    investmentAccountRepository.save(account);
                    log.info("Round-up investment completed: {} {}", investment.getAmount(), investment.getCurrency());
                } else {
                    log.info("No round-up needed for amount: {}", event.getAmount());
                }

            } catch (IllegalStateException e) {
                log.warn("Round-up failed for account {}: {}", account.getId(), e.getMessage());
            } catch (Exception e) {
                log.error("Unexpected error processing round-up for account {}", account.getId(), e);
            }
        }
    }
}
