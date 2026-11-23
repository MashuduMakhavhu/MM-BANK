package com.mmbank.backend.ledger;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LedgerService {

    private final LedgerPostingService ledgerPostingService;

    public LedgerService(LedgerPostingService ledgerPostingService) {
        this.ledgerPostingService = ledgerPostingService;
    }

    public FinancialTransaction postTransaction(
            String description,
            List<LedgerEntryRequest> requests) {

        return ledgerPostingService.postTransaction(description, requests);
    }

    public FinancialTransaction postTransaction(
            String description,
            String idempotencyKey,
            List<LedgerEntryRequest> requests) {

        try {
            return ledgerPostingService.postTransaction(
                    description,
                    idempotencyKey,
                    requests
            );
        } catch (DataIntegrityViolationException exception) {

            FinancialTransaction existing =
                    ledgerPostingService.findMatchingTransaction(
                            description,
                            idempotencyKey,
                            requests
                    );

            if (existing != null) {
                return existing;
            }

            throw exception;
        }
    }
}
