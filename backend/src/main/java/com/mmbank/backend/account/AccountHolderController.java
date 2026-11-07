package com.mmbank.backend.account;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/accounts")
public class AccountHolderController {
    private final BankAccountRepository repository;

    @Value("${MM_ADMIN_KEY:}")
    private String adminKey;

    public AccountHolderController(BankAccountRepository repository) {
        this.repository = repository;
    }

    @PutMapping("/{accountNumber}/holder-name")
    @Transactional
    public BankAccount updateName(
        @PathVariable String accountNumber,
        @RequestHeader("X-Admin-Key") String key,
        @Valid @RequestBody UpdateAccountHolderRequest request) {

        if (adminKey.isBlank() || !adminKey.equals(key)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid admin key");
        }

        BankAccount account = repository.findByAccountNumberForUpdate(accountNumber)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        account.setAccountHolderName(request.accountHolderName().trim());
        return repository.save(account);
    }
}
