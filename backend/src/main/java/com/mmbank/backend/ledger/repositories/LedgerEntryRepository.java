package com.mmbank.backend.ledger.repositories;

import com.mmbank.backend.ledger.FinancialTransaction;
import com.mmbank.backend.ledger.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LedgerEntryRepository
        extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByFinancialTransaction(
            FinancialTransaction financialTransaction);

}
