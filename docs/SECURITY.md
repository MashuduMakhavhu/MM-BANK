M&M BANK – Security Documentation

1. Overview

Security is an important part of the M&M BANK banking simulation ecosystem.

The backend implements authentication, session management, password hashing, CSRF protection and access-control mechanisms to demonstrate security considerations in a multi-application banking environment.

The system supports three separate authentication workflows:

- Customer authentication
- ATM card authentication
- Staff authentication

M&M BANK is an educational project and has not undergone the security assessments required for production banking systems.

2. Password Security

The backend uses BCrypt password hashing through Spring Security's password encoder.

BCrypt is designed to make password verification computationally expensive, helping reduce the impact of exposed password hashes.

Passwords should never be stored as plain text.

3. Session Management

The system uses HTTP sessions to maintain authenticated application access.

Configured session timeouts include:

Session Type| Timeout
Customer| 15 minutes
ATM| 5 minutes
Staff| 15 minutes

Separate authentication services manage the different application workflows.

The backend also configures session cookie properties, including HTTP-only and secure-cookie settings.

For HTTPS deployments, secure cookie configuration must be enabled and cookie settings must be aligned with the deployment environment.

4. CSRF Protection

The backend implements Cross-Site Request Forgery protection using Spring Security's "CookieCsrfTokenRepository".

The CSRF token is made available to frontend applications so that protected requests can include the appropriate token.

CSRF protection is particularly important when applications use cookie-based session authentication.

5. Staff Authorisation

The staff authentication system supports role-based access control.

Implemented staff roles include:

- ADMIN
- MANAGER
- TELLER

Method-level security is enabled through Spring Security.

Authorisation checks help restrict sensitive operations to staff members with the required permissions.

6. ATM Security

ATM authentication is handled separately from customer and staff authentication.

The ATM workflow includes:

- Card-based authentication.
- PIN verification through the ATM PIN service.
- ATM session management.
- Card status validation.
- Card administration.
- Restrictions on unauthorised transaction operations.

ATM sessions have a configured timeout of five minutes.

7. Account and Transaction Validation

The backend performs server-side validation before processing sensitive account operations.

Examples include:

- Validating transaction amounts.
- Checking account status.
- Preventing transfers to the same account.
- Checking available funds.
- Locking accounts during transfer processing.
- Validating ATM card status.
- Checking recipient account information.

These checks help protect application consistency and reduce invalid or unauthorised operations.

8. Database and Configuration Security

The project uses environment-based configuration for database credentials and deployment-specific settings.

Local database files, backups, environment files and other sensitive development artefacts are excluded through ".gitignore".

Configuration values such as database credentials and PIN-related secrets must not be committed to GitHub.

Deployment environments should provide their own secrets through secure environment-variable management.

9. Security Limitations and Future Improvements

Although the project implements several security mechanisms, it is not a production banking platform.

Further security work would be required before any real-world deployment.

Potential improvements include:

- Independent penetration testing.
- Formal security threat modelling.
- Comprehensive audit logging.
- Stronger secrets-management practices.
- Rate limiting and abuse prevention.
- Additional authentication controls.
- Security monitoring and alerting.
- Database migration management.
- Deployment hardening and HTTPS enforcement.
- Detailed authorisation testing for every sensitive endpoint.

10. Responsible Use

M&M BANK is intended for software engineering education, demonstration and portfolio evaluation.

It must not be used to store real customer information, process real money or serve as a replacement for a regulated banking platform.

Security features demonstrated in this project should not be interpreted as certification or proof of production readiness.

