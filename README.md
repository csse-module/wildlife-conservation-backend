# Wildlife conservation backend

The backend implements **44 APIs** under `http://localhost:8080/api/v1`: authentication, parks/users, patrols, media, incidents, alerts, analytics/reports, community reporting, and camera monitoring.

The read-only `cdb-merchant` soundbox registration API informed the organization: controller → validated request DTO → explicit mapper → domain request → service interface → service implementation → repository/entity → response DTO → response generator. Controllers validate and map inputs, then return the service response directly. Feature services perform lookups, business checks, persistence, DTO mapping, and response generation. Dependencies use constructor injection. Responses use the reference-style `status`, `description`, `data`, and `error` envelope, with meaningful HTTP error codes.

Request and response DTOs, query models, domain models, and entities are regular Java classes with private fields and Lombok-generated getters, setters, and constructors, following the merchant model style. MongoDB collection, ID, and index annotations are on the entity classes and fields. Field validation belongs to request DTOs, including separate nested DTOs for track points, waypoints, observations, and locations; domain models and entities contain no validation annotations. `PatrolMapper` converts the validated DTOs into separate domain objects. Lombok's `@ToString` omits login credentials, password hashes, and access tokens. Pagination defaults are applied when creating the pageable query. Services retain patrol timing, ownership, and other business rules.

All service implementations use Lombok's `@RequiredArgsConstructor` with final dependencies. `ApplicationConfig` creates the `PasswordVerifier` with a dummy password hash for unknown accounts, keeping password verification and initialization outside the login service constructor.

Services are grouped by feature, with one interface and implementation per feature:

| Service | Responsibility | Controllers |
| --- | --- | --- |
| `LoginService` | Login and authentication workflow | `LoginController` |
| `RegistrationService` | Community sign-up in enabled parks | `RegistrationController` |
| `PasswordService` | Own-password changes and token invalidation | `PasswordController` |
| `UserService` | Current profile, user lookup, and manager-created staff | `ProfileController`, `UserController` |
| `ParkService` | Park lookup and park access checks | `ParkController` |
| `PatrolService` | Routes, assignments, completed patrols, and ownership checks | `PatrolRouteController`, `PatrolAssignmentController`, `PatrolController` |
| `MediaService` | Validated image uploads, attachment ownership, and authorized downloads | `MediaController` |
| `IncidentService` | Incident submission, evidence checks, and ranger/manager queries | `IncidentController` |
| `AlertService` | Alert setup, acceptance, decline, support, and resolution | `AlertController` |
| `AnalyticsService` | Date-scoped counts, hotspots, and route completion coverage | `AnalyticsController` |
| `ReportService` | Saved analytics snapshots and PDF downloads | `ReportController` |
| `CommunityReportService` | Community submissions, scoped inboxes, atomic acceptance and resolution | `CommunityReportController` |
| `CameraTrapService` | Camera lookup, image submission, and final manual review | `CameraTrapController` |

`utility.ResponseGenerator` is a concrete Spring component following the merchant generator class pattern. Feature services call `generateSuccessResponse` and return a typed `ResponseEntity`; exception advice and security handlers call `generateErrorResponse`. The generator owns envelope construction, HTTP statuses, and creation `Location` headers. Its `SaveResult` overload returns 201 for creation and 200 for an identical retry. `PatrolServiceImpl` separates shared lookups, ranger eligibility, persistence, duplicate-write recovery, and access checks into named private helpers. `JwtTokenProvider`, `PatrolValidator`, and `PatrolDateFilter` are focused helpers in `utility`.

## Run locally

Requirements: JDK 17+, MongoDB 5.0+, and the Maven wrapper. MongoDB can be local or a hosted instance. The app creates its collection indexes when it starts, including the unique assignment/patrol indexes.

The app automatically loads an optional `.env` file from its working directory. Keep `MONGODB_URI` and a stable `JWT_SECRET` there for local development; `.env` is ignored by Git. Include `/wildlife_conservation` in the connection URI to select the application database. Environment variables override the values in this file. Run from the backend directory so that the file is found.

PowerShell, from this directory:

```powershell
# JAVA_HOME points to the JDK directory, not its bin directory.
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
./mvnw.cmd spring-boot:run
```

To use a local MongoDB instead of the connection in `.env`, set these overrides before running:

```powershell
$env:MONGODB_URI = 'mongodb://localhost:27017/wildlife_conservation'
$jwtKeyBytes = New-Object byte[] 32
$jwtRandom = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtRandom.GetBytes($jwtKeyBytes)
$jwtRandom.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($jwtKeyBytes)
```

Optional demo records are enabled separately:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'dev'
$env:DEV_SEED_ENABLED = 'true'
$env:DEV_SEED_PASSWORD = 'choose-your-local-demo-password'
```

Set a stable, randomly generated `JWT_SECRET` in the deployed environment; regenerating it invalidates existing tokens. The key must be Base64 encoding of at least 32 bytes. Tokens last 3,600 seconds by default (`JWT_TTL_SECONDS`, allowed 60–86,400). On authenticated requests the backend reads the active account and its current role/parks from MongoDB.

The optional demo seed creates one park (`park-yala`), area `area-b1`, one route (`route-demo`), camera trap `CT-DEMO`, and these accounts with the supplied `DEV_SEED_PASSWORD`:

| Email | ID | Role |
| --- | --- | --- |
| `manager@wildguard.local` | `usr-manager` | PARK_MANAGER |
| `ranger@wildguard.local` | `usr-ranger` | RANGER |
| `liaison@wildguard.local` | `usr-liaison` | LIAISON_OFFICER |
| `researcher@wildguard.local` | `usr-researcher` | RESEARCHER |
| `community@wildguard.local` | `usr-community` | COMMUNITY_MEMBER |

Seed runs preserve existing data and passwords. The route coordinates are demonstration data. For Flutter web, set `CORS_ALLOWED_ORIGINS` to the exact comma-separated browser origins. Native Flutter does not require CORS configuration. Set `PORT` to change the default port 8080.

## Account onboarding — new in 1.2.0

- `POST /api/v1/auth/register`: public community registration; the backend always assigns `COMMUNITY_MEMBER`.
- `POST /api/v1/users`: managers create ranger, liaison, or researcher accounts within their own parks.
- `POST /api/v1/auth/change-password`: authenticated users change their own password.

Community registration accepts only existing parks in `COMMUNITY_REGISTRATION_PARK_IDS` (default `park-yala`; comma-separated IDs; empty disables registration). Set this in the local `.env` or deployment environment. New passwords require 6–72 Unicode code points and at most 72 UTF-8 bytes. Duplicate normalized emails return 409.

New staff accounts have `passwordChangeRequired=true`. Their JWT permits profile access and password change; other protected APIs return `403 PASSWORD_CHANGE_REQUIRED`. After changing a password, every existing token is revoked, and the user must log in again. Existing MongoDB accounts default to no required password change until explicitly provisioned as new staff. The first manager uses trusted setup; the development seed creates the demo manager.

See the newly added account API section in the [API guide](docs/implemented-api.md) for complete requests, responses, errors, and Flutter flows. Both `openapi.json` and `implemented-openapi.json` now describe the current 44-operation contract.

## API usage and checks

- [Implemented API guide](docs/implemented-api.md): flows, role matrix, requests/responses, errors, and limits.
- [Implemented OpenAPI specification](docs/implemented-openapi.json): import into a Swagger viewer or Postman.
- [Postman collection](docs/wildlife-api.postman_collection.json): all endpoints and logins for all five roles. Set its local password variable before use; select a file for multipart image uploads.
- `./mvnw.cmd clean verify`: compile, run tests, package, and run the existing PMD quality gate. Tests use mocked database boundaries and do not require a running MongoDB.

Logs use the same layout as the supplied example:

```text
[<traceId>][<requestId>][WILDLIFE-CONSERVATION][DEBUG] yyyy-MM-dd HH:mm:ss.SSS [thread] ClassName:line - message
```

Each HTTP request has request/response BEGIN and END blocks, plus its method, path, status and duration. At `DEBUG` level, the blocks include sanitized headers and JSON bodies. Passwords, tokens, API keys, cookies, names, email addresses and phone numbers are redacted; GPS data is omitted. Binary, multipart, invalid JSON and bodies over 16 KiB are summarized. The request body is logged after the application consumes it, without reading it ahead of the controller. Response capture is bounded and streams the full response to the client, including image/PDF downloads. Errors are handled centrally and return no stack traces or database details to clients.

`APP_LOG_LEVEL` defaults to `DEBUG`; set it to `INFO` for summaries without headers or bodies. `LOG_SERVICE_NAME` changes the service label. Logs appear in the console; optionally set `LOGGING_FILE_NAME=logs/wildlife-conservation.log` to also write a file in the same format. Restart the backend after changing these environment settings. Startup/background logs use `SYSTEM` for IDs when no request context exists.

Every response includes `X-Request-ID` and `X-Trace-ID`, which match the log prefix. A valid incoming `X-Request-ID` is reused. An optional `X-B3-TraceId` (preferred) or `X-Trace-ID` accepts a nonzero 16- or 32-digit hexadecimal trace ID; missing or invalid IDs are generated. Both response headers are exposed to browser clients through CORS.

Images use a persistent directory, configured by `MEDIA_STORAGE_DIR` (default `uploads` beside the application). Keep that directory with the MongoDB data across restarts/deployments. JPEG/PNG files are limited to 5 MiB and 20 million decoded pixels. Upload categories are role restricted; unlinked uploads are owner-only. Linked media follows the parent resource's read permissions. File storage keys and checksums stay internal.

Alert acceptance, decline, support, and resolution use conditional MongoDB updates. Camera reviews use the same approach. Submissions are insert-only with request fingerprints; repeated identical requests return existing data rather than overwriting it. Declines are bounded to 100 officers and support requests to 20 per alert.

Analytics uses inclusive park-local dates, up to 92 days. Coverage means unique assigned routes completed, with null coverage when there are no assigned routes. Report creation saves the complete analytics snapshot; PDF download renders that saved snapshot using PDFBox 3.0.8. The default PDF font supports Western text. Set `REPORT_FONT_PATH` to an available TrueType font when report content needs additional scripts.

Alerts are created manually by a manager for this milestone. Automatic collars, background push delivery, SMS gateways, and AI camera classification remain integration work. Support status `REQUESTED` records a request; it does not indicate external delivery. The original planning documents remain in `docs`; use the implemented guide/specification for current contracts.

## Case-study frontend integration

Villagers register publicly and submit sightings or crop damage; managers see their parks’ incoming reports. Rangers and liaison officers accept and resolve reports, and villagers can track the outcome. Managers and researchers generate/download conservation PDFs. Researchers also review camera images. See [the demo and setup guide](docs/case-study-demo.md) for the complete flow and frontend configuration. The frontend uses the existing Spring Boot media API, with no Supabase setup required for these flows.
