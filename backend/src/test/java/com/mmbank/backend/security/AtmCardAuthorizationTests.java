package com.mmbank.backend.security;

import com.mmbank.backend.atm.AtmCardAdminController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AtmCardAuthorizationTests {

    @Autowired
    private AtmCardAdminController atmCardAdminController;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String role) {
        var authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        "TEST-STAFF",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);
    }

    private void assertAuthorizedRoleCanReachController(String role) {
        authenticateAs(role);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> atmCardAdminController.issueCard(null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void unauthenticatedUserCannotIssueAtmCard() {
        SecurityContextHolder.clearContext();

        assertThrows(
                AuthenticationCredentialsNotFoundException.class,
                () -> atmCardAdminController.issueCard(null)
        );
    }

    @Test
    void customerCannotIssueAtmCard() {
        authenticateAs("CUSTOMER");

        assertThrows(
                AccessDeniedException.class,
                () -> atmCardAdminController.issueCard(null)
        );
    }

    @Test
    void adminCanIssueAtmCard() {
        assertAuthorizedRoleCanReachController("ADMIN");
    }

    @Test
    void managerCanIssueAtmCard() {
        assertAuthorizedRoleCanReachController("MANAGER");
    }

    @Test
    void tellerCanIssueAtmCard() {
        assertAuthorizedRoleCanReachController("TELLER");
    }
}
