package com.mmbank.backend.account;

import java.math.BigDecimal;

public record TransferResponse(
    String senderAccountNumber,
    String recipientAccountNumber,
    BigDecimal amount,
    TransactionResponse senderTransaction,
    TransactionResponse recipientTransaction
) {
}
