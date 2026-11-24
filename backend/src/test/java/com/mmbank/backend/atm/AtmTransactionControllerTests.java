package com.mmbank.backend.atm;

import com.mmbank.backend.account.*;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AtmTransactionControllerTests {

    private static final String ACCOUNT_NUMBER = "1234567890";
    private static final String RECIPIENT_NUMBER = "9876543210";

    private AccountService accountService;
    private AtmTransactionController controller;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        accountService = mock(AccountService.class);
        controller = new AtmTransactionController(accountService);
        request = new MockHttpServletRequest();
    }

    private void authenticate() {
        request.getSession(true)
                .setAttribute("ATM_ACCOUNT_NUMBER", ACCOUNT_NUMBER);
    }

    private void assertUnauthorized(Runnable action) {
        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, action::run);

        assertEquals(401, exception.getStatusCode().value());
        verifyNoInteractions(accountService);
    }

    @Test
    void balanceRejectsMissingSession() {
        assertUnauthorized(() -> controller.balance(request));
    }

    @Test
    void transactionsRejectsMissingSession() {
        assertUnauthorized(() -> controller.transactions(request));
    }

    @Test
    void withdrawalRejectsMissingSession() {
        assertUnauthorized(() ->
                controller.withdraw(new WithdrawalRequest(null), request));
    }

    @Test
    void transferRejectsMissingSession() {
        assertUnauthorized(() ->
                controller.transfer(new TransferRequest(null, null), request));
    }

    @Test
    void depositRejectsMissingSession() {
        assertUnauthorized(() ->
                controller.deposit(RECIPIENT_NUMBER, new DepositRequest(null), request));
    }

    @Test
    void recipientLookupRejectsMissingSession() {
        assertUnauthorized(() ->
                controller.recipient(RECIPIENT_NUMBER, request));
    }

    @Test
    void balanceRejectsSessionWithoutAccountNumber() {
        HttpSession session = request.getSession(true);

        assertUnauthorized(() -> {
            session.removeAttribute("ATM_ACCOUNT_NUMBER");
            controller.balance(request);
        });
    }

    @Test
    void balanceUsesAuthenticatedAccount() {
        authenticate();

        AccountBalanceResponse expected = new AccountBalanceResponse(
                ACCOUNT_NUMBER, "Test Customer", AccountType.SAVINGS,
                new BigDecimal("1500.00"));

        when(accountService.getBalance(ACCOUNT_NUMBER)).thenReturn(expected);

        assertEquals(expected, controller.balance(request));
        verify(accountService).getBalance(ACCOUNT_NUMBER);
    }

    @Test
    void transactionsUseAuthenticatedAccount() {
        authenticate();

        List<TransactionResponse> expected = List.of();

        when(accountService.getRecentTransactions(ACCOUNT_NUMBER))
                .thenReturn(expected);

        assertEquals(expected, controller.transactions(request));
        verify(accountService).getRecentTransactions(ACCOUNT_NUMBER);
    }

    @Test
    void withdrawalUsesAuthenticatedAccount() {
        authenticate();

        WithdrawalRequest body =
                new WithdrawalRequest(new BigDecimal("100.00"));

        TransactionResponse expected = null;

        when(accountService.withdraw(ACCOUNT_NUMBER, body))
                .thenReturn(expected);

        assertNull(controller.withdraw(body, request));
        verify(accountService).withdraw(ACCOUNT_NUMBER, body);
    }

    @Test
    void transferUsesAuthenticatedAccount() {
        authenticate();

        TransferRequest body = new TransferRequest(
                RECIPIENT_NUMBER, new BigDecimal("200.00"));

        TransferResponse expected = null;

        when(accountService.transfer(ACCOUNT_NUMBER, body))
                .thenReturn(expected);

        assertNull(controller.transfer(body, request));
        verify(accountService).transfer(ACCOUNT_NUMBER, body);
    }

    @Test
    void depositUsesExplicitDestinationAccount() {
        authenticate();

        DepositRequest body =
                new DepositRequest(new BigDecimal("300.00"));

        when(accountService.deposit(RECIPIENT_NUMBER, body))
                .thenReturn(null);

        assertNull(controller.deposit(RECIPIENT_NUMBER, body, request));

        verify(accountService).deposit(RECIPIENT_NUMBER, body);
        verifyNoMoreInteractions(accountService);
    }

    @Test
    void recipientLookupReturnsActiveRecipient() {
        authenticate();

        BankAccount recipient = new BankAccount(
                RECIPIENT_NUMBER, "Recipient Customer",
                AccountType.SAVINGS, new BigDecimal("500.00"));

        when(accountService.findAccount(RECIPIENT_NUMBER))
                .thenReturn(recipient);

        AtmRecipientResponse response =
                controller.recipient(RECIPIENT_NUMBER, request);

        assertEquals(RECIPIENT_NUMBER, response.accountNumber());
        assertEquals("Recipient Customer", response.accountHolderName());

        verify(accountService).findAccount(RECIPIENT_NUMBER);
    }

    @Test
    void recipientLookupRejectsInactiveRecipient() {
        authenticate();

        BankAccount recipient = new BankAccount(
                RECIPIENT_NUMBER, "Frozen Customer",
                AccountType.SAVINGS, new BigDecimal("500.00"));
        recipient.freeze();

        when(accountService.findAccount(RECIPIENT_NUMBER))
                .thenReturn(recipient);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.recipient(RECIPIENT_NUMBER, request));

        assertEquals(409, exception.getStatusCode().value());
        verify(accountService).findAccount(RECIPIENT_NUMBER);
    }
}
