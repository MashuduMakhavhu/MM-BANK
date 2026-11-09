package com.mmbank.backend.auth;

import com.mmbank.backend.customer.Customer;
import com.mmbank.backend.customer.CustomerRepository;
import com.mmbank.backend.account.BankAccount;
import com.mmbank.backend.account.BankAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class AuthService {

    private final CustomerRepository customers;
    private final BankAccountRepository accounts;
    private final CustomerCredentialRepository credentials;
    private final PasswordEncoder encoder;

    @Value("${mmbank.auth.activation-code:}")
    private String activationCode;

    public AuthService(CustomerRepository customers,
                       CustomerCredentialRepository credentials,
                       BankAccountRepository accounts,
                       PasswordEncoder encoder) {
        this.customers = customers;
        this.accounts = accounts;
        this.credentials = credentials;
        this.encoder = encoder;
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

        Customer customer = customers.findByCustomerNumber(
            request.customerNumber()).orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Customer not found"));

        BankAccount account = accounts.findByAccountNumber(
            request.accountNumber()).orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Account not found"));

        if (!account.getAccountHolderName().trim().equalsIgnoreCase(
                customer.getFullName().trim())) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Account ownership could not be verified");
        }

        if (account.getCustomer() != null &&
                !account.getCustomer().getId().equals(customer.getId())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Account is linked to another customer");
        }

        account.setCustomer(customer);
        accounts.save(account);

        if (credentials.findByCustomer_CustomerNumber(
                request.customerNumber()).isPresent()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Login already registered");
        }

        CustomerCredential credential = new CustomerCredential(
            customer, encoder.encode(request.password()));

        credentials.save(credential);

        return new AuthResponse(
            customer.getCustomerNumber(),
            customer.getFullName(),
            "Registration successful");
    }

    @Transactional(readOnly = true)
    public Customer authenticate(LoginRequest request) {
        CustomerCredential credential = credentials
            .findByCustomer_CustomerNumber(request.customerNumber())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid customer number or password"
            ));

        if (!encoder.matches(
                request.password(),
                credential.getPasswordHash())) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid customer number or password"
            );
        }

        return customers.findByCustomerNumber(request.customerNumber())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid customer number or password"
            ));
    }

    public Customer findCustomer(String customerNumber) {
        return customers.findByCustomerNumber(customerNumber)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Session is invalid"));
    }
}
