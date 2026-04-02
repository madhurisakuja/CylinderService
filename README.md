# Cylinder Tracker

Production-grade gas cylinder tracking system for dealership operations.

## Tech stack
- Java 21 · Spring Boot 3.3 · Thymeleaf · Spring Security + Google OAuth2
- PostgreSQL (Supabase / Neon / Render) · JPA/Hibernate · SuperCSV

## Package structure
```
com.cylindertrack.app
├── CylinderTrackerApplication.java   # Entry point
├── config/
│   ├── ApplicationConfig.java        # MVC / static resources
│   └── SecurityConfig.java           # OAuth2 + email whitelist
├── controller/
│   └── MainController.java           # All routes
├── exception/
│   └── GlobalExceptionHandler.java   # Error handling
└── model/
    ├── MainCylinderEntry.java         # Cylinder movement entity
    ├── PartyNames.java                # Customer/party entity
    ├── NewCylinderFService.java       # Cylinder repository
    ├── PartyNamesRepository.java      # Party repository
    ├── CylinderTypeF.java             # Gas type enum
    └── CylinderStatus.java            # FULL/EMPTY enum
```

## Environment variables (set in Railway / Render)

| Variable | Description |
|---|---|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | DB password |
| `GOOGLE_CLIENT_ID` | From Google Cloud Console |
| `GOOGLE_CLIENT_SECRET` | From Google Cloud Console |
| `ALLOWED_EMAILS` | Comma-separated allowed Google emails |
| `PORT` | Auto-set by host (defaults to 8080) |

## Local development

1. Create `src/main/resources/application-local.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/cylindertrack
spring.datasource.username=youruser
spring.datasource.password=yourpassword
spring.security.oauth2.client.registration.google.client-id=YOUR_ID
spring.security.oauth2.client.registration.google.client-secret=YOUR_SECRET
app.allowed-emails=you@gmail.com
```

2. Run: `mvn spring-boot:run -Dspring-boot.run.profiles=local`

## Build & deploy

```bash
mvn clean package -DskipTests
java -jar target/cylinder-tracker-1.0.0.jar
```

## Run tests

```bash
mvn test
```

Tests use H2 in-memory database — no external DB required.

## Google OAuth2 setup

1. Go to https://console.cloud.google.com → APIs & Services → Credentials
2. Create OAuth 2.0 Client ID (Web application)
3. Add authorised redirect URI:
   `https://your-domain.up.railway.app/login/oauth2/code/google`
4. Copy Client ID and Secret to env vars

## Routes

| Route | Method | Description |
|---|---|---|
| `/` or `/newhome` | GET | Home |
| `/newCylinderEntryF` | GET/POST | Log cylinder movement |
| `/CylinderHistoryF` | GET/POST | Search cylinder history |
| `/searchResultF` | GET | View search results |
| `/notInRotation` | GET | Cylinders idle 15+ days |
| `/exportF?cylinderNo=X` | GET | Download history as CSV |
| `/deleteCylinderEntryF` | GET/POST | Delete an entry |
| `/newPartyEntryF` | GET/POST | Register a customer |
| `/logout` | POST | Sign out |
