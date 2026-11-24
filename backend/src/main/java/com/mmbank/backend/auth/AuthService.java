package com.mmbank.backend.auth;

import com.mmbank.backend.account.AccountStatus;
import com.mmbank.backend.account.BankAccount;
import com.mmbank.backend.account.BankAccountRepository;
import com.mmbank.backend.customer.Customer;
import com.mmbank.backend.customer.CustomerRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;

@Service
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;

    private final CustomerRepository customers;
    private final BankAccountRepository accounts;
    private final CustomerCredentialRepository credentials;
    private final AppPinService appPinService;

    @Value("${mmbank.auth.activation-code:}")
    private String activationCode;

    public AuthService(
            CustomerRepository customers,
            CustomerCredentialRepository credentials,
            BankAccountRepository accounts,
            AppPinService appPinService) {
        this.customers = customers;
        this.accounts = accounts;
        this.credentials = credentials;
        this.appPinService = appPinService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (activationCode == null || activationCode.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Registration is not configured");
        }

        boolean validCode = MessageDigest.isEqual(
                activationCode.getBytes(StandardCharsets.UTF_8),
                request.activationCode().getBytes(StandardCharsets.UTF_8));

        if (!validCode) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Invalid activation code");
        }

        BankAccount account = accounts.findByAccountNumber(
                request.accountNumber()).orElseThrow(() ->
                new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Account not found"));

        Customer customer = account.getCustomer();

        if (customer == null ||
                account.getStatus() != AccountStatus.ACTIVE ||
                !account.getAccountHolderName().trim().equalsIgnoreCase(
                        customer.getFullName().trim())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Account is not eligible for mobile activation");
        }

        if (credentials.findByCustomer_CustomerNumber(
                customer.getCustomerNumber()).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Mobile banking is already activated");
        }

        CustomerCredential credential = new CustomerCredential(
                customer, appPinService.encode(request.appPin()));

        credentials.save(credential);

        return new AuthResponse(
                customer.getFullName(),
                "Mobile banking activated successfully");
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public Customer authenticate(LoginRequest request) {
        BankAccount account = accounts.findByAccountNumber(
                request.accountNumber()).orElseThrow(() ->
                invalidCredentials());

        Customer customer = account.getCustomer();

        if (customer == null || account.getStatus() != AccountStatus.ACTIVE) {
            throw invalidCredentials();
        }

        CustomerCredential credential = credentials
                .findByCustomer_CustomerNumber(customer.getCustomerNumber())
                .orElseThrow(() -> invalidCredentials());

        LocalDateTime now = LocalDateTime.now();

        if (credential.getLockedUntil() != null) {
            if (credential.getLockedUntil().isAfter(now)) {
                throw new ResponseStatusException(
                        HttpStatus.LOCKED,
                        "Too many incorrect PIN attempts. Try again later.");
            }

            credential.setLockedUntil(null);
            credential.setFailedAttempts(0);
        }

        if (!appPinService.matches(
                request.appPin(), credential.getAppPinHash())) {

            int attempts = credential.getFailedAttempts() + 1;
            credential.setFailedAttempts(attempts);

            if (attempts >= MAX_FAILED_ATTEMPTS) {
                credential.setLockedUntil(
                        now.plusMinutes(LOCKOUT_MINUTES));
            }

            credentials.save(credential);

            throw invalidCredentials();
        }

        credential.setFailedAttempts(0);
        credential.setLockedUntil(null);
        credentials.save(credential);

        return customer;
    }

    @Transactional(readOnly = true)
    public Customer findCustomer(String customerNumber) {
        return customers.findByCustomerNumber(customerNumber)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Session is invalid"));
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid account number or app PIN");
    }
}
