<p align="center">
<img src="https://raw.githubusercontent.com/nullata/containers/refs/heads/main/images/logo.png" alt="Logo" width="96">
</p>

# nullInvoice - Automated Invoice Generation

**nullInvoice** is a Spring Boot microservice for automated invoice generation and management with fully customizable HTML templates, designed for integration with webstores and SaaS platforms.

Generate compliant invoices on-demand via REST API with PDF export, multi-tenant support, and document immutability for financial compliance.

## Quick Start with Docker Compose (Recommended)

Create a `docker-compose.yml`:

```yaml
version: '3.8'

services:
  nullinvoice:
    image: nullata/nullinvoice:latest
    container_name: nullinvoice
    ports:
      - "8080:8080"
    environment:
      - TZ=Europe/Sofia
      - DB_HOST=mariadb
      - DB_PORT=3306
      - DB_USER=nullinvoice
      - DB_PASSWORD=changeme
      - DB_NAME=nullinvoice
      - DB_PARAMS=?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Europe/Sofia
    depends_on:
      - mariadb
    restart: unless-stopped

  mariadb:
    image: mariadb:10.11
    container_name: nullinvoice-db
    environment:
      - MYSQL_ROOT_PASSWORD=rootpassword
      - MYSQL_DATABASE=nullinvoice
      - MYSQL_USER=nullinvoice
      - MYSQL_PASSWORD=changeme
      - TZ=Europe/Sofia
    volumes:
      - mariadb_data:/var/lib/mysql
    restart: unless-stopped

volumes:
  mariadb_data:
```

Start the stack:

```bash
docker compose up -d
```

Access the application at `http://localhost:8080`

## Alternative: Docker Run

If you have an existing MariaDB/MySQL instance:

```bash
docker run -d \
  --name nullinvoice \
  -p 8080:8080 \
  -e TZ=Europe/Sofia \
  -e DB_HOST=your-db-host \
  -e DB_PORT=3306 \
  -e DB_USER=nullinvoice \
  -e DB_PASSWORD=your_password \
  -e DB_NAME=nullinvoice \
  -e DB_PARAMS='?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Europe/Sofia' \
  --restart unless-stopped \
  nullata/nullinvoice:latest
```

## Database Setup

The application user needs these permissions (DELETE intentionally excluded for compliance):

```sql
CREATE DATABASE nullinvoice CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER 'nullinvoice'@'%' IDENTIFIED BY 'your_secure_password';
GRANT SELECT, INSERT, UPDATE ON nullinvoice.* TO 'nullinvoice'@'%';
GRANT CREATE, ALTER, INDEX, REFERENCES ON nullinvoice.* TO 'nullinvoice'@'%';
FLUSH PRIVILEGES;
```

The database schema is managed automatically by Flyway migrations on startup.

## Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `TZ` | **Yes** | - | System timezone (e.g., `Europe/Sofia`, `America/New_York`) |
| `APP_PORT` | No | 8080 | Application port |
| `DB_HOST` | **Yes** | localhost | Database host |
| `DB_PORT` | No | 3306 | Database port |
| `DB_USER` | **Yes** | nullinvoice | Database username |
| `DB_PASSWORD` | **Yes** | - | Database password |
| `DB_NAME` | **Yes** | nullinvoice | Database name |
| `DB_PARAMS` | **Yes** | - | JDBC connection parameters including `serverTimezone` |

**CRITICAL:** The `TZ` environment variable and `serverTimezone` in `DB_PARAMS` **must match** your database server timezone to ensure correct date/time handling.

## First Run Setup

On first access, you'll be redirected to `/setup`:

1. Navigate to `http://localhost:8080`
2. Create admin account with username, password, and optional password hint
3. After setup, login at `/login`
4. Access admin dashboard to generate API keys

## Session Timeout

By default, UI sessions expire after **30 minutes of inactivity**. To customize, you can mount a custom `application.yml`:

```yaml
server:
  servlet:
    session:
      timeout: 60m  # Options: 15m, 30m, 1h, 2h, etc.
```

Mount the file in your container:

```yaml
volumes:
  - ./custom-application.yml:/app/config/application.yml:ro
```

## Key Features

- **REST API** for invoice generation with JSON or PDF response
- **Customizable HTML templates** with 30+ placeholders
- **Document immutability** - HTML snapshots prevent retroactive changes
- **Multi-tenant ready** - multiple suppliers with independent settings
- **OpenAPI documentation** at `/swagger` (requires login)
- **Session + API key authentication** for UI and API access

## API Quick Example

Generate an invoice via API:

```bash
curl -X POST http://localhost:8080/api/v1/invoices/generate \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "response_type": "pdf",
    "supplier_id": 1,
    "client": {
      "name": "Client Co",
      "addressLine1": "123 Main St",
      "city": "Sofia",
      "country": "BG"
    },
    "items": [
      {"description": "Service", "quantity": 1, "unit_price": 1000, "tax_rate": 0.2}
    ]
  }' -o invoice.pdf
```

Generate API keys from Admin > API Keys after logging in.

## Security Notice

**⚠️ This application is designed for internal/private network deployment.**

**Recommended deployment:**
- Behind a firewall or VPN
- Within a private network accessible only to trusted applications
- With HTTPS/TLS enabled (use a reverse proxy like nginx or Traefik)
- With rate limiting configured at reverse proxy level

**Production checklist:**
- Use strong admin password
- Generate separate API keys per application/environment
- Keep API keys in environment variables, never in code
- Regularly rotate API keys
- Review API key usage timestamps

## Supported Languages

UI available in: English, Bulgarian, German, Spanish, Italian, Russian

Invoice templates can be created in any language with proper font support.

## Invoice Templates

Example invoice templates are available in 6 languages in the repository:

**[View Example Templates on GitHub](https://github.com/nullata/nullInvoice/tree/main/templates)**

Templates are fully customizable XHTML with inline CSS and support 30+ placeholders for supplier, client, and financial data. Upload custom templates via the web UI after setup.

## Documentation

- **Full Documentation:** [GitHub Repository](https://github.com/nullata/nullInvoice)
- **API Documentation:** Available at `/swagger` (requires login)
- **OpenAPI Spec:** Available at `/openapi`

## Volumes & Data Persistence

The application stores all data in the MariaDB database. Ensure you persist the database volume:

```yaml
volumes:
  - mariadb_data:/var/lib/mysql
```

No application-level volumes are required.

## Health Check

```bash
curl http://localhost:8080/api/v1/health
```

Returns `200 OK` when the application is healthy.

## Logs

View application logs:

```bash
docker logs nullinvoice
docker logs -f nullinvoice  # Follow logs
```

## Stack

- Java 21, Spring Boot 3.5.3
- MariaDB 10.5+ (MySQL 8.0+ compatible)
- OpenHTMLToPDF for PDF generation
- BCrypt password hashing

## License

This project is licensed under the **Elastic License 2.0**.

Copyright 2026 nullata

## Support

For issues, questions, or contributions, visit the [GitHub repository](https://github.com/nullata/nullInvoice).
