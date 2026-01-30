# Deployment Guide

This guide covers all deployment options for nullInvoice, including Docker, local development, and database configuration.

## Docker Compose (Recommended)

**Official Docker Images:**
Pre-built images are available on [Docker Hub](https://hub.docker.com/r/nullata/nullinvoice).

Build without cache:

```bash
docker compose build --no-cache
```

Bring the stack up:

```bash
docker compose up -d
```

Bring the stack down:

```bash
docker compose down
```

## Database Setup

### Create Database

```sql
CREATE DATABASE nullinvoice CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Create Application User

**Create a dedicated application user with restricted permissions:**

```sql
CREATE USER 'nullinvoice'@'localhost' IDENTIFIED BY 'your_secure_password';

-- Grant permissions for normal operations and schema migrations
GRANT SELECT, INSERT, UPDATE ON nullinvoice.* TO 'nullinvoice'@'localhost';
GRANT CREATE, ALTER, INDEX, REFERENCES ON nullinvoice.* TO 'nullinvoice'@'localhost';

FLUSH PRIVILEGES;
```

**For remote access, adjust the host:**

```sql
CREATE USER 'nullinvoice'@'%' IDENTIFIED BY 'your_secure_password';
GRANT SELECT, INSERT, UPDATE ON nullinvoice.* TO 'nullinvoice'@'%';
GRANT CREATE, ALTER, INDEX, REFERENCES ON nullinvoice.* TO 'nullinvoice'@'%';
FLUSH PRIVILEGES;
```

### Why Restricted Permissions?

Invoices are **immutable financial documents** that should never be deleted once created. The application user is intentionally restricted from:

- `DELETE` - cannot delete records
- `DROP` - cannot drop tables or database
- `TRUNCATE` - cannot truncate tables
- `GRANT` - cannot grant permissions to others

This ensures data integrity and compliance with financial record-keeping requirements. The `UPDATE` permission is granted for status changes (e.g., marking invoices as paid) and soft deletes on party records.

### Schema Management

- Database schema is managed by **Flyway** migrations
- Initial schema: `nullInvoice/src/main/resources/db/migration/V1__initial_schema.sql`
- Schema is automatically created/updated on application startup
- Hibernate DDL mode is set to `none` (Flyway handles all schema changes)

## Local Development (Non-Docker)

### Prerequisites

- Java 21 (JDK - Eclipse Temurin or OpenJDK)
- Maven 3.9+
- MariaDB 10.5+ (or MySQL 8.0+)
- Tailwind CSS standalone binary (for building CSS - from [GitHub releases](https://github.com/tailwindlabs/tailwindcss/releases))

### Setup

1. Ensure you have a MariaDB instance running and create the database. See [Database Setup](#database-setup)

2. Build Tailwind CSS (required before first run):

   ```bash
   ./build-tailwind.sh
   ```

3. Build the project with Maven:

   ```bash
   cd nullInvoice
   mvn clean package
   ```

4. Run the application:

   ```bash
   java -jar target/nullinvoice-0.0.1-SNAPSHOT.jar
   ```

### Using NetBeans IDE

The project was built with NetBeans and can be opened as a Maven project with the Spring Boot plugin.

**Setting environment variables in NetBeans:**

Option 1 - Via IDE:

1. Right-click project >> Properties
2. Navigate to Actions >> Run
3. Set environment variables: `APP_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`

Option 2 - Edit directly:

- Modify `nbactions.xml` in the project root

## Getting Started

### First Run Setup

On first access, you'll be redirected to `/setup` to create the initial admin account:

1. Navigate to `http://localhost:8080` (or your configured URL)
2. You'll be automatically redirected to `/setup`
3. Create admin account with username, password, and optional password hint
4. After setup, you'll be redirected to login

**Login:**

- Access the login page at `/login`
- Use the admin credentials you created
- Password hint available via info icon (if configured)

**Admin Dashboard:**
After logging in, access the admin dashboard from the user dropdown menu:

- Change admin password
- Generate API keys for REST API access
- Revoke API keys
- View API key usage (last used timestamp)

### Setting Up Your First Supplier

**Prerequisites:**

- Complete first-run setup (admin account created)
- Login with admin credentials

Before you can generate invoices via the API, you need to configure at least one supplier through the web UI. Suppliers define the company details, locale, currency, tax rates, custom branding (via templates), and invoice numbering that will be used for invoice generation.

1. Login to the web UI at `http://localhost:8080` (or your configured port)
2. Navigate to Suppliers
3. Create a new supplier with company details
4. Configure locale, currency, and tax settings
5. Set invoice numbering preferences (prefix, padding)
6. Note the Supplier ID for API integration
7. Generate an API key from Admin > API Keys for REST API access

**For examples on setting up a supplier, take a look at `docs/example-images` - `en-us` for a US or non-EU example setup; and `eu-de` for an EU example setup.**

Once configured, you can setup an XHTML template and use the Supplier ID, shown in the edit supplier menu, to make API calls to `/api/v1/invoices/generate`.

## Emergency Admin Recovery

**To reset and create a new admin account (emergency recovery):**

1. Stop the application
2. Truncate the `users` and `api_keys` tables in the database
3. Restart the application
4. Complete the `/setup` flow again

## Security Recommendations

- Use a strong password for the admin account
- Set a password hint (optional but recommended)
- Generate separate API keys for different environments (dev, staging, prod)
- Revoke unused API keys
- Deploy behind firewall or VPN
- Enable HTTPS/TLS for all connections
- Configure rate limiting at reverse proxy level
- Keep API keys in environment variables, never in code
