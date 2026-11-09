package com.mmbank.backend.mobile;

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

    private BankAccount customerAccount(Customer customer) {
        return accounts.findByCustomer_CustomerNumber(
            customer.getCustomerNumber()
        ).orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "No linked bank account found"
        ));
    }

    @GetMapping("/account")
    public MobileAccountResponse account(HttpSession session) {
        Customer customer = loggedInCustomer(session);
        BankAccount account = customerAccount(customer);
        return new MobileAccountResponse(
            customer.getCustomerNumber(),
            customer.getFullName(),
            account.getAccountNumber(),
            account.getAccountType().name(),
            account.getBalance()
        );
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
