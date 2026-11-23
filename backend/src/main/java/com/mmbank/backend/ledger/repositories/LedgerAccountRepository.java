package com.mmbank.backend.ledger.repositories;

import com.mmbank.backend.ledger.LedgerAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LedgerAccountRepository
        extends JpaRepository<LedgerAccount, Long> {

    Optional<LedgerAccount> findByAccountCode(String accountCode);

    boolean existsByAccountCode(String accountCode);
}
