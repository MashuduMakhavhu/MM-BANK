package com.mmbank.backend.atm;

import com.mmbank.backend.account.BankAccount;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "atm_cards")
public class AtmCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 19)
    private String cardNumber;

    @Column(name = "pin_hash", nullable = false)
    private String pinHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private BankAccount account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AtmCardStatus status = AtmCardStatus.ACTIVE;

    @Column(nullable = false)
    private int failedAttempts = 0;

    private LocalDateTime lockedUntil;

    @Column(nullable = false)
    private LocalDate expiryDate;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected AtmCard() {
    }

    public AtmCard(String cardNumber, String pinHash,
                   BankAccount account, LocalDate expiryDate) {
        this.cardNumber = cardNumber;
        this.pinHash = pinHash;
        this.account = account;
        this.expiryDate = expiryDate;
    }

    public Long getId() { return id; }
    public String getCardNumber() { return cardNumber; }
    public String getPinHash() { return pinHash; }
    public BankAccount getAccount() { return account; }
    public AtmCardStatus getStatus() { return status; }
    public int getFailedAttempts() { return failedAttempts; }
    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void recordFailedAttempt() {
        failedAttempts++;
        if (failedAttempts >= 3) {
            lockedUntil = LocalDateTime.now().plusMinutes(15);
        }
    }

    public void resetFailedAttempts() {
        failedAttempts = 0;
        lockedUntil = null;
    }

    public void block() {
        status = AtmCardStatus.BLOCKED;
    }

    public boolean isExpired() {
        return !expiryDate.isAfter(LocalDate.now());
    }
}
