package com.mmbank.backend.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateCustomerRequest(

    @NotBlank
    String customerNumber,

    @NotBlank
    String fullName,

    @NotBlank
    @Email
    String email,

    @NotBlank
    @Pattern(regexp = "^\\+?[0-9]{9,15}$")
    String phoneNumber

) {
}
