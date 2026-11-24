package com.mmbank.backend.mobile;

import com.mmbank.backend.atm.AtmRecipientResponse;
import com.mmbank.backend.account.*;
import com.mmbank.backend.auth.AuthService;
import com.mmbank.backend.customer.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MobileAccountControllerTests {

    private static final String CUSTOMER_NUMBER = "CUST000001";
    private static final String RECIPIENT_NUMBER = "9876543210";

    private AuthService authService;
    private BankAccountRepository accounts;
    private MobileAccountController controller;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        accounts = mock(BankAccountRepository.class);
        controller = new MobileAccountController(
                authService, accounts, mock(AccountService.class)
        );
        session = new MockHttpSession();
    }

    private void authenticate() {
        session.setAttribute("CUSTOMER_NUMBER", CUSTOMER_NUMBER);
        when(authService.findCustomer(CUSTOMER_NUMBER)).thenReturn(
                new Customer(CUSTOMER_NUMBER, "Test Customer",
                        "test@example.com", "0123456789")
        );
    }

    @Test
    void recipientLookupRejectsMissingSession() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.recipient(session, RECIPIENT_NUMBER)
        );

        assertEquals(401, exception.getStatusCode().value());
        verifyNoInteractions(accounts);
    }

    @Test
    void recipientLookupRejectsInvalidCustomerSession() {
        session.setAttribute("CUSTOMER_NUMBER", CUSTOMER_NUMBER);
        when(authService.findCustomer(CUSTOMER_NUMBER)).thenThrow(
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session is invalid")
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.recipient(session, RECIPIENT_NUMBER)
        );

        assertEquals(401, exception.getStatusCode().value());
        verifyNoInteractions(accounts);
    }

    @Test
    void recipientLookupReturnsActiveRecipient() {
        authenticate();
        BankAccount recipient = new BankAccount(
                RECIPIENT_NUMBER, "Recipient Customer",
                AccountType.SAVINGS, new BigDecimal("500.00")
        );
        when(accounts.findByAccountNumber(RECIPIENT_NUMBER))
                .thenReturn(Optional.of(recipient));

        AtmRecipientResponse response = controller.recipient(session, RECIPIENT_NUMBER);

        assertEquals(RECIPIENT_NUMBER, response.accountNumber());
        assertEquals("Recipient Customer", response.accountHolderName());
        verify(accounts).findByAccountNumber(RECIPIENT_NUMBER);
    }

    @Test
    void recipientLookupRejectsUnknownAccount() {
        authenticate();
        when(accounts.findByAccountNumber(RECIPIENT_NUMBER))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.recipient(session, RECIPIENT_NUMBER)
        );

        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    void recipientLookupRejectsInactiveRecipient() {
        authenticate();
        BankAccount recipient = new BankAccount(
                RECIPIENT_NUMBER, "Frozen Customer",
                AccountType.SAVINGS, new BigDecimal("500.00")
        );
        recipient.freeze();
        when(accounts.findByAccountNumber(RECIPIENT_NUMBER))
                .thenReturn(Optional.of(recipient));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.recipient(session, RECIPIENT_NUMBER)
        );

        assertEquals(409, exception.getStatusCode().value());
    }
}
