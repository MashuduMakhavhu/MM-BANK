package com.mmbank.backend.auth;

public record AuthResponse(
    String customerNumber,
    String fullName,
    String message
) {}
