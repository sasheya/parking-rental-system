# Local E2E Demonstration

This procedure is deterministic when Docker Desktop and the required local environment are available. Use test-only values; never use real credentials.

## Start

```powershell
Copy-Item .env.example .env
# Set MYSQL_ROOT_PASSWORD, JWT_PRIVATE_KEY, and JWT_PUBLIC_KEY in .env.
# Leave STRIPE_SECRET_KEY empty to use the backend simulation mode.
docker compose up --build
```

Wait for MySQL, Eureka, the gateway, and the application services to report healthy/started. The backend gateway is available at `http://localhost:8080`.

## Flow

1. Register a new test account with a unique email, or log in with an account created for this run.
2. Search parking through the gateway at `http://localhost:8080`.
3. Retrieve a listing and its availability slots.
4. Select an available slot and create a booking.
5. Initiate payment with the booking identifier. With no Stripe secret, the backend's configured simulation path is expected.
6. Confirm the simulated payment and verify the booking status transition.
7. Retrieve booking history and confirm the new booking is listed.

## API smoke equivalents

Use an HTTP client with the access token from login. The expected public paths and response envelopes are documented in `api-endpoint-verification.md`. Do not manually fabricate success responses; a failed prerequisite must remain visible.

## Cleanup

```powershell
docker compose down
# Add -v only when discarding the local MySQL test volume is intended.
```

Current run status: BLOCKED on 2026-09-11 because local service processes exited and MySQL/Eureka were not available as a stable running stack.
