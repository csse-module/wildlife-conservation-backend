# Deploy the backend on Render

The Dockerfile builds the existing Maven application with Java 17 and runs the packaged Spring Boot JAR. Render supplies `PORT`; local runs still default to port 8080. The deployment uses the existing public registration-parks endpoint for health checks. No additional API endpoints or application logic are required.

## 1. Verify and push

From the backend directory:

```powershell
# Set JAVA_HOME to the JDK directory, not its bin directory.
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
.\mvnw.cmd clean verify
```

Commit and push the deployment files to the branch you want Render to deploy. Local `.env`, uploads, and logs are excluded from the Docker build context. Secrets are supplied through Render's environment settings.

## 2. Set up MongoDB Atlas

Use an existing Atlas cluster or create one, then create a database user with read/write access to the application database. Copy the application's connection string and include the database name:

```text
mongodb+srv://USERNAME:PASSWORD@CLUSTER/wildlife_conservation?retryWrites=true&w=majority
```

Replace the placeholders and URL-encode any reserved characters in the username/password. An Atlas connection string must point to a database reachable from Render; `localhost` would refer to the Render container.

For Atlas setup, see [Connect to an Atlas cluster](https://www.mongodb.com/docs/atlas/connect-to-database-deployment/).

## 3. Create the Render service

In the [Render dashboard](https://dashboard.render.com), choose **New > Blueprint**, connect `csse-module/wildlife-conservation-backend`, and select the branch containing `render.yaml`. Review the free web service and supply `MONGODB_URI` and `JWT_SECRET` when prompted.

Alternatively, choose **New > Web Service** and use these settings:

| Setting | Value |
| --- | --- |
| Language | Docker |
| Root Directory | Leave blank; this repository contains the backend at its root |
| Dockerfile Path | `./Dockerfile` |
| Docker Build Context | `.` |
| Docker Command | Leave blank; use the image entrypoint |
| Health Check Path | `/api/v1/auth/registration-parks` |
| Instance Type | Free for initial testing |

Render builds the image from the Dockerfile; no separate build or start command is needed. See [Docker on Render](https://render.com/docs/docker).

## 4. Configure environment variables and network access

| Variable | Value |
| --- | --- |
| `MONGODB_URI` | Required: complete Atlas connection string with `/wildlife_conservation` |
| `JWT_SECRET` | Required: stable Base64 encoding of at least 32 random bytes |
| `APP_LOG_LEVEL` | `INFO` is configured by the Blueprint; optional for manual setup |
| `COMMUNITY_REGISTRATION_PARK_IDS` | Optional: comma-separated existing park IDs; defaults to `park-yala` |
| `CORS_ALLOWED_ORIGINS` | For Flutter web: exact comma-separated frontend origins, without paths or trailing slashes; native Flutter does not need this |
| `MEDIA_STORAGE_DIR` | For a persistent disk: `/var/data/uploads`, with the disk mounted at `/var/data` |

Generate a JWT secret locally in PowerShell, then paste the output into Render:

```powershell
$jwtBytes = New-Object byte[] 32
$jwtRandom = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtRandom.GetBytes($jwtBytes)
$jwtRandom.Dispose()
[Convert]::ToBase64String($jwtBytes)
```

Keep the same secret across deployments to preserve existing tokens. Demo seeding remains disabled by default. If using a new database, provision the initial park and manager through the existing trusted setup described in the backend README.

Open the Render service's **Connect > Outbound** tab. Add all listed outbound IP ranges to Atlas's network access list. Redeploy if the initial deployment failed before database access was allowed. See [Render outbound IP addresses](https://render.com/docs/outbound-ip-addresses).

## 5. Verify and connect Flutter

After deployment, open:

```text
https://YOUR-SERVICE.onrender.com/api/v1/auth/registration-parks
```

The existing endpoint should return a successful JSON response. An empty park list means no eligible park records are available in the connected database. The root URL `/` is not an application endpoint.

From the Flutter project directory:

```powershell
flutter run --dart-define=API_BASE_URL=https://YOUR-SERVICE.onrender.com/api/v1
```

To build an APK using the deployed API:

```powershell
flutter build apk --release --dart-define=API_BASE_URL=https://YOUR-SERVICE.onrender.com/api/v1
```

Include `/api/v1` and omit a trailing slash. Rebuild an existing APK to change its compiled API URL.

## Upload persistence and free service limits

The backend stores uploaded images as local files and their metadata in MongoDB. Render's free filesystem is ephemeral: uploads are lost on restarts, redeploys, and idle shutdowns even though the MongoDB metadata remains. To preserve uploads with the current application, select a paid web service, attach a persistent disk at `/var/data`, and set `MEDIA_STORAGE_DIR=/var/data/uploads`. Existing local uploads must be copied separately if you need to retain them.

The included Blueprint creates a free service without a disk. Free services sleep after 15 minutes without incoming traffic, and waking can take about a minute; allow the service to wake before testing the mobile app. See [persistent disks](https://render.com/docs/disks) and [free service limits](https://render.com/docs/free).
