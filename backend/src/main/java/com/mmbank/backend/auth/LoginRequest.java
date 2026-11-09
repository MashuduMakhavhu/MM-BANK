package com.mmbank.backend.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank String customerNumber,
    @NotBlank String password
) {}
