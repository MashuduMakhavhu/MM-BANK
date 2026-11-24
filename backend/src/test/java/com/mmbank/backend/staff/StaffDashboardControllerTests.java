package com.mmbank.backend.staff;

import com.mmbank.backend.account.AccountStatus;
import com.mmbank.backend.account.BankAccount;
import com.mmbank.backend.account.BankAccountRepository;
import com.mmbank.backend.account.BankTransaction;
import com.mmbank.backend.account.BankTransactionRepository;
import com.mmbank.backend.account.TransactionType;
import com.mmbank.backend.atm.AtmCardRepository;
import com.mmbank.backend.atm.AtmCardStatus;
import com.mmbank.backend.customer.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaffDashboardControllerTests {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private BankAccountRepository accountRepository;

    @Mock
    private BankTransactionRepository transactionRepository;

    @Mock
    private AtmCardRepository cardRepository;

    @InjectMocks
    private StaffDashboardController dashboardController;

    @Test
    void dashboardReturnsOperationalCountsAndRecentActivity() {
        when(customerRepository.count()).thenReturn(12L);

        when(accountRepository.count()).thenReturn(18L);
        when(accountRepository.countByStatus(AccountStatus.ACTIVE)).thenReturn(14L);
        when(accountRepository.countByStatus(AccountStatus.FROZEN)).thenReturn(2L);
        when(accountRepository.countByStatus(AccountStatus.SUSPENDED)).thenReturn(1L);

        when(cardRepository.count()).thenReturn(15L);
        when(cardRepository.countByStatus(AtmCardStatus.ACTIVE)).thenReturn(11L);
        when(cardRepository.countByStatus(AtmCardStatus.BLOCKED)).thenReturn(2L);
        when(cardRepository.countByStatus(AtmCardStatus.EXPIRED)).thenReturn(1L);
        when(cardRepository.countByLockedUntilAfter(any(LocalDateTime.class))).thenReturn(1L);

        BankAccount account = mock(BankAccount.class);
        when(account.getAccountNumber()).thenReturn("1000000001");
        when(account.getAccountHolderName()).thenReturn("Test Customer");

        LocalDateTime transactionTime = LocalDateTime.of(2026, 11, 24, 8, 0);

        BankTransaction transaction = mock(BankTransaction.class);
        when(transaction.getId()).thenReturn(25L);
        when(transaction.getAccount()).thenReturn(account);
        when(transaction.getType()).thenReturn(TransactionType.DEPOSIT);
        when(transaction.getAmount()).thenReturn(new BigDecimal("500.00"));
        when(transaction.getBalanceAfter()).thenReturn(new BigDecimal("1500.00"));
        when(transaction.getCreatedAt()).thenReturn(transactionTime);

        when(transactionRepository.findTop10ByOrderByCreatedAtDesc())
                .thenReturn(List.of(transaction));

        StaffDashboardResponse response = dashboardController.getDashboard();

        assertEquals(12L, response.totalCustomers());
        assertEquals(18L, response.totalAccounts());
        assertEquals(14L, response.activeAccounts());
        assertEquals(2L, response.frozenAccounts());
        assertEquals(1L, response.suspendedAccounts());

        assertEquals(15L, response.totalCards());
        assertEquals(11L, response.activeCards());
        assertEquals(2L, response.blockedCards());
        assertEquals(1L, response.expiredCards());
        assertEquals(1L, response.temporarilyLockedCards());

        assertEquals(1, response.recentActivity().size());

        StaffDashboardResponse.RecentActivity activity =
                response.recentActivity().get(0);

        assertEquals(25L, activity.transactionId());
        assertEquals("1000000001", activity.accountNumber());
        assertEquals("Test Customer", activity.accountHolderName());
        assertEquals(TransactionType.DEPOSIT, activity.type());
        assertEquals(new BigDecimal("500.00"), activity.amount());
        assertEquals(new BigDecimal("1500.00"), activity.balanceAfter());
        assertEquals(transactionTime, activity.createdAt());
    }

    @Test
    void dashboardReturnsEmptyActivityWhenNoTransactionsExist() {
        when(customerRepository.count()).thenReturn(0L);
        when(accountRepository.count()).thenReturn(0L);
        when(accountRepository.countByStatus(any(AccountStatus.class))).thenReturn(0L);
        when(cardRepository.count()).thenReturn(0L);
        when(cardRepository.countByStatus(any(AtmCardStatus.class))).thenReturn(0L);
        when(cardRepository.countByLockedUntilAfter(any(LocalDateTime.class))).thenReturn(0L);
        when(transactionRepository.findTop10ByOrderByCreatedAtDesc())
                .thenReturn(List.of());

        StaffDashboardResponse response = dashboardController.getDashboard();

        assertTrue(response.recentActivity().isEmpty());
        assertEquals(0L, response.totalCustomers());
        assertEquals(0L, response.totalAccounts());
        assertEquals(0L, response.totalCards());
    }
}
