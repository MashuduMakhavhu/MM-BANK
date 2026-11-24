package com.mmbank.backend.atm;

import com.mmbank.backend.account.AccountType;
import com.mmbank.backend.account.BankAccount;
import com.mmbank.backend.atm.AtmCardStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AtmAuthServiceTests {

    private AtmCardRepository cardRepository;
    private AtmPinService pinService;
    private AtmAuthService authService;
    private BankAccount account;
    private AtmCard card;

    @BeforeEach
    void setUp() {
        cardRepository = mock(AtmCardRepository.class);

        pinService = new AtmPinService(
                new BCryptPasswordEncoder(),
                "test-pepper-for-atm-auth-service-32-characters"
        );

        authService = new AtmAuthService(cardRepository, pinService);

        account = new BankAccount(
                "1000000001",
                "Test Customer",
                AccountType.SAVINGS,
                new BigDecimal("5000.00")
        );

        card = new AtmCard(
                "1234567890123456",
                pinService.encode("1234"),
                account,
                LocalDate.now().plusYears(2)
        );

        when(cardRepository.findByCardNumberForUpdate(card.getCardNumber()))
                .thenReturn(Optional.of(card));
    }

    @Test
    void shouldAuthenticateWithValidCardAndPin() {
        AtmLoginResponse response = authService.authenticate(
                new AtmLoginRequest(card.getCardNumber(), "1234")
        );

        assertEquals("1000000001", response.accountNumber());
        assertEquals("Test Customer", response.accountHolderName());
        assertEquals(AccountType.SAVINGS, response.accountType());
        assertTrue(response.maskedCardNumber().endsWith("3456"));
    }

    @Test
    void shouldRejectIncorrectPin() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.authenticate(
                        new AtmLoginRequest(card.getCardNumber(), "9999")
                )
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals(1, card.getFailedAttempts());
    }

    @Test
    void shouldLockCardAfterThreeIncorrectAttempts() {
        for (int attempt = 1; attempt <= 2; attempt++) {
            assertThrows(ResponseStatusException.class,
                    () -> authService.authenticate(
                            new AtmLoginRequest(card.getCardNumber(), "9999")
                    ));
        }

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.authenticate(
                        new AtmLoginRequest(card.getCardNumber(), "9999")
                )
        );

        assertEquals(HttpStatus.LOCKED, exception.getStatusCode());
        assertEquals(3, card.getFailedAttempts());
        assertNotNull(card.getLockedUntil());
    }

    @Test
    void shouldRejectBlockedCard() {
        card.block();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.authenticate(
                        new AtmLoginRequest(card.getCardNumber(), "1234")
                )
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void shouldRejectExpiredCard() {
        AtmCard expiredCard = new AtmCard(
                card.getCardNumber(),
                pinService.encode("1234"),
                account,
                LocalDate.now().minusDays(1)
        );

        when(cardRepository.findByCardNumberForUpdate(card.getCardNumber()))
                .thenReturn(Optional.of(expiredCard));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.authenticate(
                        new AtmLoginRequest(card.getCardNumber(), "1234")
                )
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void shouldRejectMalformedCredentials() {
        assertThrows(ResponseStatusException.class,
                () -> authService.authenticate(
                        new AtmLoginRequest("123", "1234")
                ));

        verify(cardRepository, never()).findByCardNumberForUpdate(anyString());
    }

    @Test
    void shouldResetFailedAttemptsAfterSuccessfulLogin() {
        card.recordFailedAttempt();
        card.recordFailedAttempt();

        authService.authenticate(
                new AtmLoginRequest(card.getCardNumber(), "1234")
        );

        assertEquals(0, card.getFailedAttempts());
        assertNull(card.getLockedUntil());
    }
}
