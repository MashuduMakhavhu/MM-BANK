package com.mmbank.backend.staff;

import jakarta.validation.constraints.NotBlank;

public record StaffLoginRequest(
        @NotBlank String staffNumber,
        @NotBlank String password
) {
}
