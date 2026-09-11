# API Endpoint Verification

## Scope and evidence

Inventory source: controller and Feign annotations under `*/src/main/java`, gateway routes, and frontend API client. Public paths below are routed through `http://localhost:8080`; direct service ports are included for diagnostics. `PENDING` means the endpoint is inventoried but requires a running MySQL/Eureka stack and an authenticated test flow. `PASS` is reserved for fresh executable evidence.

Backend evidence collected on 2026-09-11:

- `common-security\mvnw.cmd -f pom.xml test`: PASS, 6 tests, 0 failures.
- `common-security\mvnw.cmd -f pom.xml verify`: PASS, reactor and JaCoCo aggregate goal completed.
- `common-security\mvnw.cmd -f pom.xml clean verify`: BLOCKED before compilation because OneDrive/Java tooling held `common-security\target\test-classes` open.
- `frontend\npm ci`: BLOCKED by Windows `EPERM` on `frontend\node_modules\@esbuild\win32-x64\esbuild.exe`; the subsequent build could not find `vite`.
- Real HTTP gateway/E2E verification: BLOCKED because the local service launches exited with code 1 and no stable MySQL/Eureka stack was available.

## Public endpoint matrix

| Method | Gateway path | Owner/direct path | Auth | Expected behavior | Verification |
|---|---|---|---|---|---|
| POST | `/api/auth/register` | auth:8081 same | Public | 201, `ApiResponse<AuthResponse>`; invalid DTO 400; duplicate 400 | PENDING; controller present |
| POST | `/api/auth/login` | auth:8081 same | Public | 200 and refresh cookie; invalid credentials 400 | PENDING; controller present |
| POST | `/api/auth/refresh` | auth:8081 same | Refresh cookie/body | 200 rotated tokens; invalid/revoked token 400 | PENDING; controller present |
| POST | `/api/auth/logout` | auth:8081 same | Access token optional, refresh cookie | 200 and expired refresh cookie | PENDING; controller present |
| GET | `/api/auth/validate?token=` | auth:8081 same | Public | 200 validation result; malformed token is a client error | PENDING; controller present |
| GET | `/api/auth/me` | auth:8081 same | Access token | 200 current user; missing token 401 | PENDING; controller present |
| GET | `/api/users/profile` | user:8082 same | Authenticated | 200 own profile | PENDING; controller present |
| PUT | `/api/users/profile` | user:8082 same | Authenticated | 200 updated own profile; validation 400 | PENDING; controller present |
| GET | `/api/users/{userId}` | user:8082 same | Authenticated/ownership policy | 200 profile or 404 | PENDING; controller present |
| PUT | `/api/users/{userId}` | user:8082 same | Authenticated/ownership policy | 200 update or 403/404 | PENDING; controller present |
| GET | `/api/users/{userId}/vehicles` | user:8082 same | Authenticated | 200 vehicle list | PENDING; controller present |
| POST | `/api/users/{userId}/vehicles` | user:8082 same | Authenticated | 201 vehicle; invalid DTO 400 | PENDING; controller present |
| DELETE | `/api/users/{userId}/vehicles/{vehicleId}` | user:8082 same | Authenticated | 204/200 delete; wrong owner 403; missing 404 | PENDING; controller present |
| GET | `/api/vehicles` | user:8082 same | Authenticated | 200 current user's vehicles | PENDING; controller present |
| POST | `/api/vehicles` | user:8082 same | Authenticated | 201 vehicle | PENDING; controller present |
| PUT | `/api/vehicles/{id}` | user:8082 same | Authenticated | 200 update or 404 | PENDING; controller present |
| DELETE | `/api/vehicles/{id}` | user:8082 same | Authenticated | 200/204 delete | PENDING; controller present |
| GET | `/api/parking` | parking:8083 same | Public | 200 listing collection | PENDING; controller present |
| GET | `/api/parking/{id}` | parking:8083 same | Public | 200 detail or 404 | PENDING; controller present |
| GET | `/api/parking/search` | parking:8083 same | Public | 200 filtered listings | PENDING; controller present |
| GET | `/api/parking/owner/my-listings` | parking:8083 same | Authenticated owner | 200 own listings | PENDING; controller present |
| GET | `/api/parking/owner/{ownerId}` | parking:8083 same | Authenticated | 200 owner listings | PENDING; controller present |
| POST | `/api/parking` | parking:8083 same | Authenticated owner | 201 listing; validation 400 | PENDING; controller present |
| PUT | `/api/parking/{id}` | parking:8083 same | Owner | 200 update; forbidden 403 | PENDING; controller present |
| DELETE | `/api/parking/{id}` | parking:8083 same | Owner | 204/200 delete; forbidden 403 | PENDING; controller present |
| GET | `/api/parking/{spaceId}/slots` | parking:8083 same | Public/auth policy | 200 slots | PENDING; controller present |
| GET | `/api/parking/{spaceId}/availability` | parking:8083 same | Public/auth policy | 200 availability | PENDING; controller present |
| POST | `/api/parking/{spaceId}/slots` | parking:8083 same | Owner | 201 slot | PENDING; controller present |
| POST | `/api/parking/{spaceId}/availability` | parking:8083 same | Owner | 201 availability | PENDING; controller present |
| PUT | `/api/parking/slots/{slotId}/status` | parking:8083 same | Owner | 200 status update | PENDING; controller present |
| POST | `/api/bookings` | booking:8084 same | Authenticated renter | 201 booking; unavailable slot/conflict 409 | PENDING; controller present |
| GET | `/api/bookings/my-bookings` | booking:8084 same | Authenticated | 200 own bookings | PENDING; controller present |
| GET | `/api/bookings/renter/{renterId}` | booking:8084 same | Authenticated/policy | 200 renter bookings | PENDING; controller present |
| GET | `/api/bookings/{id}` | booking:8084 same | Authenticated owner | 200 detail or 404/403 | PENDING; controller present |
| GET | `/api/bookings/space/{spaceId}` | booking:8084 same | Owner | 200 space bookings | PENDING; controller present |
| PUT/PATCH | `/api/bookings/{id}/status` | booking:8084 same | Authorized role | 200 state transition; invalid state 409 | PENDING; controller present |
| POST/PATCH | `/api/bookings/{id}/cancel` | booking:8084 same | Booking owner | 200 cancellation; invalid state 409 | PENDING; controller present |
| POST | `/api/payments/create-intent` | payment:8085 same | Authenticated | 200 payment intent/simulation | PENDING; controller present |
| POST | `/api/payments/initiate` | payment:8085 same | Authenticated | 201/200 transaction | PENDING; controller present |
| GET | `/api/payments/{id}` | payment:8085 same | Authenticated owner | 200 transaction or 404 | PENDING; controller present |
| POST | `/api/payments/confirm` | payment:8085 same | Authenticated | 200 confirmation; amount mismatch rejected | PENDING; controller present |
| GET | `/api/payments/booking/{bookingId}` | payment:8085 same | Authenticated owner | 200 payment lookup | PENDING; controller present |
| POST | `/api/payments/refund` | payment:8085 same | Authenticated owner | 200 refund; limit/state conflict 409 | PENDING; controller present |
| POST | `/api/payments/{id}/refund` | payment:8085 same | Authenticated owner | 200 refund | PENDING; controller present |
| POST | `/api/payments/webhook` | payment:8085 same | Stripe signature | 200 received only after signature/event validation | PENDING; signature must remain enforced |

## Internal service calls

These are not public gateway endpoints and must be protected by service credentials or network policy: parking internal detail/availability and slot transitions; booking internal detail/status; payment Feign calls to booking. They are included because failures here affect booking and payment correctness.

## Gateway and public infrastructure

- `GET /` is handled by the gateway controller.
- `GET /actuator/**`, `/v3/api-docs/**`, `/swagger-ui/**`, and `/webjars/**` are configured as public/documentation paths; review exposure before production.
- Gateway routes are configured for `/api/auth/**`, `/api/users/**`, `/api/vehicles/**`, `/api/parking/**`, `/api/bookings/**`, and `/api/payments/**` using Eureka load-balanced URIs.
- Gateway security permits `OPTIONS`, auth, actuator, documentation, and public parking GET requests; all other requests require authentication.

## Remaining verification blockers

1. Start MySQL, Eureka, and all services with non-secret environment variables before running HTTP checks.
2. Release the OneDrive/Java language-server lock on `common-security/target` and `frontend/node_modules` before repeating clean/npm-ci gates.
3. Add MockMvc tests for controllers and focused business-rule tests for the services currently represented only by context tests.
