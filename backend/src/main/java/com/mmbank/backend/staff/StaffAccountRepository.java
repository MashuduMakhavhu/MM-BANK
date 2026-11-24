package com.mmbank.backend.staff;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StaffAccountRepository
        extends JpaRepository<StaffAccount, Long> {

    Optional<StaffAccount> findByStaffNumber(String staffNumber);

    Optional<StaffAccount> findByEmailIgnoreCase(String email);

    boolean existsByStaffNumber(String staffNumber);

    boolean existsByEmailIgnoreCase(String email);
}
