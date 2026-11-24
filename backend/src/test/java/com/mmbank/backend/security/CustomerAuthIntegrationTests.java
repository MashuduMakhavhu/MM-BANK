package com.mmbank.backend.security;

import com.mmbank.backend.account.*;
import com.mmbank.backend.auth.AppPinService;
import com.mmbank.backend.auth.CustomerCredentialRepository;
import com.mmbank.backend.customer.Customer;
import com.mmbank.backend.customer.CustomerRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CustomerAuthIntegrationTests {

    private static final String ACTIVATION_CODE =
            "TEST-ACTIVATION-2026";

    private static final String PIN = "482916";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customers;

    @Autowired
    private BankAccountRepository accounts;

    @Autowired
    private CustomerCredentialRepository credentials;

    @Autowired
    private AppPinService appPinService;

    @BeforeEach
    void cleanDatabase() {
        credentials.deleteAll();
        accounts.deleteAll();
        customers.deleteAll();
    }

    private Customer createCustomer(
            String customerNumber,
            String fullName
    ) {
        return customers.saveAndFlush(
                new Customer(
                        customerNumber,
                        fullName,
                        customerNumber.toLowerCase()
                                + "@test.mmbank",
                        "0712345678"
                )
        );
    }

    private void createAccount(
            Customer customer,
            String accountNumber,
            String accountHolderName
    ) {
        BankAccount account = new BankAccount(
                accountNumber,
                accountHolderName,
                AccountType.SAVINGS,
                new java.math.BigDecimal("1000.00")
        );

        account.setCustomer(customer);

        accounts.saveAndFlush(account);
    }

    private void createCustomerWithAccount(
            String customerNumber,
            String accountNumber,
            String fullName
    ) {
        Customer customer = createCustomer(
                customerNumber,
                fullName
        );

        createAccount(
                customer,
                accountNumber,
                fullName
        );
    }

    private String registrationJson(
            String accountNumber,
            String activationCode,
            String pin
    ) {
        return """
                {
                    "accountNumber": "%s",
                    "activationCode": "%s",
                    "appPin": "%s"
                }
                """.formatted(
                accountNumber,
                activationCode,
                pin
        );
    }

    private String loginJson(
            String accountNumber,
            String pin
    ) {
        return """
                {
                    "accountNumber": "%s",
                    "appPin": "%s"
                }
                """.formatted(
                accountNumber,
                pin
        );
    }

    private void activateCustomer(
            String accountNumber
    ) throws Exception {

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson(
                                accountNumber,
                                ACTIVATION_CODE,
                                PIN
                        )))
                .andExpect(status().isCreated());
    }

    private MockHttpSession loginCustomer(
            String accountNumber
    ) throws Exception {

        MvcResult result = mockMvc.perform(
                        post("/api/auth/login")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginJson(
                                        accountNumber,
                                        PIN
                                )))
                .andExpect(status().isOk())
                .andReturn();

        return (MockHttpSession) result
                .getRequest()
                .getSession(false);
    }

    @Test
    void validActivationStoresHashedPin() throws Exception {

        createCustomerWithAccount(
                "CUST001",
                "1000000001",
                "Test Customer"
        );

        activateCustomer("1000000001");

        var credential = credentials
                .findByCustomer_CustomerNumber("CUST001")
                .orElseThrow();

        assertNotEquals(
                PIN,
                credential.getAppPinHash()
        );

        assertTrue(
                appPinService.matches(
                        PIN,
                        credential.getAppPinHash()
                )
        );
    }

    @Test
    void invalidActivationCodeIsRejected() throws Exception {

        createCustomerWithAccount(
                "CUST002",
                "1000000002",
                "Test Customer"
        );

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson(
                                "1000000002",
                                "WRONG-CODE",
                                PIN
                        )))
                .andExpect(status().isForbidden());

        assertTrue(
                credentials
                        .findByCustomer_CustomerNumber("CUST002")
                        .isEmpty()
        );
    }

    @Test
    void accountHolderNameMismatchIsRejected() throws Exception {

        Customer customer = createCustomer(
                "CUST003",
                "Test Customer"
        );

        createAccount(
                customer,
                "1000000003",
                "Different Account Name"
        );

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson(
                                "1000000003",
                                ACTIVATION_CODE,
                                PIN
                        )))
                .andExpect(status().isForbidden());

        assertTrue(
                credentials
                        .findByCustomer_CustomerNumber("CUST003")
                        .isEmpty()
        );
    }

    @Test
    void accountNotLinkedToCustomerCannotBeActivated()
            throws Exception {

        BankAccount account = new BankAccount(
                "1000000004",
                "Unlinked Customer",
                AccountType.SAVINGS,
                new java.math.BigDecimal("1000.00")
        );

        accounts.saveAndFlush(account);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson(
                                "1000000004",
                                ACTIVATION_CODE,
                                PIN
                        )))
                .andExpect(status().isForbidden());
    }

    @Test
    void duplicateActivationIsRejected() throws Exception {

        createCustomerWithAccount(
                "CUST005",
                "1000000005",
                "Test Customer"
        );

        activateCustomer("1000000005");

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson(
                                "1000000005",
                                ACTIVATION_CODE,
                                PIN
                        )))
                .andExpect(status().isConflict());
    }

    @Test
    void validPinCreatesAuthenticatedSession() throws Exception {

        createCustomerWithAccount(
                "CUST006",
                "1000000006",
                "Test Customer"
        );

        activateCustomer("1000000006");

        MockHttpSession session =
                loginCustomer("1000000006");

        assertNotNull(
                session,
                "Login should create a session"
        );

        mockMvc.perform(get("/api/auth/me")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName")
                        .value("Test Customer"))
                .andExpect(jsonPath("$.message")
                        .value("Authenticated"))
                .andExpect(jsonPath("$.customerNumber")
                        .doesNotExist());
    }

    @Test
    void incorrectPinIsRejected() throws Exception {

        createCustomerWithAccount(
                "CUST007",
                "1000000007",
                "Test Customer"
        );

        activateCustomer("1000000007");

        mockMvc.perform(post("/api/auth/login")
                                .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(
                                "1000000007",
                                "111111"
                        )))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void fifthFailedAttemptLocksMobileLogin()
            throws Exception {

        createCustomerWithAccount(
                "CUST008",
                "1000000008",
                "Test Customer"
        );

        activateCustomer("1000000008");

        for (int attempt = 1; attempt <= 5; attempt++) {
            mockMvc.perform(post("/api/auth/login")
                                .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson(
                                    "1000000008",
                                    "111111"
                            )))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login")
                                .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(
                                "1000000008",
                                PIN
                        )))
                .andExpect(status().isLocked());
    }

    @Test
    void customerCanAccessAllTheirLinkedAccounts()
            throws Exception {

        Customer customer = createCustomer(
                "CUST009",
                "Customer One"
        );

        createAccount(
                customer,
                "1000000009",
                "Customer One"
        );

        createAccount(
                customer,
                "1000000010",
                "Customer One"
        );

        activateCustomer("1000000009");

        MockHttpSession session =
                loginCustomer("1000000009");

        mockMvc.perform(get("/api/mobile/accounts")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void customerCannotSeeAnotherCustomersAccounts()
            throws Exception {

        createCustomerWithAccount(
                "CUST010",
                "1000000011",
                "Customer One"
        );

        createCustomerWithAccount(
                "CUST011",
                "1000000012",
                "Customer Two"
        );

        activateCustomer("1000000011");

        MockHttpSession session =
                loginCustomer("1000000011");

        mockMvc.perform(get("/api/mobile/accounts")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].accountNumber")
                        .value("1000000011"));
    }

    @Test
    void customerCannotAccessAnotherCustomersAccountEndpoints()
            throws Exception {

        createCustomerWithAccount(
                "CUST013", "1000000014", "Customer One"
        );

        createCustomerWithAccount(
                "CUST014", "1000000015", "Customer Two"
        );

        activateCustomer("1000000014");
        MockHttpSession session = loginCustomer("1000000014");

        mockMvc.perform(get(
                        "/api/mobile/accounts/1000000015/balance")
                        .session(session))
                .andExpect(status().isNotFound());

        mockMvc.perform(get(
                        "/api/mobile/accounts/1000000015/transactions")
                        .session(session))
                .andExpect(status().isNotFound());

        mockMvc.perform(post(
                        "/api/mobile/accounts/1000000015/transfer")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipientAccountNumber": "1000000014",
                                  "amount": 10.00
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticatedCustomerCannotAccessMobileAccounts()
            throws Exception {

        mockMvc.perform(get("/api/mobile/accounts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutInvalidatesCustomerSession()
            throws Exception {

        createCustomerWithAccount(
                "CUST012",
                "1000000013",
                "Test Customer"
        );

        activateCustomer("1000000013");

        MockHttpSession session =
                loginCustomer("1000000013");

        assertNotNull(session);

        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Logged out"));

        assertTrue(session.isInvalid());

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
