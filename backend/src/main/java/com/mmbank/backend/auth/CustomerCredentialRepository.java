package com.mmbank.backend.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CustomerCredentialRepository
        extends JpaRepository<CustomerCredential, Long> {

    Optional<CustomerCredential> findByCustomer_CustomerNumber(
            String customerNumber);
}
