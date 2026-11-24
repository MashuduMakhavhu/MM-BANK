M&M BANK – API Documentation

1. Overview

The M&M BANK backend exposes REST API endpoints that connect the ATM application, mobile banking application and staff management portal.

The API is developed using Java 21 and Spring Boot.

Base URL (local development):

http://localhost:8080

All endpoints listed below are relative to the base URL.

The API uses JSON for request and response bodies where applicable.

2. Health

Method| Endpoint| Description
GET| "/health"| Checks backend availability

3. Customer Authentication

Base path: "/api/auth"

Method| Endpoint| Description
POST| "/register"| Registers a customer
POST| "/login"| Authenticates a customer
GET| "/me"| Retrieves the current customer session
POST| "/logout"| Ends the customer session
GET| "/csrf"| Provides CSRF token information

4. Account Management

Base path: "/api/accounts"

Method| Endpoint| Description
POST| "/"| Creates a bank account
GET| "/{accountNumber}"| Retrieves account information
GET| "/{accountNumber}/balance"| Retrieves account balance
POST| "/{accountNumber}/deposit"| Deposits funds
POST| "/{accountNumber}/withdraw"| Withdraws funds
POST| "/{accountNumber}/transfer"| Transfers funds
GET| "/{accountNumber}/transactions"| Retrieves transaction history
PATCH| "/{accountNumber}/freeze"| Freezes an account
PATCH| "/{accountNumber}/suspend"| Suspends an account
PATCH| "/{accountNumber}/activate"| Activates an account
PATCH| "/{accountNumber}/close"| Closes an account
PUT| "/{accountNumber}/holder-name"| Updates the account holder name

Administrative Account Identity

Method| Endpoint| Description
PUT| "/api/admin/accounts/{accountNumber}/identity"| Updates account identity information

5. Customer Management

Base path: "/api/customers"

Method| Endpoint| Description
POST| "/"| Creates a customer
GET| "/{customerNumber}"| Retrieves a customer
GET| "/"| Retrieves customer records

6. ATM API

Base path: "/api/atm"

ATM Authentication

Method| Endpoint| Description
POST| "/login"| Authenticates an ATM card
GET| "/session"| Checks the ATM session
POST| "/logout"| Ends the ATM session

ATM Transactions

Method| Endpoint| Description
GET| "/balance"| Retrieves the authenticated account balance
GET| "/transactions"| Retrieves transaction history
POST| "/withdraw"| Processes a withdrawal
POST| "/transfer"| Processes an account transfer
POST| "/deposit"| Processes a cash deposit
GET| "/recipient/{accountNumber}"| Looks up a transfer recipient

ATM Card Administration

Base path: "/api/atm/cards"

Method| Endpoint| Description
POST| "/"| Issues an ATM card
GET| "/{cardNumber}"| Retrieves ATM card information
PATCH| "/{cardNumber}/block"| Blocks an ATM card

7. Mobile Banking API

Base path: "/api/mobile"

Account Operations

Method| Endpoint| Description
GET| "/accounts"| Retrieves customer accounts
GET| "/accounts/{accountNumber}/balance"| Retrieves an account balance
GET| "/accounts/{accountNumber}/transactions"| Retrieves account transactions
POST| "/accounts/{accountNumber}/transfer"| Transfers funds from an account
GET| "/recipient/{accountNumber}"| Looks up a recipient

Primary Account Operations

Method| Endpoint| Description
GET| "/account"| Retrieves the primary account
GET| "/balance"| Retrieves the primary account balance
GET| "/transactions"| Retrieves primary account transactions
POST| "/transfer"| Transfers funds from the primary account

8. Staff API

Base path: "/api/staff-auth"

Method| Endpoint| Description
POST| "/login"| Authenticates staff
GET| "/me"| Retrieves the current staff session
POST| "/logout"| Ends the staff session

Staff Dashboard

Method| Endpoint| Description
GET| "/api/staff/dashboard"| Retrieves operational dashboard information

The dashboard includes customer, account and ATM card statistics, account and card exceptions, and recent transaction activity.

9. Authentication and Request Security

The backend uses session-based authentication for customer, ATM and staff workflows.

Security-related considerations include:

- Customer, ATM and staff sessions are managed separately.
- Session cookies are used for authenticated workflows.
- CSRF protection is implemented for relevant requests.
- Sensitive operations use application-level session checks and method-level authorisation where implemented.
- Requests must satisfy the relevant validation and account-status requirements.

Endpoints should not be assumed to be publicly usable simply because they are listed in this document.

10. Transaction Processing

The API supports several financial-operation simulations:

- Deposits
- Withdrawals
- Account transfers
- Balance enquiries
- Transaction history retrieval

Transfers are validated by the backend before account balances and transaction records are updated. Corresponding ledger entries are posted through the shared ledger services.

11. Error Handling

The backend includes a global exception-handling component:

"GlobalExceptionHandler"

It centralises application exception handling and provides structured error responses for supported failure scenarios.

Common failure scenarios include:

- Invalid request data.
- Unknown account numbers.
- Inactive or restricted accounts.
- Insufficient funds.
- Unauthorised operations.
- Invalid authentication details.

Exact HTTP status codes and response structures depend on the individual endpoint and exception handled.

12. API Scope and Limitations

This document describes the implemented backend route structure.

It does not represent a complete OpenAPI/Swagger specification. Request schemas, response examples, exact validation constraints and endpoint-specific authorisation requirements should be verified against the corresponding controller and service implementations.

M&M BANK is an educational banking simulation and must not be used for real financial transactions.
