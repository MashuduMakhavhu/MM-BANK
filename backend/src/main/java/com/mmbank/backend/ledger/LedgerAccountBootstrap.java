package com.mmbank.backend.ledger;

import com.mmbank.backend.ledger.repositories.LedgerAccountRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class LedgerAccountBootstrap implements ApplicationRunner {

    private final LedgerAccountRepository repository;

    public LedgerAccountBootstrap(LedgerAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!repository.existsByAccountCode("BANK-CASH")) {
            repository.save(new LedgerAccount(
                    "BANK-CASH",
                    "Bank Cash",
                    LedgerAccountType.ASSET
            ));
        }

        if (!repository.existsByAccountCode("CUSTOMER-DEPOSITS")) {
            repository.save(new LedgerAccount(
                    "CUSTOMER-DEPOSITS",
                    "Customer Deposits",
                    LedgerAccountType.LIABILITY,
                    true
            ));
        }
    }
}
