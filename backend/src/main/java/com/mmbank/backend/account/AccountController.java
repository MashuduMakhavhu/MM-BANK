package com.mmbank.backend.account;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<BankAccount> createAccount(
            @Valid @RequestBody CreateAccountRequest request) {

        BankAccount account = accountService.createAccount(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(account);
    }

    @GetMapping("/{accountNumber}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','TELLER')")
    public ResponseEntity<BankAccount> getAccount(
            @PathVariable String accountNumber) {

        return ResponseEntity.ok(
                accountService.findAccount(accountNumber));
    }

    @GetMapping("/{accountNumber}/balance")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','TELLER')")
    public ResponseEntity<AccountBalanceResponse> getBalance(
            @PathVariable String accountNumber) {

        return ResponseEntity.ok(
                accountService.getBalance(accountNumber));
    }

    @PostMapping("/{accountNumber}/deposit")
    @PreAuthorize("hasAnyRole('TELLER','MANAGER','ADMIN')")
    public ResponseEntity<TransactionResponse> deposit(
            @PathVariable String accountNumber,
            @Valid @RequestBody DepositRequest request) {

        return ResponseEntity.ok(
                accountService.deposit(accountNumber, request));
    }

    @PostMapping("/{accountNumber}/withdraw")
    @PreAuthorize("hasAnyRole('TELLER','MANAGER','ADMIN')")
    public ResponseEntity<TransactionResponse> withdraw(
            @PathVariable String accountNumber,
            @Valid @RequestBody WithdrawalRequest request) {

        return ResponseEntity.ok(
                accountService.withdraw(accountNumber, request));
    }

    @PostMapping("/{accountNumber}/transfer")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<TransferResponse> transfer(
            @PathVariable String accountNumber,
            @Valid @RequestBody TransferRequest request) {

        return ResponseEntity.ok(
                accountService.transfer(accountNumber, request));
    }

    @GetMapping("/{accountNumber}/transactions")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','TELLER')")
    public ResponseEntity<List<TransactionResponse>> getTransactions(
            @PathVariable String accountNumber) {

        return ResponseEntity.ok(
                accountService.getRecentTransactions(accountNumber));
    }

    @PatchMapping("/{accountNumber}/freeze")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<BankAccount> freezeAccount(
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.freezeAccount(accountNumber));
    }

    @PatchMapping("/{accountNumber}/suspend")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<BankAccount> suspendAccount(
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.suspendAccount(accountNumber));
    }

    @PatchMapping("/{accountNumber}/activate")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<BankAccount> activateAccount(
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.activateAccount(accountNumber));
    }

    @PatchMapping("/{accountNumber}/close")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<BankAccount> closeAccount(
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.closeAccount(accountNumber));
    }

}
