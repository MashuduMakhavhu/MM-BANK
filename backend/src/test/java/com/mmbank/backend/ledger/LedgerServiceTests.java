package com.mmbank.backend.ledger;

import com.mmbank.backend.account.*;
import com.mmbank.backend.ledger.repositories.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LedgerServiceTests {

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private LedgerAccountRepository ledgerAccountRepository;

    @Autowired
    private FinancialTransactionRepository financialTransactionRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        ledgerEntryRepository.deleteAll();
        financialTransactionRepository.deleteAll();
        ledgerAccountRepository.deleteAll();
        bankAccountRepository.findByAccountNumber("LEDGER1001").ifPresent(bankAccountRepository::delete);
    }

    private void createLedgerAccounts() {
        ledgerAccountRepository.save(
                new LedgerAccount(
                        "TEST-CASH",
                        "Test Bank Cash",
                        LedgerAccountType.ASSET
                )
        );

        ledgerAccountRepository.save(
                new LedgerAccount(
                        "TEST-DEPOSITS",
                        "Test Customer Deposits",
                        LedgerAccountType.LIABILITY,
                        true
                )
        );
    }

    private void createBankAccount() {
        bankAccountRepository.save(
                new BankAccount(
                        "LEDGER1001",
                        "Ledger Test Customer",
                        AccountType.SAVINGS,
                        new BigDecimal("100.00")
                )
        );
    }

    private List<LedgerEntryRequest> validEntries() {
        return List.of(
                new LedgerEntryRequest(
                        "TEST-CASH",
                        null,
                        LedgerEntryType.DEBIT,
                        new BigDecimal("100.00")
                ),
                new LedgerEntryRequest(
                        "TEST-DEPOSITS",
                        "LEDGER1001",
                        LedgerEntryType.CREDIT,
                        new BigDecimal("100.00")
                )
        );
    }

    @Test
    void balancedTransactionShouldPostSuccessfully() {
        createLedgerAccounts();
        createBankAccount();

        FinancialTransaction transaction =
                ledgerService.postTransaction(
                        "Test customer deposit",
                        validEntries()
                );

        assertEquals(
                FinancialTransactionStatus.POSTED,
                transaction.getStatus()
        );

        assertEquals(1, financialTransactionRepository.count());
        assertEquals(2, ledgerEntryRepository.count());

        List<LedgerEntry> entries =
                ledgerEntryRepository.findByFinancialTransaction(transaction);

        assertTrue(entries.stream().anyMatch(entry -> entry.getBankAccount() != null));
    }

    @Test
    void unbalancedTransactionShouldBeRejected() {
        createLedgerAccounts();
        createBankAccount();

        List<LedgerEntryRequest> entries = List.of(
                new LedgerEntryRequest(
                        "TEST-CASH",
                        null,
                        LedgerEntryType.DEBIT,
                        new BigDecimal("100.00")
                ),
                new LedgerEntryRequest(
                        "TEST-DEPOSITS",
                        "LEDGER1001",
                        LedgerEntryType.CREDIT,
                        new BigDecimal("80.00")
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> ledgerService.postTransaction(
                        "Unbalanced transaction",
                        entries
                )
        );

        assertEquals(0, financialTransactionRepository.count());
        assertEquals(0, ledgerEntryRepository.count());
    }

    @Test
    void inactiveLedgerAccountShouldBeRejected() {
        createLedgerAccounts();
        createBankAccount();

        LedgerAccount cash =
                ledgerAccountRepository.findByAccountCode("TEST-CASH")
                        .orElseThrow();

        cash.deactivate();
        ledgerAccountRepository.save(cash);

        assertThrows(
                IllegalArgumentException.class,
                () -> ledgerService.postTransaction(
                        "Inactive account test",
                        validEntries()
                )
        );

        assertEquals(0, financialTransactionRepository.count());
        assertEquals(0, ledgerEntryRepository.count());
    }

    @Test
    void customerDepositEntryMustReferenceBankAccount() {
        createLedgerAccounts();

        List<LedgerEntryRequest> entries = List.of(
                new LedgerEntryRequest(
                        "TEST-CASH",
                        null,
                        LedgerEntryType.DEBIT,
                        new BigDecimal("100.00")
                ),
                new LedgerEntryRequest(
                        "TEST-DEPOSITS",
                        null,
                        LedgerEntryType.CREDIT,
                        new BigDecimal("100.00")
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> ledgerService.postTransaction(
                        "Missing customer account",
                        entries
                )
        );

        assertEquals(0, financialTransactionRepository.count());
        assertEquals(0, ledgerEntryRepository.count());
    }

    @Test
    void failedPostingShouldNotLeavePartialRecords() {
        createLedgerAccounts();

        List<LedgerEntryRequest> entries = List.of(
                new LedgerEntryRequest(
                        "TEST-CASH",
                        null,
                        LedgerEntryType.DEBIT,
                        new BigDecimal("100.00")
                ),
                new LedgerEntryRequest(
                        "TEST-DEPOSITS",
                        "DOES-NOT-EXIST",
                        LedgerEntryType.CREDIT,
                        new BigDecimal("100.00")
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> ledgerService.postTransaction(
                        "Invalid customer account",
                        entries
                )
        );

        assertEquals(0, financialTransactionRepository.count());
        assertEquals(0, ledgerEntryRepository.count());
    }


    @Test
    void duplicateRequestShouldReturnOriginalTransaction() {
        createLedgerAccounts();
        createBankAccount();

        FinancialTransaction first = ledgerService.postTransaction(
                "Customer deposit",
                "DEPOSIT-001",
                validEntries()
        );

        FinancialTransaction second = ledgerService.postTransaction(
                "Customer deposit",
                "DEPOSIT-001",
                validEntries()
        );

        assertEquals(first.getId(), second.getId());
        assertEquals(1, financialTransactionRepository.count());
        assertEquals(2, ledgerEntryRepository.count());
    }

    @Test
    void reusedKeyWithDifferentAmountShouldBeRejected() {
        createLedgerAccounts();
        createBankAccount();

        ledgerService.postTransaction(
                "Customer deposit",
                "DEPOSIT-002",
                validEntries()
        );

        List<LedgerEntryRequest> differentEntries = List.of(
                new LedgerEntryRequest(
                        "TEST-CASH",
                        null,
                        LedgerEntryType.DEBIT,
                        new BigDecimal("200.00")
                ),
                new LedgerEntryRequest(
                        "TEST-DEPOSITS",
                        "LEDGER1001",
                        LedgerEntryType.CREDIT,
                        new BigDecimal("200.00")
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> ledgerService.postTransaction(
                        "Customer deposit",
                        "DEPOSIT-002",
                        differentEntries
                )
        );

        assertEquals(1, financialTransactionRepository.count());
        assertEquals(2, ledgerEntryRepository.count());
    }

    @Test
    void reusedKeyWithDifferentAccountShouldBeRejected() {
        createLedgerAccounts();
        createBankAccount();

        ledgerAccountRepository.save(
                new LedgerAccount(
                        "TEST-CASH-2",
                        "Alternative Test Cash",
                        LedgerAccountType.ASSET
                )
        );

        ledgerService.postTransaction(
                "Customer deposit",
                "DEPOSIT-003",
                validEntries()
        );

        List<LedgerEntryRequest> differentEntries = List.of(
                new LedgerEntryRequest(
                        "TEST-CASH-2",
                        null,
                        LedgerEntryType.DEBIT,
                        new BigDecimal("100.00")
                ),
                new LedgerEntryRequest(
                        "TEST-DEPOSITS",
                        "LEDGER1001",
                        LedgerEntryType.CREDIT,
                        new BigDecimal("100.00")
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> ledgerService.postTransaction(
                        "Customer deposit",
                        "DEPOSIT-003",
                        differentEntries
                )
        );

        assertEquals(1, financialTransactionRepository.count());
        assertEquals(2, ledgerEntryRepository.count());
    }

    @Test
    void differentKeysShouldCreateSeparateTransactions() {
        createLedgerAccounts();
        createBankAccount();

        ledgerService.postTransaction(
                "Customer deposit",
                "DEPOSIT-004",
                validEntries()
        );

        ledgerService.postTransaction(
                "Customer deposit",
                "DEPOSIT-005",
                validEntries()
        );

        assertEquals(2, financialTransactionRepository.count());
        assertEquals(4, ledgerEntryRepository.count());
    }


    @Test
    void concurrentDuplicateRequestsShouldReturnSameTransaction() throws Exception {
        createLedgerAccounts();
        createBankAccount();

        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        var ready = new java.util.concurrent.CountDownLatch(2);
        var start = new java.util.concurrent.CountDownLatch(1);

        try {
            java.util.concurrent.Callable<FinancialTransaction> request = () -> {
                ready.countDown();
                if (!start.await(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Concurrent test timed out");
                }

                return ledgerService.postTransaction(
                        "Concurrent customer deposit",
                        "CONCURRENT-DEPOSIT-001",
                        validEntries()
                );
            };

            var first = executor.submit(request);
            var second = executor.submit(request);

            assertTrue(ready.await(5, java.util.concurrent.TimeUnit.SECONDS));
            start.countDown();

            FinancialTransaction result1 =
                    first.get(20, java.util.concurrent.TimeUnit.SECONDS);
            FinancialTransaction result2 =
                    second.get(20, java.util.concurrent.TimeUnit.SECONDS);

            assertEquals(result1.getId(), result2.getId());
            assertEquals(1, financialTransactionRepository.count());
            assertEquals(2, ledgerEntryRepository.count());

        } finally {
            executor.shutdownNow();
        }
    }

}
