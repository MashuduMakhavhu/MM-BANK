package com.mmbank.backend.atm;

import com.mmbank.backend.account.AccountType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AtmAuthControllerTests {

    private AtmAuthService authService;
    private AtmAuthController controller;

    private final AtmLoginRequest loginRequest =
            new AtmLoginRequest("1234567890123456", "1234");

    private final AtmLoginResponse loginResponse =
            new AtmLoginResponse(
                    "ACC-123",
                    "Test Customer",
                    AccountType.SAVINGS,
                    "************3456"
            );

    @BeforeEach
    void setUp() {
        authService = mock(AtmAuthService.class);
        controller = new AtmAuthController(authService);
    }

    @Test
    void successfulLoginCreatesFiveMinuteSession() {
        when(authService.authenticate(loginRequest))
                .thenReturn(loginResponse);

        MockHttpServletRequest request = new MockHttpServletRequest();

        AtmLoginResponse response =
                controller.login(loginRequest, request);

        assertEquals("ACC-123", response.accountNumber());

        var session = request.getSession(false);

        assertNotNull(session);
        assertEquals(300, session.getMaxInactiveInterval());
        assertEquals("ACC-123",
                session.getAttribute("ATM_ACCOUNT_NUMBER"));
        assertEquals("1234567890123456",
                session.getAttribute("ATM_CARD_NUMBER"));
        assertEquals("************3456",
                session.getAttribute("ATM_CARD_MASKED"));
    }

    @Test
    void successfulLoginInvalidatesPreviousSession() {
        when(authService.authenticate(loginRequest))
                .thenReturn(loginResponse);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession oldSession =
                (MockHttpSession) request.getSession(true);

        controller.login(loginRequest, request);

        assertThrows(
                IllegalStateException.class,
                () -> oldSession.getAttribute("ATM_ACCOUNT_NUMBER")
        );

        assertNotSame(oldSession, request.getSession(false));
    }

    @Test
    void invalidCredentialsDoNotCreateNewSession() {
        when(authService.authenticate(loginRequest))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED
                ));

        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThrows(
                ResponseStatusException.class,
                () -> controller.login(loginRequest, request)
        );

        assertNull(request.getSession(false));
    }

    @Test
    void sessionEndpointRejectsMissingSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.session(request)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void sessionEndpointReturnsAuthenticatedAccount() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        var session = request.getSession(true);

        session.setAttribute("ATM_ACCOUNT_NUMBER", "ACC-123");
        session.setAttribute("ATM_CARD_MASKED", "************3456");

        AtmLoginResponse response = controller.session(request);

        assertEquals("ACC-123", response.accountNumber());
        assertEquals("************3456", response.maskedCardNumber());
    }

    @Test
    void logoutInvalidatesExistingSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session =
                (MockHttpSession) request.getSession(true);

        session.setAttribute("ATM_ACCOUNT_NUMBER", "ACC-123");

        controller.logout(request);

        assertThrows(
                IllegalStateException.class,
                () -> session.getAttribute("ATM_ACCOUNT_NUMBER")
        );
    }
}
