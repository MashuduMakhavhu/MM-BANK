package com.mmbank.backend.account;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BankTransactionRepository
        extends JpaRepository<BankTransaction, Long> {

    List<BankTransaction>
    findTop10ByAccount_AccountNumberOrderByCreatedAtDesc(
            String accountNumber);
}
