M&M BANK – System Architecture

1. Overview

M&M BANK is a multi-application banking simulation ecosystem developed to demonstrate software engineering principles, backend development, application integration, authentication, transaction processing and automated testing.

The ecosystem consists of three frontend applications connected to a shared backend:

- ATM Application
- Mobile Banking Application
- Staff Management Portal

All three applications communicate with the same Spring Boot backend through REST APIs.

Important: This is an educational and portfolio project, not a licensed banking institution or a production-ready financial service.

2. High-Level Architecture

The system follows a client-server architecture.

                  M&M BANK ECOSYSTEM

       +-------------+-------------+
       |             |             |
    ATM App      Mobile App    Staff Portal
       |             |             |
       +-------------+-------------+
                     |
                  REST APIs
                     |
             Spring Boot Backend
                     |
       +-------------+-------------+
       |             |             |
  Account       Authentication   ATM Services
  Services        Services
       |             |             |
       +-------------+-------------+
                     |
             Transaction Processing
                     |
                Shared Ledger
                     |
                  Database

The frontend applications provide separate interfaces for different banking operations, while the backend centralises business logic and data management.

3. Frontend Applications

3.1 ATM Application

The ATM application simulates common automated teller machine operations.

Its implemented functionality includes:

- Card-based authentication.
- Balance enquiries.
- Cash withdrawals.
- Cash deposits with an explicit destination account.
- Account transfers.
- Transaction history.
- Recipient lookup.
- Card administration through the backend.

The ATM frontend communicates with the backend through REST API requests.

3.2 Mobile Banking Application

The mobile application provides a customer-facing banking interface.

Its functionality includes:

- Customer registration and login.
- Account information.
- Balance enquiries.
- Transaction history.
- Recipient lookup.
- Account transfers.
- Customer session management.

The application uses JavaScript and is configured with Capacitor for Android integration.

3.3 Staff Management Portal

The staff portal provides an interface for authorised banking staff.

Its functionality includes:

- Staff authentication.
- Role-based access.
- Operational dashboard.
- Customer and account statistics.
- ATM card statistics.
- Account and card exception monitoring.
- Recent transaction activity.

The dashboard retrieves its information from backend endpoints rather than relying on hardcoded statistics.

4. Backend Architecture

The backend is developed using Java 21 and Spring Boot.

It follows a layered structure that separates responsibilities.

Controllers

Controllers expose REST endpoints and handle incoming HTTP requests.

Examples include:

- "AccountController"
- "AuthController"
- "AtmAuthController"
- "AtmTransactionController"
- "MobileAccountController"
- "StaffAuthController"
- "StaffDashboardController"

Services

Service classes contain application and business logic.

Examples include:

- "AccountService"
- "AuthService"
- "AtmAuthService"
- "AtmPinService"
- "StaffAuthService"
- "LedgerService"
- "LedgerPostingService"

Repositories

Spring Data JPA repositories provide database access and persistence operations.

Entities and Models

Entity classes represent persistent application data, including:

- Customers
- Bank accounts
- ATM cards
- Customer credentials
- Bank transactions
- Ledger accounts
- Ledger entries
- Financial transactions
- Staff accounts

5. Transaction Processing and Shared Ledger

Transaction processing is central to the M&M BANK ecosystem.

The backend handles deposits, withdrawals and transfers through shared account and transaction services.

Account transfers include several validation and consistency measures:

1. Validate the requested amount.
2. Prevent transfers between the same account.
3. Lock the affected accounts in a stable order.
4. Verify account status.
5. Check available funds.
6. Record the corresponding transaction information.
7. Post debit and credit entries to the shared ledger.

The ledger provides a record of financial transaction activity through financial transactions, ledger accounts and ledger entries.

This design demonstrates the importance of transaction consistency and maintaining a central record of account activity.

6. Authentication and Security

The system implements separate authentication flows for customers, ATM users and staff.

Security-related implementation includes:

- BCrypt password hashing.
- Session-based authentication.
- Customer, ATM and staff session timeouts.
- CSRF protection using "CookieCsrfTokenRepository".
- Role-based staff authorisation.
- Account and card status validation.
- Server-side validation of sensitive operations.

Sensitive operations use application-level session checks and method-level authorisation where implemented.

Security configuration is an ongoing engineering consideration, and the project should not be considered production-ready.

7. Database and Persistence

The backend uses Spring Data JPA for persistence.

H2 is configured for local development, while PostgreSQL support is included in the project configuration.

The database stores information relating to customers, accounts, authentication credentials, ATM cards, staff accounts, transactions and ledger records.

Local database files and backups are excluded from version control to avoid publishing development data.

8. Technology Stack

Area| Technology
Backend| Java 21
Framework| Spring Boot 4
Build tool| Maven
Persistence| Spring Data JPA
Databases| H2, PostgreSQL
Backend security| Spring Security
Frontend| HTML, CSS, JavaScript
Mobile integration| Capacitor
Containerisation| Docker
Version control| Git and GitHub
Automated testing| JUnit and Spring Boot testing support

9. Testing

The backend has an automated test suite covering account operations, authentication, authorisation, ATM operations, ledger processing, mobile account functionality and staff dashboard functionality.

The recorded test run on 24 November 2026 completed successfully:

- 107 tests
- 0 failures
- 0 errors
- 0 skipped

This result represents the recorded test run, not a guarantee that every possible application scenario has been tested.

10. Design Goals

The architecture was developed around the following goals:

- Shared backend logic across multiple applications.
- Separation of frontend and backend responsibilities.
- Centralised transaction processing.
- Consistent account and ledger records.
- Authentication and access control.
- Maintainable and testable application structure.
- Practical demonstration of full-stack software engineering.

11. Limitations

M&M BANK is a banking simulation intended for educational and portfolio purposes.

It has not undergone the independent security audits, regulatory assessment, operational resilience testing or financial compliance processes required of a real banking service.

It must not be used to process real money or store real customer financial information.
