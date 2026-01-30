# Configuration Reference

Complete configuration guide for nullInvoice including environment variables, timezone setup, and advanced settings.

## Environment Variables

An example configuration file is provided at `.env.example`. Copy this to `.env` and adjust values for your environment.

### Required Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `TZ` | System timezone | `Europe/Sofia`, `America/New_York`, `UTC` |
| `DB_HOST` | Database host | `localhost`, `mariadb` |
| `DB_USER` | Database username | `nullinvoice` |
| `DB_PASSWORD` | Database password | `your_secure_password` |
| `DB_NAME` | Database name | `nullinvoice` |
| `DB_PARAMS` | JDBC connection parameters (must include `serverTimezone`) | `?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Europe/Sofia` |

### Optional Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `APP_PORT` | 8080 | Application port |
| `DB_PORT` | 3306 | Database port |

## Timezone Configuration

**⚠️ CRITICAL:** You MUST set the timezone in TWO places:

1. **`TZ`** environment variable - sets the system/application timezone
2. **`serverTimezone`** parameter in `DB_PARAMS` - sets the database connection timezone

**Both values MUST match your database server's timezone** to ensure date/time values are correctly interpreted and stored.

### Example Configuration

```bash
TZ=Europe/Sofia
DB_PARAMS=?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Europe/Sofia
```

### Common Timezone Values

- `UTC` - Coordinated Universal Time
- `Europe/London` - UK time
- `Europe/Sofia` - Bulgaria time
- `America/New_York` - US Eastern time
- `America/Los_Angeles` - US Pacific time
- `Asia/Tokyo` - Japan time

See [full list of timezones](https://en.wikipedia.org/wiki/List_of_tz_database_time_zones).

**Warning:** Mismatched timezones will cause incorrect invoice dates and timestamps.

## Session Timeout

By default, UI sessions expire after **30 minutes of inactivity** (Spring Boot/Tomcat default). Users will be automatically logged out and redirected to the login page.

To customize the session timeout, add the following to `nullInvoice/src/main/resources/application.yml`:

```yaml
server:
  servlet:
    session:
      timeout: 60m  # Options: 15m, 30m, 1h, 2h, etc.
```

### Common Timeout Values

- `15m` - 15 minutes (stricter security)
- `30m` - 30 minutes (default)
- `1h` - 1 hour (convenience for active users)
- `8h` - 8 hours (extended for long sessions)

## Security & Best Practices

### Authentication Architecture

- **UI Access:** Session-based authentication with form login
- **API Access:** Stateless Bearer token authentication (API keys)
- **Passwords:** BCrypt hashed with 10 rounds
- **API Keys:** UUID format, BCrypt hashed, shown once on generation

### Production Deployment Checklist

- ✅ Enable HTTPS/TLS for all connections
- ✅ Use strong admin password
- ✅ Generate separate API keys per application/environment
- ✅ Deploy behind firewall or VPN
- ✅ Configure rate limiting at reverse proxy level
- ✅ Set up proper logging and monitoring
- ✅ Regularly review API key usage (last used timestamps)
- ✅ Revoke unused or compromised API keys immediately
- ✅ Keep API keys in environment variables, never in code
- ✅ Use secure secret management (HashiCorp Vault, AWS Secrets Manager, etc.)

### CSRF Protection

- Enabled for all UI form submissions
- Disabled for API endpoints (stateless Bearer token auth)

### First-Run Security

- Application is unusable until admin account is created via `/setup`
- Setup page is only accessible when no admin exists
- After setup, requires login for all functionality

## Database Configuration

### Connection Parameters

The `DB_PARAMS` variable accepts standard JDBC connection parameters:

```bash
DB_PARAMS=?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Europe/Sofia
```

Common parameters:
- `useSSL=false` - Disable SSL (enable in production)
- `allowPublicKeyRetrieval=true` - Allow MySQL public key retrieval
- `serverTimezone=Europe/Sofia` - Database timezone (REQUIRED)
- `characterEncoding=utf8` - Character encoding
- `autoReconnect=true` - Automatic reconnection

### Connection Pool

Spring Boot uses HikariCP for connection pooling. Default settings are optimized for most use cases. To customize, add to `application.yml`:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
```

## Application Properties

The main configuration file is `nullInvoice/src/main/resources/application.yml`.

### Current Default Configuration

```yaml
server:
  port: ${APP_PORT:8080}

spring:
  datasource:
    url: jdbc:mariadb://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:nullinvoice}${DB_PARAMS:?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Europe/Sofia}
    username: ${DB_USER:nullinvoice}
    password: ${DB_PASSWORD:}
  flyway:
    baseline-on-migrate: true
    baseline-version: 1
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: none
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MariaDBDialect

springdoc:
  api-docs:
    path: /openapi
  swagger-ui:
    path: /swagger
```

### Customization

You can override these settings by:

1. **Environment variables** (recommended for Docker)
2. **Custom application.yml** mounted as volume
3. **System properties** via JVM args

Example with custom config file:

```yaml
# docker-compose.yml
volumes:
  - ./custom-application.yml:/app/config/application.yml:ro
```

## Logging

Default logging level is INFO. To customize, add to `application.yml`:

```yaml
logging:
  level:
    root: INFO
    com.nullinvoice: DEBUG
    org.springframework: WARN
```

Or set via environment variable:

```bash
LOGGING_LEVEL_COM_NULLINVOICE=DEBUG
```

## Health Checks

Health check endpoint: `GET /api/v1/health`

Returns `200 OK` when the application is healthy (no authentication required).

Example Docker health check:

```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8080/api/v1/health"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 40s
```
