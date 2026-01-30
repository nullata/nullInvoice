# Extended Documentation

Welcome to the extended documentation for nullInvoice. This directory contains detailed guides organized by topic.

## Documentation Structure

### [Deployment Guide](DEPLOYMENT.md)
Complete deployment instructions including:
- Docker Compose setup
- Database configuration and permissions
- Local development setup
- NetBeans IDE configuration
- First-run setup and admin account creation
- Emergency recovery procedures

### [API Reference](API.md)
Complete REST API documentation:
- Authentication methods (API keys and sessions)
- Invoice generation endpoints
- Invoice listing and filtering
- PDF retrieval
- Parties API
- OpenAPI/Swagger usage

### [Template Customization](TEMPLATES.md)
Invoice template creation and customization:
- Template format requirements (XHTML)
- Available placeholders (30+ variables)
- PDF fonts and Unicode support
- Example templates in 6 languages
- Localization and multi-currency support

### [Configuration Reference](CONFIGURATION.md)
Environment variables and settings:
- Required and optional variables
- Timezone configuration (critical!)
- Session timeout customization
- Security best practices
- Production deployment checklist
- Database connection pooling

### [Development Guide](DEVELOPMENT.md)
For contributors and developers:
- Project structure
- Building with Maven
- Tailwind CSS development
- Load testing scripts
- Database migrations
- Internationalization
- Code style guidelines

## Quick Links

- **Main README:** [../README.md](../../README.md)
- **Example Templates:** [../../templates/](../../templates/)
- **Docker Hub:** https://hub.docker.com/r/nullata/nullinvoice
- **GitHub Repository:** https://github.com/nullata/nullInvoice

## Navigation

Each guide is self-contained and includes cross-references to related topics. You can read them in any order based on your needs:

- **New to nullInvoice?** Start with [Deployment Guide](DEPLOYMENT.md)
- **Ready to integrate?** Check [API Reference](API.md)
- **Customizing invoices?** See [Template Customization](TEMPLATES.md)
- **Production deployment?** Review [Configuration Reference](CONFIGURATION.md)
- **Contributing code?** Read [Development Guide](DEVELOPMENT.md)
