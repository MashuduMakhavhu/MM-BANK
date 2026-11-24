package com.mmbank.backend.staff;

import com.mmbank.backend.account.AccountStatus;
import com.mmbank.backend.account.BankAccountRepository;
import com.mmbank.backend.account.BankTransaction;
import com.mmbank.backend.account.BankTransactionRepository;
import com.mmbank.backend.atm.AtmCardRepository;
import com.mmbank.backend.atm.AtmCardStatus;
import com.mmbank.backend.customer.CustomerRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/staff/dashboard")
public class StaffDashboardController {

    private final CustomerRepository customerRepository;
    private final BankAccountRepository accountRepository;
    private final BankTransactionRepository transactionRepository;
    private final AtmCardRepository cardRepository;

    public StaffDashboardController(
            CustomerRepository customerRepository,
            BankAccountRepository accountRepository,
            BankTransactionRepository transactionRepository,
            AtmCardRepository cardRepository) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.cardRepository = cardRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TELLER')")
    @Transactional(readOnly = true)
    public StaffDashboardResponse getDashboard() {
        List<StaffDashboardResponse.RecentActivity> activity =
                transactionRepository.findTop10ByOrderByCreatedAtDesc()
                        .stream()
                        .map(this::toRecentActivity)
                        .toList();

        return new StaffDashboardResponse(
                customerRepository.count(),
                accountRepository.count(),
                accountRepository.countByStatus(AccountStatus.ACTIVE),
                accountRepository.countByStatus(AccountStatus.FROZEN),
                accountRepository.countByStatus(AccountStatus.SUSPENDED),
                cardRepository.count(),
                cardRepository.countByStatus(AtmCardStatus.ACTIVE),
                cardRepository.countByStatus(AtmCardStatus.BLOCKED),
                cardRepository.countByStatus(AtmCardStatus.EXPIRED),
                cardRepository.countByLockedUntilAfter(LocalDateTime.now()),
                activity
        );
    }

    private StaffDashboardResponse.RecentActivity toRecentActivity(
            BankTransaction transaction) {
        return new StaffDashboardResponse.RecentActivity(
                transaction.getId(),
                transaction.getAccount().getAccountNumber(),
                transaction.getAccount().getAccountHolderName(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getBalanceAfter(),
                transaction.getCreatedAt()
        );
    }
}
