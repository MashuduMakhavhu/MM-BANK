package com.mmbank.backend.staff;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StaffAuthService {

    private final StaffAccountRepository staffAccounts;
    private final PasswordEncoder passwordEncoder;

    public StaffAuthService(
            StaffAccountRepository staffAccounts,
            PasswordEncoder passwordEncoder
    ) {
        this.staffAccounts = staffAccounts;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public StaffAccount authenticate(StaffLoginRequest request) {
        StaffAccount staff = staffAccounts
                .findByStaffNumber(request.staffNumber())
                .orElseThrow(this::invalidCredentials);

        if (!staff.isActive()
                || !passwordEncoder.matches(
                        request.password(),
                        staff.getPasswordHash()
                )) {
            throw invalidCredentials();
        }

        return staff;
    }

    @Transactional(readOnly = true)
    public StaffAccount findByStaffNumber(String staffNumber) {
        return staffAccounts.findByStaffNumber(staffNumber)
                .filter(StaffAccount::isActive)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Staff session is invalid"
                ));
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid staff number or password"
        );
    }
}
