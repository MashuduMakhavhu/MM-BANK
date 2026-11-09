package com.mmbank.backend.account;

import jakarta.validation.constraints.NotBlank;

public record UpdateAccountIdentityRequest(
    @NotBlank String customerNumber,
    @NotBlank String fullName
) {}
