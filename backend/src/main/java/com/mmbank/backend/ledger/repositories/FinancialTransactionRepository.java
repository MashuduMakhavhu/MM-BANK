package com.mmbank.backend.ledger.repositories;

import com.mmbank.backend.ledger.FinancialTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FinancialTransactionRepository
        extends JpaRepository<FinancialTransaction, Long> {

    Optional<FinancialTransaction> findByTransactionReference(
            String transactionReference
    );

    boolean existsByTransactionReference(String transactionReference);

    Optional<FinancialTransaction> findByIdempotencyKey(
            String idempotencyKey
    );
}
