package com.mmbank.backend.atm;

public record AtmCardIssueRequest(
        String accountNumber,
        String pin
) {
}
