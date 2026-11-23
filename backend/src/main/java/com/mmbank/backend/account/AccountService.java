package com.mmbank.backend.account;

import com.mmbank.backend.ledger.LedgerEntryRequest;
import com.mmbank.backend.ledger.LedgerEntryType;
import com.mmbank.backend.ledger.LedgerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.math.BigDecimal;

@Service
public class AccountService {

    private final BankAccountRepository accountRepository;
    private final BankTransactionRepository transactionRepository;
    private final LedgerService ledgerService;

    public AccountService(
            BankAccountRepository accountRepository,
            BankTransactionRepository transactionRepository,
            LedgerService ledgerService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerService = ledgerService;
    }

    @Transactional
    public BankAccount createAccount(CreateAccountRequest request) {
        if (accountRepository.findByAccountNumber(
                request.accountNumber()).isPresent()) {
            throw new IllegalArgumentException("Account number already exists");
        }

        BankAccount account = new BankAccount(
                request.accountNumber(),
                request.accountHolderName(),
                request.accountType(),
                request.initialBalance()
        );

        BankAccount savedAccount = accountRepository.save(account);

        if (request.initialBalance().compareTo(BigDecimal.ZERO) > 0) {
            ledgerService.postTransaction(
                    "Account opening deposit",
                    "ACCOUNT-OPENING-" + savedAccount.getAccountNumber(),
                    List.of(
                            new LedgerEntryRequest(
                                    "BANK-CASH",
                                    null,
                                    LedgerEntryType.DEBIT,
                                    request.initialBalance()
                            ),
                            new LedgerEntryRequest(
                                    "CUSTOMER-DEPOSITS",
                                    savedAccount.getAccountNumber(),
                                    LedgerEntryType.CREDIT,
                                    request.initialBalance()
                            )
                    )
            );
        }

        return savedAccount;
    }

    public BankAccount findAccount(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() ->
                        new IllegalArgumentException("Account not found"));
    }

    public AccountBalanceResponse getBalance(String accountNumber) {
        BankAccount account = findAccount(accountNumber);

        return new AccountBalanceResponse(
                account.getAccountNumber(),
                account.getAccountHolderName(),
                account.getAccountType(),
                account.getBalance()
        );
    }

    @Transactional
    public TransactionResponse deposit(
            String accountNumber,
            DepositRequest request) {

        validateTransactionAmount(request == null ? null : request.amount());

        BankAccount account = accountRepository
                .findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() ->
                        new IllegalArgumentException("Account not found"));

        ensureAccountCanTransact(account);
        account.setBalance(account.getBalance().add(request.amount()));

        BankTransaction transaction = new BankTransaction(
                account,
                TransactionType.DEPOSIT,
                request.amount(),
                account.getBalance()
        );

        transactionRepository.save(transaction);

        ledgerService.postTransaction(
                "Customer deposit",
                "BANK-DEPOSIT-" + transaction.getId(),
                List.of(
                        new LedgerEntryRequest(
                                "BANK-CASH",
                                null,
                                LedgerEntryType.DEBIT,
                                request.amount()
                        ),
                        new LedgerEntryRequest(
                                "CUSTOMER-DEPOSITS",
                                account.getAccountNumber(),
                                LedgerEntryType.CREDIT,
                                request.amount()
                        )
                )
        );

        return toTransactionResponse(transaction);
    }

    @Transactional
    public TransactionResponse withdraw(
            String accountNumber,
            WithdrawalRequest request) {

        validateTransactionAmount(request == null ? null : request.amount());

        BankAccount account = accountRepository
                .findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() ->
                        new IllegalArgumentException("Account not found"));

        ensureAccountCanTransact(account);

        if (account.getBalance().compareTo(request.amount()) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Insufficient funds"
            );
        }

        account.setBalance(account.getBalance().subtract(request.amount()));

        BankTransaction transaction = new BankTransaction(
                account,
                TransactionType.WITHDRAWAL,
                request.amount(),
                account.getBalance()
        );

        transactionRepository.save(transaction);

        ledgerService.postTransaction(
                "Customer withdrawal",
                "BANK-WITHDRAWAL-" + transaction.getId(),
                List.of(
                        new LedgerEntryRequest(
                                "CUSTOMER-DEPOSITS",
                                account.getAccountNumber(),
                                LedgerEntryType.DEBIT,
                                request.amount()
                        ),
                        new LedgerEntryRequest(
                                "BANK-CASH",
                                null,
                                LedgerEntryType.CREDIT,
                                request.amount()
                        )
                )
        );

        return toTransactionResponse(transaction);
    }

    @Transactional
    public TransferResponse transfer(
            String senderNumber,
            TransferRequest request) {

        validateTransactionAmount(request == null ? null : request.amount());

        String recipientNumber = request.recipientAccountNumber();

        if (senderNumber.equals(recipientNumber)) {
            throw new IllegalArgumentException(
                    "Cannot transfer to the same account");
        }

        // Lock accounts in a consistent order to reduce deadlock risk.
        String firstNumber = senderNumber.compareTo(recipientNumber) < 0
                ? senderNumber : recipientNumber;

        String secondNumber = senderNumber.compareTo(recipientNumber) < 0
                ? recipientNumber : senderNumber;

        BankAccount firstAccount = accountRepository
                .findByAccountNumberForUpdate(firstNumber)
                .orElseThrow(() ->
                        new IllegalArgumentException("Account not found"));

        BankAccount secondAccount = accountRepository
                .findByAccountNumberForUpdate(secondNumber)
                .orElseThrow(() ->
                        new IllegalArgumentException("Account not found"));

        BankAccount sender = senderNumber.equals(firstNumber)
                ? firstAccount : secondAccount;

        BankAccount recipient = recipientNumber.equals(firstNumber)
                ? firstAccount : secondAccount;

        ensureAccountCanTransact(sender);
        ensureAccountCanTransact(recipient);

        if (sender.getBalance().compareTo(request.amount()) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Insufficient funds"
            );
        }

        sender.setBalance(sender.getBalance().subtract(request.amount()));
        recipient.setBalance(recipient.getBalance().add(request.amount()));

        BankTransaction sent = new BankTransaction(
                sender,
                TransactionType.TRANSFER_SENT,
                request.amount(),
                sender.getBalance()
        );

        BankTransaction received = new BankTransaction(
                recipient,
                TransactionType.TRANSFER_RECEIVED,
                request.amount(),
                recipient.getBalance()
        );

        transactionRepository.saveAll(List.of(sent, received));

        ledgerService.postTransaction(
                "Internal account transfer",
                "BANK-TRANSFER-" + sent.getId(),
                List.of(
                        new LedgerEntryRequest(
                                "CUSTOMER-DEPOSITS",
                                sender.getAccountNumber(),
                                LedgerEntryType.DEBIT,
                                request.amount()
                        ),
                        new LedgerEntryRequest(
                                "CUSTOMER-DEPOSITS",
                                recipient.getAccountNumber(),
                                LedgerEntryType.CREDIT,
                                request.amount()
                        )
                )
        );

        return new TransferResponse(
                senderNumber,
                recipientNumber,
                request.amount(),
                toTransactionResponse(sent),
                toTransactionResponse(received)
        );
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getRecentTransactions(
            String accountNumber) {

        findAccount(accountNumber);

        return transactionRepository
                .findTop10ByAccount_AccountNumberOrderByCreatedAtDesc(
                        accountNumber)
                .stream()
                .map(this::toTransactionResponse)
                .toList();
    }


    @Transactional
    public BankAccount freezeAccount(String accountNumber) {
        BankAccount account = lockAccount(accountNumber);
        account.freeze();
        return account;
    }

    @Transactional
    public BankAccount suspendAccount(String accountNumber) {
        BankAccount account = lockAccount(accountNumber);
        account.suspend();
        return account;
    }

    @Transactional
    public BankAccount activateAccount(String accountNumber) {
        BankAccount account = lockAccount(accountNumber);
        account.activate();
        return account;
    }

    @Transactional
    public BankAccount closeAccount(String accountNumber) {
        BankAccount account = lockAccount(accountNumber);
        account.close();
        return account;
    }

    private BankAccount lockAccount(String accountNumber) {
        return accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() ->
                        new IllegalArgumentException("Account not found"));
    }

    private void ensureAccountCanTransact(BankAccount account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Account is " + account.getStatus()
                            + " and cannot perform transactions");
        }
    }

    private void validateTransactionAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Transaction amount must be greater than zero");
        }

        if (amount.scale() > 2) {
            throw new IllegalArgumentException(
                    "Transaction amount cannot have more than two decimal places");
        }

        int integerDigits = amount.precision() - amount.scale();

        if (integerDigits > 13) {
            throw new IllegalArgumentException(
                    "Transaction amount exceeds the supported limit");
        }
    }

    private TransactionResponse toTransactionResponse(
            BankTransaction transaction) {

        return new TransactionResponse(
                transaction.getId(),
                transaction.getAccount().getAccountNumber(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getBalanceAfter(),
                transaction.getCreatedAt()
        );
    }
}
