# WildGuard: simple Spring Boot backend plan

Original plan prepared 8 October 2026. All 36 listed API operations are now implemented; see [implemented-api.md](implemented-api.md) and [implemented-openapi.json](implemented-openapi.json) for current contracts. This document preserves the original scope and integration tradeoffs. Automatic collars, external notifications/SMS, and AI classification remain future integrations.

## 1. Decision: use the starter you already have

Keep Java 17, Spring Boot 4.1.1, Spring MVC, Spring Security, Bean Validation and Spring Data MongoDB from the existing `pom.xml`. Build **one backend application**, with one MongoDB database and a persistent folder for image uploads. This avoids changing your database stack or building infrastructure that your deadline does not need.

Add JWT support through Spring Security's resource-server/Jose support, Swagger UI through springdoc-openapi 3.x, and PDFBox 3.x for a basic downloadable report. Pin exact compatible versions when implementation starts. Boot 4.1.1 supports Java 17; springdoc documents its 3.x line for Boot 4. Sources: [Spring requirements](https://docs.spring.io/spring-boot/system-requirements.html), [springdoc](https://springdoc.org/), [Spring Security JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html), [PDFBox](https://pdfbox.apache.org/3.0/getting-started.html).

The deliverables beside this plan are:

- `api-reference.md`: every endpoint, allowed roles, request/response examples and relevant errors.
- `openapi.json`: the same API contract in OpenAPI 3.0.3 format, suitable for importing into Postman or a Swagger viewer. It describes a proposed API, not a running server.

## 2. What to build first

| Build now | Keep for a later integration |
| --- | --- |
| Seeded accounts, login and role/park checks | Public registration, phone OTP and email password recovery |
| Seeded parks, areas, routes and camera-trap records | Editors for parks/routes/zones and an administration portal |
| Manager patrol assignment and ranger completed-patrol upload | Continuous server-side patrol streaming |
| Incident evidence uploads, submission and retrieval | Cloud media storage and advanced image processing |
| Conflict alert acceptance, support, decline and resolution | Real collars, automatic zone detection, push notifications and automatic escalation |
| Basic analytics, saved report snapshots and PDF download | Geographic area-coverage calculations and advanced prediction |
| Community app reports and manual camera-image review | SMS gateway ingestion, camera-sensor ingestion and AI classification |

All six user journeys get usable APIs. For the first demo, create conflict alerts through a manager-only setup endpoint or seed script; poll for them from Flutter. This supports response testing but does **not** fulfill automatic collar-triggered detection or background push delivery. If those are assessed requirements, finish their integration before calling UC3 complete. Similarly, an SMS composer alone does not implement the backend SMS-report channel.

The first version uses seeded community accounts. Real community onboarding is a later task; do not present seeded login as a public registration system.

## 3. Simple architecture

```text
Flutter app
    -> REST controller + request validation
    -> service (authorization, rules, state transitions)
    -> MongoDB repository
    -> MongoDB

Media service -> persistent upload directory
Report service -> stored analytics snapshot -> PDFBox download
```

Use the project's current `controller`, `service`, `repository`, `entity`, `dto`, `mapper`, `config`, `filter`, `enums`, `exception` and `adviser` packages. Group classes by module names inside those packages if useful. Existing placeholder classes can be replaced when coding starts.

Keep controllers short. Services own business rules. Repositories own persistence queries. Request/response DTOs are separate from Mongo documents. Use constructor injection, explicit enums, `@Valid`, one `@RestControllerAdvice`, and manual mappings for small DTOs. Extract a small interface for file storage or token creation when it helps testing. Do not create an interface plus implementation for every class or add a second domain model for every DTO merely for layering.

SOLID here means clear responsibilities and replaceable dependencies. It does not require microservices, an API gateway, Kafka, Redis, Kubernetes, CQRS or a separate generic synchronization service.

## 4. Shared API rules

- Base path: `/api/v1`. JSON uses camelCase. Enums use uppercase strings.
- Bearer JWT on every operation except `POST /auth/login`.
- Client-generated UUIDs for newly uploaded records and images. Seed/reference IDs are readable strings such as `park-yala` and `route-yala-b1`.
- Timestamps are ISO 8601 instants, preferably UTC: `2026-10-08T04:54:00Z`. Store Java `Instant`; Flutter displays local time.
- Date filters are inclusive calendar dates in the selected park's timezone. Yala uses `Asia/Colombo`. Convert the start to local midnight and the end to the following midnight, then query a UTC half-open interval.
- List endpoints use `page=0&size=20`, maximum `size=100`, and a stable newest-first order with ID as a tie-breaker. List DTOs omit large tracks/image bytes.
- Single success responses are plain objects; list responses are `{items, page, size, totalItems}`. Do not add multiple nested wrappers.
- `201`: first creation. `200`: retrieval, state change or an identical retry. `400`: invalid input. `401`: missing/invalid token or invalid login. `403`: forbidden role/park. `404`: missing or inaccessible resource. `409`: state conflict or changed retry. `413`: file too large. `415`: unsupported image type. `500/503`: unexpected/temporary server failure.
- Server derives reporter, officer, reviewer and creation timestamps from authentication; clients cannot choose another user's identity or send server statuses.
- Required fields, enums and shape constraints are in `openapi.json`. Unknown request fields should be rejected. State/ownership rules also belong in services.

Common error:

```json
{
  "timestamp": "2026-10-08T04:54:01Z",
  "status": 400,
  "code": "VALIDATION_FAILED",
  "message": "Check the highlighted fields.",
  "path": "/api/v1/incidents/44444444-4444-4444-8444-444444444444",
  "fieldErrors": {
    "description": "must not be blank"
  }
}
```

## 5. Authentication and access

`POST /auth/login -> accessToken -> GET /auth/me -> account-specific frontend home`.

Use BCrypt password hashes. Proposed token lifetime: one hour, followed by login again; keep local drafts while asking for login. Use a fixed JWT signing algorithm, issuer and audience, and load signing material from environment configuration. Validate signature and expiration with Spring Security; do not write JWT cryptography yourself. No refresh-token or server-side logout subsystem in this milestone: Flutter discards its token on logout, and an issued token remains valid until expiration unless account checks revoke access.

Before accessing data, verify the account is active, its role is allowed, and it belongs to the requested park. The role/park authorization comes from the authenticated account, not a request field. Password hashes never appear in a DTO.

| Role | Scope |
| --- | --- |
| `PARK_MANAGER` | Assigned parks: staff lookup, assignments, patrol/incident/community reads, alert setup/response coordination, analytics/reports and camera monitoring |
| `RANGER` | Own assignments/patrols/incidents; assigned-park alerts; accept, decline, support and resolve where eligible |
| `LIAISON_OFFICER` | Assigned-park alerts and community report reads; response operations where eligible |
| `RESEARCHER` | Authorized camera traps and images, species review and verification flags |
| `COMMUNITY_MEMBER` | Own community reports and their media |

Alert acceptance/support/resolution are officer operations, not unrestricted manager actions. A manager coordinates through existing data and the setup endpoint. If managers also act as field officers, model that eligibility explicitly later.

## 6. API flows

### Patrol assignment and recording

```text
Manager: GET /users?role=RANGER&parkId=...
         GET /patrol-routes?parkId=...
         PUT /patrol-assignments/{clientUuid}

Ranger:  GET /patrol-assignments?status=ASSIGNED
         GET /patrol-routes/{routeId}
         Start/track/waypoints/observations in local Flutter storage
         End patrol and review
         PUT /patrols/{clientUuid} with the completed record
         GET /patrols/{id} for acknowledged details
```

There are no backend calls for every GPS fix or wizard step. The first version stores active patrols on the phone and uploads one completed snapshot. An interrupted session restores locally. Manager live patrol tracking and cross-device active-session recovery are outside this milestone.

Patrol submission validates assignment ownership, ordered start/end/point timestamps, location ranges and array limits. One assignment allows one completed patrol. Keep up to 10,000 track points, 100 waypoints and 100 observations; downsample locally before exceeding the limit. Compute recorded distance from continuous GPS segments; exclude segments with a gap over 120 seconds or a reported accuracy worse than 100 meters, and exclude manual waypoints from inferred travel distance. Treat missing GPS as missing data, not a fabricated route.

Assignment `COMPLETED` is derived from the existence of its submitted patrol; avoid updating multiple documents to maintain the same status. Query completed patrols for the assignment IDs in one batch when assembling a list.

### Incident report

```text
Type/photo/location/description/review in Flutter
    -> PUT /media/{photoUuid} (multipart image)
    -> PUT /incidents/{incidentUuid} (JSON referencing photoUuid)
    -> service validates owned media and optional assignment link
    -> 201 SUBMITTED, or 200 for identical replay
    -> GET /incidents and GET /incidents/{id}
```

Required: park, area, incident type, observation time, location, description and evidence photo. The optional `assignmentId` must belong to the same ranger/park. Linking to the existing assignment lets an incident be submitted immediately while the patrol is still being recorded locally. The completed patrol uses that same assignment ID, so the records stay related without waiting for the patrol upload. Drafts and Pending sync exist locally; the server stores only acknowledged Submitted incidents. Uploaded media alone does not mean an incident was submitted.

### Conflict response

```text
Demo setup: manager PUT /alerts/{id}
Officer: GET /alerts?status=NEW -> GET /alerts/{id}
         POST /alerts/{id}/accept
         poll GET /alerts/{id} while responding
         optional POST /alerts/{id}/support or /decline
         optional PUT /media/{photoUuid}
         PUT /alerts/{id}/response
         200 RESOLVED with the saved outcome
```

State: `NEW -> RESPONDING -> RESOLVED`. Declining before acceptance records a reason and leaves the alert New for another officer. Support is recorded as `REQUESTED`; that is not a delivery receipt. Automatic dispatch/reassignment/escalation is deferred.

Use a **single conditional MongoDB update** for acceptance: match alert ID, allowed park, `status=NEW`, and no assigned officer. Set officer and Responding together. Two concurrent accept attempts must not both succeed; return `409 ALERT_ALREADY_ASSIGNED` to the loser. A retry by the same officer returns 200. Use the same atomic approach to save the response and set Resolved in the alert document. Spring Data exposes `findAndModify`/conditional updates for this pattern: [MongoDB operations](https://docs.spring.io/spring-data/mongodb/reference/mongodb/template-crud-operations.html).

Only the assigned officer can resolve. Failed save retains Responding. While a response is active, poll approximately every 15 seconds in the foreground; alert lists can poll every 30 seconds. Stop polling when the app is inactive. Polling does not provide background emergency notifications. Location timestamps come from the observation/device event, not the last GET request; label old data as Last known.

### Analytics and reports

```text
GET /analytics/summary?parkId=...&from=...&to=...
GET /incidents with type/area/date filters for map points and details
GET /patrols for submitted tracks and coverage details
GET /community-reports for conflict evidence
PUT /reports/{clientUuid} to save a report snapshot
GET /reports/{id} to preview it
GET /reports/{id}/download to generate/download its PDF
```

Use simple MongoDB counts/grouping by type, day and area. Limit reporting periods to 92 days. A potential hotspot is an area with at least three submitted incidents during the period; this is a transparent count threshold, not an AI conclusion.

**Time-saving coverage definition:** unique routes whose assignments have a scheduled start in the period form the denominator. Among those routes, count the ones with a completed patrol ending in that period as the numerator. Return `basis=ASSIGNED_ROUTES_COMPLETED`, numerator and denominator. Flutter must label this **Route completion coverage**. It is not the percentage of geographic park area patrolled. True area coverage from GPS tracks remains a separate requirement if assessed.

No assignments -> coveragePercent is null, not zero. No matching data -> 200 with `dataAvailable=false`. Database failure -> 503, not an empty successful dashboard.

Save report metadata and the calculated summary snapshot in MongoDB. Download generates a basic PDF from that immutable snapshot using PDFBox; no job queue or separate report server. Keep type/daily/area tables and findings in the first PDF. Map screenshots and elaborate layouts can follow. Creating the report already saves it; the UI's Save action need not create it again.

### Community reporting

```text
Seeded member login -> fill and review locally
optional PUT /media/{photoUuid}
PUT /community-reports/{clientUuid}
GET /community-reports -> own reports
GET /community-reports/{id}
```

Types: `WILDLIFE_SIGHTING` and `CROP_DAMAGE`. Location can be an actual GPS/manual coordinate or a village/landmark without coordinates; represent unknown coordinates as absent. Photos are optional. Managers/liaison officers read reports only in their authorized parks. Real SMS ingestion needs a provider, recipient number, webhook verification and delivery semantics; it is outside these initial APIs.

### Camera-trap monitoring

```text
GET /camera-traps?parkId=...
PUT /media/{photoUuid} category CAMERA_TRAP
PUT /camera-trap-images/{clientUuid} with camera/capture metadata
GET /camera-trap-images?status=PENDING_REVIEW
GET /camera-trap-images/{id}
PUT /camera-trap-images/{id}/review
200 REVIEWED
```

Manual uploader and reviewer are a researcher or manager. A species can be Unknown; `possiblePoacher=true` means Requires verification, not a proven identity. Initial reviews are final for this milestone: exact retry is safe, but a conflicting second review returns 409. Editing/re-review and AI predictions can be added after the basic flow works.

## 7. Offline synchronization without another subsystem

Flutter owns a local queue. Use the same normal endpoints when connectivity returns.

1. Save the complete record, attachments, generated resource UUIDs and logged-in owner locally.
2. Never automatically upload a Draft. Only explicitly submitted items enter the queue.
3. Authenticate as the original owner; do not upload another account's queue after an account switch.
4. Upload media first; keep its returned ID. Then upload the incident/community/camera record or completed patrol.
5. After 201 or an identical-retry 200, mark that item Synced locally. No server `PENDING_SYNC` state or `/sync` endpoint is needed.
6. Retry network failures, timeouts and 5xx with bounded backoff, for example 5 s, 15 s, 60 s, then 5 min. Handle 401 by login, 400 by user correction and 403/409 by user review; do not blindly retry them.

For immutable record `PUT`s: insert with the client UUID as Mongo `_id`; keep a canonical request hash and owner. Duplicate ID plus same owner/payload -> return existing response with 200; changed payload -> `409 ID_REUSED_WITH_DIFFERENT_DATA`. Use unique insertion rather than a check followed by an unrestricted `save`, which could overwrite existing data. Retry detection must be race-safe and check authorization before exposing a prior record. Once a queued record is acknowledged, it is not editable through these APIs.

Support requests carry a `requestId`; duplicate request IDs do not append another request. Declines are keyed by officer; repeat identical reason is safe. These small per-resource rules replace a general idempotency service.

## 8. MongoDB documents

| Collection | Important fields | Essential index/rule |
| --- | --- | --- |
| users | id, name, normalizedEmail, passwordHash, role, parkIds, active | Unique normalizedEmail |
| parks | id, name, timezone, areas[{id,name}] | Seed/reference data |
| patrol_routes | id, parkId, areaId, name, plannedDistanceMeters, pathPoints | parkId |
| patrol_assignments | id, parkId, routeId, rangerId, scheduledStartAt, scheduledEndAt, assignedBy, requestHash | rangerId + scheduledStartAt unique; parkId |
| patrols | id, assignmentId, rangerId, parkId, routeId, startedAt, endedAt, trackPoints, waypoints, observations, recordedDistanceMeters, requestHash | Unique assignmentId; parkId + endedAt; rangerId |
| media | id, ownerId, parkId, category, contentType, sizeBytes, sha256, storageKey | Unique id; do not expose storageKey |
| incidents | id, parkId, areaId, rangerId, optional assignmentId, type, location, detectedAt, description, photoId, requestHash | parkId + detectedAt; rangerId + detectedAt; assignmentId |
| alerts | id, parkId, animal/collar details, location + locationUpdatedAt, riskLevel, status, assignedOfficerId, declines, supportRequests, embedded response | parkId + status; assignedOfficerId |
| community_reports | id, parkId, areaId, reporterId, type, species, village, optional coordinates, occurredAt, description, optional photoId, requestHash | parkId + occurredAt; reporterId |
| camera_traps | id, parkId, areaId, name, location | parkId; seeded |
| camera_trap_images | id, cameraTrapId, parkId, uploaderId, mediaId, capturedAt, status, embedded review, requestHash | parkId + status + capturedAt |
| reports | id, parkId, generatedBy, reportType, from, to, sections, generatedAt, snapshot, requestHash | generatedBy + generatedAt; parkId |

Embed patrol points/notes and alert response/support details with the documented limits. Store large images as files, not Base64 fields inside these documents. MongoDB does not enforce reference integrity for you: services must validate referenced park/area/route/assignment/media ownership and existence. Keep critical alert transitions in one document so they do not need multi-document transactions.

## 9. Uploads, configuration and basic operation

- Images: JPEG/PNG only; maximum 5 MiB per file. Validate decoded content as well as declared MIME type. Multipart maximum request size can be 6 MiB.
- Use server-generated storage paths based on the resource UUID. Ignore uploaded filenames as paths. Replays compare the bytes' hash and metadata.
- Unlinked media are readable only by their owner. Once referenced by a record, download authorization follows that parent record's role/park/ownership checks.
- Use a persistent uploads directory/volume. On file/database failure, return an error and clean up that attempt's staging file; do not acknowledge nonexistent media. An orphan-media cleanup task is a later improvement.
- Configuration: Mongo URI, JWT signing material/issuer/audience, upload directory and allowed web origins come from environment or non-secret configuration. Do not commit credentials. In Boot 4 use the configuration metadata/IDE suggestions for that version rather than copying older Boot property names; for example, confirm the Mongo connection property against the Boot 4 configuration reference when wiring the starter.
- Keep Swagger UI enabled in development. Springdoc exposes `/v3/api-docs` and a Swagger UI entry at `/swagger-ui.html`; see [springdoc documentation](https://springdoc.org/).
- Native Flutter clients do not need CORS; permit explicit development origins if using Flutter Web. HTTPS for the shared/deployed API.
- Record structured errors and operation IDs; omit passwords, bearer tokens and image bytes from logs. Expose a simple health endpoint through Actuator if needed for hosting.
- One backend process plus MongoDB and a persistent upload folder is enough for the first deployment. Do not store uploads only in an ephemeral deployment filesystem.

## 10. Build order and checks

| Milestone | Work | Exit check |
| --- | --- | --- |
| 1. Foundation | Mongo connection, seed accounts/parks/routes/cameras, login, role checks, validation/error handling, Swagger | Login and wrong-role/wrong-park rejection work |
| 2. Patrol and incidents | Assignment, completed snapshot, media and incident APIs; pair with Flutter queue | Duplicate uploads create one record; draft stays local; patrol link validated |
| 3. Conflict response | Seed/setup alerts, list/detail, atomic accept, decline/support, atomic resolve | Two officers cannot both accept; failed save never resolves; retry safe |
| 4. Analytics/reports | Summary queries, honest coverage basis, snapshot/PDF endpoints | Counts match stored records; no-data differs from failure; PDF uses saved snapshot |
| 5. Basic additions | Community submission/history and manual camera review | Owner scoping works; media/park checks enforced; duplicate review handled |
| 6. Integration | Phone/API errors, offline retry, seeded demo data and deployment | `mvnw.cmd verify` including current PMD rules; agreed demo journeys pass |

For a small team, work by the four main use cases with one shared API contract and short integration checks each day. Estimates depend on team experience; use this build order to fit the actual deadline rather than treating an untested day count as a guarantee.

Important tests are authorization by role/owner/park, duplicate-ID races, alert acceptance races, timestamp/reference validation, media failures and analytics totals. Use controller tests plus integration tests against MongoDB for database-specific atomic behavior. Do not spend the deadline testing getters or adding infrastructure for every minor endpoint.

Before final submission, compare the deferred integrations with the marking requirements. Add real collar ingestion, background notifications, SMS or geographic coverage if required; keep all demo-only behavior identified.
