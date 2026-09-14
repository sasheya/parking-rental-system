# Parking Rental System: beginner-friendly architecture guide

This project is a Spring Boot microservices app for renting parking spaces. The core idea is:

- users sign up and log in
- owners create parking listings
- drivers search and book slots
- payments are created and confirmed
- the system keeps each business concern in a separate service
- a gateway and service discovery route traffic between services

The project is described in [README.md](../README.md), and the main runtime pieces are configured in [docker-compose.yml](../docker-compose.yml).

## 1. The big picture

Think of the system like a small city with separate departments:

- registration/authentication department: handles sign-up, login, token generation
- user department: stores profiles and vehicles
- parking department: stores listings and space availability
- booking department: manages reservations and booking lifecycle
- payment department: creates payment intents and confirms/refunds
- gateway: one front door for all requests
- discovery server: helps services find each other

The app uses:

- Java 17
- Spring Boot 4.1.1
- Spring Cloud
- Eureka for service discovery
- Spring Security + JWT
- MySQL per service
- Feign for inter-service calls
- Stripe integration for real/virtual payment flows

---

## 2. How the request moves through the system

### Request path through the API gateway

1. An API client sends requests to the API gateway at port 8080.
2. The gateway routes the request to the correct microservice using a path rule.
3. The destination service validates the JWT and applies its own business logic.
4. If a service needs data from another service, it does a service-to-service call (Feign client).
5. The service writes or reads MySQL data and returns a business response.

### Data and identity flow

- JWT access tokens are issued by the auth-service.
- The shared security library validates tokens in each service.
- The gateway checks JWTs before passing traffic onward.
- The auth-service also issues refresh tokens and stores them in a database.
- Each service trusts the signed JWT and reads the user id and role from it.

---

## 3. Service-by-service explanation

### 3.1 Eureka server

Files:

- [eureka-server/src/main/java/com/parking/eureka_server/EurekaServerApplication.java](../eureka-server/src/main/java/com/parking/eureka_server/EurekaServerApplication.java)
- [eureka-server/src/main/resources/application.properties](../eureka-server/src/main/resources/application.properties)

What it does:

- keeps a registry of all Spring Boot services
- gives other services a way to discover each other by name
- runs on port 8761

Why it matters:

- the gateway and services do not all need hardcoded IPs
- services register as names like auth-service, parking-service, booking-service

Status:

- Present and working as a registry pattern.
- Implementation level: done.

---

### 3.2 API gateway

Files:

- [api-gateway/src/main/java/com/parking/api_gateway/ApiGatewayApplication.java](../api-gateway/src/main/java/com/parking/api_gateway/ApiGatewayApplication.java)
- [api-gateway/src/main/resources/application.yml](../api-gateway/src/main/resources/application.yml)
- [api-gateway/src/main/java/com/parking/api_gateway/config/SecurityConfig.java](../api-gateway/src/main/java/com/parking/api_gateway/config/SecurityConfig.java)

What it does:

- acts as the single public entrypoint on port 8080
- routes traffic to auth-service, user-service, parking-service, booking-service, and payment-service
- exposes OpenAPI/Swagger documentation entries for each service
- validates public JWT access tokens before letting traffic continue

Example routing:

- `/api/auth/**` -> auth-service
- `/api/users/**` -> user-service
- `/api/parking/**` -> parking-service
- `/api/bookings/**` -> booking-service
- `/api/payments/**` -> payment-service

Status:

- Present and routing is configured.
- Implementation level: done for the intended gateway pattern.

---

### 3.3 Common security library

Files:

- [common-security/src/main/java/com/parking/common_security/JwtAuthenticationFilter.java](../common-security/src/main/java/com/parking/common_security/JwtAuthenticationFilter.java)
- [common-security/src/main/java/com/parking/common_security/JwtValidator.java](../common-security/src/main/java/com/parking/common_security/JwtValidator.java)
- [common-security/src/main/java/com/parking/common_security/JwtProperties.java](../common-security/src/main/java/com/parking/common_security/JwtProperties.java)

What it does:

- reads JWT public key configuration
- validates the bearer token on each request
- extracts the user id and role from the token
- puts the authenticated principal into Spring Security context

Important behavior:

- it skips auth routes and public endpoints
- it permits GET requests on parking listings without forcing an auth token
- for protected routes, it builds an authentication object from the JWT

The filter is the real auth enforcement mechanism used by service security configs.

Status:

- Present and central to auth across services.
- Implementation level: done.

---

### 3.4 Auth service

Files:

- [auth-service/src/main/java/com/parking/auth_service/controller/AuthController.java](../auth-service/src/main/java/com/parking/auth_service/controller/AuthController.java)
- [auth-service/src/main/java/com/parking/auth_service/service/AuthServiceImpl.java](../auth-service/src/main/java/com/parking/auth_service/service/AuthServiceImpl.java)
- [auth-service/src/main/java/com/parking/auth_service/service/JwtService.java](../auth-service/src/main/java/com/parking/auth_service/service/JwtService.java)
- [auth-service/src/main/java/com/parking/auth_service/model/User.java](../auth-service/src/main/java/com/parking/auth_service/model/User.java)
- [auth-service/src/main/java/com/parking/auth_service/model/RefreshToken.java](../auth-service/src/main/java/com/parking/auth_service/model/RefreshToken.java)

What it does:

- registers a new user
- logs a user in with email/password
- returns access token + refresh token
- stores hashes of refresh tokens for later validation
- supports token refresh and logout
- validates tokens and returns current-user details

Authentication flow:

1. An API client posts email + password to `/api/auth/login`.
2. Auth service loads the user from MySQL.
3. It checks password hash using Spring Security encoder.
4. It generates a JWT access token and a refresh token.
5. It returns both to the client, plus a secure HttpOnly refresh cookie.
6. The client sends the access token in the Authorization header.
7. On 401, the client calls `/api/auth/refresh`.

Security details:

- access tokens expire in 15 minutes by default
- refresh tokens expire after 7 days
- refresh tokens are hashed before storage
- logout revokes both current access token and user refresh tokens

Status:

- registration, login, refresh, logout, current-user lookup are implemented.
- Feature readiness: near complete for an MVP.

---

### 3.5 User service

Files:

- [user-service/src/main/java/com/parking/user_service/controller/UserController.java](../user-service/src/main/java/com/parking/user_service/controller/UserController.java)
- [user-service/src/main/java/com/parking/user_service/model/UserProfile.java](../user-service/src/main/java/com/parking/user_service/model/UserProfile.java)
- [user-service/src/main/java/com/parking/user_service/model/Vehicle.java](../user-service/src/main/java/com/parking/user_service/model/Vehicle.java)

What it does:

- stores user profile information
- stores vehicle information for each user
- allows owner or driver to update their profile
- allows users to add and delete vehicles

Example data model:

- user profile: user id, full name, contact, preferences
- vehicle: plate, model, type, user association

Access pattern:

- a user can only access their own profile and their own vehicle list
- the service checks the authenticated user id against the target resource

Status:

- profile and vehicle CRUD are implemented.
- Feature readiness: done for a basic user profile feature.

---

### 3.6 Parking service

Files:

- [parking-service/src/main/java/com/parking/parking_service/controller/ParkingSpaceController.java](../parking-service/src/main/java/com/parking/parking_service/controller/ParkingSpaceController.java)
- [parking-service/src/main/java/com/parking/parking_service/controller/AvailabilityController.java](../parking-service/src/main/java/com/parking/parking_service/controller/AvailabilityController.java)
- [parking-service/src/main/java/com/parking/parking_service/service/ParkingSpaceServiceImpl.java](../parking-service/src/main/java/com/parking/parking_service/service/ParkingSpaceServiceImpl.java)
- [parking-service/src/main/java/com/parking/parking_service/model/ParkingSpace.java](../parking-service/src/main/java/com/parking/parking_service/model/ParkingSpace.java)
- [parking-service/src/main/java/com/parking/parking_service/model/AvailabilitySlot.java](../parking-service/src/main/java/com/parking/parking_service/model/AvailabilitySlot.java)

What it does:

- stores parking space listings owned by users
- allows owners to create, update, and delete listings
- supports search by city or coordinates
- exposes availability slots for date/time ranges
- tracks slot booking state

Typical flow:

1. Owner creates a listing with title, address, city, price, and total slots.
2. Parking service stores the listing and sets available slots equal to total slots.
3. Driver searches spaces by city or location.
4. The UI shows a list of spaces and available slots.
5. Booking service validates the slot before creation.

Important internal contract:

- booking-service calls parking-service to fetch a parking space and availability slot
- booking-service marks a slot as booked or unbooked via internal secret headers

Status:

- listing CRUD, search, and slot logic are present.
- Feature readiness: done to MVP level.

---

### 3.7 Booking service

Files:

- [booking-service/src/main/java/com/parking/booking_service/controller/BookingController.java](../booking-service/src/main/java/com/parking/booking_service/controller/BookingController.java)
- [booking-service/src/main/java/com/parking/booking_service/service/BookingServiceImpl.java](../booking-service/src/main/java/com/parking/booking_service/service/BookingServiceImpl.java)
- [booking-service/src/main/java/com/parking/booking_service/model/Booking.java](../booking-service/src/main/java/com/parking/booking_service/model/Booking.java)
- [booking-service/src/main/java/com/parking/booking_service/model/BookingStatusHistory.java](../booking-service/src/main/java/com/parking/booking_service/model/BookingStatusHistory.java)

What it does:

- creates a booking for a parking slot
- validates slot timing and price
- ensures the selected slot is still free
- stores booking lifecycle and status history
- supports cancelling and updating booking status

State transitions:

- PENDING_PAYMENT
- CONFIRMED
- CANCELLED
- COMPLETED

Business logic:

- the system checks the parking space is active
- a selected slot must match the requested times
- booking total is computed from price per hour and duration
- slot is marked booked during the booking creation flow
- on payment confirmation, Booking Service is notified and booking status is updated

Status:

- booking creation, lookup, cancel, and status updates are implemented.
- Feature readiness: largely done for the core reservation flow.

---

### 3.8 Payment service

Files:

- [payment-service/src/main/java/com/parking/payment_service/controller/PaymentController.java](../payment-service/src/main/java/com/parking/payment_service/controller/PaymentController.java)
- [payment-service/src/main/java/com/parking/payment_service/service/PaymentServiceImpl.java](../payment-service/src/main/java/com/parking/payment_service/service/PaymentServiceImpl.java)
- [payment-service/src/main/java/com/parking/payment_service/controller/WebhookController.java](../payment-service/src/main/java/com/parking/payment_service/controller/WebhookController.java)
- [payment-service/src/main/java/com/parking/payment_service/model/Transaction.java](../payment-service/src/main/java/com/parking/payment_service/model/Transaction.java)

What it does:

- validates the booking before starting a payment
- creates a Stripe PaymentIntent when a Stripe secret is configured
- falls back to mock payment ids when no Stripe key is present
- confirms a payment and updates the booking status
- supports refund processing
- listens for Stripe webhook events

Important logic:

- payment-service calls Booking Service via Feign to confirm that the booking belongs to the user and to update booking status
- if Stripe is not configured, the app runs in simulation mode and uses fake payment IDs like `pi_mock_...`
- confirm-payment can succeed without live Stripe when the app is in simulation mode

Status:

- payment initiation, confirmation, refund logic, and webhooks are implemented.
- Feature readiness: done for a functional prototype / MVP; real production readiness still requires more verification and hardening.

---

---

## 4. Data model overview

The project is organized around a few core entities:

### Auth data

- User
- RefreshToken
- RevokedToken

Stored in the auth-service database.

### User data

- UserProfile
- Vehicle

Stored in the user-service database.

### Parking data

- ParkingSpace
- AvailabilitySlot

Stored in the parking-service database.

### Booking data

- Booking
- BookingStatusHistory

Stored in the booking-service database.

### Payment data

- Transaction
- Refund

Stored in the payment-service database.

Each service owns its own database, which is the standard microservice pattern.

---

## 5. Authentication and authorization flow

### Login

1. Browser sends email/password to `/api/auth/login`.
2. Auth service verifies user credentials.
3. Auth service creates an access token and refresh token.
4. Browser stores access token in localStorage.
5. Refresh token is returned in an HttpOnly cookie.

### Request authorization

1. Browser sends request with `Authorization: Bearer <token>`.
2. Each service includes a `JwtAuthenticationFilter`.
3. The filter reads the JWT, validates it, and extracts `userId` and `role`.
4. The filter creates a Spring Security `Authentication` object.
5. Controller methods use `Authentication` and `@PreAuthorize` to decide if the user is allowed.

### Internal service-to-service authorization

- some endpoints are protected by a header like `X-Internal-Secret`
- the secret is configured in each service via `internal.service-secret`
- this prevents random callers from directly hitting internal endpoints

### Role model

The app is built around roles such as:

- `ROLE_DRIVER`
- `ROLE_OWNER`
- `ROLE_ADMIN`

The code checks these in controllers using `@PreAuthorize("hasRole('OWNER')")` and similar expressions.

---

## 6. End-to-end booking/payment flow

Here is the most important operational flow in plain English:

1. Driver logs in.
2. Driver searches for parking spaces.
3. Driver picks a listing and a slot.
4. The client sends a booking create request to booking-service.
5. Booking service checks space status, slot availability, and time validity.
6. Booking service saves a booking in `PENDING_PAYMENT` state.
7. The client sends a payment request to payment-service.
8. Payment service validates the booking belongs to the user and the amount matches.
9. Payment service creates a Stripe PaymentIntent or a mock payment object.
10. The client confirms payment.
11. Payment service marks the transaction successful.
12. Payment service tells Booking Service to set the booking status to `CONFIRMED`.
13. Booking service stores status history and the reservation becomes active.

If Stripe is not configured, the payment system still simulates a payment so the app can be used without external services.

---

## 7. Feature checklist and completion status

| Feature | In project | Status | Notes |
| --- | --- | --- | --- |
| Microservice architecture | Yes | Done | Eureka + gateway + multiple services |
| JWT-based auth | Yes | Done | Access + refresh tokens, secure cookie, validation filter |
| Register/login/logout | Yes | Done | Implemented in auth-service |
| Owner profile management | Yes | Done | user-service handles profiles and vehicles |
| Parking listing CRUD | Yes | Done | parking-service supports create/update/delete |
| City/location search | Yes | Done | search by city / lat-long radius |
| Slot availability model | Yes | Done | parking-service has availability slots |
| Booking creation | Yes | Done | booking-service validates slot and creates booking |
| Booking status changes | Yes | Done | status history and update endpoints are implemented |
| Stripe payment integration | Yes | Done (with fallback) | real Stripe when key is present, mock mode otherwise |
| Webhook support | Yes | Partial/Done | endpoint exists and logic is implemented |
| Refund flow | Yes | Done | logic exists in payment-service |
| Docker Compose startup | Yes | Done | build and service wiring are configured |
| Production security hardening | No | Partial | README explicitly notes remaining hardening work |
| Distribution/revocation storage | No | Partial | not a full production-grade token revocation model |
| Comprehensive test coverage | Partially | Partial | unit/integration tests exist in some services but broad production coverage is not described as complete |
| Commercial production readiness | No | Partial | project is a working MVP/prototype |

---

## 8. Honest assessment: how far along is this project?

Based on the repository content, this is best described as:

- a working MVP / prototype for a parking rental platform
- a real microservice architecture with real Spring Boot services and database separation
- close to feature-complete for the core business workflow
- not yet a production-hardened system

The README itself says the project still needs:

- production hardening
- controller/integration testing breadth
- distributed revocation storage
- observability
- deployment hardening

So: the main functionality is there, but the system is not yet fully “battle-tested production ready.”

---

## 9. Quick mental model

If you want the simplest explanation:

- API client is the consumer application
- gateway is the front door
- auth-service is the identity system
- user-service holds people and cars
- parking-service holds spaces and schedules
- booking-service handles reservations
- payment-service handles money and refunds
- everything is coordinated by JWT, MySQL, and service-to-service calls

That is the core architecture of this codebase.
