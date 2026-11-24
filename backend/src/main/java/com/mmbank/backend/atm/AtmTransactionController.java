package com.mmbank.backend.atm;

import com.mmbank.backend.account.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/atm")
public class AtmTransactionController {

    private final AccountService accountService;

    public AtmTransactionController(AccountService accountService) {
        this.accountService = accountService;
    }

    private String authenticatedAccount(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "ATM session expired. Please authenticate again.");
        }

        Object account = session.getAttribute("ATM_ACCOUNT_NUMBER");

        if (!(account instanceof String accountNumber)
                || accountNumber.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "ATM session expired. Please authenticate again.");
        }

        return accountNumber;
    }

    @GetMapping("/balance")
    public AccountBalanceResponse balance(HttpServletRequest request) {
        return accountService.getBalance(authenticatedAccount(request));
    }

    @GetMapping("/transactions")
    public List<TransactionResponse> transactions(
            HttpServletRequest request) {
        return accountService.getRecentTransactions(
                authenticatedAccount(request));
    }

    @PostMapping("/withdraw")
    public TransactionResponse withdraw(
            @RequestBody WithdrawalRequest body,
            HttpServletRequest request) {
        return accountService.withdraw(
                authenticatedAccount(request), body);
    }

    @PostMapping("/transfer")
    public TransferResponse transfer(
            @RequestBody TransferRequest body,
            HttpServletRequest request) {
        return accountService.transfer(
                authenticatedAccount(request), body);
    }

    @PostMapping("/deposit")
    public TransactionResponse deposit(
            @RequestParam String destinationAccount,
            @RequestBody DepositRequest body,
            HttpServletRequest request) {
        authenticatedAccount(request);
        return accountService.deposit(destinationAccount, body);
    }

    @GetMapping("/recipient/{accountNumber}")
    public AtmRecipientResponse recipient(
            @PathVariable String accountNumber,
            HttpServletRequest request) {
        authenticatedAccount(request);

        BankAccount recipient;
        try {
            recipient = accountService.findAccount(accountNumber);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND,
                    "Recipient account not found.");
        }

        if (recipient.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "Recipient account is not active.");
        }

        return new AtmRecipientResponse(
                recipient.getAccountNumber(),
                recipient.getAccountHolderName());
    }
}
