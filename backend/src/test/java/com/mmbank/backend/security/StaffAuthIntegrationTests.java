package com.mmbank.backend.security;

import com.mmbank.backend.staff.StaffAccount;
import com.mmbank.backend.staff.StaffAccountRepository;
import com.mmbank.backend.staff.StaffRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class StaffAuthIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StaffAccountRepository staffAccounts;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        staffAccounts.deleteAll();
    }

    private StaffAccount createStaff(String staffNumber, boolean active) {
        StaffAccount staff = new StaffAccount(
                staffNumber,
                "Test Staff",
                staffNumber.toLowerCase() + "@mmbank.test",
                passwordEncoder.encode("CorrectPassword123!"),
                StaffRole.TELLER
        );

        if (!active) {
            staff.deactivate();
        }

        return staffAccounts.saveAndFlush(staff);
    }

    @Test
    void validCredentialsCreateAuthenticatedSession() throws Exception {
        createStaff("STAFF001", true);

        MvcResult result = mockMvc.perform(post("/api/staff-auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "staffNumber": "STAFF001",
                                    "password": "CorrectPassword123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.staffNumber").value("STAFF001"))
                .andExpect(jsonPath("$.role").value("TELLER"))
                .andReturn();

        MockHttpSession session =
                (MockHttpSession) result.getRequest().getSession(false);

        assertNotNull(session, "Login should create an HTTP session");

        mockMvc.perform(get("/api/staff-auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.staffNumber").value("STAFF001"))
                .andExpect(jsonPath("$.fullName").value("Test Staff"));
    }

    @Test
    void invalidPasswordIsRejected() throws Exception {
        createStaff("STAFF002", true);

        mockMvc.perform(post("/api/staff-auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "staffNumber": "STAFF002",
                                    "password": "WrongPassword123!"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void inactiveStaffCannotLogin() throws Exception {
        createStaff("STAFF003", false);

        mockMvc.perform(post("/api/staff-auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "staffNumber": "STAFF003",
                                    "password": "CorrectPassword123!"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutInvalidatesStaffSession() throws Exception {
        createStaff("STAFF004", true);

        MvcResult loginResult = mockMvc.perform(post("/api/staff-auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "staffNumber": "STAFF004",
                                    "password": "CorrectPassword123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session =
                (MockHttpSession) loginResult.getRequest().getSession(false);

        assertNotNull(session);

        mockMvc.perform(post("/api/staff-auth/logout")
                        .with(csrf())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out"));

        assertTrue(session.isInvalid(), "Logout should invalidate the session");

        mockMvc.perform(get("/api/staff-auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
