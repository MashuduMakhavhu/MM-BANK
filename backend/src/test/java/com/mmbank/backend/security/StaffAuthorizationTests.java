package com.mmbank.backend.security;

import com.mmbank.backend.account.AccountController;
import com.mmbank.backend.customer.CustomerController;
import com.mmbank.backend.staff.StaffDashboardController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class StaffAuthorizationTests {

    @Autowired
    private AccountController accountController;

    @Autowired
    private CustomerController customerController;

    @Autowired
    private StaffDashboardController dashboardController;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String role) {
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                "TEST-STAFF",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);
    }

    @Test
    void unauthenticatedUserCannotFreezeAccount() {
        SecurityContextHolder.clearContext();

        assertThrows(
                AuthenticationCredentialsNotFoundException.class,
                () -> accountController.freezeAccount("TEST-ACCOUNT")
        );
    }

    @Test
    void tellerCannotFreezeAccount() {
        authenticateAs("TELLER");

        assertThrows(
                AccessDeniedException.class,
                () -> accountController.freezeAccount("TEST-ACCOUNT")
        );
    }

    @Test
    void tellerCannotManageCustomers() {
        authenticateAs("TELLER");

        assertThrows(
                AccessDeniedException.class,
                () -> customerController.getAllCustomers()
        );
    }

    @Test
    void managerCanAccessCustomerManagement() {
        authenticateAs("MANAGER");

        assertDoesNotThrow(
                () -> customerController.getAllCustomers()
        );
    }

    @Test
    void unauthenticatedUserCannotAccessDashboard() {
        SecurityContextHolder.clearContext();
        assertThrows(AuthenticationCredentialsNotFoundException.class,
            () -> dashboardController.getDashboard());
    }

    @Test
    void tellerCanAccessDashboard() {
        authenticateAs("TELLER");
        assertDoesNotThrow(() -> dashboardController.getDashboard());
    }

    @Test
    void unauthenticatedUserCannotAccessAccountDetails() {
        SecurityContextHolder.clearContext();
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> accountController.getAccount("TEST-ACCOUNT"));
    }

    @Test
    void unauthenticatedUserCannotAccessAccountBalance() {
        SecurityContextHolder.clearContext();
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> accountController.getBalance("TEST-ACCOUNT"));
    }

    @Test
    void unauthenticatedUserCannotAccessAccountTransactions() {
        SecurityContextHolder.clearContext();
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> accountController.getTransactions("TEST-ACCOUNT"));
    }

    @Test
    void unauthenticatedUserCannotTransferThroughGeneralAccountEndpoint() {
        SecurityContextHolder.clearContext();
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> accountController.transfer("TEST-ACCOUNT", null));
    }

    @Test
    void tellerCannotUseGeneralAccountTransferEndpoint() {
        authenticateAs("TELLER");
        assertThrows(AccessDeniedException.class,
                () -> accountController.transfer("TEST-ACCOUNT", null));
    }
}
