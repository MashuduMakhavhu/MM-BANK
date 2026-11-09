package com.mmbank.backend.auth;

import com.mmbank.backend.customer.Customer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(
            @Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        Customer customer = authService.authenticate(request);

        HttpSession oldSession = httpRequest.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }

        HttpSession session = httpRequest.getSession(true);
        session.setMaxInactiveInterval(900);
        session.setAttribute(
            "CUSTOMER_NUMBER", customer.getCustomerNumber());

        return new AuthResponse(
            customer.getCustomerNumber(),
            customer.getFullName(),
            "Login successful");
    }

    @GetMapping("/me")
    public AuthResponse me(HttpSession session) {
        Object number = session.getAttribute("CUSTOMER_NUMBER");

        if (number == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Please log in");
        }

        Customer customer = authService.findCustomer(number.toString());

        return new AuthResponse(
            customer.getCustomerNumber(),
            customer.getFullName(),
            "Authenticated");
    }

    @PostMapping("/logout")
    public AuthResponse logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        return new AuthResponse(null, null, "Logged out");
    }
}
