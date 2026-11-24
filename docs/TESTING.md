M&M BANK – Testing Documentation

1. Overview

Testing is an important part of the M&M BANK development process.

The backend includes an automated test suite designed to verify account operations, authentication, authorisation, ATM functionality, ledger processing, mobile banking operations and staff dashboard functionality.

The project uses Java testing tools integrated with the Maven build process.

2. Testing Technologies

Technology| Purpose
JUnit| Automated unit and integration testing
Spring Boot Test| Testing Spring Boot application components
Spring Security Test| Testing security and authorisation behaviour
Maven| Test execution and build management

3. Test Coverage Areas

3.1 Account Management

Account tests cover:

- Account creation.
- Account balance operations.
- Deposits and withdrawals.
- Transfers between accounts.
- Account validation.
- Account status restrictions.

3.2 ATM Operations

ATM tests cover:

- ATM authentication.
- PIN verification.
- Card administration.
- ATM transaction operations.
- Card authorisation.

3.3 Authentication and Security

Security tests cover:

- Customer authentication.
- Staff authentication.
- Session-related authentication behaviour.
- Staff authorisation.
- ATM card authorisation.

3.4 Mobile Banking

Mobile banking tests cover:

- Account retrieval.
- Customer account access.
- Recipient lookup and related validation.
- Mobile account operations.

3.5 Staff Management

Staff dashboard tests cover the retrieval of operational dashboard information.

3.6 Ledger Processing

Ledger tests cover the backend's financial transaction and ledger processing functionality.

4. Recorded Test Results

<escape>Recorded test run: 24 November 2026</escape>

The complete backend automated test suite completed successfully.

Result| Count
Tests executed| 107
Failures| 0
Errors| 0
Skipped| 0
Build result| SUCCESS

<escape>Result: 107 tests passed with no recorded failures, errors or skipped tests.</escape>

These results reflect the specific recorded test run and should not be interpreted as proof that every possible application scenario has been tested.

5. Additional Focused Test Results

The following focused test groups were also recorded as passing:

Test Class| Tests
MobileAccountControllerTests| 5
StaffAuthorizationTests| 11
StaffDashboardControllerTests| 2
CustomerAuthIntegrationTests| 13

These results are included within the broader testing history and should not be added to the full-suite total as separate tests.

6. Frontend Validation

JavaScript syntax validation was performed using Node.js for the following frontend files:

- "frontend/atm/script.js"
- "frontend/mobile/app.js"
- "frontend/mobile/www/app.js"
- "frontend/staff/app.js"

All four files passed the recorded syntax checks.

The mobile source application, web distribution copy and Android asset copy were also checked for matching SHA256 hashes.

7. Running the Tests

The backend tests can be executed from the backend directory using the Maven wrapper:

./mvnw test

A successful Maven test execution should report a successful build, provided all tests complete without failures or errors.

Java 21 is the configured project development version.

8. Testing Limitations

Automated tests improve confidence in application behaviour, but they do not guarantee the absence of defects.

Further testing would be valuable in areas such as:

- End-to-end testing across all three frontend applications.
- Performance and load testing.
- Concurrent transaction testing.
- Additional negative and boundary-case testing.
- Security penetration testing.
- Cross-device and browser compatibility.
- Deployment and infrastructure testing.

9. Conclusion

The automated testing suite provides a foundation for verifying important backend functionality across the M&M BANK ecosystem.

The recorded results demonstrate successful execution of 107 backend tests, alongside focused frontend syntax validation.

Testing remains an ongoing part of the project's development and improvement.
