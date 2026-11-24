package com.mmbank.backend.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Service
public class AppPinService {

    private final PasswordEncoder passwordEncoder;
    private final String pepper;

    public AppPinService(
            PasswordEncoder passwordEncoder,
            @Value("${MM_PIN_PEPPER:}") String pepper) {
        this.passwordEncoder = passwordEncoder;
        this.pepper = pepper;
    }

    public String encode(String pin) {
        validatePin(pin);
        return passwordEncoder.encode(pepperHash(pin));
    }

    public boolean matches(String pin, String storedHash) {
        if (pin == null || !pin.matches("\\d{6}")) {
            return false;
        }

        return passwordEncoder.matches(pepperHash(pin), storedHash);
    }

    private String pepperHash(String pin) {
        if (pepper == null || pepper.length() < 32) {
            throw new IllegalStateException(
                    "MM_PIN_PEPPER must contain at least 32 characters");
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(
                    pepper.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");

            mac.init(key);

            byte[] result = mac.doFinal(
                    pin.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(result);

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to process app PIN", exception);
        }
    }

    private void validatePin(String pin) {
        if (pin == null || !pin.matches("\\d{6}")) {
            throw new IllegalArgumentException(
                    "App PIN must contain exactly six digits");
        }
    }
}
