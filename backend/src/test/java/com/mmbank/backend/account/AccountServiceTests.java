package com.mmbank.backend.account;

import com.mmbank.backend.ledger.LedgerAccount;
import com.mmbank.backend.ledger.LedgerAccountType;
import com.mmbank.backend.ledger.LedgerEntry;
import com.mmbank.backend.ledger.repositories.LedgerAccountRepository;
import com.mmbank.backend.ledger.LedgerEntryType;
import com.mmbank.backend.ledger.repositories.LedgerEntryRepository;
import com.mmbank.backend.ledger.repositories.FinancialTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AccountServiceTests {

    @Autowired
    private AccountService accountService;

    @Autowired
    private BankAccountRepository accountRepository;

    @Autowired
    private BankTransactionRepository transactionRepository;

    @Autowired
    private LedgerAccountRepository ledgerAccountRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private FinancialTransactionRepository financialTransactionRepository;

    @BeforeEach
    void setUp() {
        ledgerEntryRepository.deleteAll();
        financialTransactionRepository.deleteAll();
        transactionRepository.deleteAll();
        accountRepository.deleteAll();

        accountService.createAccount(new CreateAccountRequest(
                "TEST1001",
                "Test Sender",
                AccountType.SAVINGS,
                new BigDecimal("100.00")
        ));

        accountService.createAccount(new CreateAccountRequest(
                "TEST1002",
                "Test Recipient",
                AccountType.SAVINGS,
                new BigDecimal("50.00")
        ));
    }

    @Test
    @Transactional
    void accountOpeningShouldPostInitialDepositsToLedger() {
        assertEquals(new BigDecimal("100.00"),
                accountService.findAccount("TEST1001").getBalance());
        assertEquals(new BigDecimal("50.00"),
                accountService.findAccount("TEST1002").getBalance());

        assertEquals(2, financialTransactionRepository.count());
        assertEquals(4, ledgerEntryRepository.count());

        assertTrue(financialTransactionRepository
                .findByIdempotencyKey("ACCOUNT-OPENING-TEST1001").isPresent());
        assertTrue(financialTransactionRepository
                .findByIdempotencyKey("ACCOUNT-OPENING-TEST1002").isPresent());

        assertOpeningPosting("TEST1001", new BigDecimal("100.00"));
        assertOpeningPosting("TEST1002", new BigDecimal("50.00"));
    }

    @Test
    void zeroBalanceAccountShouldNotCreateOpeningLedgerTransaction() {
        accountService.createAccount(new CreateAccountRequest(
                "TEST1003",
                "Zero Balance Customer",
                AccountType.SAVINGS,
                BigDecimal.ZERO
        ));

        assertEquals(0, BigDecimal.ZERO.compareTo(
                accountService.findAccount("TEST1003").getBalance()));
        assertEquals(2, financialTransactionRepository.count());
        assertEquals(4, ledgerEntryRepository.count());
    }

    @Test
    void failedOpeningLedgerPostingShouldRollbackAccountCreation() {
        ledgerEntryRepository.deleteAll();
        financialTransactionRepository.deleteAll();

        LedgerAccount depositsAccount = ledgerAccountRepository
                .findByAccountCode("CUSTOMER-DEPOSITS")
                .orElseThrow();

        ledgerAccountRepository.delete(depositsAccount);

        try {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> accountService.createAccount(new CreateAccountRequest(
                            "TEST1003",
                            "Opening Rollback Customer",
                            AccountType.SAVINGS,
                            new BigDecimal("75.00")
                    ))
            );

            assertTrue(accountRepository.findByAccountNumber("TEST1003").isEmpty());
            assertEquals(0, financialTransactionRepository.count());
            assertEquals(0, ledgerEntryRepository.count());
        } finally {
            ledgerAccountRepository.save(new LedgerAccount(
                    "CUSTOMER-DEPOSITS",
                    "Customer Deposits",
                    LedgerAccountType.LIABILITY,
                    true
            ));
        }
    }

    @Test
    void depositShouldIncreaseBalanceAndPostLedgerEntries() {
        BigDecimal amount = new BigDecimal("25.00");

        accountService.deposit("TEST1001", new DepositRequest(amount));

        assertEquals(
                new BigDecimal("125.00"),
                accountService.findAccount("TEST1001").getBalance()
        );
        assertEquals(1, transactionRepository.count());
        assertLedgerPosting(amount);
    }

    @Test
    void withdrawalShouldDecreaseBalanceAndPostLedgerEntries() {
        BigDecimal amount = new BigDecimal("30.00");

        accountService.withdraw("TEST1001", new WithdrawalRequest(amount));

        assertEquals(
                new BigDecimal("70.00"),
                accountService.findAccount("TEST1001").getBalance()
        );
        assertEquals(1, transactionRepository.count());
        assertLedgerPosting(amount);
    }

    @Test
    void withdrawalShouldFailWhenFundsAreInsufficient() {
        assertThrows(
                ResponseStatusException.class,
                () -> accountService.withdraw(
                        "TEST1001",
                        new WithdrawalRequest(new BigDecimal("150.00"))
                )
        );

        assertEquals(
                new BigDecimal("100.00"),
                accountService.findAccount("TEST1001").getBalance()
        );
        assertEquals(0, transactionRepository.count());
        assertEquals(2, financialTransactionRepository.count());
        assertEquals(4, ledgerEntryRepository.count());
    }

    @Test
    void transferShouldDebitSenderCreditRecipientAndPostLedgerEntries() {
        BigDecimal amount = new BigDecimal("40.00");

        accountService.transfer(
                "TEST1001",
                new TransferRequest("TEST1002", amount)
        );

        assertEquals(
                new BigDecimal("60.00"),
                accountService.findAccount("TEST1001").getBalance()
        );
        assertEquals(
                new BigDecimal("90.00"),
                accountService.findAccount("TEST1002").getBalance()
        );
        assertEquals(2, transactionRepository.count());
        assertLedgerPosting(amount);
    }

    @Test
    void failedTransferShouldLeaveBalancesAndLedgerUnchanged() {
        assertThrows(
                ResponseStatusException.class,
                () -> accountService.transfer(
                        "TEST1001",
                        new TransferRequest("TEST1002", new BigDecimal("150.00"))
                )
        );

        assertEquals(
                new BigDecimal("100.00"),
                accountService.findAccount("TEST1001").getBalance()
        );
        assertEquals(
                new BigDecimal("50.00"),
                accountService.findAccount("TEST1002").getBalance()
        );
        assertEquals(0, transactionRepository.count());
        assertEquals(2, financialTransactionRepository.count());
        assertEquals(4, ledgerEntryRepository.count());
    }


    @Test
    void failedLedgerPostingShouldRollbackEntireDeposit() {
        ledgerEntryRepository.deleteAll();
        financialTransactionRepository.deleteAll();

        LedgerAccount depositsAccount = ledgerAccountRepository
                .findByAccountCode("CUSTOMER-DEPOSITS")
                .orElseThrow();

        ledgerAccountRepository.delete(depositsAccount);

        try {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> accountService.deposit(
                            "TEST1001",
                            new DepositRequest(new BigDecimal("25.00"))
                    )
            );

            assertEquals(
                    new BigDecimal("100.00"),
                    accountService.findAccount("TEST1001").getBalance()
            );
            assertEquals(0, transactionRepository.count());
            assertEquals(0, financialTransactionRepository.count());
            assertEquals(0, ledgerEntryRepository.count());
        } finally {
            ledgerAccountRepository.save(new LedgerAccount(
                    "CUSTOMER-DEPOSITS",
                    "Customer Deposits",
                    LedgerAccountType.LIABILITY,
                    true
            ));
        }
    }

    private void assertOpeningPosting(String accountNumber, BigDecimal amount) {
        List<LedgerEntry> entries = ledgerEntryRepository.findAll();

        LedgerEntry cashDebit = entries.stream()
                .filter(entry -> entry.getAmount().compareTo(amount) == 0)
                .filter(entry -> entry.getEntryType() == LedgerEntryType.DEBIT)
                .filter(entry -> entry.getLedgerAccount().getAccountCode().equals("BANK-CASH"))
                .findFirst()
                .orElseThrow();

        LedgerEntry customerCredit = entries.stream()
                .filter(entry -> entry.getAmount().compareTo(amount) == 0)
                .filter(entry -> entry.getEntryType() == LedgerEntryType.CREDIT)
                .filter(entry -> entry.getLedgerAccount().getAccountCode().equals("CUSTOMER-DEPOSITS"))
                .filter(entry -> entry.getBankAccount() != null)
                .filter(entry -> entry.getBankAccount().getAccountNumber().equals(accountNumber))
                .findFirst()
                .orElseThrow();

        assertNull(cashDebit.getBankAccount());
        assertEquals(accountNumber, customerCredit.getBankAccount().getAccountNumber());
    }

    private void assertLedgerPosting(BigDecimal amount) {
        List<LedgerEntry> entries = ledgerEntryRepository.findAll();
        List<LedgerEntry> transactionEntries = entries.stream()
                .filter(entry -> entry.getAmount().compareTo(amount) == 0)
                .toList();

        assertEquals(6, entries.size());
        assertEquals(3, financialTransactionRepository.count());
        assertEquals(2, transactionEntries.size());

        assertEquals(1L, transactionEntries.stream()
                .filter(entry -> entry.getEntryType() == LedgerEntryType.DEBIT)
                .count());

        assertEquals(1L, transactionEntries.stream()
                .filter(entry -> entry.getEntryType() == LedgerEntryType.CREDIT)
                .count());
    }


    @Test
    void frozenAccountShouldRejectDepositsAndWithdrawals() {
        accountService.freezeAccount("TEST1001");

        assertEquals(AccountStatus.FROZEN,
                accountService.findAccount("TEST1001").getStatus());

        assertThrows(ResponseStatusException.class,
                () -> accountService.deposit("TEST1001",
                        new DepositRequest(new BigDecimal("10.00"))));

        assertThrows(ResponseStatusException.class,
                () -> accountService.withdraw("TEST1001",
                        new WithdrawalRequest(new BigDecimal("10.00"))));

        assertEquals(new BigDecimal("100.00"),
                accountService.findAccount("TEST1001").getBalance());
    }

    @Test
    void suspendedAccountShouldRejectTransfers() {
        accountService.suspendAccount("TEST1001");

        assertThrows(ResponseStatusException.class,
                () -> accountService.transfer("TEST1001",
                        new TransferRequest("TEST1002",
                                new BigDecimal("10.00"))));

        accountService.activateAccount("TEST1001");
        accountService.suspendAccount("TEST1002");

        assertThrows(ResponseStatusException.class,
                () -> accountService.transfer("TEST1001",
                        new TransferRequest("TEST1002",
                                new BigDecimal("10.00"))));

        assertEquals(new BigDecimal("100.00"),
                accountService.findAccount("TEST1001").getBalance());
        assertEquals(new BigDecimal("50.00"),
                accountService.findAccount("TEST1002").getBalance());
    }

    @Test
    void reactivatedAccountShouldAllowTransactions() {
        accountService.freezeAccount("TEST1001");
        accountService.activateAccount("TEST1001");

        assertEquals(AccountStatus.ACTIVE,
                accountService.findAccount("TEST1001").getStatus());

        accountService.deposit("TEST1001",
                new DepositRequest(new BigDecimal("25.00")));

        assertEquals(new BigDecimal("125.00"),
                accountService.findAccount("TEST1001").getBalance());
    }

    @Test
    void accountCannotCloseWithOutstandingBalance() {
        assertThrows(IllegalStateException.class,
                () -> accountService.closeAccount("TEST1001"));

        assertEquals(AccountStatus.ACTIVE,
                accountService.findAccount("TEST1001").getStatus());

        assertEquals(new BigDecimal("100.00"),
                accountService.findAccount("TEST1001").getBalance());
    }

    @Test
    void zeroBalanceAccountCanClosePermanently() {
        accountService.withdraw("TEST1001",
                new WithdrawalRequest(new BigDecimal("100.00")));

        BankAccount closed =
                accountService.closeAccount("TEST1001");

        assertEquals(AccountStatus.CLOSED, closed.getStatus());
        assertEquals(new BigDecimal("0.00"), closed.getBalance());

        assertThrows(IllegalStateException.class,
                () -> accountService.activateAccount("TEST1001"));

        assertThrows(ResponseStatusException.class,
                () -> accountService.deposit("TEST1001",
                        new DepositRequest(new BigDecimal("10.00"))));
    }
}
