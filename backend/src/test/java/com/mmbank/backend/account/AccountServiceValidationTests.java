package com.mmbank.backend.account;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AccountServiceValidationTests {

    @Autowired
    private AccountService accountService;

    @Autowired
    private BankAccountRepository accountRepository;

    @Autowired
    private BankTransactionRepository transactionRepository;

    
    @org.springframework.beans.factory.annotation.Autowired
    private com.mmbank.backend.ledger.repositories.LedgerEntryRepository ledgerEntryRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private com.mmbank.backend.ledger.repositories.FinancialTransactionRepository financialTransactionRepository;

    @BeforeEach
    void setUp() {
        ledgerEntryRepository.deleteAll();
        financialTransactionRepository.deleteAll();

        transactionRepository.deleteAll();
        accountRepository.deleteAll();

        accountService.createAccount(new CreateAccountRequest(
                "VALID1001",
                "Validation Sender",
                AccountType.SAVINGS,
                new BigDecimal("100.00")
        ));

        accountService.createAccount(new CreateAccountRequest(
                "VALID1002",
                "Validation Recipient",
                AccountType.SAVINGS,
                new BigDecimal("50.00")
        ));
    }

    @Test
    void depositShouldRejectZeroAmount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> accountService.deposit(
                        "VALID1001",
                        new DepositRequest(BigDecimal.ZERO)
                )
        );
    }

    @Test
    void withdrawalShouldRejectNegativeAmount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> accountService.withdraw(
                        "VALID1001",
                        new WithdrawalRequest(new BigDecimal("-10.00"))
                )
        );
    }

    @Test
    void transferShouldRejectAmountWithMoreThanTwoDecimalPlaces() {
        assertThrows(
                IllegalArgumentException.class,
                () -> accountService.transfer(
                        "VALID1001",
                        new TransferRequest("VALID1002", new BigDecimal("10.001"))
                )
        );
    }

    @Test
    void depositShouldRejectAmountExceedingSupportedPrecision() {
        assertThrows(
                IllegalArgumentException.class,
                () -> accountService.deposit(
                        "VALID1001",
                        new DepositRequest(new BigDecimal("10000000000000.00"))
                )
        );
    }
}
