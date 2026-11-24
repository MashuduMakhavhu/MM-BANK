
# M&M BANK

**A multi-application banking simulation ecosystem built with Java, Spring Boot and JavaScript.**

M&M BANK is a software engineering project that brings together three banking interfaces through a shared backend: an ATM application, a mobile banking application and a staff management portal.

The system simulates core banking operations while demonstrating backend development, application integration, authentication, transaction processing, database management and automated testing.

> **Disclaimer:** M&M BANK is an educational and portfolio project. It is not a licensed financial institution or a production-ready banking service. It must not be used to store real financial information or process real money.

## Project Overview

| Application | Description |
|---|---|
| ATM Interface | Simulates card-based access, balance enquiries, withdrawals, deposits and transfers. |
| Mobile Banking | Provides customer account information, transaction history, recipient lookup and transfers. |
| Staff Portal | Provides staff authentication, role-based access and an operational dashboard. |
| Shared Backend | Handles business logic, authentication, account operations, transaction processing and ledger records. |

## System Architecture

```text
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
```

## Core Features

### ATM Application
- Card-based authentication.
- Balance enquiries.
- Cash withdrawals.
- Cash deposits with an explicit destination account.
- Account transfers.
- Transaction history.
- Recipient lookup.
- ATM card administration.

### Mobile Banking Application
- Customer registration and login.
- Account information and balance enquiries.
- Transaction history.
- Recipient lookup.
- Account transfers.
- Customer session management.

### Staff Management Portal
- Staff authentication.
- Role-based access control.
- Operational dashboard.
- Customer and account statistics.
- ATM card statistics.
- Account and card exception monitoring.
- Recent transaction activity.

### Shared Backend
- Centralised account management.
- Customer, ATM and staff authentication.
- Transaction processing.
- Shared financial transaction records.
- Ledger accounts and ledger entries.
- Server-side validation and access controls.

## Technology Stack

| Category | Technologies |
|---|---|
| Backend | Java 21, Spring Boot 4 |
| Build Tool | Maven |
| Persistence | Spring Data JPA |
| Databases | H2, PostgreSQL |
| Security | Spring Security |
| Frontend | HTML, CSS, JavaScript |
| Mobile Integration | Capacitor |
| Containerisation | Docker |
| Testing | JUnit, Spring Boot Test |
| Version Control | Git, GitHub |

## Transaction Processing

The system uses a shared backend to process account operations across its applications.

Account transfers include validation, account status checks, balance verification and account locking. Corresponding transaction records are created, and debit and credit entries are posted through the shared ledger services.

This demonstrates the importance of transaction consistency and centralised financial record management.

## Project Structure

```text
MM-BANK/
├── .github/
│   └── workflows/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   └── test/
│   ├── Dockerfile
│   └── pom.xml
├── frontend/
│   ├── atm/
│   ├── mobile/
│   └── staff/
├── docs/
│   ├── ARCHITECTURE.md
│   ├── API.md
│   ├── SECURITY.md
│   └── TESTING.md
└── README.md
```

## Testing

The backend automated test suite recorded the following results on 24 November 2026:

| Metric | Result |
|---|---:|
| Tests executed | 107 |
| Failures | 0 |
| Errors | 0 |
| Skipped | 0 |
| Build status | SUCCESS |

Additional frontend JavaScript syntax checks were successfully completed for the ATM, mobile and staff applications.

These results reflect the recorded test run and do not guarantee the absence of defects.

## Technical Documentation

- [System Architecture](docs/ARCHITECTURE.md)
- [REST API Documentation](docs/API.md)
- [Security Documentation](docs/SECURITY.md)
- [Testing Documentation](docs/TESTING.md)

## Running Locally

The backend requires Java 21 and Maven.

From the backend directory, run:

```bash
./mvnw spring-boot:run
```

The backend is configured to use port `8080` by default.

The frontend applications are maintained separately under the `frontend` directory. Refer to the project configuration for further setup details.

## Project Goals

M&M BANK was developed to demonstrate practical software engineering skills through:

- Multi-application system integration.
- REST API development.
- Backend architecture and service design.
- Authentication and access control.
- Database persistence.
- Transaction processing and ledger management.
- Automated testing.
- Version control and technical documentation.

## Author

**Mashudu Makhavhu**

GitHub: [MashuduMakhavhu](https://github.com/MashuduMakhavhu)



