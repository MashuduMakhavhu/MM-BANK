package com.mmbank.backend.account;
import jakarta.validation.constraints.NotBlank;
public record UpdateAccountHolderRequest(@NotBlank String accountHolderName) {}
