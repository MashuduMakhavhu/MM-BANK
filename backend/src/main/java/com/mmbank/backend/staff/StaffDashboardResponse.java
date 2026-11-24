package com.mmbank.backend.staff;

import com.mmbank.backend.account.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record StaffDashboardResponse(
    long totalCustomers,
    long totalAccounts,
    long activeAccounts,
    long frozenAccounts,
    long suspendedAccounts,
    long totalCards,
    long activeCards,
    long blockedCards,
    long expiredCards,
    long temporarilyLockedCards,
    List<RecentActivity> recentActivity
) {
    public record RecentActivity(
        Long transactionId,
        String accountNumber,
        String accountHolderName,
        TransactionType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        LocalDateTime createdAt
    ) {}
}
