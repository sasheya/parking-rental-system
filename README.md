# Parking Rental System

Spring Boot backend microservices for renting parking spaces, with Eureka service discovery, an API gateway, RS256 JWT authentication, MySQL persistence, and Stripe-compatible payments.

## Modules

- `eureka-server`: service registry on `8761`
- `api-gateway`: gateway on `8080`
- `auth-service`: authentication on `8081`
- `user-service`: profiles and vehicles on `8082`
- `parking-service`: listings and availability on `8083`
- `booking-service`: bookings on `8084`
- `payment-service`: payments and webhooks on `8085`
- `common-security`: shared JWT library

## Local backend build

Global Maven is not required. Use a module Maven Wrapper from PowerShell:

```powershell
Push-Location common-security
.\mvnw.cmd -f ..\pom.xml test
Pop-Location
```

The root reactor builds `common-security` before the dependent services. If you start a service directly from its own folder, install the shared library first:

```powershell
cd common-security
.\mvnw.cmd install -DskipTests
```

## Configuration

Copy `.env.example` to `.env` and set values before starting services. Generate one RSA key pair and provide the Base64-encoded PKCS#8 private key as `JWT_PRIVATE_KEY` and X.509 public key as `JWT_PUBLIC_KEY`. Do not commit real keys or credentials.

`JWT_GENERATE_DEV_KEYS=true` is available only for short-lived local experiments. Tokens generated with ephemeral keys become invalid after auth-service restart.

MySQL must be installed and running, but you do not need to create the service databases manually. Each datasource URL includes `createDatabaseIfNotExist=true`, so the configured MySQL user creates its database on first startup. The user must have permission to create databases. Flyway then creates the service tables.

Set the same `INTERNAL_SERVICE_SECRET` value in booking and payment services for payment-to-booking calls. Leave `STRIPE_SECRET_KEY` empty to use the backend payment simulation mode.

## Docker Compose

```powershell
Copy-Item .env.example .env
# Edit .env with real values
 docker compose up --build
```

The backend API is available through the gateway at `http://localhost:8080`.

## Quality checks

Run the reactor from the repository root using the wrapper in `common-security`:

```powershell
common-security\mvnw.cmd -f pom.xml test
common-security\mvnw.cmd -f pom.xml verify
```

`verify` runs the aggregate JaCoCo report at `target/site/jacoco-aggregate`. Sonar properties are read from `SONAR_HOST_URL` and `SONAR_TOKEN`; credentials are never stored in the repository. Run Sonar only when a server is available:

```powershell
common-security\mvnw.cmd -f pom.xml verify sonar:sonar
```

Application logs should be written under `logs/` when file logging is enabled by the runtime configuration. Generated logs and reports are ignored by Git. Endpoint evidence and current blockers are tracked in [docs/api-endpoint-verification.md](docs/api-endpoint-verification.md), with the quality summary in [docs/project-quality-report.md](docs/project-quality-report.md) and the local flow in [docs/e2e-demo.md](docs/e2e-demo.md).

## Coding standards

Keep controllers focused on HTTP mapping, services responsible for business rules, and repositories responsible for persistence. Use Bean Validation at request boundaries, domain-specific failures for business conflicts, SLF4J for application logging, and tests that assert observable behavior for both success and failure paths. Do not log credentials, tokens, payment data, or private key material.

## Quality remediation agent prompt

The detailed prompt for verifying every API endpoint, fixing backend integration issues, adding custom exception handling, implementing SLF4J/AOP file logging, expanding JUnit/Mockito tests, configuring JaCoCo/SonarQube, and producing verification reports is available at:

- [docs/api-quality-remediation-agent-prompt.md](docs/api-quality-remediation-agent-prompt.md)

The prompt requires fresh evidence for compilation, tests, coverage, endpoint behavior, and the end-to-end booking/payment flow.

## Security notes

- Access tokens expire after 15 minutes by default.
- Refresh tokens expire after seven days and are intended for HttpOnly cookies.
- Webhook requests are publicly routable only at the payment webhook endpoint and require a configured Stripe signature secret.
- Services validate JWTs independently; the gateway is not the only security boundary.

## Remaining production hardening

The backend keeps raw DTO responses on internal Feign routes for compatibility. Public success responses should be migrated to the standard envelope in a dedicated API-versioning change. Broader controller/integration tests, distributed revocation storage, observability, and deployment hardening are still recommended before production use.
