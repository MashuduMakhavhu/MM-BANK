package com.mmbank.backend.atm;

public record AtmLoginRequest(
        String cardNumber,
        String pin
) {
}
