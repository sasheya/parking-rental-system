# Project Quality Report

Date: 2026-09-11

## Results

| Area | Result | Evidence / limitation |
|---|---|---|
| Full Maven reactor test | PASS | `common-security\mvnw.cmd -f pom.xml test`; 6 tests, 0 failures |
| Full Maven reactor verify | PASS | `common-security\mvnw.cmd -f pom.xml verify`; all 9 projects successful |
| Clean verify gate | BLOCKED | Windows/OneDrive lock on `common-security\target\test-classes` |
| JaCoCo | CONFIGURED and EXECUTED | Aggregate report goal completed under `target/site/jacoco-aggregate`; no 80% claim because suite is incomplete |
| Sonar | UNVERIFIED | Environment-driven properties added; no `SONAR_HOST_URL`/`SONAR_TOKEN` execution was available |
| Gateway HTTP/E2E | BLOCKED | Services did not remain running and MySQL/Eureka were unavailable |
| Endpoint inventory | COMPLETE | Controller, Feign, gateway, and security-derived matrix in `api-endpoint-verification.md` |
| Exception handling | PARTIAL | Per-service `@RestControllerAdvice` exists; shared timestamp/status/path envelope and broader mappings remain |
| Logging | PARTIAL | Existing SLF4J service logging is present; file rolling/AOP correlation logging remains to implement |

## Changes made

- Added root-level Sonar project properties sourced from `SONAR_HOST_URL` and `SONAR_TOKEN`.
- Added root JaCoCo aggregate reporting during `verify`.
- Added ignores for `logs/`, nested logs, and generated JaCoCo site output.
- Added the controller-derived endpoint verification matrix and explicit evidence/blockers.

## Security notes

No credentials, tokens, private keys, or environment files were printed or added. Existing JWT and Stripe signature validation paths were not weakened. The gateway currently exposes actuator and OpenAPI paths as configured; production exposure should be reviewed.

## Known risks

- Six automated tests are insufficient for the requested service-level coverage.
- No fresh authenticated HTTP booking/payment flow was possible without running infrastructure.
- Service startup failures need separate diagnosis after the local MySQL/Eureka prerequisites are available.
