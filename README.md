# <img src="nullInvoice/src/main/resources/static/images/logo.svg" alt="logo" width="24"> nullInvoice

**nullInvoice** is a Spring Boot microservice for **automated invoice generation and management** with **fully customizable HTML templates**, designed for integration with webstores and SaaS platforms.

> **📖 Extended Documentation:** For detailed guides on deployment, API reference, templates, configuration, and development, see the [Extended Documentation](docs/extended/) section below.

## Overview

Businesses use nullInvoice to handle invoice generation after sales are completed. Suppliers are configured once through the web UI, then your application calls the REST API to automatically generate compliant invoices on-demand.

**How it works:**

1. **Setup**: Configure suppliers in the UI with company details, locale, currency, tax rates, custom branding, and invoice templates
2. **Authentication**: Generate API keys from the Admin dashboard for secure REST API access
3. **Integration**: Your webstore/SaaS makes authenticated API calls to `/api/v1/invoices/generate` using the supplier ID
4. **Generation**: Invoices are created from fully customizable HTML templates and returned as JSON or PDF with metadata headers
5. **Delivery**: Your application receives the invoice and can forward it to customers or store it for records

### Typical Integration Flow

```
┌─────────────────┐
│   Customer      │
│   completes     │
│   purchase      │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────────────┐
│  Your Webstore/SaaS Platform            │
│  ─────────────────────────────────────  │
│  1. Process payment                     │
│  2. Issue digital receipt (required)    │
│  3. Customer requests invoice? ──────┐  │
└──────────────────────────────────────┼──┘
                                       │
                                       │ API Call
                                       ▼
                        ┌──────────────────────────────┐
                        │  nullInvoice Service         │
                        │  ──────────────────────────  │
                        │  POST /api/v1/invoices/      │
                        │       generate               │
                        │                              │
                        │  - Validates supplier ID     │
                        │  - Applies custom template   │
                        │  - Stores HTML snapshot      │
                        │  - Generates PDF             │
                        │  - Returns invoice           │
                        └──────────────┬───────────────┘
                                       │
                                       │ Response (JSON or PDF)
                                       ▼
┌──────────────────────────────────────────┐
│  Your Webstore/SaaS Platform             │
│  ──────────────────────────────────────  │
│  - Receives JSON metadata OR PDF file    │
│  - Stores invoice number for records     │
│  - Forwards PDF to customer via email    │
└──────────────────────────────────────────┘
```

## Key Features

- **Fully Customizable Templates** - HTML templates with inline CSS and 30+ placeholders
- **Document Immutability** - HTML snapshots prevent retroactive changes (financial compliance)
- **Multi-tenant Ready** - Multiple suppliers with independent settings
- **Flexible Delivery** - Return JSON metadata or PDF directly
- **OpenAPI Documentation** - Interactive API docs at `/swagger`

## Quick Start

### Prerequisites

**For Docker deployment (recommended):**
- Docker
- Docker Compose

**For local development:**
- Java 21 (JDK - Eclipse Temurin or OpenJDK)
- Maven 3.9+
- MariaDB 10.5+ (or MySQL 8.0+)

### Docker Deployment

Pre-built images are available on [Docker Hub](https://hub.docker.com/r/nullata/nullinvoice).

```bash
docker compose up -d
```

See [Deployment Guide](docs/extended/DEPLOYMENT.md) for complete setup instructions.

### Configuration

Essential environment variables:

| Variable | Required | Description |
|----------|----------|-------------|
| `TZ` | **Yes** | System timezone (e.g., `Europe/Sofia`) |
| `DB_HOST` | **Yes** | Database host |
| `DB_USER` | **Yes** | Database username |
| `DB_PASSWORD` | **Yes** | Database password |
| `DB_NAME` | **Yes** | Database name |
| `DB_PARAMS` | **Yes** | JDBC parameters including `serverTimezone` |

**Example:**
```bash
TZ=Europe/Sofia
DB_HOST=localhost
DB_USER=nullinvoice
DB_PASSWORD=your_secure_password
DB_NAME=nullinvoice
DB_PARAMS=?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Europe/Sofia
```

See [Configuration Reference](docs/extended/CONFIGURATION.md) for all options.

### First Run

On first access to `http://localhost:8080`:

1. You'll be redirected to `/setup` to create the admin account
2. After setup, login at `/login`
3. Configure your first supplier in the UI
4. Generate an API key from Admin > API Keys
5. Start generating invoices via API!

## API Quick Example

Generate an invoice and get a PDF:

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

See [API Reference](docs/extended/API.md) for complete documentation.

## Important Notices

### ⚠️ Security Notice

nullInvoice includes **built-in authentication** (session-based UI login + API key authentication for REST endpoints). The application is designed for **internal/private network deployment**.

**Recommended deployment:**
- Behind a firewall or VPN
- Within a private network accessible only to trusted applications
- With HTTPS/TLS enabled for all connections
- Behind a reverse proxy with rate limiting configured

See [Configuration > Security](docs/extended/CONFIGURATION.md#security--best-practices) for production checklist.

### ⚠️ Not an Accounting Instrument

nullInvoice is an **invoice generation pipeline** designed to create, store, and deliver invoice documents. It does not:

- Track payments or payment status beyond basic "unpaid/issued" flags
- Manage accounts receivable or payable
- Generate financial reports or balance sheets
- Integrate with accounting systems (ledgers, journals, etc.)
- Handle bookkeeping, reconciliation, or tax filing

For comprehensive financial management, integrate nullInvoice with a dedicated accounting system.

## Extended Documentation

Detailed guides organized by topic:

- **[Deployment Guide](docs/extended/DEPLOYMENT.md)** - Database setup, Docker, local development, first-run configuration
- **[API Reference](docs/extended/API.md)** - Complete REST API documentation with examples
- **[Template Customization](docs/extended/TEMPLATES.md)** - HTML templates, placeholders, fonts, PDF rendering
- **[Configuration](docs/extended/CONFIGURATION.md)** - Environment variables, security, session management
- **[Development Guide](docs/extended/DEVELOPMENT.md)** - Contributing, building, testing, internationalization

## Stack

- Java 21, Spring Boot 3.5.3
- MariaDB + JPA
- Thymeleaf (UI)
- OpenHTMLToPDF (PDFBox)
- OpenAPI at `/openapi`, Swagger UI at `/swagger`

## Internationalization

**Supported UI Languages:** English, Bulgarian, German, Spanish, Italian, Russian

**README Translations:**
- [Bulgarian](docs/README-BG.md)
- [German](docs/README-DE.md)
- [Spanish](docs/README-ES.md)
- [Italian](docs/README-IT.md)
- [Russian](docs/README-RU.md)

**Invoice Templates:** Example templates available in [`templates/`](templates/) directory for all 6 languages

## License

This project is licensed under the **Elastic License 2.0** - see the [LICENSE](LICENSE) file for details.

Copyright 2026 nullata

## Third-party Licenses

- Font Awesome Free: `nullInvoice/src/main/resources/static/fontawesome-free-7.1.0-web/LICENSE.txt`
- DejaVu Fonts: `nullInvoice/src/main/resources/fonts/LICENSE.txt`
