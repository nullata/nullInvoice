# API Reference

Complete REST API documentation for nullInvoice.

## Authentication

**Authentication Required:** All API endpoints require either:

- Bearer token in `Authorization` header (recommended for integrations)
- Active session (if logged in via web UI)

### API Key Authentication (Recommended for external integrations)

Generate an API key from the Admin dashboard and include it in the `Authorization` header:

```bash
curl -H "Authorization: Bearer YOUR_API_KEY_HERE" \
     http://localhost:8080/api/v1/invoices
```

### Session Authentication (For UI-initiated requests)

If you're logged in via the web UI, your session is automatically used for API requests.

### Generating an API Key

1. Login to the web UI
2. Navigate to Admin (user dropdown menu)
3. Scroll to "API Keys" section
4. Enter optional description and click "Generate Key"
5. **Copy the key immediately** - it won't be shown again
6. The key is displayed as `Authorization: Bearer {key}` format

### Security Notes

- API keys are hashed in the database (BCrypt)
- Keys can be revoked at any time from the Admin dashboard
- Last used timestamp is tracked for each key
- Generate separate keys for different applications/environments

## Invoice Generation

`POST /api/v1/invoices/generate`

**Authentication required** - Include Bearer token in `Authorization` header.

- Requires `supplier_id` and `client`.
- `response_type` supports `number` (default) or `pdf`.
  - `number`: Returns JSON with invoice metadata only
  - `pdf`: Returns PDF file directly with metadata in response headers
- Status is always `issued` for API-generated invoices.

### Example: Existing Client by ID

```bash
curl -X POST http://localhost:8080/api/v1/invoices/generate \
  -H "Authorization: Bearer YOUR_API_KEY_HERE" \
  -H "Content-Type: application/json" \
  -d '{
  "response_type": "number",
  "supplier_id": 1,
  "client": { "id": 42 },
  "items": [
    { "description": "Consulting", "quantity": 1, "unit_price": 1000, "tax_rate": 0.2 }
  ],
  "issue_date": "2026-01-16",
  "due_date": "2026-01-30",
  "currency_code": "EUR",
  "notes": "Thank you"
}'
```

### Example: New Client Details

```bash
curl -X POST http://localhost:8080/api/v1/invoices/generate \
  -H "Authorization: Bearer YOUR_API_KEY_HERE" \
  -H "Content-Type: application/json" \
  -d '{
  "response_type": "number",
  "supplier_id": 1,
  "client": {
    "name": "Client Co",
    "addressLine1": "2 Side St",
    "city": "Burgas",
    "country": "BG",
    "taxId": "123",
    "vatId": "BG123"
  },
  "items": [
    { "description": "Consulting", "quantity": 1, "unit_price": 1000, "tax_rate": 0.2 }
  ]
}'
```

### Response: JSON Metadata (response_type: number)

```json
{
  "status": "issued",
  "message": "invoice generated",
  "invoiceNumber": "INV-000001",
  "issueDate": "2026-01-16"
}
```

### Example: Direct PDF Download

```bash
curl -X POST http://localhost:8080/api/v1/invoices/generate \
  -H "Authorization: Bearer YOUR_API_KEY_HERE" \
  -H "Content-Type: application/json" \
  -d '{
  "response_type": "pdf",
  "supplier_id": 1,
  "client": {
    "name": "Client Co",
    "addressLine1": "2 Side St",
    "city": "Plovdiv",
    "country": "BG",
    "taxId": "123",
    "vatId": "BG123"
  },
  "items": [
    { "description": "Consulting", "quantity": 1, "unit_price": 1000, "tax_rate": 0.2 }
  ]
}' \
  -o invoice.pdf -i
```

### Response: PDF with Metadata Headers (response_type: pdf)

```
HTTP/1.1 200 OK
Content-Type: application/pdf
Content-Disposition: attachment; filename="INV-000001.pdf"
X-Invoice-Number: INV-000001
X-Invoice-Status: issued
X-Invoice-Issue-Date: 2026-01-16

[PDF binary data]
```

The PDF response includes invoice metadata in custom response headers (`X-Invoice-Number`, `X-Invoice-Status`, `X-Invoice-Issue-Date`), allowing your application to store the invoice details while receiving the PDF file directly.

## Invoice Listing and Filtering

`GET /api/v1/invoices`

**Authentication required** - Include Bearer token in `Authorization` header.

- Optional filter: `status=unpaid` or `status=issued`

Example request:

```bash
curl -H "Authorization: Bearer YOUR_API_KEY_HERE" \
     http://localhost:8080/api/v1/invoices?status=unpaid
```

Example response (filtered):

```json
[
  { "invoiceNumber": "INV-000002", "status": "unpaid" }
]
```

## Invoice Retrieval

**Authentication required** - Include Bearer token in `Authorization` header.

### Get Invoice Metadata

```bash
curl -H "Authorization: Bearer YOUR_API_KEY_HERE" \
     http://localhost:8080/api/v1/invoices/INV-000001
```

### Download Invoice PDF

```bash
curl -H "Authorization: Bearer YOUR_API_KEY_HERE" \
     http://localhost:8080/api/v1/invoices/INV-000001/pdf \
     -o invoice.pdf
```

## Parties API

**Authentication required** - Include Bearer token in `Authorization` header.

### Get Client by Tax/VAT ID

`GET /api/v1/parties/client?taxId=...&vatId=...` (requires one of taxId/vatId)

### Search Clients

`GET /api/v1/parties/clients/search?q=...` (minimum 2 characters)

### List Suppliers

`GET /api/v1/parties/suppliers`

Example:

```bash
curl -H "Authorization: Bearer YOUR_API_KEY_HERE" \
     http://localhost:8080/api/v1/parties/suppliers
```

## Health Check

`GET /api/v1/health` (no authentication required)

## Invoice Lifecycle and Status

- Status values are `unpaid` and `issued`. `issued` is considered paid and final.
- API invoice creation always results in `issued` status, and the API does not accept status overrides.
- UI invoice creation can mark an invoice as `unpaid` only when a due date is set.
- Unpaid invoices can be marked as `issued` from the invoice details page.
- Issued invoices cannot be reverted back to unpaid.

## UI Workflow

1) **Suppliers**: set up supplier details first. The supplier profile drives locale, currency, invoice numbering, and default tax rate.
2) **Templates**: create a template for custom branding and set a default. Use a global default, or set a supplier-specific default to override the global choice.
3) **Clients** (optional): you can add clients manually, but invoice generation also creates/updates clients from the entered details.
4) **Select active supplier**: choose a default supplier in the UI, which sets a cookie used by invoice creation.
5) **Supplier ID for API**: open a supplier in edit mode and use the supplier ID shown in the top-left of the supplier page.
6) **Invoices**: list, search, and filter invoices; open an invoice to review details and mark unpaid invoices as issued/paid.
7) **Generate invoice**: enter client details or search for an existing client, add line items, and set per-item tax. If an item omits tax, the supplier default tax rate applies.
8) **Discounts and notes**: enter a flat discount or use the discount % calculator; add notes and generate the invoice to see the overview page.

Invoice generation uses a pessimistic write lock on the supplier record to avoid race conditions when calculating the next invoice number. This blocks concurrent requests for the same supplier until the number is assigned.

## OpenAPI / Swagger

**Interactive API Documentation:**

- OpenAPI JSON specification: `/openapi`
- Swagger UI: `/swagger`

**Accessing Swagger UI:**

1. Login to the web UI
2. Click user dropdown menu > "API Docs"
3. Or navigate directly to `/swagger` (requires login)

**Testing Endpoints in Swagger:**

1. Click the "Authorize" button (lock icon) in the top right
2. Enter your API key (generate from Admin > API Keys if needed)
3. Click "Authorize"
4. All requests will now include the Bearer token automatically
5. Use "Try it out" to test endpoints interactively

**Note:** Swagger UI requires authentication to access and is only available to logged-in admin users.
