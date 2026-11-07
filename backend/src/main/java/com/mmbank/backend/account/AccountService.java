package com.mmbank.backend.account;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AccountService {

    private final BankAccountRepository accountRepository;
    private final BankTransactionRepository transactionRepository;

    public AccountService(
            BankAccountRepository accountRepository,
            BankTransactionRepository transactionRepository) {

        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    public BankAccount createAccount(CreateAccountRequest request) {

        if (accountRepository.findByAccountNumber(
                request.accountNumber()).isPresent()) {

            throw new IllegalArgumentException(
                    "Account number already exists");
        }

        BankAccount account = new BankAccount(
                request.accountNumber(),
                request.accountHolderName(),
                request.accountType(),
                request.initialBalance()
        );

        return accountRepository.save(account);
    }

    public BankAccount findAccount(String accountNumber) {

        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Account not found"));
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

        BankAccount account = accountRepository
                .findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Account not found"));

        account.setBalance(
                account.getBalance().add(request.amount())
        );

        BankTransaction transaction = new BankTransaction(
                account,
                TransactionType.DEPOSIT,
                request.amount(),
                account.getBalance()
        );

        transactionRepository.save(transaction);

        return toTransactionResponse(transaction);
    }

    @Transactional
    public TransactionResponse withdraw(
            String accountNumber,
            WithdrawalRequest request) {

        BankAccount account = accountRepository
                .findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Account not found"));

        if (account.getBalance().compareTo(request.amount()) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Insufficient funds");
        }

        account.setBalance(
                account.getBalance().subtract(request.amount())
        );

        BankTransaction transaction = new BankTransaction(
                account,
                TransactionType.WITHDRAWAL,
                request.amount(),
                account.getBalance()
        );

        transactionRepository.save(transaction);

        return toTransactionResponse(transaction);
    }

    @Transactional
    public TransferResponse transfer(
            String senderNumber,
            TransferRequest request) {

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
                .orElseThrow(() -> new IllegalArgumentException(
                        "Account not found"));

        BankAccount secondAccount = accountRepository
                .findByAccountNumberForUpdate(secondNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Account not found"));

        BankAccount sender = senderNumber.equals(firstNumber)
                ? firstAccount : secondAccount;

        BankAccount recipient = recipientNumber.equals(firstNumber)
                ? firstAccount : secondAccount;

        if (sender.getBalance().compareTo(request.amount()) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Insufficient funds");
        }

        sender.setBalance(
                sender.getBalance().subtract(request.amount()));

        recipient.setBalance(
                recipient.getBalance().add(request.amount()));

        BankTransaction sent = new BankTransaction(
                sender,
                TransactionType.TRANSFER_SENT,
                request.amount(),
                sender.getBalance());

        BankTransaction received = new BankTransaction(
                recipient,
                TransactionType.TRANSFER_RECEIVED,
                request.amount(),
                recipient.getBalance());

        transactionRepository.saveAll(List.of(sent, received));

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
