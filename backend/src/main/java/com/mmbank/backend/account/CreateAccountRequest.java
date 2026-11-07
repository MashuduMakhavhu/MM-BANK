package com.mmbank.backend.account;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CreateAccountRequest(

    @NotBlank
    String accountNumber,

    @NotBlank
    String accountHolderName,

    @NotNull
    AccountType accountType,

    @NotNull
    @DecimalMin("0.00")
    BigDecimal initialBalance

) {
}
