package com.mmbank.backend.atm;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/atm")
public class AtmAuthController {

    private final AtmAuthService authService;

    public AtmAuthController(AtmAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AtmLoginResponse login(
            @RequestBody AtmLoginRequest request,
            HttpServletRequest httpRequest) {

        AtmLoginResponse response = authService.authenticate(request);

        HttpSession oldSession = httpRequest.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }

        HttpSession session = httpRequest.getSession(true);
        session.setMaxInactiveInterval(300);
        session.setAttribute("ATM_ACCOUNT_NUMBER", response.accountNumber());
        session.setAttribute("ATM_CARD_NUMBER", request.cardNumber());
        session.setAttribute("ATM_CARD_MASKED", response.maskedCardNumber());

        return response;
    }

    @GetMapping("/session")
    public AtmLoginResponse session(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null
                || session.getAttribute("ATM_ACCOUNT_NUMBER") == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "No active ATM session.");
        }

        return new AtmLoginResponse(
                (String) session.getAttribute("ATM_ACCOUNT_NUMBER"),
                "",
                null,
                (String) session.getAttribute("ATM_CARD_MASKED")
        );
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }
    }
}
