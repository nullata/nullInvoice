# Development Guide

Guide for developers contributing to or customizing nullInvoice.

## Technology Stack

- Java 21, Spring Boot 3.5.3
- MariaDB + JPA
- Thymeleaf (UI)
- OpenHTMLToPDF (PDFBox)
- Tailwind CSS
- OpenAPI at `/openapi`, Swagger UI at `/swagger`

## Project Structure

```
nullInvoice/
├── src/main/java/              # Application code
├── src/main/resources/
│   ├── templates/              # UI templates (Thymeleaf)
│   ├── static/                 # JS/CSS assets
│   │   ├── css/
│   │   ├── js/
│   │   ├── images/
│   │   └── fontawesome-free-7.1.0-web/
│   ├── fonts/                  # DejaVu fonts for PDF
│   ├── db/migration/           # Flyway database migrations
│   ├── messages*.properties    # i18n translations
│   └── application.yml         # Main config
├── src/test/                   # Tests
└── target/                     # Build output

templates/                      # Example invoice templates (6 languages)
integration-tests/              # Load testing scripts
build-tailwind.sh              # Tailwind CSS build script
twbin/                         # Tailwind CSS standalone binary
docker-compose.yml             # Docker setup
```

## Frontend Development (Tailwind CSS)

The UI uses Tailwind CSS, which must be rebuilt when CSS changes are made.

### Building Tailwind CSS for Production

1. Download the Tailwind CSS standalone binary from [GitHub releases](https://github.com/tailwindlabs/tailwindcss/releases)
2. Place the binary in the `twbin/` directory (e.g., `twbin/tailwindcss-linux-x64`)
3. Run the build script:

   ```bash
   ./build-tailwind.sh
   ```

This rebuilds `nullInvoice/src/main/resources/static/css/tailwind.css` from the source file `tailwind-src.css`.

### Development Alternative (CDN)

For rapid development without rebuilding Tailwind, uncomment the CDN script in `nullInvoice/src/main/resources/templates/fragments/head.html`:

```html
<script src="https://cdn.tailwindcss.com"></script>
```

**Remember to rebuild Tailwind CSS before deploying to production.**

## Load Testing

A script is included to stress test the invoice generation API with concurrent requests.

### Usage

```bash
API_KEY=your_api_key ./integration-tests/gen-test.sh [SUPPLIER_ID] [COUNT] [BASE_URL]
```

Or pass the API key as the 4th argument:

```bash
./integration-tests/gen-test.sh [SUPPLIER_ID] [COUNT] [BASE_URL] [API_KEY]
```

### Parameters

- `SUPPLIER_ID` - supplier ID to use for test invoices (default: 1)
- `COUNT` - number of concurrent invoice requests to generate (default: 20)
- `BASE_URL` - application base URL (default: http://localhost:8080)
- `API_KEY` - your API key (required, can be set as environment variable)

### Example

```bash
API_KEY=abc123-your-key ./integration-tests/gen-test.sh 1 50 http://localhost:8080
```

This fires concurrent invoice generation requests to test the pessimistic locking mechanism and overall API performance under load.

## Building the Application

### With Maven

```bash
cd nullInvoice
mvn clean package
```

The built JAR will be in `target/nullinvoice-0.0.1-SNAPSHOT.jar`

### Running Tests

```bash
mvn test
```

### Running Locally

```bash
java -jar target/nullinvoice-0.0.1-SNAPSHOT.jar
```

Or via Maven:

```bash
mvn spring-boot:run
```

## Database Migrations

Database schema changes are managed with Flyway migrations.

### Creating a New Migration

1. Create a new SQL file in `nullInvoice/src/main/resources/db/migration/`
2. Follow naming convention: `V{version}__{description}.sql`
   - Example: `V3__add_customer_notes.sql`
3. Migrations run automatically on application startup
4. Flyway tracks applied migrations in the `flyway_schema_history` table

### Migration Best Practices

- Never modify existing migrations that have been applied
- Use descriptive names for migrations
- Test migrations on a copy of production data
- Ensure migrations are idempotent when possible
- Keep migrations small and focused

## Internationalization

### Supported UI Languages

The application UI is fully internationalized with message bundles:

- English (EN) ✅
- Bulgarian (BG) ✅
- German (DE) ✅
- Spanish (ES) ✅
- Italian (IT) ✅
- Russian (RU) ✅

### Adding a New Language

1. Create `nullInvoice/src/main/resources/messages_{lang}.properties`
2. Copy keys from `messages.properties` (English)
3. Translate all values to your language
4. Test by setting your locale in the UI

### Message Files

- `messages.properties` - English (default/fallback)
- `messages_bg.properties` - Bulgarian
- `messages_de.properties` - German
- `messages_es.properties` - Spanish
- `messages_it.properties` - Italian
- `messages_ru.properties` - Russian

## Contributing

**We welcome contributions for:**

- 🌍 UI translations (`messages_{lang}.properties` in `nullInvoice/src/main/resources/`)
- 📄 Example invoice templates for your language/region (`templates/{lang}/`)
- 🎨 Font recommendations for optimal PDF rendering in your language
- 📝 Documentation improvements and translations
- 🐛 Bug fixes and feature improvements

### Technical Support for Additional Languages

The application can generate invoices in **any language** with proper font support:

- **Arabic** (UAE, Saudi Arabia, etc.) - RTL support via CSS, needs templates & UI translations
- **East Asian** (Chinese, Japanese, Korean) - Unicode fonts supported, needs templates & UI translations
- **Hebrew** - RTL support via CSS, needs templates & UI translations
- Any other Unicode-based language

The DejaVu font family bundled with the application supports extensive Unicode coverage. For languages requiring specific fonts, use `@font-face` in invoice templates to load web fonts with fallback to DejaVu.

## Code Style

- Follow standard Java conventions
- Use meaningful variable and method names
- Document public APIs with Javadoc
- Keep methods focused and concise
- Write tests for new features and bug fixes

## IDE Setup

### NetBeans

The project was built with NetBeans and includes `nbactions.xml` for IDE integration.

### IntelliJ IDEA

1. Import as Maven project
2. Set JDK to Java 21
3. Enable annotation processing
4. Set environment variables in run configurations

### VS Code

1. Install Java Extension Pack
2. Install Spring Boot Extension Pack
3. Configure environment variables in `launch.json`

## Debugging

### Enable Debug Logging

```yaml
logging:
  level:
    com.nullinvoice: DEBUG
```

### Remote Debugging

Run with debug enabled:

```bash
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 \
     -jar target/nullinvoice-0.0.1-SNAPSHOT.jar
```

Connect your IDE debugger to `localhost:5005`

## Docker Development

### Building Custom Image

```bash
docker compose build
```

### Running with Local Changes

```bash
docker compose up -d
```

### Viewing Logs

```bash
docker logs -f nullinvoice
```

## Performance Considerations

### Invoice Generation

- Uses pessimistic locking to prevent race conditions on invoice numbering
- Concurrent requests for the same supplier are serialized
- Different suppliers can process concurrently

### PDF Rendering

- PDFs are rendered on-demand from stored HTML snapshots
- First PDF generation may be slower due to font loading
- Subsequent generations are faster with cached resources

### Database Queries

- JPA queries are optimized with proper indexing
- Soft deletes use `deleted` flag instead of actual deletion
- Invoice searches use database indexes on number, date, supplier, and client
