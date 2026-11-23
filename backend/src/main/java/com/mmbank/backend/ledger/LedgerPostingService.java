package com.mmbank.backend.ledger;

import com.mmbank.backend.account.BankAccount;
import com.mmbank.backend.account.BankAccountRepository;
import com.mmbank.backend.ledger.repositories.FinancialTransactionRepository;
import com.mmbank.backend.ledger.repositories.LedgerAccountRepository;
import com.mmbank.backend.ledger.repositories.LedgerEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class LedgerPostingService {

    private static final BigDecimal MAX_AMOUNT =
            new BigDecimal("9999999999999.99");

    private final LedgerAccountRepository ledgerAccountRepository;
    private final FinancialTransactionRepository financialTransactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final BankAccountRepository bankAccountRepository;

    public LedgerPostingService(
            LedgerAccountRepository ledgerAccountRepository,
            FinancialTransactionRepository financialTransactionRepository,
            LedgerEntryRepository ledgerEntryRepository,
            BankAccountRepository bankAccountRepository) {

        this.ledgerAccountRepository = ledgerAccountRepository;
        this.financialTransactionRepository = financialTransactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.bankAccountRepository = bankAccountRepository;
    }

    @Transactional
    public FinancialTransaction postTransaction(
            String description,
            List<LedgerEntryRequest> requests) {

        return postTransactionInternal(description, null, requests);
    }

    @Transactional
    public FinancialTransaction postTransaction(
            String description,
            String idempotencyKey,
            List<LedgerEntryRequest> requests) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException(
                    "An idempotency key is required"
            );
        }

        String normalizedKey = idempotencyKey.trim();

        if (normalizedKey.length() > 100) {
            throw new IllegalArgumentException(
                    "Idempotency key cannot exceed 100 characters"
            );
        }

        return postTransactionInternal(
                description,
                normalizedKey,
                requests
        );
    }


    @Transactional(readOnly = true)
    public FinancialTransaction findMatchingTransaction(
            String description,
            String idempotencyKey,
            List<LedgerEntryRequest> requests) {

        validateTransaction(description, requests);

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }

        String normalizedKey = idempotencyKey.trim();

        if (normalizedKey.length() > 100) {
            throw new IllegalArgumentException(
                    "Idempotency key cannot exceed 100 characters"
            );
        }

        var existingTransaction =
                financialTransactionRepository.findByIdempotencyKey(
                        normalizedKey
                );

        if (existingTransaction.isEmpty()) {
            return null;
        }

        FinancialTransaction existing = existingTransaction.get();

        if (existing.getStatus() != FinancialTransactionStatus.POSTED
                || !existing.getDescription().equals(description.trim())
                || !matchesExistingEntries(existing, requests)) {

            throw new IllegalArgumentException(
                    "Idempotency key has already been used with different transaction details"
            );
        }

        return existing;
    }

    private FinancialTransaction postTransactionInternal(
            String description,
            String idempotencyKey,
            List<LedgerEntryRequest> requests) {

        validateTransaction(description, requests);

        String normalizedDescription = description.trim();

        if (idempotencyKey != null) {
            var existingTransaction =
                    financialTransactionRepository.findByIdempotencyKey(
                            idempotencyKey
                    );

            if (existingTransaction.isPresent()) {
                FinancialTransaction existing = existingTransaction.get();

                if (existing.getStatus() != FinancialTransactionStatus.POSTED
                        || !existing.getDescription().equals(normalizedDescription)
                        || !matchesExistingEntries(existing, requests)) {

                    throw new IllegalArgumentException(
                            "Idempotency key has already been used with different transaction details"
                    );
                }

                return existing;
            }
        }

        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (LedgerEntryRequest request : requests) {
            if (request.entryType() == LedgerEntryType.DEBIT) {
                totalDebits = totalDebits.add(request.amount());
            } else {
                totalCredits = totalCredits.add(request.amount());
            }
        }

        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new IllegalArgumentException(
                    "Transaction is not balanced: total debits must equal total credits"
            );
        }

        FinancialTransaction transaction =
                financialTransactionRepository.save(
                        new FinancialTransaction(
                                normalizedDescription,
                                idempotencyKey
                        )
                );

        List<LedgerEntry> entries = new ArrayList<>();

        for (LedgerEntryRequest request : requests) {

            String accountCode = request.ledgerAccountCode().trim();

            LedgerAccount ledgerAccount =
                    ledgerAccountRepository.findByAccountCode(
                            accountCode
                    ).orElseThrow(() -> new IllegalArgumentException(
                            "Ledger account not found: " + accountCode
                    ));

            if (!ledgerAccount.isActive()) {
                throw new IllegalArgumentException(
                        "Ledger account is inactive: "
                                + ledgerAccount.getAccountCode()
                );
            }

            BankAccount bankAccount = null;

            if (ledgerAccount.requiresBankAccount()) {

                if (request.bankAccountNumber() == null
                        || request.bankAccountNumber().isBlank()) {

                    throw new IllegalArgumentException(
                            "This ledger account requires a bank account reference"
                    );
                }

                bankAccount = bankAccountRepository.findByAccountNumber(
                        request.bankAccountNumber().trim()
                ).orElseThrow(() -> new IllegalArgumentException(
                        "Bank account not found: "
                                + request.bankAccountNumber()
                ));

            } else if (request.bankAccountNumber() != null
                    && !request.bankAccountNumber().isBlank()) {

                throw new IllegalArgumentException(
                        "This ledger account does not accept a bank account reference"
                );
            }

            entries.add(new LedgerEntry(
                    transaction,
                    ledgerAccount,
                    bankAccount,
                    request.entryType(),
                    request.amount()
            ));
        }

        ledgerEntryRepository.saveAll(entries);

        transaction.markAsPosted();

        return financialTransactionRepository.save(transaction);
    }

    private boolean matchesExistingEntries(
            FinancialTransaction transaction,
            List<LedgerEntryRequest> requests) {

        List<LedgerEntry> unmatchedEntries = new ArrayList<>(
                ledgerEntryRepository.findByFinancialTransaction(transaction)
        );

        if (unmatchedEntries.size() != requests.size()) {
            return false;
        }

        for (LedgerEntryRequest request : requests) {

            String requestedAccountCode =
                    request.ledgerAccountCode().trim();

            String requestedBankAccountNumber =
                    request.bankAccountNumber() == null
                            || request.bankAccountNumber().isBlank()
                            ? null
                            : request.bankAccountNumber().trim();

            int matchingIndex = -1;

            for (int i = 0; i < unmatchedEntries.size(); i++) {

                LedgerEntry entry = unmatchedEntries.get(i);

                String existingBankAccountNumber =
                        entry.getBankAccount() == null
                                ? null
                                : entry.getBankAccount().getAccountNumber();

                boolean sameAccount =
                        entry.getLedgerAccount().getAccountCode()
                                .equals(requestedAccountCode);

                boolean sameBankAccount =
                        java.util.Objects.equals(
                                existingBankAccountNumber,
                                requestedBankAccountNumber
                        );

                boolean sameType =
                        entry.getEntryType() == request.entryType();

                boolean sameAmount =
                        entry.getAmount().compareTo(request.amount()) == 0;

                if (sameAccount && sameBankAccount
                        && sameType && sameAmount) {

                    matchingIndex = i;
                    break;
                }
            }

            if (matchingIndex == -1) {
                return false;
            }

            unmatchedEntries.remove(matchingIndex);
        }

        return unmatchedEntries.isEmpty();
    }

    private void validateTransaction(
            String description,
            List<LedgerEntryRequest> requests) {

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException(
                    "Transaction description is required"
            );
        }

        if (description.trim().length() > 255) {
            throw new IllegalArgumentException(
                    "Transaction description cannot exceed 255 characters"
            );
        }

        if (requests == null || requests.size() < 2) {
            throw new IllegalArgumentException(
                    "A transaction must contain at least two entries"
            );
        }

        boolean hasDebit = false;
        boolean hasCredit = false;

        for (LedgerEntryRequest request : requests) {

            if (request == null
                    || request.ledgerAccountCode() == null
                    || request.ledgerAccountCode().isBlank()
                    || request.entryType() == null
                    || request.amount() == null) {

                throw new IllegalArgumentException(
                        "Every ledger entry must have an account code, type and amount"
                );
            }

            BigDecimal amount = request.amount();

            if (amount.signum() <= 0
                    || amount.scale() > 2
                    || amount.compareTo(MAX_AMOUNT) > 0) {

                throw new IllegalArgumentException(
                        "Entry amounts must be positive, have at most two decimal places and fit the ledger"
                );
            }

            if (request.entryType() == LedgerEntryType.DEBIT) {
                hasDebit = true;
            } else {
                hasCredit = true;
            }
        }

        if (!hasDebit || !hasCredit) {
            throw new IllegalArgumentException(
                    "A transaction must contain at least one debit and one credit"
            );
        }
    }
}
