package com.mmbank.backend.account;

import com.mmbank.backend.customer.Customer;
import com.mmbank.backend.customer.CustomerRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin")
public class AccountIdentityController {

    private final CustomerRepository customers;
    private final BankAccountRepository accounts;

    @Value("${MM_ADMIN_KEY:}")
    private String adminKey;

    public AccountIdentityController(
        CustomerRepository customers,
        BankAccountRepository accounts
    ) {
        this.customers = customers;
        this.accounts = accounts;
    }

    @PutMapping("/accounts/{accountNumber}/identity")
    @Transactional
    public String updateIdentity(
        @PathVariable String accountNumber,
        @RequestHeader("X-Admin-Key") String key,
        @Valid @RequestBody UpdateAccountIdentityRequest request
    ) {
        if (adminKey.isBlank() || !adminKey.equals(key)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Invalid admin key"
            );
        }

        Customer customer = customers.findByCustomerNumber(
            request.customerNumber()
        ).orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Customer not found"
        ));

        BankAccount account = accounts.findByAccountNumberForUpdate(
            accountNumber
        ).orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Account not found"
        ));

        if (account.getCustomer() != null &&
            !account.getCustomer().getCustomerNumber()
                .equals(customer.getCustomerNumber())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Account belongs to another customer"
            );
        }

        String name = request.fullName().trim();
        customer.setFullName(name);
        account.setAccountHolderName(name);
        account.setCustomer(customer);

        customers.save(customer);
        accounts.save(account);

        return "Customer and account identity updated successfully";
    }
}
