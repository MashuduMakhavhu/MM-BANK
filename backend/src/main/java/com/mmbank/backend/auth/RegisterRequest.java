package com.mmbank.backend.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank String customerNumber,
    @NotBlank String accountNumber,
    @NotBlank String activationCode,
    @NotBlank @Size(min = 10, max = 72) String password
) {}
