# WildGuard API specification

Original proposed contract: **36 operations**. The first 11 operations are now implemented with the reference-style response envelope. Use [implemented-api.md](implemented-api.md) and [implemented-openapi.json](implemented-openapi.json) for their current contracts; the examples below remain planning material for the wider system. Base URL: `http://localhost:8080/api/v1`.

Requests/responses below agree with `openapi.json`. OpenAPI contains full schemas, limits and complete examples. Longer arrays in this readable guide show representative rows; do not use a shortened analytics example as a complete dataset. Coordinates and observations are illustrative, not verified wildlife locations. Examples illustrate separate calls and are not a preloaded live dataset. For a camera/community/response photo, upload a different UUID with the matching category and owner; do not reuse the incident photo example across account roles.

## Shared conventions

Authenticated request headers:

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
Accept: application/json
```

For media upload use `multipart/form-data` with the boundary supplied by the HTTP client. For image/PDF downloads, request the appropriate binary content type. Seed/reference IDs are strings; IDs for created records are client UUIDs.

A first immutable-record PUT returns **201**; identical retries return **200** and the original record. Changed reuse is **409**. Records are finalized at submission; drafts and active patrols remain local. Identity/status fields are server-owned. Errors follow the `ApiError` schema.

Pagination is zero-based, default 20, maximum 100. Date pairs are inclusive park-local dates, at most 92 days. Optional fields should be omitted rather than set to null, except `coveragePercent`, which is null when there are no assigned routes.

## Endpoint inventory

| Method | Path | Access |
| --- | --- | --- |
| POST | `/auth/login` | Public login |
| GET | `/auth/me` | PARK_MANAGER, RANGER, LIAISON_OFFICER, RESEARCHER, COMMUNITY_MEMBER |
| GET | `/parks` | PARK_MANAGER, RANGER, LIAISON_OFFICER, RESEARCHER, COMMUNITY_MEMBER |
| GET | `/users` | PARK_MANAGER |
| GET | `/patrol-routes` | PARK_MANAGER, RANGER |
| GET | `/patrol-routes/{id}` | PARK_MANAGER, RANGER |
| PUT | `/patrol-assignments/{id}` | PARK_MANAGER |
| GET | `/patrol-assignments` | PARK_MANAGER, RANGER |
| PUT | `/patrols/{id}` | RANGER |
| GET | `/patrols` | PARK_MANAGER, RANGER |
| GET | `/patrols/{id}` | PARK_MANAGER, RANGER |
| PUT | `/media/{id}` | PARK_MANAGER, RANGER, LIAISON_OFFICER, RESEARCHER, COMMUNITY_MEMBER |
| GET | `/media/{id}/content` | PARK_MANAGER, RANGER, LIAISON_OFFICER, RESEARCHER, COMMUNITY_MEMBER |
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

## Authentication

### POST `/auth/login`

Sign in.

Access: Public login.

Only this operation is public. Validate active account and BCrypt password; generic 401 for invalid credentials. The token string in this example is a placeholder.

Request: `application/json`; schema `LoginRequest`.

```json
{
  "email": "ranger@example.com",
  "password": "demo-password"
}
```

Response: **200**, `application/json`.

```json
{
  "accessToken": "<signed-jwt>",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": "usr-ranger-1",
    "name": "Nimal Perera",
    "email": "ranger@example.com",
    "role": "RANGER",
    "parkIds": [
      "park-yala"
    ]
  }
}
```

Error statuses: 400, 401, 500, 503. See the common error shape below.

### GET `/auth/me`

Get the signed-in account.

Access: PARK_MANAGER, RANGER, LIAISON_OFFICER, RESEARCHER, COMMUNITY_MEMBER.

Return the active account from the authenticated subject. Never include password hashes.

Request body: none.

Response: **200**, `application/json`.

```json
{
  "id": "usr-ranger-1",
  "name": "Nimal Perera",
  "email": "ranger@example.com",
  "role": "RANGER",
  "parkIds": [
    "park-yala"
  ]
}
```

Error statuses: 400, 401, 403, 404, 500, 503. See the common error shape below.


## Reference

### GET `/parks`

List authorized parks.

Access: PARK_MANAGER, RANGER, LIAISON_OFFICER, RESEARCHER, COMMUNITY_MEMBER.

Return only parks assigned to this account, including their known areas and timezone.

Parameters:

| Name | In | Required | Type | Example |
| --- | --- | --- | --- | --- |
| `page` | query | No | integer | `0` |
| `size` | query | No | integer | `20` |

Request body: none.

Response: **200**, `application/json`.

```json
{
  "items": [
    {
      "id": "park-yala",
      "name": "Yala National Park",
      "timezone": "Asia/Colombo",
      "areas": [
        {
          "id": "area-b1",
          "name": "Block 1"
        },
        {
          "id": "area-north",
          "name": "Northern boundary"
        }
      ]
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 1
}
```

Error statuses: 400, 401, 403, 404, 500, 503. See the common error shape below.

### GET `/users`

List eligible rangers.

Access: PARK_MANAGER.

Active rangers in the authorized park only; excludes private account details.

Parameters:

| Name | In | Required | Type | Example |
| --- | --- | --- | --- | --- |
| `parkId` | query | Yes | string | `park-yala` |
| `role` | query | Yes | RANGER | `RANGER` |
| `page` | query | No | integer | `0` |
| `size` | query | No | integer | `20` |

Request body: none.

Response: **200**, `application/json`.

```json
{
  "items": [
    {
      "id": "usr-ranger-1",
      "name": "Nimal Perera",
      "role": "RANGER"
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 1
}
```

Error statuses: 400, 401, 403, 404, 500, 503. See the common error shape below.

### GET `/patrol-routes`

List authorized patrol routes.

Access: PARK_MANAGER, RANGER.

Seed routes for the first milestone; routes are not edited through this contract.

Parameters:

| Name | In | Required | Type | Example |
| --- | --- | --- | --- | --- |
| `parkId` | query | Yes | string | `park-yala` |
| `page` | query | No | integer | `0` |
| `size` | query | No | integer | `20` |

Request body: none.

Response: **200**, `application/json`.

```json
{
  "items": [
    {
      "id": "route-yala-b1",
      "parkId": "park-yala",
      "areaId": "area-b1",
      "name": "Block 1 wildlife trail",
      "plannedDistanceMeters": 8500,
      "pathPoints": [
        {
          "latitude": 6.35,
          "longitude": 81.5
        },
        {
          "latitude": 6.351,
          "longitude": 81.501
        }
      ]
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 1
}
```

Error statuses: 400, 401, 403, 404, 500, 503. See the common error shape below.

### GET `/patrol-routes/{id}`

Get a patrol route.

Access: PARK_MANAGER, RANGER.

Park-scoped route geometry and metadata.

Parameters:

| Name | In | Required | Type | Example |
| --- | --- | --- | --- | --- |
| `id` | path | Yes | string | `route-yala-b1` |

Request body: none.

Response: **200**, `application/json`.

```json
{
  "id": "route-yala-b1",
  "parkId": "park-yala",
  "areaId": "area-b1",
  "name": "Block 1 wildlife trail",
  "plannedDistanceMeters": 8500,
  "pathPoints": [
    {
      "latitude": 6.35,
      "longitude": 81.5
    },
    {
      "latitude": 6.351,
      "longitude": 81.501
    }
  ]
}
```

Error statuses: 400, 401, 403, 404, 500, 503. See the common error shape below.


## Patrols

### PUT `/patrol-assignments/{id}`

Assign a patrol route.

Access: PARK_MANAGER.

Route determines park/area. Ranger must be active and assigned to that park; end must be after start. Reject a second assignment for the same ranger/start instant. Identical UUID/payload replay returns 200; changed replay returns 409. No general overlap scheduler.

Parameters:

| Name | In | Required | Type | Example |
| --- | --- | --- | --- | --- |
| `id` | path | Yes | uuid | `11111111-1111-4111-8111-111111111111` |

Request: `application/json`; schema `AssignmentRequest`.

```json
{
  "routeId": "route-yala-b1",
  "rangerId": "usr-ranger-1",
  "scheduledStartAt": "2026-10-08T02:30:00Z",
  "scheduledEndAt": "2026-10-08T06:30:00Z"
}
```

Response: **201**, `application/json`.

```json
{
  "id": "11111111-1111-4111-8111-111111111111",
  "routeId": "route-yala-b1",
  "rangerId": "usr-ranger-1",
  "scheduledStartAt": "2026-10-08T02:30:00Z",
  "scheduledEndAt": "2026-10-08T06:30:00Z",
  "parkId": "park-yala",
  "status": "ASSIGNED",
  "assignedBy": "usr-manager-1",
  "createdAt": "2026-10-07T10:00:00Z"
}
```

First creation returns a `Location` header pointing to the resource; identical replay returns the same object with 200.

Error statuses: 400, 401, 403, 404, 409, 500, 503. See the common error shape below.

### GET `/patrol-assignments`

List patrol assignments.

Access: PARK_MANAGER, RANGER.

Ranger sees own assignments. Manager sees authorized-park assignments. Status is derived from submitted completed patrols.

Parameters:

| Name | In | Required | Type | Example |
| --- | --- | --- | --- | --- |
| `parkId` | query | No | string | `park-yala` |
| `status` | query | No | ASSIGNED / COMPLETED | `ASSIGNED` |
| `page` | query | No | integer | `0` |
| `size` | query | No | integer | `20` |

Request body: none.

Response: **200**, `application/json`.

```json
{
  "items": [
    {
      "id": "11111111-1111-4111-8111-111111111111",
      "routeId": "route-yala-b1",
      "rangerId": "usr-ranger-1",
      "scheduledStartAt": "2026-10-08T02:30:00Z",
      "scheduledEndAt": "2026-10-08T06:30:00Z",
      "parkId": "park-yala",
      "status": "ASSIGNED",
      "assignedBy": "usr-manager-1",
      "createdAt": "2026-10-07T10:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 1
}
```

Error statuses: 400, 401, 403, 404, 500, 503. See the common error shape below.

### PUT `/patrols/{id}`

Submit a completed patrol.

Access: RANGER.

Immutable completed snapshot; active recording remains local. Validate assignment ownership, one patrol per assignment, ordered timestamps and location/array limits. Compute distance from continuous GPS segments, ignoring gaps over 120 seconds and fixes with reported accuracy worse than 100 meters; manual waypoints do not add travel distance. Identical replay is safe; changed replay or reused assignment is 409.

Parameters:

| Name | In | Required | Type | Example |
| --- | --- | --- | --- | --- |
| `id` | path | Yes | uuid | `22222222-2222-4222-8222-222222222222` |

Request: `application/json`; schema `PatrolRequest`.

```json
{
  "assignmentId": "11111111-1111-4111-8111-111111111111",
  "startedAt": "2026-10-08T02:45:00Z",
  "endedAt": "2026-10-08T06:40:00Z",
  "trackPoints": [
    {
      "latitude": 6.35,
      "longitude": 81.5,
      "recordedAt": "2026-10-08T02:45:00Z",
      "accuracyMeters": 8
    },
    {
      "latitude": 6.3501,
      "longitude": 81.5001,
      "recordedAt": "2026-10-08T02:45:30Z",
      "accuracyMeters": 8
    }
  ],
  "waypoints": [
    {
      "id": "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
      "label": "Fresh footprints",
      "recordedAt": "2026-10-08T04:00:00Z",
      "location": {
        "latitude": 6.35,
        "longitude": 81.5,
        "source": "GPS",
        "accuracyMeters": 8
      }
    }
  ],
  "observations": [
    {
      "id": "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb",
      "text": "Fresh elephant footprints near the trail.",
      "observedAt": "2026-10-08T04:00:00Z",
      "location": {
        "latitude": 6.35,
        "longitude": 81.5,
        "source": "GPS",
        "accuracyMeters": 8
      }
    }
  ]
}
```

Response: **201**, `application/json`.

```json
{
  "id": "22222222-2222-4222-8222-222222222222",
  "assignmentId": "11111111-1111-4111-8111-111111111111",
  "routeId": "route-yala-b1",
  "parkId": "park-yala",
  "rangerId": "usr-ranger-1",
  "status": "COMPLETED",
  "startedAt": "2026-10-08T02:45:00Z",
  "endedAt": "2026-10-08T06:40:00Z",
  "recordedDistanceMeters": 15.7,
  "waypointCount": 1,
  "observationCount": 1,
  "createdAt": "2026-10-08T06:41:00Z",
  "trackPoints": [
    {
      "latitude": 6.35,
      "longitude": 81.5,
      "recordedAt": "2026-10-08T02:45:00Z",
      "accuracyMeters": 8
    },
    {
      "latitude": 6.3501,
      "longitude": 81.5001,
      "recordedAt": "2026-10-08T02:45:30Z",
      "accuracyMeters": 8
    }
  ],
  "waypoints": [
    {
      "id": "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
      "label": "Fresh footprints",
      "recordedAt": "2026-10-08T04:00:00Z",
      "location": {
        "latitude": 6.35,
        "longitude": 81.5,
        "source": "GPS",
        "accuracyMeters": 8
      }
    }
  ],
  "observations": [
    {
      "id": "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb",
      "text": "Fresh elephant footprints near the trail.",
      "observedAt": "2026-10-08T04:00:00Z",
      "location": {
        "latitude": 6.35,
        "longitude": 81.5,
        "source": "GPS",
        "accuracyMeters": 8
      }
    }
  ]
}
```

First creation returns a `Location` header pointing to the resource; identical replay returns the same object with 200.

Error statuses: 400, 401, 403, 404, 409, 500, 503. See the common error shape below.

### GET `/patrols`

List completed patrol summaries.

Access: PARK_MANAGER, RANGER.

Ranger sees own records; manager sees authorized parks. No track arrays in list responses. Date filters use endedAt; supply both dates or neither, range at most 92 days.

Parameters:

| Name | In | Required | Type | Example |
| --- | --- | --- | --- | --- |
| `parkId` | query | No | string | `park-yala` |
| `from` | query | No | date | `2026-08-01` |
| `to` | query | No | date | `2026-08-31` |
| `routeId` | query | No | string | `route-yala-b1` |
| `page` | query | No | integer | `0` |
| `size` | query | No | integer | `20` |

Request body: none.

Response: **200**, `application/json`.

```json
{
  "items": [
    {
      "id": "22222222-2222-4222-8222-222222222222",
      "assignmentId": "11111111-1111-4111-8111-111111111111",
      "routeId": "route-yala-b1",
      "parkId": "park-yala",
      "rangerId": "usr-ranger-1",
      "status": "COMPLETED",
      "startedAt": "2026-10-08T02:45:00Z",
      "endedAt": "2026-10-08T06:40:00Z",
      "recordedDistanceMeters": 15.7,
      "waypointCount": 1,
      "observationCount": 1,
      "createdAt": "2026-10-08T06:41:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 1
}
```

Error statuses: 400, 401, 403, 404, 500, 503. See the common error shape below.

### GET `/patrols/{id}`

Get completed patrol and recorded path.

Access: PARK_MANAGER, RANGER.

Owner ranger or authorized park manager. Includes stored waypoints, observations and track points.

Parameters:

| Name | In | Required | Type | Example |
| --- | --- | --- | --- | --- |
| `id` | path | Yes | uuid | `22222222-2222-4222-8222-222222222222` |

Request body: none.

Response: **200**, `application/json`.

```json
{
  "id": "22222222-2222-4222-8222-222222222222",
  "assignmentId": "11111111-1111-4111-8111-111111111111",
  "routeId": "route-yala-b1",
  "parkId": "park-yala",
  "rangerId": "usr-ranger-1",
  "status": "COMPLETED",
  "startedAt": "2026-10-08T02:45:00Z",
  "endedAt": "2026-10-08T06:40:00Z",
  "recordedDistanceMeters": 15.7,
  "waypointCount": 1,
  "observationCount": 1,
  "createdAt": "2026-10-08T06:41:00Z",
  "trackPoints": [
    {
      "latitude": 6.35,
      "longitude": 81.5,
      "recordedAt": "2026-10-08T02:45:00Z",
      "accuracyMeters": 8
    },
    {
      "latitude": 6.3501,
      "longitude": 81.5001,
      "recordedAt": "2026-10-08T02:45:30Z",
      "accuracyMeters": 8
    }
  ],
  "waypoints": [
    {
      "id": "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
      "label": "Fresh footprints",
      "recordedAt": "2026-10-08T04:00:00Z",
      "location": {
        "latitude": 6.35,
        "longitude": 81.5,
        "source": "GPS",
        "accuracyMeters": 8
      }
    }
  ],
  "observations": [
    {
      "id": "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb",
      "text": "Fresh elephant footprints near the trail.",
      "observedAt": "2026-10-08T04:00:00Z",
      "location": {
        "latitude": 6.35,
        "longitude": 81.5,
        "source": "GPS",
        "accuracyMeters": 8
      }
    }
  ]
}
```

Error statuses: 400, 401, 403, 404, 500, 503. See the common error shape below.


## Media

### PUT `/media/{id}`

Upload an image with a stable client UUID.

Access: PARK_MANAGER, RANGER, LIAISON_OFFICER, RESEARCHER, COMMUNITY_MEMBER.

Multipart form fields file, parkId and category. Decode/validate JPEG or PNG, max 5 MiB. Category must match role: ranger INCIDENT/ALERT_RESPONSE; liaison ALERT_RESPONSE; community member COMMUNITY_REPORT; researcher/manager CAMERA_TRAP. Same UUID, owner, category, park and bytes returns 200; changed bytes/metadata is 409. Only owner can access unlinked uploads.

Parameters:

| Name | In | Required | Type | Example |
| --- | --- | --- | --- | --- |
| `id` | path | Yes | uuid | `33333333-3333-4333-8333-333333333333` |

Request: `multipart/form-data`; schema `MediaUpload`.

```text
file: <JPEG/PNG binary, maximum 5 MiB>
parkId: park-yala
category: INCIDENT
```

Response: **201**, `application/json`.

```json
{
  "id": "33333333-3333-4333-8333-333333333333",
  "parkId": "park-yala",
  "category": "INCIDENT",
  "contentType": "image/jpeg",
  "sizeBytes": 245800,
  "contentUrl": "/api/v1/media/33333333-3333-4333-8333-333333333333/content",
  "createdAt": "2026-10-08T06:41:00Z"
}
```

First creation returns a `Location` header pointing to the resource; identical replay returns the same object with 200.

Error statuses: 400, 401, 403, 404, 409, 413, 415, 500, 503. See the common error shape below.

### GET `/media/{id}/content`

Download authorized image content.

Access: PARK_MANAGER, RANGER, LIAISON_OFFICER, RESEARCHER, COMMUNITY_MEMBER.

Owner or caller authorized to read the linked parent record. Unlinked media are owner-only. Authenticated download; never expose local file paths.

Parameters:

| Name | In | Required | Type | Example |
| --- | --- | --- | --- | --- |
| `id` | path | Yes | uuid | `33333333-3333-4333-8333-333333333333` |

Request body: none.

Response: **200**, `image/jpeg`.

```text
<JPEG or PNG bytes>
```

Error statuses: 400, 401, 403, 404, 500, 503. See the common error shape below.


## Incidents

### PUT `/incidents/{id}`

Submit a wildlife incident.

Access: RANGER.

Required evidence photo must already exist, belong to this ranger and park, and have INCIDENT category. Optional assignmentId must belong to this ranger in the same park. That assignment can link the incident to a still-local patrol without delaying incident submission; the final patrol uses the same assignmentId. Server sets reporter and SUBMITTED. Only explicitly submitted records are uploaded; no server drafts.

Parameters:

| Name | In | Required | Type | Example |
| --- | --- | --- | --- | --- |
| `id` | path | Yes | uuid | `44444444-4444-4444-8444-444444444444` |

Request: `application/json`; schema `IncidentRequest`.

```json
{
  "parkId": "park-yala",
  "areaId": "area-b1",
  "assignmentId": "11111111-1111-4111-8111-111111111111",
  "type": "SNARE",
  "detectedAt": "2026-10-08T04:54:00Z",
  "location": {
    "latitude": 6.35,
    "longitude": 81.5,
    "source": "GPS",
    "accuracyMeters": 8
  },
  "description": "Wire snare found beside a wildlife trail. No animal was trapped.",
  "photoId": "33333333-3333-4333-8333-333333333333"
}
```

Response: **201**, `application/json`.

```json
{
  "id": "44444444-4444-4444-8444-444444444444",
  "parkId": "park-yala",
  "areaId": "area-b1",
  "assignmentId": "11111111-1111-4111-8111-111111111111",
  "type": "SNARE",
  "detectedAt": "2026-10-08T04:54:00Z",
  "location": {
    "latitude": 6.35,
    "longitude": 81.5,
    "source": "GPS",
    "accuracyMeters": 8
  },
  "description": "Wire snare found beside a wildlife trail. No animal was trapped.",
  "photoId": "33333333-3333-4333-8333-333333333333",
  "reportedBy": "usr-ranger-1",
  "status": "SUBMITTED",
  "createdAt": "2026-10-08T06:41:00Z"
}
```

First creation returns a `Location` header pointing to the resource; identical replay returns the same object with 200.

Error statuses: 400, 401, 403, 404, 409, 500, 503. See the common error shape below.

### GET `/incidents`

List submitted incidents.

Access: PARK_MANAGER, RANGER.

Ranger sees own incidents; manager sees authorized parks. Dates filter detectedAt; provide both or neither, max 92 days. Page through the list for map points; do not truncate the map silently.

Parameters:

| Name | In | Required | Type | Example |
| --- | --- | --- | --- | --- |
| `parkId` | query | No | string | `park-yala` |
| `from` | query | No | date | `2026-08-01` |
| `to` | query | No | date | `2026-08-31` |
| `type` | query | No | SNARE / INJURED_ANIMAL / ANIMAL_CARCASS / ILLEGAL_CAMPSITE / POACHING_EVIDENCE / ANIMAL_FOOTPRINTS / OTHER | `SNARE` |
| `areaId` | query | No | string | `area-b1` |
| `page` | query | No | integer | `0` |
| `size` | query | No | integer | `20` |

Request body: none.

Response: **200**, `application/json`.

```json
{
  "items": [
    {
      "id": "44444444-4444-4444-8444-444444444444",
      "parkId": "park-yala",
      "areaId": "area-b1",
      "assignmentId": "11111111-1111-4111-8111-111111111111",
      "type": "SNARE",
      "detectedAt": "2026-10-08T04:54:00Z",
      "location": {
        "latitude": 6.35,
        "longitude": 81.5,
        "source": "GPS",
        "accuracyMeters": 8
      },
      "description": "Wire snare found beside a wildlife trail. No animal was trapped.",
      "photoId": "33333333-3333-4333-8333-333333333333",
      "reportedBy": "usr-ranger-1",
      "status": "SUBMITTED",
      "createdAt": "2026-10-08T06:41:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 1
}
```

Error statuses: 400, 401, 403, 404, 500, 503. See the common error shape below.

### GET `/incidents/{id}`

Get an incident.

Access: PARK_MANAGER, RANGER.

Owner ranger or manager in the incident park.

Parameters:

| Name | In | Required | Type | Example |
| --- | --- | --- | --- | --- |
| `id` | path | Yes | uuid | `44444444-4444-4444-8444-444444444444` |

Request body: none.

Response: **200**, `application/json`.

```json
{
  "id": "44444444-4444-4444-8444-444444444444",
  "parkId": "park-yala",
  "areaId": "area-b1",
  "assignmentId": "11111111-1111-4111-8111-111111111111",
  "type": "SNARE",
  "detectedAt": "2026-10-08T04:54:00Z",
  "location": {
    "latitude": 6.35,
    "longitude": 81.5,
    "source": "GPS",
    "accuracyMeters": 8
  },
  "description": "Wire snare found beside a wildlife trail. No animal was trapped.",
  "photoId": "33333333-3333-4333-8333-333333333333",
  "reportedBy": "usr-ranger-1",
  "status": "SUBMITTED",
  "createdAt": "2026-10-08T06:41:00Z"
}
```

Error statuses: 400, 401, 403, 404, 500, 503. See the common error shape below.


## Alerts

### PUT `/alerts/{id}`

Create a demo/setup conflict alert.

Access: PARK_MANAGER.

Manager-only manual setup for the first milestone, not automatic collar detection. LocationUpdatedAt is the observation time, never the GET time. All park/area and time references are validated. Server sets NEW.

Parameters:

| Name | In | Required | Type | Example |
| --- | --- | --- | --- | --- |
| `id` | path | Yes | uuid | `55555555-5555-4555-8555-555555555555` |

Request: `application/json`; schema `AlertSetupRequest`.

```json
{
  "parkId": "park-yala",
  "areaId": "area-north",
  "animal": "Asian elephant",
  "collarId": "ELE-017",
  "riskLevel": "HIGH",
  "location": {
    "latitude": 6.35,
    "longitude": 81.5,
    "source": "GPS",
    "accuracyMeters": 8
  },
  "locationUpdatedAt": "2026-10-08T13:13:00Z",
  "detectedAt": "2026-10-08T13:12:00Z"
}
```

Response: **201**, `application/json`.

```json
{
  "id": "55555555-5555-4555-8555-555555555555",
  "parkId": "park-yala",
  "areaId": "area-north",
  "animal": "Asian elephant",
  "collarId": "ELE-017",
  "riskLevel": "HIGH",
  "location": {
    "latitude": 6.35,
    "longitude": 81.5,
    "source": "GPS",
    "accuracyMeters": 8
  },
  "locationUpdatedAt": "2026-10-08T13:13:00Z",
  "detectedAt": "2026-10-08T13:12:00Z",
  "status": "NEW",
  "createdAt": "2026-10-08T13:12:01Z"
}
```

First creation returns a `Location` header pointing to the resource; identical replay returns the same object with 200.

Error statuses: 400, 401, 403, 404, 409, 500, 503. See the common error shape below.

### GET `/alerts`

List eligible conflict alerts.

Access: PARK_MANAGER, RANGER, LIAISON_OFFICER.

Park-scoped; officers see New alerts except their own declines, their own Responding alerts and eligible Resolved history. Managers can inspect all in their parks. Poll in foreground; not background delivery.

Parameters:

| Name | In | Required | Type | Example |
| --- | --- | --- | --- | --- |
| `parkId` | query | No | string | `park-yala` |
| `status` | query | No | NEW / RESPONDING / RESOLVED | `NEW` |
| `page` | query | No | integer | `0` |
| `size` | query | No | integer | `20` |

Request body: none.

Response: **200**, `application/json`.

```json
{
  "items": [
    {
      "id": "55555555-5555-4555-8555-555555555555",
      "parkId": "park-yala",
      "areaId": "area-north",
      "animal": "Asian elephant",
      "collarId": "ELE-017",
      "riskLevel": "HIGH",
      "location": {
        "latitude": 6.35,
        "longitude": 81.5,
        "source": "GPS",
        "accuracyMeters": 8
      },
      "locationUpdatedAt": "2026-10-08T13:13:00Z",
      "detectedAt": "2026-10-08T13:12:00Z",
      "status": "NEW",
      "createdAt": "2026-10-08T13:12:01Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 1
}
```

Error statuses: 400, 401, 403, 404, 500, 503. See the common error shape below.

### GET `/alerts/{id}`

Get alert and last-known location.

Access: PARK_MANAGER, RANGER, LIAISON_OFFICER.

Authorized park scope; preserve locationUpdatedAt. Do not label a stale observation Live.

Parameters:

| Name | In | Required | Type | Example |
| --- | --- | --- | --- | --- |
| `id` | path | Yes | uuid | `55555555-5555-4555-8555-555555555555` |

Request body: none.

Response: **200**, `application/json`.

```json
{
  "id": "55555555-5555-4555-8555-555555555555",
  "parkId": "park-yala",
  "areaId": "area-north",
  "animal": "Asian elephant",
  "collarId": "ELE-017",
  "riskLevel": "HIGH",
  "location": {
    "latitude": 6.35,
    "longitude": 81.5,
    "source": "GPS",
    "accuracyMeters": 8
  },
  "locationUpdatedAt": "2026-10-08T13:13:00Z",
  "detectedAt": "2026-10-08T13:12:00Z",
  "status": "RESPONDING",
  "createdAt": "2026-10-08T13:12:01Z",
  "assignedOfficerId": "usr-ranger-1",
  "acceptedAt": "2026-10-08T13:14:00Z",
  "supportRequests": []
}
```

Error statuses: 400, 401, 403, 404, 500, 503. See the common error shape below.

### POST `/alerts/{id}/accept`

Accept an unassigned alert.

Access: RANGER, LIAISON_OFFICER.

No body. Atomic conditional NEW-to-RESPONDING transition; authenticated officer must be eligible and not have declined. Same officer retry returns 200. Another owner or invalid state is 409.

Parameters:

| Name | In | Required | Type | Example |
| --- | --- | --- | --- | --- |
| `id` | path | Yes | uuid | `55555555-5555-4555-8555-555555555555` |

Request body: none.

Response: **200**, `application/json`.

```json
{
  "id": "55555555-5555-4555-8555-555555555555",
  "parkId": "park-yala",
  "areaId": "area-north",
  "animal": "Asian elephant",
  "collarId": "ELE-017",
  "riskLevel": "HIGH",
  "location": {
    "latitude": 6.35,
    "longitude": 81.5,
    "source": "GPS",
    "accuracyMeters": 8
  },
  "locationUpdatedAt": "2026-10-08T13:13:00Z",
  "detectedAt": "2026-10-08T13:12:00Z",
  "status": "RESPONDING",
  "createdAt": "2026-10-08T13:12:01Z",
  "assignedOfficerId": "usr-ranger-1",
  "acceptedAt": "2026-10-08T13:14:00Z",
  "supportRequests": []
}
```

Error statuses: 400, 401, 403, 404, 409, 500, 503. See the common error shape below.

### POST `/alerts/{id}/decline`

Record inability to respond.

Access: RANGER, LIAISON_OFFICER.

NEW alerts only, before acceptance. Leave alert NEW and record one decline per officer. Same reason retry returns 200; changed repeated decline or invalid state is 409. Automatic reassignment/escalation is deferred.

Parameters:

| Name | In | Required | Type | Example |
| --- | --- | --- | --- | --- |
| `id` | path | Yes | uuid | `55555555-5555-4555-8555-555555555555` |

Request: `application/json`; schema `DeclineRequest`.

```json
{
  "reason": "Already responding to another incident."
}
```

Response: **200**, `application/json`.

```json
{
  "alertId": "55555555-5555-4555-8555-555555555555",
  "officerId": "usr-ranger-1",
  "status": "NEW",
  "reason": "Already responding to another incident.",
  "recordedAt": "2026-10-08T13:14:00Z"
}
```

Error statuses: 400, 401, 403, 404, 409, 500, 503. See the common error shape below.

### POST `/alerts/{id}/support`

Record a support request.

Access: RANGER, LIAISON_OFFICER.

Assigned officer on RESPONDING alert only. Embed bounded support requests (max 20). Atomic deduplication by requestId: exact replay 200; changed requestId payload 409. REQUESTED means recorded, not delivered.

Parameters:

| Name | In | Required | Type | Example |
| --- | --- | --- | --- | --- |
| `id` | path | Yes | uuid | `55555555-5555-4555-8555-555555555555` |

Request: `application/json`; schema `SupportRequest`.

```json
{
  "requestId": "66666666-6666-4666-8666-666666666666",
  "reason": "Additional officers needed near the village boundary."
}
```

Response: **200**, `application/json`.

```json
{
  "requestId": "66666666-6666-4666-8666-666666666666",
  "alertId": "55555555-5555-4555-8555-555555555555",
  "requestedBy": "usr-ranger-1",
  "reason": "Additional officers needed near the village boundary.",
  "status": "REQUESTED",
  "requestedAt": "2026-10-08T13:20:00Z"
}
```

Error statuses: 400, 401, 403, 404, 409, 500, 503. See the common error shape below.

### PUT `/alerts/{id}/response`

Save response and resolve alert.

Access: RANGER, LIAISON_OFFICER.

Assigned officer only. Atomically store response and set RESOLVED in one alert document. Any photos must be owned/park-scoped ALERT_RESPONSE media. Exact response retry returns 200; a changed final response or invalid state returns 409. Failed persistence leaves RESPONDING.

Parameters:

| Name | In | Required | Type | Example |
| --- | --- | --- | --- | --- |
| `id` | path | Yes | uuid | `55555555-5555-4555-8555-555555555555` |

Request: `application/json`; schema `AlertResponseRequest`.

```json
{
  "actionTaken": "Guided the elephant away from the farmland.",
  "result": "Animal returned towards the forest.",
  "notes": "No injuries or property damage reported.",
  "photoIds": []
}
```

Response: **200**, `application/json`.

```json
{
  "id": "55555555-5555-4555-8555-555555555555",
  "parkId": "park-yala",
  "areaId": "area-north",
  "animal": "Asian elephant",
  "collarId": "ELE-017",
  "riskLevel": "HIGH",
  "location": {
    "latitude": 6.35,
    "longitude": 81.5,
    "source": "GPS",
    "accuracyMeters": 8
  },
  "locationUpdatedAt": "2026-10-08T13:13:00Z",
  "detectedAt": "2026-10-08T13:12:00Z",
  "status": "RESOLVED",
  "createdAt": "2026-10-08T13:12:01Z",
  "assignedOfficerId": "usr-ranger-1",
  "acceptedAt": "2026-10-08T13:14:00Z",
  "supportRequests": [],
  "response": {
    "actionTaken": "Guided the elephant away from the farmland.",
    "result": "Animal returned towards the forest.",
    "notes": "No injuries or property damage reported.",
    "photoIds": [],
    "recordedBy": "usr-ranger-1",
    "recordedAt": "2026-10-08T13:55:00Z"
  }
}
```

Error statuses: 400, 401, 403, 404, 409, 500, 503. See the common error shape below.


## Analytics

### GET `/analytics/summary`

Get simple conservation analytics.

Access: PARK_MANAGER.

Inclusive park-local dates, at most 92 days. Aggregate stored incidents, completed patrols, community reports and resolved alerts. Potential hotspot threshold: 3 incidents per area. Route coverage denominator is unique routes with assignments scheduled to start in the period; numerator is those routes with an assignment patrol completed in the period. This is not geographic area coverage. No assignments => coveragePercent=null. No data => dataAvailable=false, 200; database failure =>503.

Parameters:

| Name | In | Required | Type | Example |
| --- | --- | --- | --- | --- |
| `parkId` | query | Yes | string | `park-yala` |
| `from` | query | Yes | date | `2026-08-01` |
| `to` | query | Yes | date | `2026-08-31` |

Request body: none.

Response: **200**, `application/json`.

```json
{
  "parkId": "park-yala",
  "from": "2026-08-01",
  "to": "2026-08-31",
  "dataAvailable": true,
  "totalIncidents": 58,
  "incidentTypeCounts": [
    {
      "type": "SNARE",
      "count": 24
    },
    {
      "type": "ANIMAL_CARCASS",
      "count": 10
    }
  ],
  "dailyIncidentCounts": [
    {
      "date": "2026-08-01",
      "count": 2
    },
    {
      "date": "2026-08-02",
      "count": 2
    }
  ],
  "areaCounts": [
    {
      "areaId": "area-b1",
      "areaName": "Block 1",
      "incidentCount": 20,
      "potentialHotspot": true
    },
    {
      "areaId": "area-north",
      "areaName": "Northern boundary",
      "incidentCount": 38,
      "potentialHotspot": true
    }
  ],
  "patrolCoverage": {
    "basis": "ASSIGNED_ROUTES_COMPLETED",
    "assignedRouteCount": 10,
    "completedRouteCount": 7,
    "coveragePercent": 70
  },
  "communityReportCount": 18,
  "resolvedAlertCount": 12,
  "generatedAt": "2026-10-08T06:41:00Z"
}
```

Error statuses: 400, 401, 403, 404, 500, 503. See the common error shape below.


## Reports

### PUT `/reports/{id}`

Save a generated report snapshot.

Access: PARK_MANAGER.

Compute bounded analytics and store an immutable snapshot synchronously, max 92 days. Identical UUID/request returns original snapshot even if source data changed. Creation is also Save; no second save operation. PDF is generated from this snapshot on download.

Parameters:

| Name | In | Required | Type | Example |
| --- | --- | --- | --- | --- |
| `id` | path | Yes | uuid | `77777777-7777-4777-8777-777777777777` |

Request: `application/json`; schema `ReportRequest`.

```json
{
  "parkId": "park-yala",
  "reportType": "MONTHLY_CONSERVATION",
  "from": "2026-08-01",
  "to": "2026-08-31",
  "sections": [
    "INCIDENT_STATISTICS",
    "PATROL_COVERAGE",
    "CONFLICT_SUMMARY"
  ]
}
```

Response: **201**, `application/json`.

```json
{
  "id": "77777777-7777-4777-8777-777777777777",
  "parkId": "park-yala",
  "reportType": "MONTHLY_CONSERVATION",
  "from": "2026-08-01",
  "to": "2026-08-31",
  "sections": [
    "INCIDENT_STATISTICS",
    "PATROL_COVERAGE",
    "CONFLICT_SUMMARY"
  ],
  "status": "GENERATED",
  "generatedBy": "usr-manager-1",
  "generatedAt": "2026-10-08T06:41:00Z",
  "downloadUrl": "/api/v1/reports/77777777-7777-4777-8777-777777777777/download",
  "snapshot": {
    "parkId": "park-yala",
    "from": "2026-08-01",
    "to": "2026-08-31",
    "dataAvailable": true,
    "totalIncidents": 58,
    "incidentTypeCounts": [
      {
        "type": "SNARE",
        "count": 24
      },
      {
        "type": "ANIMAL_CARCASS",
        "count": 10
      }
    ],
    "dailyIncidentCounts": [
      {
        "date": "2026-08-01",
        "count": 2
      },
      {
        "date": "2026-08-02",
        "count": 2
      }
    ],
    "areaCounts": [
      {
        "areaId": "area-b1",
        "areaName": "Block 1",
        "incidentCount": 20,
        "potentialHotspot": true
      },
      {
        "areaId": "area-north",
        "areaName": "Northern boundary",
        "incidentCount": 38,
        "potentialHotspot": true
      }
    ],
    "patrolCoverage": {
      "basis": "ASSIGNED_ROUTES_COMPLETED",
      "assignedRouteCount": 10,
      "completedRouteCount": 7,
      "coveragePercent": 70
    },
    "communityReportCount": 18,
    "resolvedAlertCount": 12,
    "generatedAt": "2026-10-08T06:41:00Z"
  }
}
```

First creation returns a `Location` header pointing to the resource; identical replay returns the same object with 200.

Error statuses: 400, 401, 403, 404, 409, 500, 503. See the common error shape below.

### GET `/reports`

List saved report summaries.

Access: PARK_MANAGER.

Reports in authorized parks; snapshot omitted from list.

Parameters:

| Name | In | Required | Type | Example |
| --- | --- | --- | --- | --- |
| `parkId` | query | Yes | string | `park-yala` |
| `page` | query | No | integer | `0` |
| `size` | query | No | integer | `20` |

Request body: none.

Response: **200**, `application/json`.

```json
{
  "items": [
    {
      "id": "77777777-7777-4777-8777-777777777777",
      "parkId": "park-yala",
      "reportType": "MONTHLY_CONSERVATION",
      "from": "2026-08-01",
      "to": "2026-08-31",
      "sections": [
        "INCIDENT_STATISTICS",
        "PATROL_COVERAGE",
        "CONFLICT_SUMMARY"
      ],
      "status": "GENERATED",
      "generatedBy": "usr-manager-1",
      "generatedAt": "2026-10-08T06:41:00Z",
      "downloadUrl": "/api/v1/reports/77777777-7777-4777-8777-777777777777/download"
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 1
}
```

Error statuses: 400, 401, 403, 404, 500, 503. See the common error shape below.

### GET `/reports/{id}`

Preview a saved report.

Access: PARK_MANAGER.

Read the stored immutable snapshot, not current analytics.

Parameters:

| Name | In | Required | Type | Example |
| --- | --- | --- | --- | --- |
| `id` | path | Yes | uuid | `77777777-7777-4777-8777-777777777777` |

Request body: none.

Response: **200**, `application/json`.

```json
{
  "id": "77777777-7777-4777-8777-777777777777",
  "parkId": "park-yala",
  "reportType": "MONTHLY_CONSERVATION",
  "from": "2026-08-01",
  "to": "2026-08-31",
  "sections": [
    "INCIDENT_STATISTICS",
    "PATROL_COVERAGE",
    "CONFLICT_SUMMARY"
  ],
  "status": "GENERATED",
  "generatedBy": "usr-manager-1",
  "generatedAt": "2026-10-08T06:41:00Z",
  "downloadUrl": "/api/v1/reports/77777777-7777-4777-8777-777777777777/download",
  "snapshot": {
    "parkId": "park-yala",
    "from": "2026-08-01",
    "to": "2026-08-31",
    "dataAvailable": true,
    "totalIncidents": 58,
    "incidentTypeCounts": [
      {
        "type": "SNARE",
        "count": 24
      },
      {
        "type": "ANIMAL_CARCASS",
        "count": 10
      }
    ],
    "dailyIncidentCounts": [
      {
        "date": "2026-08-01",
        "count": 2
      },
      {
        "date": "2026-08-02",
        "count": 2
      }
    ],
    "areaCounts": [
      {
        "areaId": "area-b1",
        "areaName": "Block 1",
        "incidentCount": 20,
        "potentialHotspot": true
      },
      {
        "areaId": "area-north",
        "areaName": "Northern boundary",
        "incidentCount": 38,
        "potentialHotspot": true
      }
    ],
    "patrolCoverage": {
      "basis": "ASSIGNED_ROUTES_COMPLETED",
      "assignedRouteCount": 10,
      "completedRouteCount": 7,
      "coveragePercent": 70
    },
    "communityReportCount": 18,
    "resolvedAlertCount": 12,
    "generatedAt": "2026-10-08T06:41:00Z"
  }
}
```

Error statuses: 400, 401, 403, 404, 500, 503. See the common error shape below.

### GET `/reports/{id}/download`

Download report as PDF.

Access: PARK_MANAGER.

Generate a small PDF from the stored snapshot. Return Content-Disposition attachment; retain snapshot after download failure. First PDF uses summary tables; maps are a later enhancement.

Parameters:

| Name | In | Required | Type | Example |
| --- | --- | --- | --- | --- |
| `id` | path | Yes | uuid | `77777777-7777-4777-8777-777777777777` |

Request body: none.

Response: **200**, `application/pdf`.

```text
<PDF bytes>
```

Error statuses: 400, 401, 403, 404, 500, 503. See the common error shape below.


## Community

### PUT `/community-reports/{id}`

Submit community wildlife report.

Access: COMMUNITY_MEMBER.

Member has authorized park; server sets reporter and SUBMITTED. Require cropDetails for CROP_DAMAGE. Location coordinates may be omitted when only village/landmark is known. Any photo must be owned COMMUNITY_REPORT media in the same park. No SMS ingestion or automatic alert creation in this operation.

Parameters:

| Name | In | Required | Type | Example |
| --- | --- | --- | --- | --- |
| `id` | path | Yes | uuid | `88888888-8888-4888-8888-888888888888` |

Request: `application/json`; schema `CommunityRequest`.

```json
{
  "parkId": "park-yala",
  "areaId": "area-north",
  "type": "CROP_DAMAGE",
  "species": "Elephant",
  "village": "Village boundary Area B",
  "occurredAt": "2026-10-08T13:00:00Z",
  "description": "Elephants damaged the paddy field near the village.",
  "cropDetails": "Paddy; damage near the northern edge of the field."
}
```

Response: **201**, `application/json`.

```json
{
  "id": "88888888-8888-4888-8888-888888888888",
  "parkId": "park-yala",
  "areaId": "area-north",
  "type": "CROP_DAMAGE",
  "species": "Elephant",
  "village": "Village boundary Area B",
  "occurredAt": "2026-10-08T13:00:00Z",
  "description": "Elephants damaged the paddy field near the village.",
  "cropDetails": "Paddy; damage near the northern edge of the field.",
  "reportedBy": "usr-community-1",
  "status": "SUBMITTED",
  "createdAt": "2026-10-08T13:05:00Z"
}
```

First creation returns a `Location` header pointing to the resource; identical replay returns the same object with 200.

Error statuses: 400, 401, 403, 404, 409, 500, 503. See the common error shape below.

### GET `/community-reports`

List community reports.

Access: COMMUNITY_MEMBER, PARK_MANAGER, LIAISON_OFFICER.

Member sees own records only; manager/liaison sees their parks. Dates filter occurredAt, both or neither, maximum 92 days.

Parameters:

| Name | In | Required | Type | Example |
| --- | --- | --- | --- | --- |
| `parkId` | query | No | string | `park-yala` |
| `from` | query | No | date | `2026-08-01` |
| `to` | query | No | date | `2026-08-31` |
| `type` | query | No | WILDLIFE_SIGHTING / CROP_DAMAGE | `CROP_DAMAGE` |
| `areaId` | query | No | string | `area-north` |
| `page` | query | No | integer | `0` |
| `size` | query | No | integer | `20` |

Request body: none.

Response: **200**, `application/json`.

```json
{
  "items": [
    {
      "id": "88888888-8888-4888-8888-888888888888",
      "parkId": "park-yala",
      "areaId": "area-north",
      "type": "CROP_DAMAGE",
      "species": "Elephant",
      "village": "Village boundary Area B",
      "occurredAt": "2026-10-08T13:00:00Z",
      "description": "Elephants damaged the paddy field near the village.",
      "cropDetails": "Paddy; damage near the northern edge of the field.",
      "reportedBy": "usr-community-1",
      "status": "SUBMITTED",
      "createdAt": "2026-10-08T13:05:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 1
}
```

Error statuses: 400, 401, 403, 404, 500, 503. See the common error shape below.

### GET `/community-reports/{id}`

Get community report.

Access: COMMUNITY_MEMBER, PARK_MANAGER, LIAISON_OFFICER.

Owner member or authorized park manager/liaison only.

Parameters:

| Name | In | Required | Type | Example |
| --- | --- | --- | --- | --- |
| `id` | path | Yes | uuid | `88888888-8888-4888-8888-888888888888` |

Request body: none.

Response: **200**, `application/json`.

```json
{
  "id": "88888888-8888-4888-8888-888888888888",
  "parkId": "park-yala",
  "areaId": "area-north",
  "type": "CROP_DAMAGE",
  "species": "Elephant",
  "village": "Village boundary Area B",
  "occurredAt": "2026-10-08T13:00:00Z",
  "description": "Elephants damaged the paddy field near the village.",
  "cropDetails": "Paddy; damage near the northern edge of the field.",
  "reportedBy": "usr-community-1",
  "status": "SUBMITTED",
  "createdAt": "2026-10-08T13:05:00Z"
}
```

Error statuses: 400, 401, 403, 404, 500, 503. See the common error shape below.


## Cameras

### GET `/camera-traps`

List camera traps.

Access: PARK_MANAGER, RESEARCHER.

Seeded camera-trap metadata in authorized parks.

Parameters:

| Name | In | Required | Type | Example |
| --- | --- | --- | --- | --- |
| `parkId` | query | Yes | string | `park-yala` |
| `page` | query | No | integer | `0` |
| `size` | query | No | integer | `20` |

Request body: none.

Response: **200**, `application/json`.

```json
{
  "items": [
    {
      "id": "CT-003",
      "parkId": "park-yala",
      "areaId": "area-b1",
      "name": "Wildlife trail camera",
      "location": {
        "latitude": 6.35,
        "longitude": 81.5,
        "source": "GPS",
        "accuracyMeters": 8
      }
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 1
}
```

Error statuses: 400, 401, 403, 404, 500, 503. See the common error shape below.

### PUT `/camera-trap-images/{id}`

Submit manually uploaded camera image.

Access: PARK_MANAGER, RESEARCHER.

Camera determines park/area; caller must be authorized. First upload camera media with PUT /media/cccccccc-cccc-4ccc-8ccc-cccccccccccc and multipart category=CAMERA_TRAP, parkId=park-yala as the researcher/manager. Media must belong to caller, share park and have CAMERA_TRAP category. Store capture time separately from upload time. Server sets PENDING_REVIEW. Sensor ingestion is deferred.

Parameters:

| Name | In | Required | Type | Example |
| --- | --- | --- | --- | --- |
| `id` | path | Yes | uuid | `99999999-9999-4999-8999-999999999999` |

Request: `application/json`; schema `CameraImageRequest`.

```json
{
  "cameraTrapId": "CT-003",
  "mediaId": "cccccccc-cccc-4ccc-8ccc-cccccccccccc",
  "capturedAt": "2026-10-08T00:50:00Z"
}
```

Response: **201**, `application/json`.

```json
{
  "id": "99999999-9999-4999-8999-999999999999",
  "cameraTrapId": "CT-003",
  "mediaId": "cccccccc-cccc-4ccc-8ccc-cccccccccccc",
  "capturedAt": "2026-10-08T00:50:00Z",
  "parkId": "park-yala",
  "status": "PENDING_REVIEW",
  "uploadedBy": "usr-researcher-1",
  "createdAt": "2026-10-08T06:41:00Z"
}
```

First creation returns a `Location` header pointing to the resource; identical replay returns the same object with 200.

Error statuses: 400, 401, 403, 404, 409, 500, 503. See the common error shape below.

### GET `/camera-trap-images`

List camera images.

Access: PARK_MANAGER, RESEARCHER.

Authorized-park images, newest capture first. Image bytes downloaded separately.

Parameters:

| Name | In | Required | Type | Example |
| --- | --- | --- | --- | --- |
| `parkId` | query | Yes | string | `park-yala` |
| `cameraTrapId` | query | No | string | `CT-003` |
| `status` | query | No | PENDING_REVIEW / REVIEWED | `PENDING_REVIEW` |
| `page` | query | No | integer | `0` |
| `size` | query | No | integer | `20` |

Request body: none.

Response: **200**, `application/json`.

```json
{
  "items": [
    {
      "id": "99999999-9999-4999-8999-999999999999",
      "cameraTrapId": "CT-003",
      "mediaId": "cccccccc-cccc-4ccc-8ccc-cccccccccccc",
      "capturedAt": "2026-10-08T00:50:00Z",
      "parkId": "park-yala",
      "status": "PENDING_REVIEW",
      "uploadedBy": "usr-researcher-1",
      "createdAt": "2026-10-08T06:41:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 1
}
```

Error statuses: 400, 401, 403, 404, 500, 503. See the common error shape below.

### GET `/camera-trap-images/{id}`

Inspect camera image metadata.

Access: PARK_MANAGER, RESEARCHER.

Authorized park scope; review is absent before review.

Parameters:

| Name | In | Required | Type | Example |
| --- | --- | --- | --- | --- |
| `id` | path | Yes | uuid | `99999999-9999-4999-8999-999999999999` |

Request body: none.

Response: **200**, `application/json`.

```json
{
  "id": "99999999-9999-4999-8999-999999999999",
  "cameraTrapId": "CT-003",
  "mediaId": "cccccccc-cccc-4ccc-8ccc-cccccccccccc",
  "capturedAt": "2026-10-08T00:50:00Z",
  "parkId": "park-yala",
  "status": "PENDING_REVIEW",
  "uploadedBy": "usr-researcher-1",
  "createdAt": "2026-10-08T06:41:00Z"
}
```

Error statuses: 400, 401, 403, 404, 500, 503. See the common error shape below.

### PUT `/camera-trap-images/{id}/review`

Save species and verification flag.

Access: PARK_MANAGER, RESEARCHER.

Atomic first review stores reviewer/time and sets REVIEWED. Species may be Unknown. possiblePoacher is a requires-verification flag, not a proven identity. Same reviewer/payload retry returns 200; another or different review is 409. No automated species/confidence prediction.

Parameters:

| Name | In | Required | Type | Example |
| --- | --- | --- | --- | --- |
| `id` | path | Yes | uuid | `99999999-9999-4999-8999-999999999999` |

Request: `application/json`; schema `CameraReviewRequest`.

```json
{
  "species": "Asian elephant",
  "possiblePoacher": false,
  "notes": "One elephant visible near the trail."
}
```

Response: **200**, `application/json`.

```json
{
  "id": "99999999-9999-4999-8999-999999999999",
  "cameraTrapId": "CT-003",
  "mediaId": "cccccccc-cccc-4ccc-8ccc-cccccccccccc",
  "capturedAt": "2026-10-08T00:50:00Z",
  "parkId": "park-yala",
  "status": "REVIEWED",
  "uploadedBy": "usr-researcher-1",
  "createdAt": "2026-10-08T06:41:00Z",
  "review": {
    "species": "Asian elephant",
    "possiblePoacher": false,
    "notes": "One elephant visible near the trail.",
    "reviewedBy": "usr-researcher-1",
    "reviewedAt": "2026-10-08T06:45:00Z"
  }
}
```

Error statuses: 400, 401, 403, 404, 409, 500, 503. See the common error shape below.

## Common error shape

```json
{
  "timestamp": "2026-10-08T06:41:00Z",
  "status": 400,
  "code": "VALIDATION_FAILED",
  "message": "Check the highlighted fields.",
  "path": "/api/v1/incidents/44444444-4444-4444-8444-444444444444",
  "fieldErrors": {
    "description": "must not be blank"
  }
}
```

Important conflict codes: `ID_REUSED_WITH_DIFFERENT_DATA`, `ASSIGNMENT_ALREADY_USED`, `ALERT_ALREADY_ASSIGNED`, `INVALID_ALERT_STATE`, `REVIEW_ALREADY_EXISTS`. A field that references another resource must match its owner, category and park as described by the endpoint. A 201/200 must be returned only after persistence succeeds.

## Request validation checklist

| Schema | Required fields |
| --- | --- | --- |
| `LoginRequest` | `email`, `password` |
| `AssignmentRequest` | `routeId`, `rangerId`, `scheduledStartAt`, `scheduledEndAt` |
| `PatrolRequest` | `assignmentId`, `startedAt`, `endedAt`, `trackPoints`, `waypoints`, `observations` |
| `MediaUpload` | `file`, `parkId`, `category` |
| `IncidentRequest` | `parkId`, `areaId`, `type`, `detectedAt`, `location`, `description`, `photoId` |
| `AlertSetupRequest` | `parkId`, `areaId`, `animal`, `collarId`, `riskLevel`, `location`, `locationUpdatedAt`, `detectedAt` |
| `DeclineRequest` | `reason` |
| `SupportRequest` | `requestId`, `reason` |
| `AlertResponseRequest` | `actionTaken`, `result` |
| `ReportRequest` | `parkId`, `reportType`, `from`, `to`, `sections` |
| `CommunityRequest` | `parkId`, `areaId`, `type`, `village`, `occurredAt`, `description` |
| `CameraImageRequest` | `cameraTrapId`, `mediaId`, `capturedAt` |
| `CameraReviewRequest` | `species`, `possiblePoacher` |

Cross-field rules: end after start; observed patrol points/waypoints/notes within the patrol time; paired valid date filters; known area within park; active ranger within route park; correct media owner/category/park; cropDetails required for CROP_DAMAGE; assigned officer for support/resolution; bounded history/track arrays. Full field lengths, ranges and enum values are in `openapi.json`.

## Integration choices for the deadline

No dedicated endpoints for each wizard step, GPS fix, draft save, sync batch, logout, public account registration, sensor ingestion, SMS or AI prediction in this milestone. Those are local UI operations or identified later integrations. The backend plan explains these scope choices and their limits.
