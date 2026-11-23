package com.mmbank.backend.ledger;

import com.mmbank.backend.account.BankAccount;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "financial_transaction_id", nullable = false)
    private FinancialTransaction financialTransaction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ledger_account_id", nullable = false)
    private LedgerAccount ledgerAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id")
    private BankAccount bankAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LedgerEntryType entryType;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    protected LedgerEntry() {
    }

    public LedgerEntry(
            FinancialTransaction financialTransaction,
            LedgerAccount ledgerAccount,
            BankAccount bankAccount,
            LedgerEntryType entryType,
            BigDecimal amount) {

        this.financialTransaction = financialTransaction;
        this.ledgerAccount = ledgerAccount;
        this.bankAccount = bankAccount;
        this.entryType = entryType;
        this.amount = amount;
    }

    public Long getId() {
        return id;
    }

    public FinancialTransaction getFinancialTransaction() {
        return financialTransaction;
    }

    public LedgerAccount getLedgerAccount() {
        return ledgerAccount;
    }

    public BankAccount getBankAccount() {
        return bankAccount;
    }

    public LedgerEntryType getEntryType() {
        return entryType;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
