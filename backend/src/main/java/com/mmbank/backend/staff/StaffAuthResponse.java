package com.mmbank.backend.staff;

public record StaffAuthResponse(
        String staffNumber,
        String fullName,
        String role,
        String message
) {
}
