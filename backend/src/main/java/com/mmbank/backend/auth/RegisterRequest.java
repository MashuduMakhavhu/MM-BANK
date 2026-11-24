package com.mmbank.backend.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterRequest(
    @NotBlank String accountNumber,
    @NotBlank String activationCode,
    @NotBlank @Pattern(regexp = "\\d{6}") String appPin
) {}
