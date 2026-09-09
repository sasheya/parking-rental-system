# Parking Rental System

Spring Boot microservices for renting parking spaces, with Eureka service discovery, an API gateway, RS256 JWT authentication, MySQL persistence, Stripe-compatible payments, and a React/Vite frontend.

## Modules

- `eureka-server`: service registry on `8761`
- `api-gateway`: gateway on `8080`
- `auth-service`: authentication on `8081`
- `user-service`: profiles and vehicles on `8082`
- `parking-service`: listings and availability on `8083`
- `booking-service`: bookings on `8084`
- `payment-service`: payments and webhooks on `8085`
- `common-security`: shared JWT library
- `frontend`: React application on `5173`

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

Set the same `INTERNAL_SERVICE_SECRET` value in booking and payment services for payment-to-booking calls. Set `VITE_STRIPE_PUBLISHABLE_KEY` in the frontend only when using real Stripe card entry; without it, the checkout uses the configured backend simulation mode.

## Docker Compose

```powershell
Copy-Item .env.example .env
# Edit .env with real values
 docker compose up --build
```

The browser client is available at `http://localhost:5173`; API traffic goes through `http://localhost:8080`.

## Frontend development

With Node.js 20 or newer installed:

```powershell
cd frontend
$env:Path = "C:\Program Files\nodejs;$env:Path"
& "C:\Program Files\nodejs\npm.cmd" install
& "C:\Program Files\nodejs\npm.cmd" run dev
```

Set `VITE_API_URL` to override the gateway URL. The frontend includes Axios token attachment and refresh handling, authentication, parking search, listing details, owner listing creation, slot selection, checkout, Stripe Elements support, and booking history.

## Security notes

- Access tokens expire after 15 minutes by default.
- Refresh tokens expire after seven days and are intended for HttpOnly cookies.
- Webhook requests are publicly routable only at the payment webhook endpoint and require a configured Stripe signature secret.
- Services validate JWTs independently; the gateway is not the only security boundary.

## Remaining production hardening

The backend keeps raw DTO responses on internal Feign routes for compatibility. Public success responses should be migrated to the standard envelope in a dedicated API-versioning change. Broader controller/integration tests, distributed revocation storage, observability, and deployment hardening are still recommended before production use.
