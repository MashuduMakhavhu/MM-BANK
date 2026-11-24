package com.mmbank.backend.atm;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.time.LocalDateTime;

public interface AtmCardRepository extends JpaRepository<AtmCard, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM AtmCard c WHERE c.cardNumber = :cardNumber")
    Optional<AtmCard> findByCardNumberForUpdate(
            @Param("cardNumber") String cardNumber);

    Optional<AtmCard> findByCardNumber(String cardNumber);

    long countByStatus(AtmCardStatus status);

    long countByLockedUntilAfter(LocalDateTime dateTime);

    boolean existsByCardNumber(String cardNumber);
}
