package com.mmbank.backend.atm;

import com.mmbank.backend.account.AccountStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class AtmAuthService {

    private final AtmCardRepository cardRepository;
    private final AtmPinService pinService;

    public AtmAuthService(
            AtmCardRepository cardRepository,
            AtmPinService pinService) {
        this.cardRepository = cardRepository;
        this.pinService = pinService;
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public AtmLoginResponse authenticate(AtmLoginRequest request) {

        if (request == null
                || request.cardNumber() == null
                || !request.cardNumber().matches("\\d{16,19}")
                || request.pin() == null
                || !request.pin().matches("\\d{4}")) {
            throw invalidCredentials();
        }

        AtmCard card = cardRepository
                .findByCardNumberForUpdate(request.cardNumber())
                .orElseThrow(this::invalidCredentials);

        if (card.getStatus() != AtmCardStatus.ACTIVE
                || card.isExpired()) {
            throw invalidCredentials();
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lockedUntil = card.getLockedUntil();

        if (lockedUntil != null && lockedUntil.isAfter(now)) {
            throw new ResponseStatusException(
                    HttpStatus.LOCKED,
                    "Card temporarily locked. Please try again later.");
        }

        if (lockedUntil != null) {
            card.resetFailedAttempts();
        }

        if (!pinService.matches(request.pin(), card.getPinHash())) {
            card.recordFailedAttempt();
            cardRepository.save(card);

            if (card.getLockedUntil() != null) {
                throw new ResponseStatusException(
                        HttpStatus.LOCKED,
                        "Too many incorrect attempts. Card temporarily locked.");
            }

            throw invalidCredentials();
        }

        if (card.getAccount().getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Linked account is not active.");
        }

        card.resetFailedAttempts();
        cardRepository.save(card);

        String number = card.getCardNumber();
        String masked = "•••• •••• •••• " +
                number.substring(number.length() - 4);

        return new AtmLoginResponse(
                card.getAccount().getAccountNumber(),
                card.getAccount().getAccountHolderName(),
                card.getAccount().getAccountType(),
                masked
        );
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid card number or PIN.");
    }
}
