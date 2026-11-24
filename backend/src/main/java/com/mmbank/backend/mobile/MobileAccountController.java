package com.mmbank.backend.mobile;

import com.mmbank.backend.atm.AtmRecipientResponse;
import com.mmbank.backend.account.*;
import com.mmbank.backend.auth.AuthService;
import com.mmbank.backend.customer.Customer;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/mobile")
public class MobileAccountController {

    private final AuthService authService;
    private final BankAccountRepository accounts;
    private final AccountService accountService;

    public MobileAccountController(
            AuthService authService,
            BankAccountRepository accounts,
            AccountService accountService
    ) {
        this.authService = authService;
        this.accounts = accounts;
        this.accountService = accountService;
    }

    private Customer loggedInCustomer(HttpSession session) {
        Object number = session.getAttribute("CUSTOMER_NUMBER");

        if (number == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Please log in"
            );
        }

        return authService.findCustomer(number.toString());
    }

    private List<BankAccount> customerAccounts(Customer customer) {
        List<BankAccount> linkedAccounts =
                accounts.findAllByCustomer_CustomerNumber(
                        customer.getCustomerNumber()
                );

        if (linkedAccounts.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "No linked bank accounts found"
            );
        }

        return linkedAccounts;
    }

    private BankAccount customerAccount(Customer customer) {
        return customerAccounts(customer).get(0);
    }

    private BankAccount customerAccount(
            Customer customer,
            String accountNumber
    ) {
        return customerAccounts(customer).stream()
                .filter(account -> account.getAccountNumber().equals(accountNumber))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Bank account not found"
                ));
    }

    private MobileAccountResponse accountResponse(
            Customer customer,
            BankAccount account
    ) {
        return new MobileAccountResponse(
                customer.getCustomerNumber(),
                customer.getFullName(),
                account.getAccountNumber(),
                account.getAccountType().name(),
                account.getBalance()
        );
    }


    @GetMapping("/recipient/{accountNumber}")
    public AtmRecipientResponse recipient(
            HttpSession session,
            @PathVariable String accountNumber
    ) {
        loggedInCustomer(session);

        BankAccount recipient = accounts.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Recipient account not found."
                ));

        if (recipient.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Recipient account is not active."
            );
        }

        return new AtmRecipientResponse(
                recipient.getAccountNumber(),
                recipient.getAccountHolderName()
        );
    }

    @GetMapping("/accounts")
    public List<MobileAccountResponse> accounts(HttpSession session) {
        Customer customer = loggedInCustomer(session);

        return customerAccounts(customer).stream()
                .map(account -> accountResponse(customer, account))
                .toList();
    }

    @GetMapping("/accounts/{accountNumber}/balance")
    public AccountBalanceResponse accountBalance(
            HttpSession session,
            @PathVariable String accountNumber
    ) {
        Customer customer = loggedInCustomer(session);
        BankAccount account = customerAccount(customer, accountNumber);

        return accountService.getBalance(account.getAccountNumber());
    }

    @GetMapping("/accounts/{accountNumber}/transactions")
    public List<TransactionResponse> accountTransactions(
            HttpSession session,
            @PathVariable String accountNumber
    ) {
        Customer customer = loggedInCustomer(session);
        BankAccount account = customerAccount(customer, accountNumber);

        return accountService.getRecentTransactions(account.getAccountNumber());
    }

    @PostMapping("/accounts/{accountNumber}/transfer")
    public TransferResponse accountTransfer(
            HttpSession session,
            @PathVariable String accountNumber,
            @Valid @RequestBody TransferRequest request
    ) {
        Customer customer = loggedInCustomer(session);
        BankAccount account = customerAccount(customer, accountNumber);

        return accountService.transfer(account.getAccountNumber(), request);
    }

    // Temporary compatibility endpoint for the existing frontend.
    @GetMapping("/account")
    public MobileAccountResponse account(HttpSession session) {
        Customer customer = loggedInCustomer(session);
        return accountResponse(customer, customerAccount(customer));
    }

    @GetMapping("/balance")
    public AccountBalanceResponse balance(HttpSession session) {
        Customer customer = loggedInCustomer(session);
        BankAccount account = customerAccount(customer);

        return accountService.getBalance(account.getAccountNumber());
    }

    @GetMapping("/transactions")
    public List<TransactionResponse> transactions(HttpSession session) {
        Customer customer = loggedInCustomer(session);
        BankAccount account = customerAccount(customer);

        return accountService.getRecentTransactions(account.getAccountNumber());
    }

    @PostMapping("/transfer")
    public TransferResponse transfer(
            HttpSession session,
            @Valid @RequestBody TransferRequest request
    ) {
        Customer customer = loggedInCustomer(session);
        BankAccount account = customerAccount(customer);

        return accountService.transfer(account.getAccountNumber(), request);
    }
}
