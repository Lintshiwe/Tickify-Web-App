# Tickify Multi-Tier Architecture

This project now follows a consistent multi-tier structure where presentation code depends on application services, and application services use composed data-access repositories.

## Target Layers

1. Presentation tier
- JSP pages in `web/`
- Servlet controllers in `src/java/za/ac/tut/servlet`

2. Application tier (business services)
- New package: `src/java/za/ac/tut/application`
- Authentication services: `za.ac.tut.application.auth`
- Registration services: `za.ac.tut.application.registration`
- Attendee services: `za.ac.tut.application.attendee`
- Admin services: `za.ac.tut.application.admin`
- Media services: `za.ac.tut.application.media`
- Scanner services: `za.ac.tut.application.scanner`
- Event manager services: `za.ac.tut.application.eventmanager`
- Presenter services: `za.ac.tut.application.presenter`
- User account services: `za.ac.tut.application.user`
- Shared service contract: `za.ac.tut.application.common.BaseService`

3. Data tier
- DAO classes in `src/java/za/ac/tut/databaseManagement`
- DB connection in `src/java/za/ac/tut/databaseConnection`
- Entities in `src/java/za/ac/tut/entities`

## Implemented Refactor Scope

1. All servlet classes in `za.ac.tut.servlet` now depend on `za.ac.tut.application.*` services.
2. Transitional inheritance services were upgraded to composition-based services using `repo()` delegation.

2. Login and registration use dedicated service contracts and DTOs:
- `AuthService` / `AuthServiceImpl`
- `AuthenticationResult`
- `RegistrationService` / `RegistrationServiceImpl`
- `RegistrationRequest`
- `RegistrationResult`

3. Existing DAO classes remain in the data tier and are not referenced directly by servlet controllers.

## Application Services Added

- `za.ac.tut.application.auth.AuthService`
- `za.ac.tut.application.auth.AuthServiceImpl`
- `za.ac.tut.application.auth.AuthenticationResult`
- `za.ac.tut.application.registration.RegistrationService`
- `za.ac.tut.application.registration.RegistrationServiceImpl`
- `za.ac.tut.application.registration.RegistrationRequest`
- `za.ac.tut.application.registration.RegistrationResult`
- `za.ac.tut.application.attendee.AttendeeService`
- `za.ac.tut.application.admin.AdminITService`
- `za.ac.tut.application.media.AdvertService`
- `za.ac.tut.application.media.EventMediaService`
- `za.ac.tut.application.scanner.ScannerService`
- `za.ac.tut.application.eventmanager.EventManagerDashboardService`
- `za.ac.tut.application.presenter.TertiaryPresenterService`
- `za.ac.tut.application.user.UserAccountService`

## Multi-Tier Conventions

1. Controller rule
- Servlets only parse requests, call application services, and shape responses.

2. Service rule
- Business rules, validation, and orchestration live in `za.ac.tut.application.*`.

3. Data rule
- SQL and persistence concerns stay in DAO classes under `za.ac.tut.databaseManagement`.

4. Test rule
- Prefer testing business logic through application services rather than servlet classes.

This structure supports ongoing growth toward a stricter layered architecture without breaking current flows.
