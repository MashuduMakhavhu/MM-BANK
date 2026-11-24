package com.mmbank.backend.atm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class AtmPinServiceTests {

    private AtmPinService atmPinService;

    private static final String PEPPER =
            "test-pepper-for-atm-pin-service-32-characters";

    @BeforeEach
    void setUp() {
        atmPinService = new AtmPinService(
                new BCryptPasswordEncoder(),
                PEPPER
        );
    }

    @Test
    void shouldEncodeValidPin() {
        String encoded = atmPinService.encode("1234");

        assertNotNull(encoded);
        assertNotEquals("1234", encoded);
    }

    @Test
    void shouldMatchCorrectPin() {
        String encoded = atmPinService.encode("1234");

        assertTrue(atmPinService.matches("1234", encoded));
    }

    @Test
    void shouldRejectIncorrectPin() {
        String encoded = atmPinService.encode("1234");

        assertFalse(atmPinService.matches("5678", encoded));
    }

    @Test
    void shouldRejectInvalidPinFormats() {
        String encoded = atmPinService.encode("1234");

        assertFalse(atmPinService.matches("123", encoded));
        assertFalse(atmPinService.matches("12345", encoded));
        assertFalse(atmPinService.matches("abcd", encoded));
        assertFalse(atmPinService.matches(null, encoded));
        assertFalse(atmPinService.matches("1234", null));
    }

    @Test
    void shouldRejectInvalidPinWhenEncoding() {
        assertThrows(IllegalArgumentException.class,
                () -> atmPinService.encode("123"));

        assertThrows(IllegalArgumentException.class,
                () -> atmPinService.encode("abcd"));

        assertThrows(IllegalArgumentException.class,
                () -> atmPinService.encode(null));
    }

    @Test
    void shouldRejectWeakPepper() {
        assertThrows(IllegalStateException.class,
                () -> new AtmPinService(
                        new BCryptPasswordEncoder(),
                        "too-short"
                ));
    }
}
