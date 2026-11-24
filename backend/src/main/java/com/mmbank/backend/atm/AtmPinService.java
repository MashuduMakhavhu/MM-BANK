package com.mmbank.backend.atm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Service
public class AtmPinService {

    private final PasswordEncoder passwordEncoder;
    private final String pepper;

    public AtmPinService(
            PasswordEncoder passwordEncoder,
            @Value("${MM_PIN_PEPPER:}") String pepper) {
        this.passwordEncoder = passwordEncoder;
        this.pepper = pepper;

        if (pepper == null || pepper.length() < 32) {
            throw new IllegalStateException(
                    "MM_PIN_PEPPER must contain at least 32 characters");
        }
    }

    public String encode(String pin) {
        validate(pin);
        return passwordEncoder.encode(hash(pin));
    }

    public boolean matches(String pin, String storedHash) {
        if (pin == null || !pin.matches("\\d{4}")
                || storedHash == null) {
            return false;
        }
        return passwordEncoder.matches(hash(pin), storedHash);
    }

    private String hash(String pin) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    pepper.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"));
            return HexFormat.of().formatHex(
                    mac.doFinal(pin.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to process ATM PIN", e);
        }
    }

    private void validate(String pin) {
        if (pin == null || !pin.matches("\\d{4}")) {
            throw new IllegalArgumentException(
                    "ATM PIN must contain exactly four digits");
        }
    }
}
