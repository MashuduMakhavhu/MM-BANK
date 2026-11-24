package com.mmbank.backend.atm;

import com.mmbank.backend.account.AccountService;
import com.mmbank.backend.account.BankAccount;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/atm/cards")
public class AtmCardAdminController {

    private final AtmCardRepository cardRepository;
    private final AccountService accountService;
    private final AtmPinService pinService;
    private final SecureRandom random = new SecureRandom();

    public AtmCardAdminController(
            AtmCardRepository cardRepository,
            AccountService accountService,
            AtmPinService pinService) {
        this.cardRepository = cardRepository;
        this.accountService = accountService;
        this.pinService = pinService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TELLER')")
    @Transactional
    public AtmCardIssueResponse issueCard(
            @RequestBody AtmCardIssueRequest request) {

        if (request == null || request.accountNumber() == null
                || request.accountNumber().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Account number is required.");
        }

        BankAccount account = accountService.findAccount(
                request.accountNumber());

        if (account.getStatus() != com.mmbank.backend.account.AccountStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Account is not active.");
        }

        String pinHash = pinService.encode(request.pin());
        String cardNumber = generateCardNumber();
        LocalDate expiryDate = LocalDate.now().plusYears(4);

        AtmCard card = new AtmCard(
                cardNumber, pinHash, account, expiryDate);

        cardRepository.save(card);

        return new AtmCardIssueResponse(
                cardNumber,
                account.getAccountNumber(),
                expiryDate,
                "ATM card issued successfully. Record the card number securely."
        );
    }

    @GetMapping("/{cardNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TELLER')")
    @Transactional(readOnly = true)
    public AtmCardAdminResponse getCard(@PathVariable String cardNumber) {
        AtmCard card = cardRepository.findByCardNumber(cardNumber)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "ATM card not found."));

        return new AtmCardAdminResponse(
                card.getCardNumber(),
                card.getAccount().getAccountNumber(),
                card.getStatus(),
                card.getExpiryDate(),
                card.getCreatedAt()
        );
    }

    @PatchMapping("/{cardNumber}/block")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Transactional
    public AtmCardAdminResponse blockCard(@PathVariable String cardNumber) {
        AtmCard card = cardRepository.findByCardNumberForUpdate(cardNumber)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "ATM card not found."));

        if (card.getStatus() == AtmCardStatus.BLOCKED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "ATM card is already blocked.");
        }

        if (card.getStatus() != AtmCardStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Only active cards can be blocked.");
        }

        card.block();

        return new AtmCardAdminResponse(
                card.getCardNumber(),
                card.getAccount().getAccountNumber(),
                card.getStatus(),
                card.getExpiryDate(),
                card.getCreatedAt()
        );
    }

    private String generateCardNumber() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder number = new StringBuilder(16);
            for (int i = 0; i < 16; i++) {
                number.append(random.nextInt(10));
            }
            String candidate = number.toString();
            if (!candidate.startsWith("0")
                    && !cardRepository.existsByCardNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "Unable to generate a unique card number.");
    }
}
