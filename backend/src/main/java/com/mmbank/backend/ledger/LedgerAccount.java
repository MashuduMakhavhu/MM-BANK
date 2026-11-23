package com.mmbank.backend.ledger;

import jakarta.persistence.*;

@Entity
@Table(
    name = "ledger_accounts",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "account_code")
    }
)
public class LedgerAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_code", nullable = false)
    private String accountCode;

    @Column(nullable = false)
    private String accountName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LedgerAccountType accountType;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean requiresBankAccount = false;

    protected LedgerAccount() {
    }

    public LedgerAccount(
            String accountCode,
            String accountName,
            LedgerAccountType accountType) {

        this(accountCode, accountName, accountType, false);
    }

    public LedgerAccount(
            String accountCode,
            String accountName,
            LedgerAccountType accountType,
            boolean requiresBankAccount) {

        this.accountCode = accountCode;
        this.accountName = accountName;
        this.accountType = accountType;
        this.requiresBankAccount = requiresBankAccount;
    }

    public Long getId() {
        return id;
    }

    public String getAccountCode() {
        return accountCode;
    }

    public String getAccountName() {
        return accountName;
    }

    public LedgerAccountType getAccountType() {
        return accountType;
    }

    public boolean isActive() {
        return active;
    }

    public boolean requiresBankAccount() {
        return requiresBankAccount;
    }

    public void deactivate() {
        this.active = false;
    }
}
