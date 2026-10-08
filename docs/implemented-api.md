# Implemented APIs

Base URL: `http://localhost:8080/api/v1`. The backend implements 36 operations. Send `Authorization: Bearer <accessToken>` after login. JSON POST/PUT requests use `Content-Type: application/json`; image uploads use `multipart/form-data`. Image/PDF downloads return binary content. An optional `X-Request-ID` containing 1–64 letters, digits or hyphens is accepted; otherwise the server generates one and returns it in the response header.

## Roles and flows

| Method | Path | Allowed roles |
| --- | --- | --- |
| POST | `/auth/login` | Public |
| GET | `/auth/me` | All five roles |
| GET | `/parks` | All five roles |
| GET | `/users` | PARK_MANAGER |
| GET | `/patrol-routes` | PARK_MANAGER, RANGER |
| GET | `/patrol-routes/{id}` | PARK_MANAGER, RANGER |
| PUT | `/patrol-assignments/{id}` | PARK_MANAGER |
| GET | `/patrol-assignments` | PARK_MANAGER, RANGER |
| PUT | `/patrols/{id}` | RANGER |
| GET | `/patrols` | PARK_MANAGER, RANGER |
| GET | `/patrols/{id}` | PARK_MANAGER, RANGER |
| PUT | `/media/{id}` | All five roles |
| GET | `/media/{id}/content` | All five roles |
| PUT | `/incidents/{id}` | RANGER |
| GET | `/incidents` | PARK_MANAGER, RANGER |
| GET | `/incidents/{id}` | PARK_MANAGER, RANGER |
| PUT | `/alerts/{id}` | PARK_MANAGER |
| GET | `/alerts` | PARK_MANAGER, RANGER, LIAISON_OFFICER |
| GET | `/alerts/{id}` | PARK_MANAGER, RANGER, LIAISON_OFFICER |
| POST | `/alerts/{id}/accept` | RANGER, LIAISON_OFFICER |
| POST | `/alerts/{id}/decline` | RANGER, LIAISON_OFFICER |
| POST | `/alerts/{id}/support` | RANGER, LIAISON_OFFICER |
| PUT | `/alerts/{id}/response` | RANGER, LIAISON_OFFICER |
| GET | `/analytics/summary` | PARK_MANAGER |
| PUT | `/reports/{id}` | PARK_MANAGER |
| GET | `/reports` | PARK_MANAGER |
| GET | `/reports/{id}` | PARK_MANAGER |
| GET | `/reports/{id}/download` | PARK_MANAGER |
| PUT | `/community-reports/{id}` | COMMUNITY_MEMBER |
| GET | `/community-reports` | COMMUNITY_MEMBER, PARK_MANAGER, LIAISON_OFFICER |
| GET | `/community-reports/{id}` | COMMUNITY_MEMBER, PARK_MANAGER, LIAISON_OFFICER |
| GET | `/camera-traps` | PARK_MANAGER, RESEARCHER |
| PUT | `/camera-trap-images/{id}` | PARK_MANAGER, RESEARCHER |
| GET | `/camera-trap-images` | PARK_MANAGER, RESEARCHER |
| GET | `/camera-trap-images/{id}` | PARK_MANAGER, RESEARCHER |
| PUT | `/camera-trap-images/{id}/review` | PARK_MANAGER, RESEARCHER |

The five roles are `PARK_MANAGER`, `RANGER`, `LIAISON_OFFICER`, `RESEARCHER`, `COMMUNITY_MEMBER`. Every resource is restricted to the account's assigned parks. Ranger assignment/patrol queries return only that ranger's records. Managers see records within their assigned parks. `/users` returns active users sharing the selected/assigned parks; its default role filter is `RANGER`.

Manager flow: login → load profile/parks → load routes and eligible rangers → create assignment with a new UUID → view assignments/patrol summaries → open patrol details.

Ranger flow: login → load assigned patrols → open route → record track, waypoints, and observations locally → PUT the completed snapshot with a new UUID → refresh assignments → view the completed patrol. Offline storage and retry scheduling belong to Flutter. The API accepts completed snapshots only.

## Common response

JSON APIs use the shared `utility.ResponseGenerator` component. Controllers validate/map inputs and delegate directly to the feature service; the service performs the operation and calls `generateSuccessResponse`. Exception advice and security handlers call `generateErrorResponse` for business, validation, and security failures, including download failures. First creation returns 201 with `Location`; an identical retry returns 200 without that header. Successful image/PDF downloads return binary content with private, no-store caching.

Success (HTTP 200, or 201 on creation):

```json
{
  "status": "00",
  "description": "SUCCESS",
  "data": {"id": "example"},
  "error": {"errorCode": "00", "errorDescription": "SUCCESS", "fieldErrors": {}}
}
```

Error (the HTTP status indicates failure):

```json
{
  "status": "01",
  "description": "FAIL",
  "data": {},
  "error": {
    "errorCode": "VALIDATION_FAILED",
    "errorDescription": "Check the highlighted fields.",
    "fieldErrors": {"email": "must not be blank"}
  }
}
```

The following examples show the value of `data`. The complete schemas, envelope, and examples for every endpoint are in `implemented-openapi.json`.

## Login and profile

`POST /auth/login` request:

```json
{"email":"ranger@wildguard.local","password":"your-local-demo-password"}
```

Response `data`:

```json
{
  "accessToken": "<signed-jwt>", "tokenType": "Bearer", "expiresIn": 3600,
  "user": {
    "id": "usr-ranger", "name": "Demo ranger", "email": "ranger@wildguard.local",
    "role": "RANGER", "parkIds": ["park-yala"]
  }
}
```

Email lookup is case insensitive. Passwords are BCrypt hashes in MongoDB. Login passwords are limited to 72 characters and 72 UTF-8 bytes. Unknown users, incorrect passwords, and inactive accounts return `401 INVALID_CREDENTIALS`. Expired/invalid tokens return `401 UNAUTHORIZED`. There is no registration/refresh-token/logout endpoint in this milestone; Flutter clears its stored token on logout.

`GET /auth/me` returns the same profile object as `login.data.user`. It never returns the password hash.

## Lookups and pagination

All list APIs return `data` shaped as `{"items":[],"page":0,"size":20,"totalItems":0}`. Query parameters: `page` defaults to 0, allowed 0–100000; `size` defaults to 20, allowed 1–100. Lookups sort by name then ID; assignments by scheduled start then ID descending; patrols by completion time then ID descending.

| Endpoint | Additional query parameters | Item fields |
| --- | --- | --- |
| `/parks` | None | `id`, `name`, `timezone`, `areas[{id,name}]` |
| `/users` | Optional `parkId`, optional `role` (default `RANGER`) | `id`, `name`, `role` |
| `/patrol-routes` | Optional `parkId` | `id`, `parkId`, `areaId`, `name`, `plannedDistanceMeters`, `pathPoints[{latitude,longitude}]` |
| `/patrol-assignments` | Optional `parkId`, `status=ASSIGNED\|COMPLETED` | Assignment object shown below |
| `/patrols` | Optional `parkId`, `routeId`, paired `from`/`to` | Patrol summary shown below |

Omitting `parkId` searches assigned parks. An explicitly unauthorized park returns 403. `/patrol-routes/{id}` returns the route object, or 404 when it does not exist.

## Assign a patrol

`PUT /patrol-assignments/11111111-1111-4111-8111-111111111111` request:

```json
{
  "routeId": "route-demo", "rangerId": "usr-ranger",
  "scheduledStartAt": "2026-10-07T01:00:00Z",
  "scheduledEndAt": "2026-10-07T03:00:00Z"
}
```

Response `data`:

```json
{
  "id": "11111111-1111-4111-8111-111111111111", "parkId": "park-yala",
  "routeId": "route-demo", "rangerId": "usr-ranger",
  "scheduledStartAt": "2026-10-07T01:00:00Z", "scheduledEndAt": "2026-10-07T03:00:00Z",
  "status": "ASSIGNED", "assignedBy": "usr-manager", "createdAt": "2026-10-07T00:30:00Z"
}
```

The route determines the park. The selected user must be an active ranger assigned to that park. End must follow start. A unique database index prevents duplicate assignments for the same ranger and start time; overlapping assignments with different start times are allowed in this milestone.

Creation returns 201 and a `Location` header. An identical retry by the original manager returns 200; different data/manager with the same UUID returns `409 IDEMPOTENCY_CONFLICT`. Assignments are immutable. A completed patrol makes the assignment's returned status `COMPLETED`; status is derived from stored patrols and is filtered before pagination.

## Complete and retrieve a patrol

`PUT /patrols/22222222-2222-4222-8222-222222222222` request:

```json
{
  "assignmentId": "11111111-1111-4111-8111-111111111111",
  "startedAt": "2026-10-07T01:00:00Z", "endedAt": "2026-10-07T03:00:00Z",
  "trackPoints": [
    {"latitude":6.37,"longitude":81.50,"recordedAt":"2026-10-07T01:00:00Z","accuracyMeters":8},
    {"latitude":6.3701,"longitude":81.5001,"recordedAt":"2026-10-07T01:00:30Z","accuracyMeters":8}
  ],
  "waypoints": [], "observations": []
}
```

Only the assigned ranger can submit. All three arrays are required; empty arrays are allowed. End must follow start and may be no more than 5 minutes in the future. Track times must be ordered; all track, waypoint, and observation times must fall within the patrol. Waypoint and observation IDs must be unique within their respective arrays.

Limits: 10000 track points, 100 waypoints, 100 observations. Latitude −90…90, longitude −180…180, optional accuracy 0…10000 meters. A waypoint requires UUID `id`, `label` (up to 100 characters), `recordedAt`, and `location`, with optional `notes` (up to 1000 characters). An observation requires UUID `id`, `text` (up to 2000 characters), `observedAt`, and `location`. Location is `{"latitude":6.37,"longitude":81.5,"source":"GPS","accuracyMeters":8}`, with source `GPS` or `MANUAL`. Optional accuracy/notes may be omitted or null. IDs submitted as UUIDs must use lowercase hexadecimal characters. Use timestamps with UTC `Z` or an explicit offset; MongoDB persists timestamps at millisecond precision.

Distance uses the Haversine formula on adjacent GPS points. It excludes gaps greater than 120 seconds, equal timestamps, and segments containing a fix with reported accuracy over 100 meters. Manual waypoints do not add distance.

Creation returns 201 and `Location`; identical retries by the ranger return 200. Changed reuse of the same UUID returns `409 IDEMPOTENCY_CONFLICT`; a different UUID for an already completed assignment returns `409 ASSIGNMENT_ALREADY_USED`. Unique indexes and insert-only writes enforce these rules under concurrent requests.

`GET /patrols` returns summaries containing `id`, `assignmentId`, `routeId`, `parkId`, `rangerId`, `status=COMPLETED`, `startedAt`, `endedAt`, `recordedDistanceMeters`, `waypointCount`, `observationCount`, `createdAt`. It excludes GPS arrays. `GET /patrols/{id}` and the PUT response include these fields plus `trackPoints`, `waypoints`, `observations`. Another ranger's record returns 404.

For date filtering, supply `parkId`, `from`, and `to` together, e.g. `/patrols?parkId=park-yala&from=2026-10-01&to=2026-10-07`. Dates are inclusive in the park's timezone, filter on patrol completion, and allow at most 92 days.

## Error codes

| HTTP | Common codes |
| --- | --- |
| 400 | `VALIDATION_FAILED`, `INVALID_REQUEST` |
| 401 | `INVALID_CREDENTIALS`, `UNAUTHORIZED` |
| 403 | `ACCESS_DENIED`, `PARK_ACCESS_DENIED` |
| 404 | `NOT_FOUND` |
| 409 | `IDEMPOTENCY_CONFLICT`, `ASSIGNMENT_SCHEDULE_CONFLICT`, `ASSIGNMENT_ALREADY_USED` |
| 413 | `FILE_TOO_LARGE` |
| 415 | `UNSUPPORTED_MEDIA_TYPE`, `UNSUPPORTED_IMAGE_TYPE` |
| 500 | `INTERNAL_ERROR`, `REPORT_RENDERING_FAILED` |
| 503 | `STORAGE_UNAVAILABLE` |

The authenticated user determines actor IDs, roles, and park permissions. Patrols derive their park/ranger/route from the assignment; camera images derive their park from the camera trap. Other resource bodies explicitly identify the park/area when required by their schema. Do not send reporter/officer/reviewer IDs, statuses, creation timestamps, or computed counts/distance. Unknown JSON fields are rejected.

## Media and incident flow

Upload `PUT /media/{uuid}` with multipart fields `file`, `parkId`, `category`. The declared MIME type must match decoded JPEG/PNG content; maximum 5 MiB and 20 million pixels. Categories: ranger `INCIDENT`/`ALERT_RESPONSE`; liaison `ALERT_RESPONSE`; member `COMMUNITY_REPORT`; manager/researcher `CAMERA_TRAP`. Media responses contain `id`, `parkId`, `category`, `contentType`, `sizeBytes`, `contentUrl`, `createdAt`. Identical owner/park/category/bytes retry returns 200; changed reuse returns 409. `GET /media/{id}/content` is authenticated and returns JPEG/PNG bytes. Unlinked images are owner-only; linked images can also be read by users permitted to read their parent resource.

Ranger flow: upload `INCIDENT` image → `PUT /incidents/{uuid}` → list/get the submitted incident. Request:

```json
{
  "parkId":"park-yala", "areaId":"area-b1", "type":"SNARE",
  "detectedAt":"2026-10-07T04:54:00Z",
  "location":{"latitude":6.35,"longitude":81.5,"source":"GPS","accuracyMeters":8},
  "description":"Wire snare found beside the trail.",
  "photoId":"33333333-3333-4333-8333-333333333333"
}
```

The service verifies area membership, an observation time no more than five minutes in the future, and owned evidence in the correct park/category. Optional `assignmentId` must belong to the same ranger/park. Response adds `id`, `reportedBy`, `status=SUBMITTED`, `createdAt`. Rangers read their own incidents; managers read their parks. List filters: optional `parkId`, `type`, `areaId`, paired `from`/`to`, `page`, `size`. Dates filter `detectedAt` in the park's timezone, up to 92 days.

## Alert flow

Manager `PUT /alerts/{uuid}` accepts `parkId`, `areaId`, `animal`, `collarId`, `riskLevel=LOW|MEDIUM|HIGH`, `location`, `locationUpdatedAt`, `detectedAt`. It creates `NEW`; this is manual setup. The recorded observation times are retained on GET.

Officer flow: list alerts → GET details → `POST /alerts/{id}/accept` with no body → optional support → `PUT /alerts/{id}/response`. Acceptance atomically sets `RESPONDING`, `assignedOfficerId`, `acceptedAt`. A concurrent different officer loses with `409 ALERT_ALREADY_ASSIGNED`; the same officer's retry returns the current alert.

`POST /alerts/{id}/decline` accepts `{"reason":"Already responding elsewhere."}` before acceptance. It records one reason per officer and leaves `NEW`. Exact retries return the original receipt; changed reasons return 409. Declines are limited to 100 officers. A declined officer cannot accept that alert.

`POST /alerts/{id}/support` accepts `{"requestId":"66666666-6666-4666-8666-666666666666","reason":"Additional officers needed."}`. Only the assigned responding officer can request support. Up to 20 requests are stored; exact request-ID retries return the existing `REQUESTED` receipt. Changed reuse returns 409. This records a request without sending a notification.

Response request: `{"actionTaken":"Guided the elephant away.","result":"Returned to the forest.","notes":"No injuries.","photoIds":[]}`. Notes/photos are optional. Missing/null `photoIds` becomes an empty list; lists allow at most three distinct, non-null owned `ALERT_RESPONSE` image IDs. The assigned officer's response and `RESOLVED` state save atomically. Exact retry is safe; changed final responses return 409. Failed persistence retains the prior state.

List filters are optional `parkId`, `status`, `page`, `size`. Officers see non-declined new alerts, their responding alerts, and resolved history; managers see all alerts in their parks. Details are park scoped. Invalid state changes return `409 ALERT_STATE_CONFLICT`; a full support list returns `409 SUPPORT_LIMIT_REACHED`.

## Analytics and reports

`GET /analytics/summary?parkId=park-yala&from=2026-10-01&to=2026-10-07` requires all three parameters. Dates are inclusive park-local dates, at most 92 days. Response includes type/daily/area incident counts, community report count, resolved alert count, and route coverage. Daily series includes zero-count dates. Areas with at least three incidents are potential hotspots.

Coverage basis is `ASSIGNED_ROUTES_COMPLETED`: unique routes with assignments starting in the period are the denominator; those routes with a linked patrol ending in that period are the numerator. No assignments gives `coveragePercent=null`. This measures route completion. No stored activity gives `dataAvailable=false`; database failures return 503.

`PUT /reports/{uuid}` request:

```json
{"parkId":"park-yala","reportType":"MONTHLY_CONSERVATION","from":"2026-10-01","to":"2026-10-07",
 "sections":["INCIDENT_STATISTICS","INCIDENT_TRENDS","HOTSPOT_SUMMARY","PATROL_COVERAGE","CONFLICT_SUMMARY"]}
```

Types: `MONTHLY_CONSERVATION`, `INCIDENTS`, `PATROL_COVERAGE`, `CONFLICTS`. Choose one to five distinct sections. Creation saves a complete immutable analytics snapshot and returns metadata plus `snapshot`, `status=GENERATED`, `generatedBy`, `generatedAt`, `downloadUrl`. Identical retry returns the original snapshot without recalculating it. `GET /reports?parkId=...` returns paged metadata; GET details includes the snapshot. `GET /reports/{id}/download` returns `application/pdf` with an attachment filename. Managers read reports in their parks. PDFs include only the selected sections and are generated from the saved snapshot.

## Community and camera flows

Member `PUT /community-reports/{uuid}` requires `parkId`, `areaId`, `type=WILDLIFE_SIGHTING|CROP_DAMAGE`, `village`, `occurredAt`, `description`. Sightings also require `species`; crop damage requires `cropDetails`. Location and an owned `COMMUNITY_REPORT` photo are optional. Response adds `id`, `reportedBy`, `status=SUBMITTED`, `createdAt`. Members read their own reports; managers/liaison officers read their parks. List filters match incidents, with dates filtering `occurredAt`. These endpoints implement app reporting; SMS ingestion remains external integration work.

Manager/researcher flow: `GET /camera-traps?parkId=...` → upload a `CAMERA_TRAP` image → `PUT /camera-trap-images/{uuid}` with `{"cameraTrapId":"CT-DEMO","mediaId":"33333333-3333-4333-8333-333333333333","capturedAt":"2026-10-07T04:00:00Z"}`. The service validates trap access and image ownership/category, then sets `PENDING_REVIEW`. List requires `parkId`, with optional `cameraTrapId`, `status`, and pagination.

`PUT /camera-trap-images/{id}/review` accepts `{"species":"Elephant","possiblePoacher":false,"notes":"Clear image."}`. Species and the boolean are required; notes are optional. Review atomically sets `REVIEWED` and records reviewer/time. Exact retries by that reviewer return the existing record; changed reviews or a second reviewer return 409. `possiblePoacher=true` is a verification flag. Reviews are manual and final in this milestone.
