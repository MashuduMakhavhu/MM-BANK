package com.mmbank.backend.atm;

import com.mmbank.backend.account.AccountService;
import com.mmbank.backend.account.AccountType;
import com.mmbank.backend.account.BankAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AtmCardAdminControllerTests {

    private AtmCardRepository cardRepository;
    private AccountService accountService;
    private AtmPinService pinService;
    private AtmCardAdminController controller;
    private BankAccount account;

    @BeforeEach
    void setUp() {
        cardRepository = mock(AtmCardRepository.class);
        accountService = mock(AccountService.class);
        pinService = new AtmPinService(
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(),
                "test-pepper-for-card-issuance-32-characters"
        );

        controller = new AtmCardAdminController(
                cardRepository,
                accountService,
                pinService
        );

        account = new BankAccount(
                "1000000001",
                "Test Customer",
                AccountType.SAVINGS,
                new BigDecimal("5000.00")
        );

        when(accountService.findAccount("1000000001"))
                .thenReturn(account);

        when(cardRepository.existsByCardNumber(anyString()))
                .thenReturn(false);
    }

    @Test
    void shouldIssueCardForActiveAccount() {
        AtmCardIssueResponse response = controller.issueCard(
                new AtmCardIssueRequest("1000000001", "1234")
        );

        assertNotNull(response);
        assertEquals("1000000001", response.accountNumber());
        assertEquals(16, response.cardNumber().length());
        assertFalse(response.cardNumber().startsWith("0"));
        assertEquals(LocalDate.now().plusYears(4), response.expiryDate());

        verify(cardRepository).save(any(AtmCard.class));
    }

    @Test
    void shouldRejectMissingAccountNumber() {
        assertThrows(ResponseStatusException.class,
                () -> controller.issueCard(
                        new AtmCardIssueRequest(" ", "1234")
                ));

        verify(cardRepository, never()).save(any(AtmCard.class));
    }

    @Test
    void shouldRejectInactiveAccount() {
        account.freeze();

        assertThrows(ResponseStatusException.class,
                () -> controller.issueCard(
                        new AtmCardIssueRequest("1000000001", "1234")
                ));

        verify(cardRepository, never()).save(any(AtmCard.class));
    }

    @Test
    void shouldRejectInvalidPin() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.issueCard(
                        new AtmCardIssueRequest("1000000001", "12")
                ));

        verify(cardRepository, never()).save(any(AtmCard.class));
    }

    @Test
    void shouldRetryWhenGeneratedCardNumberAlreadyExists() {
        when(cardRepository.existsByCardNumber(anyString()))
                .thenReturn(true, false);

        AtmCardIssueResponse response = controller.issueCard(
                new AtmCardIssueRequest("1000000001", "1234")
        );

        assertEquals(16, response.cardNumber().length());
        verify(cardRepository).save(any(AtmCard.class));
        verify(cardRepository, times(2)).existsByCardNumber(anyString());
    }
}
