# Case-study demonstration

Admin is the `PARK_MANAGER` role. Public registration creates only `COMMUNITY_MEMBER`. Staff accounts are created by a park manager; rangers, liaison officers and researchers use the normal login screen. Staff are restricted to assigned parks. Villagers need no account park assignment to report: they choose a configured park/area to route the incident, and can read only their own reports.

## Start the backend

Keep the existing MongoDB URI and stable JWT secret in the backend's ignored `.env`. Do not put them in Flutter. For an initial demonstration, enable the optional seed before starting from the backend directory:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
$env:SPRING_PROFILES_ACTIVE = 'dev'
$env:DEV_SEED_ENABLED = 'true'
$env:DEV_SEED_PASSWORD = 'choose-your-local-demo-password'
$env:COMMUNITY_REGISTRATION_PARK_IDS = 'park-yala'
$env:CORS_ALLOWED_ORIGINS = 'http://localhost:5173'
./mvnw.cmd spring-boot:run
```

The password must contain at least six characters. Seed creates missing records and preserves existing passwords. Disable the seed after initial setup. Existing installations can use their current accounts instead.

| Email | Role |
| --- | --- |
| manager@wildguard.local | PARK_MANAGER |
| ranger@wildguard.local | RANGER |
| liaison@wildguard.local | LIAISON_OFFICER |
| researcher@wildguard.local | RESEARCHER |
| community@wildguard.local | COMMUNITY_MEMBER |

These accounts use the supplied seed password only when first created. The demo park is `park-yala`, area `area-b1`, route `route-demo`, camera trap `CT-DEMO`.

The current local database contains `park-wilpatthu`; the ignored local `.env` now enables that existing ID for registration and allows the browser origin `http://localhost:5173`. The optional seed instructions above instead demonstrate a separate `park-yala` setup.

If public registration shows no parks, set `COMMUNITY_REGISTRATION_PARK_IDS` to existing park IDs and restart the backend. A manager can create a park in the app, then enable that ID in the server configuration. Give the manager, responding officer and researcher access to the same park as the villager.

## Start Flutter

From `wildlife-conservation-mobile`:

```powershell
flutter pub get
flutter run -d chrome --web-hostname localhost --web-port 5173 --dart-define=API_BASE_URL=http://localhost:8080/api/v1
```

For an Android emulator, use `--dart-define=API_BASE_URL=http://10.0.2.2:8080/api/v1`. For a phone, use the computer's reachable LAN IP with port 8080; connect both devices to the same network and allow the backend through the computer's firewall. The current computer LAN IP is `192.168.1.21`, which is the mobile default for this demonstration. The compile-time API URL includes `/api/v1` and has no trailing slash. No Supabase setup is needed for the new reporting flows.

## Demonstrate the connected flow

1. **Villager:** choose Register, select an enabled park, register and log in. Open **Report sighting or crop damage**, select an area, enter village/description and either species or crop details. GPS/manual coordinates and a photo are optional. Submit, then open **My reports**.
2. **Manager:** sign in through Admin Login. Choose the same park. **Incoming community reports** shows the submission, with new submissions first. Refresh manually or wait up to 30 seconds while the dashboard is open. Open the report to inspect details and any attached photo.
3. **Liaison officer or ranger:** normal login → **Community reports** → open the report → **Accept report** → **Record response and resolve**. Enter the action and outcome. Only the accepting officer can resolve it; concurrent claims cannot overwrite one another.
4. **Villager:** refresh the report. It now displays Responding or Resolved and the officer's outcome. Other villagers' reports are not visible.
5. **Manager/researcher:** open **Analytics & reports**, choose the park and a period of up to 92 days, inspect incident/community types, locations, daily trends and route completion, then **Generate report**. Open the saved report and **Download PDF**. Reports are saved snapshots; they retain their original figures after source data changes.
6. **Researcher/manager:** open **Camera trap images**, upload an image for a configured trap, open it, identify the species and optionally flag a possible poacher, then save the final manual review.
7. **Manager:** create a ranger/liaison/researcher using **Add staff**, selecting an assigned park. Staff must change the temporary password and sign in again. **Assign patrol** selects an existing route and a ranger in the same park.
8. **Manager/officer:** create a manual collar alert as manager, then accept/decline, request support or resolve it as an officer. Support is recorded in the API; no push notification delivery is claimed.

## Offline field reports

After a successful online login and park load, disconnect the device and submit a community or ranger incident report. It remains under **Pending sync**. Restore connectivity or tap Sync; the same report and attachment IDs are retried. It becomes visible to park operations only after delivery. The pending queue is separated by account. Local storage allows up to 20 queued reports and 4 MiB of encoded data; photos are limited to 2 MiB each by the form. Sign in again if the token expires before synchronization.

This queue covers community and incident reporting. Existing patrol tracking and its legacy local storage remain a separate feature.

## API changes

New operations:

- `GET /auth/registration-parks` — public enabled park catalog.
- `POST /community-reports/{id}/accept` — ranger/liaison claim.
- `PUT /community-reports/{id}/response` — accepting officer resolves.

Expanded access: researchers can use all analytics/report operations; rangers can read community reports in their parks. Community responses include status, assigned officer, action, result and timestamps. Analytics/report snapshots include `communityConflict` statistics. The specification documents all 44 operations, including the previously implemented park/route creation APIs.

Use [implemented-api.md](implemented-api.md), [implemented-openapi.json](implemented-openapi.json), and [the Postman collection](wildlife-api.postman_collection.json) for contracts and request examples.

## Validation and current scope

Backend tests mock database boundaries; they verify role restrictions, community ownership, atomic acceptance/resolution, retry behavior and researcher PDF access without altering the configured database. Flutter tests verify role menus, dashboard refresh, submission payloads, offline account separation, report generation, assignment and narrow layouts. Run `./mvnw.cmd verify` and `flutter test` from the respective projects.

The app supports manual alert creation and camera image upload/review. Live collar/camera ingestion, SMS gateways, automatic species recognition and push delivery remain external integrations. iOS PDF saving and permissions are configured, but an iOS build requires macOS/Xcode.

## Verification results (9 October 2026)

- Backend: 282 tests pass; Maven packaging and PMD checks pass.
- Flutter: 11 connected-flow/layout tests pass; the new feature and onboarding code analyzes with no issues.
- Android debug APK and browser builds succeed. The APK uses the current LAN backend address `192.168.1.21:8080`; the browser build uses `localhost:8080`.
- Live MongoDB startup and public registration catalog were verified. The existing `park-wilpatthu` is returned, and unauthenticated analytics returns HTTP 401. No test accounts or reports were added to the live database.
- Full-project Flutter analysis still reports 48 warning/info items in legacy code, with no compile errors. Physical camera/GPS/document-picker behavior needs a device smoke test; iOS was not built on Windows.

A reusable Windows launcher workaround is available in `wildlife-conservation-mobile/tools/flutter-local.ps1`; see the mobile README. The global Flutter SDK was not modified. Restart your normal backend on port 8080 before launching the updated app.
