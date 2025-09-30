<div align="center">

# Secure File Statement Delivery

<img src="https://raw.githubusercontent.com/jodyswartz/public-project-data/refs/heads/main/secure-file-statement-delivery/assets/images/logo.png" alt="secure-file-statement-delivery" width="40%">

</div>

## Scenario
Production-grade service to upload monthly account statements (PDF) and provide secure, time-limited download links with audit logging.

### Who would probably use it:
Admins upload statements and generate time-limited download links.
End users download via those links—no login, just a tokenized URL that expires.


## How it works
- Upload: POST /admin/statements (multipart) saves the PDF to S3/MinIO and a Statement row in Postgres.
- Create link: POST /admin/statements/{id}/links makes a DownloadToken (with expiry + createdBy) and returns a short app URL like /download/{token}.
- Download: GET /download/{token} validates the token, logs a DownloadAudit (userId, IP, userAgent, time), then redirects to a presigned S3 URL so the browser fetches the file directly.
- Security: Admin endpoints require an API key header. Download links are short-lived and single-purpose. There’s also a simple rate-limit filter.

### Persistence:
- statements — what was uploaded (accountId, period, objectKey, checksum, size).
- download_tokens — tokens + expiry.
- download_audits — every download event.

### Under the hood: 
- Spring Boot
- Spring MVC
- Spring Security (API key)
- Hibernate/JPA 
- Flyway (DB migrations)
- Postgres
- S3/MinIO for storage
- Testcontainers for integration tests
- Actuator for health (/actuator/health).

## Quickstart
```bash
docker compose up --build
```
- App: http://localhost:8080
- MinIO Console: http://localhost:9001 (Username: minioadmin / Password: minioadmin)

## Check if everything is up & running:
```bash
curl -s http://localhost:8080/actuator/health | jq
```

## API
- `POST /admin/statements` (multipart) with `X-API-Key`
- `GET /admin/statements?accountId=&page=&size=` (with `X-API-Key`)
- `POST /admin/statements/{id}/links` → `{ url, expiresAt }`
- `GET /download/{token}` → 302 to pre-signed S3 URL
- `GET /health`

## Tests (Testcontainers)
```bash
mvn test
```
Runs Postgres + LocalStack S3 containers and a full happy-path test.

## Production profile (`prod`)
Use AWS S3 + optional SSE-KMS and external Postgres.
```bash
docker run -p 8080:8080   -e SPRING_PROFILES_ACTIVE=prod   -e JDBC_URL='jdbc:postgresql://<host>:5432/<db>'   -e DB_USER='<user>'   -e DB_PASSWORD='<pass>'   -e S3_BUCKET='<bucket>'   -e AWS_REGION='eu-north-1'   -e S3_SSE_ENABLED=true   -e S3_KMS_KEY_ID='arn:aws:kms:eu-north-1:<acct>:key/<uuid>'   -e ADMIN_API_KEY='<rotate-me>'   your-image:tag
```

## Notes
Some of the liberties:
- used minIO, this is to simulate S3 locally, but in production this would be swapped out of AWS's S3. 
- Focused on getting it to work locally, I have not setup the production flow in AWS.
- Running ths mvn test, tests the happy flow. 


### Manually test

#### 1.) Upload a pdf
Have a local file
```bash
curl -sS -X POST "http://localhost:8080/admin/statements" \
  -H "X-API-Key: test-key" \
  -F "file=@statement.pdf;type=application/pdf" \
  -F "accountId=ACC-1" \
  -F "period=2025-08" \
  -F "uploadedBy=cli" | tee upload.json | jq
```

Save the statement id:
```bash
STMT_ID=$(jq -r .id upload.json)
echo "$STMT_ID"

```


#### 2.) Create a time limited download link (admin only)
```bash
curl -sS -X POST "http://localhost:8080/admin/statements/$STMT_ID/links" \
  -H "X-API-Key: test-key" \
  -H "Content-Type: application/json" \
  -d '{"expiresMinutes":30,"createdBy":"qa"}' | tee link.json | jq
```
Save the relative url:
```bash
URL=$(jq -r .url link.json)
echo "$URL"
```

#### 3.) Hit the download link
Include a user id so the audit log captures who downloaded.
```bash
curl -i "http://localhost:8080$URL" \
  -H "X-User-Id: user-42"
```
Expected: HTTP/1.1 302 Found with a Location: header pointing to a presigned S3/MinIO URL.


### 4.) Follow the redirect & save the PDF
```bash
curl -L -H "X-User-Id: user-42" \
  -o downloaded.pdf "http://localhost:8080$URL"

ls -l downloaded.pdf
```


### Possible issues:
- 401/403 on admin endpoints → your X-API-Key doesn’t match the server’s admin.api.key.
- 404 on /download/{token} → token is invalid or expired.
- S3/MinIO errors → check bucket/endpoint/credentials env vars match what the app expects.
- DB/Flyway errors → ensure Postgres is reachable and migrations ran (app logs will say “Successfully applied X migration(s)”).