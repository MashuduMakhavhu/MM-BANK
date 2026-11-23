package com.mmbank.backend.ledger;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "financial_transactions",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "transaction_reference"),
        @UniqueConstraint(columnNames = "idempotency_key")
    }
)
public class FinancialTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
        name = "transaction_reference",
        nullable = false,
        unique = true,
        updatable = false
    )
    private String transactionReference;

    @Column(
        name = "idempotency_key",
        unique = true,
        length = 100,
        updatable = false
    )
    private String idempotencyKey;

    @Column(nullable = false, length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FinancialTransactionStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime postedAt;

    protected FinancialTransaction() {
    }

    public FinancialTransaction(String description) {
        this(description, null);
    }

    public FinancialTransaction(String description, String idempotencyKey) {
        this.transactionReference = UUID.randomUUID().toString();
        this.description = description;
        this.idempotencyKey = idempotencyKey;
        this.status = FinancialTransactionStatus.DRAFT;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getDescription() {
        return description;
    }

    public FinancialTransactionStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getPostedAt() {
        return postedAt;
    }

    public void markAsPosted() {
        if (this.status != FinancialTransactionStatus.DRAFT) {
            throw new IllegalStateException(
                "Only draft transactions can be posted"
            );
        }

        this.status = FinancialTransactionStatus.POSTED;
        this.postedAt = LocalDateTime.now();
    }
}
