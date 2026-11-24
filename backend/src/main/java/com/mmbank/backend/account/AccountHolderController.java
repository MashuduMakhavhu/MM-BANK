package com.mmbank.backend.account;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/accounts")
public class AccountHolderController {
    private final BankAccountRepository repository;

    public AccountHolderController(BankAccountRepository repository) {
        this.repository = repository;
    }

    @PutMapping("/{accountNumber}/holder-name")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Transactional
    public BankAccount updateName(
        @PathVariable String accountNumber,
        @Valid @RequestBody UpdateAccountHolderRequest request) {

        BankAccount account = repository.findByAccountNumberForUpdate(accountNumber)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        account.setAccountHolderName(request.accountHolderName().trim());
        return repository.save(account);
    }
}
