# Wildlife conservation backend

The backend implements **36 APIs** under `http://localhost:8080/api/v1`: authentication, parks/users, patrols, media, incidents, alerts, analytics/reports, community reporting, and camera monitoring.

The read-only `cdb-merchant` soundbox registration API informed the organization: controller → validated request DTO → explicit mapper → domain request → service interface → service implementation → repository/entity → response DTO → response generator. Controllers validate and map inputs, then return the service response directly. Feature services perform lookups, business checks, persistence, DTO mapping, and response generation. Dependencies use constructor injection. Responses use the reference-style `status`, `description`, `data`, and `error` envelope, with meaningful HTTP error codes.

Request and response DTOs, query models, domain models, and entities are regular Java classes with private fields and Lombok-generated getters, setters, and constructors, following the merchant model style. MongoDB collection, ID, and index annotations are on the entity classes and fields. Field validation belongs to request DTOs, including separate nested DTOs for track points, waypoints, observations, and locations; domain models and entities contain no validation annotations. `PatrolMapper` converts the validated DTOs into separate domain objects. Lombok's `@ToString` omits login credentials, password hashes, and access tokens. Pagination defaults are applied when creating the pageable query. Services retain patrol timing, ownership, and other business rules.

All service implementations use Lombok's `@RequiredArgsConstructor` with final dependencies. `ApplicationConfig` creates the `PasswordVerifier` with a dummy password hash for unknown accounts, keeping password verification and initialization outside the login service constructor.

Services are grouped by feature, with one interface and implementation per feature:

| Service | Responsibility | Controllers |
| --- | --- | --- |
| `LoginService` | Login and authentication workflow | `LoginController` |
| `UserService` | Current profile and active user lookup | `ProfileController`, `UserController` |
| `ParkService` | Park lookup and park access checks | `ParkController` |
| `PatrolService` | Routes, assignments, completed patrols, and ownership checks | `PatrolRouteController`, `PatrolAssignmentController`, `PatrolController` |
| `MediaService` | Validated image uploads, attachment ownership, and authorized downloads | `MediaController` |
| `IncidentService` | Incident submission, evidence checks, and ranger/manager queries | `IncidentController` |
| `AlertService` | Alert setup, acceptance, decline, support, and resolution | `AlertController` |
| `AnalyticsService` | Date-scoped counts, hotspots, and route completion coverage | `AnalyticsController` |
| `ReportService` | Saved analytics snapshots and PDF downloads | `ReportController` |
| `CommunityReportService` | Community submissions and scoped reads | `CommunityReportController` |
| `CameraTrapService` | Camera lookup, image submission, and final manual review | `CameraTrapController` |

`utility.ResponseGenerator` is a concrete Spring component following the merchant generator class pattern. Feature services call `generateSuccessResponse` and return a typed `ResponseEntity`; exception advice and security handlers call `generateErrorResponse`. The generator owns envelope construction, HTTP statuses, and creation `Location` headers. Its `SaveResult` overload returns 201 for creation and 200 for an identical retry. `PatrolServiceImpl` separates shared lookups, ranger eligibility, persistence, duplicate-write recovery, and access checks into named private helpers. `JwtTokenProvider`, `PatrolValidator`, and `PatrolDateFilter` are focused helpers in `utility`.

## Run locally

Requirements: JDK 17+, MongoDB 5.0+, and the Maven wrapper. MongoDB can be local or a hosted instance. The app creates its collection indexes when it starts, including the unique assignment/patrol indexes.

PowerShell, from this directory:

```powershell
# JAVA_HOME points to the JDK directory, not its bin directory.
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
$env:MONGODB_URI = 'mongodb://localhost:27017/wildlife_conservation'
$jwtKeyBytes = New-Object byte[] 32
$jwtRandom = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtRandom.GetBytes($jwtKeyBytes)
$jwtRandom.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($jwtKeyBytes)

# Optional demo records: only enabled when both switches are set.
$env:SPRING_PROFILES_ACTIVE = 'dev'
$env:DEV_SEED_ENABLED = 'true'
$env:DEV_SEED_PASSWORD = 'choose-your-local-demo-password'
./mvnw.cmd spring-boot:run
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

## API usage and checks

- [Implemented API guide](docs/implemented-api.md): flows, role matrix, requests/responses, errors, and limits.
- [Implemented OpenAPI specification](docs/implemented-openapi.json): import into a Swagger viewer or Postman.
- [Postman collection](docs/wildlife-api.postman_collection.json): all endpoints and logins for all five roles. Set its local password variable before use; select a file for multipart image uploads.
- `./mvnw.cmd clean verify`: compile, run tests, package, and run the existing PMD quality gate. Tests use mocked database boundaries and do not require a running MongoDB.

Logs include HTTP method/path, status, duration, and `X-Request-ID`. Services log successful login, assignment, and completion events. Passwords, tokens, request bodies, and GPS arrays are excluded from logs. Errors are handled centrally and return no stack traces or database details to clients.

Images use a persistent directory, configured by `MEDIA_STORAGE_DIR` (default `uploads` beside the application). Keep that directory with the MongoDB data across restarts/deployments. JPEG/PNG files are limited to 5 MiB and 20 million decoded pixels. Upload categories are role restricted; unlinked uploads are owner-only. Linked media follows the parent resource's read permissions. File storage keys and checksums stay internal.

Alert acceptance, decline, support, and resolution use conditional MongoDB updates. Camera reviews use the same approach. Submissions are insert-only with request fingerprints; repeated identical requests return existing data rather than overwriting it. Declines are bounded to 100 officers and support requests to 20 per alert.

Analytics uses inclusive park-local dates, up to 92 days. Coverage means unique assigned routes completed, with null coverage when there are no assigned routes. Report creation saves the complete analytics snapshot; PDF download renders that saved snapshot using PDFBox 3.0.8. The default PDF font supports Western text. Set `REPORT_FONT_PATH` to an available TrueType font when report content needs additional scripts.

Alerts are created manually by a manager for this milestone. Automatic collars, background push delivery, SMS gateways, and AI camera classification remain integration work. Support status `REQUESTED` records a request; it does not indicate external delivery. The original planning documents remain in `docs`; use the implemented guide/specification for current contracts.
