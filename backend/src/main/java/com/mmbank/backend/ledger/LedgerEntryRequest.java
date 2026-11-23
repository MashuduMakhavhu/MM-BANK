package com.mmbank.backend.ledger;

import java.math.BigDecimal;

public record LedgerEntryRequest(
        String ledgerAccountCode,
        String bankAccountNumber,
        LedgerEntryType entryType,
        BigDecimal amount) {
}
