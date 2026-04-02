# Tickify Production Hardening Checklist

## 1. Security Baseline

- [x] Role-based route protection enabled for JSP and servlet endpoints.
- [x] Global security response headers configured.
- [x] New and seeded passwords stored with PBKDF2 hashes.
- [x] Legacy plaintext passwords remain login-compatible during migration window.
- [ ] Force HTTPS at reverse proxy/load balancer.
- [ ] Set `HttpOnly`, `Secure`, and `SameSite` for session cookies at container level.
- [ ] Rotate all default seeded account passwords before go-live.

## 2. Authentication and Authorization

- [x] Unauthorized access to role routes returns redirect or 403.
- [x] Session-based role checks enforced for Admin, Client roles, and Scanner role.
- [x] Add account lockout policy for repeated failed logins.
- [ ] Add password reset flow with signed expiring tokens.
- [ ] Add admin audit logs for privilege-sensitive actions.

## 3. Input Validation and Data Integrity

- [x] Registration rejects blank fields and weak credentials (<8 chars).
- [x] Email normalization and role validation enforced on auth path.
- [ ] Add server-side institutional domain validation for student-only events.
- [ ] Add stronger password policy (upper/lower/number/special).
- [ ] Add unique index checks and conflict responses for duplicate accounts.

## 4. Scanner Readiness (Security Scanner)

- [x] Scanner dashboard includes manual validation fallback with immediate result feedback.
- [x] Basic vibration/audio feedback implemented for success/failure outcomes.
- [x] Integrate live camera scanning endpoint and decode service.
- [x] Add replay protection for already-used ticket codes.
- [x] Persist scanner validation logs with guard ID and timestamp.

## 5. Client Site Readiness

- [x] Landing, selection, login, and signup flows modernized and responsive.
- [x] Registration success and failure messaging implemented.
- [x] Add CSRF protection tokens to all POST forms.
- [x] Add rate limiting and bot detection for login and signup routes.
- [x] Add accessibility checks (keyboard tab order, contrast, aria labels).

## 6. Admin Console Readiness

- [x] Admin dashboard replaced with operational control surface.
- [ ] Connect dashboard metrics to real database aggregates.
- [ ] Add event create/update/delete admin workflow with validation.
- [ ] Add financial report export and reconciliation view.
- [ ] Add guard and manager provisioning workflow.

## 7. Operations and Deployment

- [ ] Install Ant in build environment (`apt install ant`) and run `ant clean compile`.
- [ ] Verify GlassFish datasource and Derby/Oracle connectivity in target environment.
- [ ] Externalize DB host/user/password through environment-specific config.
- [ ] Configure centralized logging and error tracing.
- [ ] Add automated backup/restore for database.
- [ ] Run smoke tests for all roles: Admin, Attendee, Presenter, Event Manager, Venue Guard.

## 8. Final Go-Live Gate

Release only when all unchecked items above are completed or explicitly risk-accepted by the project owner.
