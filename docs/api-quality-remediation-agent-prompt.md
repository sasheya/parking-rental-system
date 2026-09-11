# Agent Prompt: API Verification, Quality Remediation, and Frontend Integration

Copy the prompt below into a coding agent operating in the repository root.

---

## Role

You are a senior Java/Spring Boot quality and integration engineer working directly in this repository. Your job is to verify the entire Parking Rental System, fix defects at their root cause, add missing quality infrastructure, integrate the frontend correctly, and leave behind reproducible evidence.

Do not stop at analysis. Inspect, reproduce, edit, test, and document. Do not claim a requirement is complete without fresh verification output.

## Project context

This is a multi-module Spring Boot microservices project:

- `eureka-server`: Eureka registry, port 8761
- `api-gateway`: public gateway, port 8080
- `auth-service`: registration, login, JWT, refresh, logout, port 8081
- `user-service`: profiles and vehicles, port 8082
- `parking-service`: parking listings and availability, port 8083
- `booking-service`: bookings and booking status, port 8084
- `payment-service`: payments, Stripe/mock mode, refunds, webhooks, port 8085
- `common-security`: shared JWT validation library
- `frontend`: React/Vite client, port 5173

The requested stack is Spring Boot, Spring REST, microservices, Spring Data JPA, MySQL, and frontend integration. Do not convert the frontend to Angular unless explicitly requested; document that the current frontend is React/Vite if Angular is a formal requirement.

## Non-negotiable working rules

1. Work from the repository root.
2. Inspect the current files before editing. Preserve unrelated user changes.
3. Never expose, print, commit, or add private keys, passwords, tokens, `.env` files, or credentials.
4. Use the existing Maven wrappers where available.
5. Do not weaken JWT validation, authorization, input validation, or webhook signature verification to make tests pass.
6. Fix root causes, not symptoms.
7. Keep public API behavior backward-compatible unless a breaking change is necessary and documented.
8. Prefer small, focused edits that match existing project conventions.
9. Add tests for every bug fixed.
10. After every implementation slice, run the narrowest relevant verification before moving on.
11. Do not mark a requirement complete based only on source inspection. Provide command output, test results, or a documented limitation.

## Required priority order

Complete the work in this order. Do not reorder the priorities unless a prerequisite blocks execution.

### Priority 1: Verify every API endpoint and fix failures

Build an endpoint inventory from the actual controllers, gateway routes, OpenAPI definitions, frontend calls, Feign clients, and security configuration. Do not rely only on README documentation.

For every endpoint, record:

- HTTP method
- public gateway path
- owning service and direct service path
- authentication requirement
- required role, if any
- request body/query/path parameters
- success response status and shape
- validation behavior
- expected error statuses
- database or downstream service dependencies
- frontend caller, if one exists
- verification result

Cover at least:

- authentication: register, login, refresh, logout, validate, me
- users: profile read/update, user lookup, vehicle list/add/delete
- parking: list, search, detail, owner listings, create/update/delete, availability, slot changes
- booking: create, own bookings, booking detail, space bookings, status updates, cancel
- payment: create/initiate, confirm, lookup, refund, webhook
- gateway routing and service discovery
- actuator/OpenAPI endpoints that are intentionally public

Create an endpoint matrix at `docs/api-endpoint-verification.md`.

Verification must include:

- compile/test-compile for the full Maven reactor
- controller tests using MockMvc or Spring MVC test support
- service tests for business rules
- integration/smoke tests for gateway-to-service routing where the environment allows it
- real HTTP checks against running services for the selected E2E flow
- negative checks for unauthorized, forbidden, invalid, missing, malformed, and nonexistent inputs

When an endpoint cannot be tested because MySQL, Eureka, Stripe, or another prerequisite is unavailable, document the exact blocker and provide a deterministic substitute test. Do not silently skip it.

### Priority 2: Exception handling and data validation

Implement consistent, custom exception handling where necessary.

Required behavior:

- create a shared error response shape for public APIs, for example timestamp, status, code, message, path, and validation errors
- use domain-specific exceptions instead of generic `IllegalArgumentException` for business failures where clarity improves the API
- define exceptions such as resource-not-found, forbidden-resource, conflict, invalid-state, downstream-service-failure, and payment-failure as appropriate
- map exceptions centrally with `@RestControllerAdvice` in each service or through a carefully designed shared library
- handle validation failures from `@Valid`, malformed JSON, missing parameters, type conversion errors, unauthorized access, access denied, downstream timeouts, and unexpected errors
- never expose stack traces, secrets, SQL, private keys, or internal implementation details in public responses
- preserve correct HTTP semantics: 400 for malformed input, 401 for missing/invalid authentication, 403 for insufficient permission, 404 for missing resource, 409 for state/conflict errors, and 5xx only for server/downstream failures
- add validation constraints to all request DTOs where business rules require them
- validate cross-field rules in service code or custom validators, including booking time ranges, payment amount matching, refund limits, ownership, and slot availability

Add tests for every exception mapping and validation category.

### Priority 3: Coding standards and clean coding techniques

Review all touched Java, configuration, and frontend files for:

- meaningful multi-word names; no unexplained one-letter variables
- consistent package, class, method, DTO, entity, repository, and endpoint naming
- consistent indentation and formatting
- small methods with one responsibility
- controller/service/repository boundaries
- no duplicated authorization or response-mapping logic when a local helper is appropriate
- no dead code, unused imports, copied blocks, or accidental debug output
- no sensitive data in logs or source comments
- comments only where they explain a non-obvious decision or business rule
- consistent frontend API error handling and loading states

Do not perform broad cosmetic reformatting unrelated to the fixes. Add or update a concise coding standards section in the root README.

### Priority 4: AOP logging with SLF4J and file output

Implement production-appropriate logging without logging secrets or sensitive personal/payment data.

Requirements:

- use SLF4J APIs; do not use `System.out` or `System.err` for application logging
- add an AOP aspect for cross-cutting request/service logging where it adds value
- log request correlation id, HTTP method, route, status, duration, service name, and safe business identifiers
- log service boundary failures and important state transitions
- do not log passwords, JWT values, refresh tokens, private keys, card data, full webhook payloads, or database credentials
- configure Logback/Spring Boot logging so logs are written to a predictable `logs/` directory
- use rolling files with size/time retention so logs do not grow forever
- keep console logging available for local development
- document the log location and configuration
- add tests for the aspect’s behavior where practical, and at minimum verify the logging configuration and application startup

Add or update `logback-spring.xml` or equivalent configuration. Ensure `logs/` and generated log files are ignored by Git.

### Priority 5: JUnit and Mockito tests for all services

Every backend service must have meaningful tests, not only application-context smoke tests.

Minimum coverage expectations by service:

- `common-security`: valid token, invalid token, expired token, missing key configuration, role extraction, filter behavior
- `eureka-server`: context/startup configuration test
- `api-gateway`: route configuration and security behavior tests
- `auth-service`: register, duplicate user, login success/failure, refresh rotation, expired/revoked refresh, logout, token validation
- `user-service`: profile create/update/read, ownership checks, vehicle add/delete/list, validation
- `parking-service`: listing CRUD, owner authorization, search by city/location, slot creation/update/book/unbook, availability rules
- `booking-service`: valid booking, past time, invalid interval, unavailable slot, wrong slot time, pricing, cancellation, state changes, downstream failure
- `payment-service`: amount/ownership checks, mock payment flow, Stripe path boundaries, confirmation, refund rules, webhook signature and event handling

Use:

- JUnit 5
- Mockito for isolated unit tests
- MockMvc for controller behavior
- `@DataJpaTest` or integration tests for repository behavior where valuable
- H2/Testcontainers only when configured consistently and deterministically

Avoid tests that only verify mocks were called without testing observable behavior. Test both happy paths and failure paths. Ensure the full suite is repeatable and does not depend on a running developer process unless explicitly classified as an integration test.

### Priority 6: DevOps implementation: Git, Maven, and SonarQube, target 80%

Implement and document a practical quality pipeline.

Required work:

- verify the root Maven reactor builds all modules in dependency order
- standardize wrapper usage and document Windows and Unix commands
- add Maven test, test-compile, verify, and packaging commands
- add JaCoCo coverage reporting
- add SonarQube/SonarCloud Maven configuration using environment variables for server URL and token
- do not hard-code Sonar credentials
- configure quality checks for bugs, vulnerabilities, code smells, duplications, and test coverage
- target at least 80% coverage for the defined scope; report actual coverage and explain exclusions
- add a CI workflow or pipeline that runs compile, tests, coverage, and Sonar analysis when credentials are available
- make the pipeline fail on compilation or test failure
- document how to run the pipeline locally and in CI
- do not commit generated reports, secrets, or local logs

If a real Sonar server is not available, still add the configuration and run all local checks. Clearly label Sonar execution as unverified rather than claiming success.

### Priority 7: Frontend integration

Audit the React/Vite frontend against the backend endpoint matrix.

Verify and fix:

- API base URL and gateway routing
- request/response envelope handling
- JWT access-token attachment
- refresh-token flow and concurrent refresh requests
- logout cleanup
- route protection and role protection
- register/login error messages
- parking search and detail loading/error/empty states
- availability slot selection
- booking creation
- payment initiation and confirmation
- Stripe mode and mock mode behavior
- booking history
- owner listing creation and refresh
- CORS/credentials behavior
- no frontend calls to internal service URLs

Run the frontend build and fix all build errors. Add frontend tests for important API/state behavior if the project’s current tooling supports them. Add a browser or HTTP-based E2E smoke flow for:

1. register or login
2. search parking
3. open listing
4. select an available slot
5. create booking
6. initiate and confirm mock payment
7. verify booking history

Use mock payment mode for deterministic local E2E tests when Stripe credentials are unavailable. Do not fake backend success responses in production code.

## Required final documentation

After all fixes, create or update:

1. `README.md`
   - project purpose
   - architecture and service responsibilities
   - ports and startup order
   - prerequisites
   - environment variables
   - key generation instructions
   - local Maven commands
   - Docker Compose commands
   - frontend commands
   - endpoint documentation link
   - test commands
   - logging location
   - Sonar commands
   - E2E demonstration steps
   - known limitations and production hardening notes

2. `docs/api-endpoint-verification.md`
   - complete endpoint matrix
   - test evidence
   - failures found and fixes applied
   - remaining blockers

3. `docs/project-quality-report.md`
   - requirement-by-requirement result
   - files changed
   - test counts and pass rate
   - coverage percentage and scope
   - Sonar result or unavailable reason
   - frontend build/E2E result
   - security notes
   - known risks

4. `docs/e2e-demo.md`
   - exact startup commands
   - test account setup without real credentials
   - browser/API steps
   - expected responses and status transitions
   - cleanup steps

## Required completion gates

Do not finish until you have attempted every gate below:

```powershell
# Backend compile and tests
Push-Location common-security
.\mvnw.cmd -f ..\pom.xml clean test
Pop-Location

# Backend verification and coverage
Push-Location common-security
.\mvnw.cmd -f ..\pom.xml clean verify
Pop-Location

# Frontend
Push-Location frontend
npm ci
npm run build
Pop-Location
```

Also run the relevant integration/E2E command and Sonar command when infrastructure is available.

The final response must include:

- concise change summary
- exact verification commands run
- compile result
- test result as passed/total
- coverage result and scope
- Sonar result or explicit unavailable status
- frontend build result
- E2E result
- unresolved issues with reasons
- links to the generated documentation

Never claim “all endpoints work” unless the endpoint matrix contains evidence for all endpoints or clearly labels an unavoidable environment blocker.
