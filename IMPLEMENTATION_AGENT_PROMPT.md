# Parking Rental System: Implementation Agent Prompt

You are working in the repository `parking-rental-system/`.

Your task is to fully complete and improve the Parking Rental System according to the project specification and the existing codebase. Do not stop after analysis. Inspect the current implementation, make the required changes, run validation, and continue until the system is substantially complete.

## Existing architecture

Backend services:

- `eureka-server`: 8761
- `api-gateway`: 8080
- `auth-service`: 8081
- `user-service`: 8082
- `parking-service`: 8083
- `booking-service`: 8084
- `payment-service`: 8085
- `common-security`: shared Maven library

The frontend should be a React/Vite application on port 5173.

## Working rules

1. Begin with a focused audit of all Maven modules, controllers, DTOs, entities, repositories, services, security configuration, migrations, tests, and application configuration.
2. Preserve unrelated user changes. Never reset or overwrite them.
3. Implement fixes directly instead of only describing them.
4. Keep services isolated. Never access another service's database directly.
5. Use DTOs in controllers; do not expose JPA entities.
6. Use environment variables for credentials, JWT keys, Stripe keys, and database passwords.
7. Use existing project conventions where practical.
8. After each major phase, run the narrowest relevant test or build command.
9. Do not commit changes.
10. Finish with a complete build/test attempt and document remaining blockers.

## Phase 1: Build and project structure

- Add a root Maven reactor `pom.xml`, or make the complete build reliable through documented commands.
- Ensure `common-security` builds as a library and is resolved by dependent services.
- Configure the Spring Boot Maven plugin so `common-security` is not repackaged as an executable application.
- Remove or consolidate empty duplicate classes and inconsistent package structures.
- Add a root README with setup, environment variables, startup order, service ports, database setup, and test commands.

## Phase 2: JWT and security

Implement a reliable RS256 security design:

- Auth service signs access tokens with a configured private key.
- Gateway and every downstream service validate tokens using the same configured public key.
- Never generate a new signing key on every restart in normal operation.
- Load keys through environment variables or mounted files.
- Include `sub`, `role`, and unique `jti` claims.
- Use a 15-minute access-token expiry and seven-day refresh-token expiry.
- Store refresh tokens hashed.
- Deliver refresh tokens through an HttpOnly, Secure cookie where appropriate.
- Implement refresh-token rotation and revocation.
- Enforce logout and revoked access-token behavior in the gateway and services.
- Enable Spring method security.
- Add meaningful `@PreAuthorize` rules for renter, owner, and admin operations.
- Prevent users from self-registering as admin.
- Protect profiles, vehicles, listings, bookings, payments, and refunds with ownership and role checks.
- Permit the payment webhook only on its specific public endpoint and validate its gateway signature there.

## Phase 3: API contract

Align routes, methods, request DTOs, response DTOs, and field names with this contract.

### Auth

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `GET /api/auth/me`

### Users

- `GET /api/users/{userId}`
- `PUT /api/users/{userId}`
- `POST /api/users/{userId}/vehicles`
- `GET /api/users/{userId}/vehicles`
- `DELETE /api/users/{userId}/vehicles/{vehicleId}`

### Parking

- `POST /api/parking`
- `GET /api/parking/{id}`
- `PUT /api/parking/{id}`
- `DELETE /api/parking/{id}`
- `GET /api/parking?lat&lng&radiusKm&date&startTime&endTime&page&size`
- `GET /api/parking/owner/{ownerId}`
- `POST /api/parking/{id}/availability`
- `GET /api/parking/{id}/availability?from&to`
- `PUT /api/parking/slots/{slotId}/mark-booked`
- `PUT /api/parking/slots/{slotId}/mark-unbooked`

### Bookings

- `POST /api/bookings?renterId=`
- `GET /api/bookings/{id}`
- `GET /api/bookings/renter/{renterId}`
- `PATCH /api/bookings/{id}/cancel`
- `PATCH /api/bookings/{id}/status?status=`

### Payments

- `POST /api/payments/initiate`
- `POST /api/payments/webhook`
- `GET /api/payments/{id}`
- `GET /api/payments/booking/{bookingId}`
- `POST /api/payments/{id}/refund`

Use this response envelope consistently:

```json
{
  "success": true,
  "data": {},
  "error": null
}
```

Use the corresponding error envelope for validation, authorization, not-found, conflict, and internal errors.

## Phase 4: Booking and availability correctness

Fix booking creation so that it:

- Fails when the parking space or requested slot does not exist.
- Never uses a hard-coded fallback price.
- Calculates the amount from authoritative parking data.
- Validates renter, vehicle, parking space, date, and time.
- Rejects past bookings.
- Prevents overlapping bookings.
- Reserves availability through the parking service.
- Handles Feign failures explicitly.
- Prevents double booking with transactional or concurrency-safe logic.
- Releases availability when a booking is cancelled.
- Maintains booking status history.
- Restricts status changes to authorized internal, payment, or admin callers.

## Phase 5: Payment correctness

Implement or fix:

- Payment initiation using the authoritative booking amount.
- Amount mismatch rejection.
- Transaction and booking ownership validation.
- Stripe PaymentIntent creation and confirmation.
- Verification of external payment status before marking a payment successful.
- Refund ownership and amount validation.
- Refund amounts that cannot exceed the paid amount.
- Real Stripe refund calls when configured.
- Explicitly enabled simulation mode only when Stripe is unavailable or intentionally disabled.
- Webhook signature verification.
- Invalid webhook rejection with a non-2xx response.
- Idempotent webhook processing.
- Correct transaction and booking status transitions.

## Phase 6: Database and migrations

- Keep one database/schema per service.
- Ensure entities, repositories, DTOs, and migrations agree.
- Use Flyway consistently.
- Avoid relying on `ddl-auto=update` in production configuration.
- Use environment variables for database credentials.
- Add appropriate indexes and uniqueness constraints for email, refresh-token hashes, booking/slot conflicts, gateway references, and vehicle plate numbers.

## Phase 7: Frontend

Create a React/Vite frontend if it does not exist. Use React Router, Axios, a state solution such as Redux Toolkit or Context API, React Hook Form, Yup, and Stripe.js.

Implement:

- Axios client targeting `http://localhost:8080`.
- Authorization interceptor.
- Silent access-token refresh on 401.
- Refresh-token cookie support.
- Auth context/store containing user and access token.
- Login, registration, and logout.
- Protected routes and role-aware routing.
- Role-aware navigation.
- Parking search with location, date, time, price filters, and pagination.
- Listing cards and listing detail pages.
- Availability calendar.
- Booking checkout and confirmation flow.
- Stripe Elements payment flow where configured.
- Renter booking history and cancellation.
- Owner dashboard, listing CRUD, availability editor, and incoming bookings.
- Admin pages for users, bookings, and transactions where supported.
- Loading, error, empty, validation, and toast states.
- Responsive desktop and mobile layouts.

Match frontend field names exactly with backend DTOs.

## Phase 8: Infrastructure

Add:

- Dockerfiles where appropriate.
- `docker-compose.yml` for MySQL databases, Eureka, all backend services, the gateway, and frontend.
- Health checks and startup dependencies.
- Environment-variable configuration.
- CORS for the frontend origin.
- Separate development and production configuration.
- Optional Nginx configuration for serving the built frontend.

## Phase 9: Testing

Add focused tests for:

- Registration and login.
- JWT validation, expiry, refresh rotation, logout, and revocation.
- Role-based authorization.
- Profile and vehicle ownership.
- Listing ownership.
- Availability creation and updates.
- Booking creation and overlap conflicts.
- Slot reservation and release.
- Feign failure behavior.
- Payment amount and ownership validation.
- Refund limits.
- Webhook signature verification and duplicate webhook handling.
- Global error envelopes.
- Gateway routes.
- End-to-end booking and payment flow where infrastructure allows.

Run tests for every service, a complete Maven build from the repository root, and frontend install/lint/test/production-build commands. If MySQL or Stripe is unavailable, use test profiles or mocks and document the limitation.

## Final acceptance criteria

The implementation is acceptable only when:

1. The project builds from a clean checkout using documented commands.
2. JWT validation works consistently across the gateway and all services.
3. Unauthorized owner, renter, payment, refund, and admin operations are rejected.
4. Invalid or overlapping bookings are rejected.
5. Availability is reserved and released correctly.
6. Payment amounts come from authoritative booking data.
7. Webhooks are signature-verified and idempotent.
8. Backend routes and DTOs match the documented contract.
9. The frontend can complete: register -> login -> create listing -> search -> select slot -> book -> pay -> confirm booking.
10. Tests cover the security and business rules above.
11. The README explains how to configure, build, run, and test the complete system.

At the end, report:

- Completed changes.
- Files or modules changed.
- Test and build commands executed with results.
- Remaining blockers or known limitations.
