# File Path Management Backend (Spring Boot)

## Stack (restricted)
- Spring Boot 3 (Web, Data JPA)
- PostgreSQL
- CORS configuration

No Lombok, no Flyway, no Swagger/OpenAPI, no Bean Validation.

## Run locally
1. Ensure PostgreSQL is running and database `fpm_db` exists.
2. Update `src/main/resources/application.properties` with your DB credentials and allowed CORS origins.
   - JPA DDL is set to `update` for local development.
3. Build and run:
   ```bash
   mvn spring-boot:run
   ```

## API
- `GET /api/paths?search=&page=1&pageSize=10`
- `POST /api/paths`
- `PUT /api/paths/{id}`
- `GET /api/paths/{id}`
- `DELETE /api/paths/{id}`
- `POST /api/routing/run` (stubbed; integrate Microsoft Graph later)

## Entity
- PathConfig: `prefix`, `sourcePath`, `outputPath`, `status`, `createdAt`

## Next steps
- Implement Microsoft Graph integration to route files from `incoming/` to `reports/...` by prefix.
- Add background scheduling (Spring @Scheduled) if desired.
