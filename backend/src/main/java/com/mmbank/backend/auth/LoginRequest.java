package com.mmbank.backend.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(
    @NotBlank String accountNumber,
    @NotBlank @Pattern(regexp = "\\d{6}") String appPin
) {}
