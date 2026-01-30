# Template Customization

Complete guide for creating and customizing invoice templates in nullInvoice.

## Template Overview

- Templates live in `invoice_templates` and must include HTML content.
- Invoice generation requires an effective default template (supplier-specific or global).
- Suppliers can override the global default with a supplier-specific template.
- Generated invoices store an HTML snapshot for consistent re-rendering.
- PDFs are rendered from the stored HTML snapshot when available.

## Example Templates

Example templates are available in 6 languages in the [`templates/`](../../templates/) directory:

- English (EN)
- Bulgarian (BG)
- German (DE)
- Spanish (ES)
- Italian (IT)
- Russian (RU)

These templates can be used as starting points for your custom designs.

## Template Format Requirements

Templates must use **XHTML** format for proper PDF rendering:

- Include XML declaration: `<?xml version="1.0" encoding="UTF-8"?>`
- Use XHTML namespace: `<html xmlns="http://www.w3.org/1999/xhtml">`
- All CSS must be **inline** within a `<style>` tag in the `<head>` section
- External stylesheets are not supported for PDF generation

### Basic Template Structure

```html
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta charset="UTF-8"/>
    <title>Invoice</title>
    <style>
        /* All your CSS goes here - inline only! */
        body {
            font-family: "DejaVu Sans", sans-serif;
            margin: 0;
            padding: 20px;
        }
        /* ... more styles ... */
    </style>
</head>
<body>
    <!-- Your invoice HTML here with {{placeholders}} -->
</body>
</html>
```

## Template Placeholders

Templates use `{{placeholder}}` variables. If a placeholder is omitted from a template, that data will not be rendered in the final invoice. The service does not validate which placeholders are present or missing.

### Supported Placeholders

**Invoice Details:**
- `{{invoiceNumber}}` - Invoice number
- `{{issueDate}}` - Issue date
- `{{dueDateRow}}` - Full `<div>` row with due date or empty if not set

**Supplier Information:**
- `{{supplierName}}` - Supplier/company name
- `{{supplierAddressLine1}}` - Address line 1
- `{{supplierAddressLine2Row}}` - Full `<div>` row with address line 2 or empty
- `{{supplierCityRegionPostal}}` - City, region, and postal code
- `{{supplierCountry}}` - Country
- `{{supplierTaxIdRow}}` - Full `<div>` row with tax ID or empty
- `{{supplierVatIdRow}}` - Full `<div>` row with VAT ID or empty
- `{{supplierEmailRow}}` - Full `<div>` row with email or empty
- `{{supplierPhoneRow}}` - Full `<div>` row with phone or empty

**Client Information:**
- `{{clientName}}` - Client/company name
- `{{clientAddressLine1}}` - Address line 1
- `{{clientAddressLine2Row}}` - Full `<div>` row with address line 2 or empty
- `{{clientCityRegionPostal}}` - City, region, and postal code
- `{{clientCountry}}` - Country
- `{{clientTaxIdRow}}` - Full `<div>` row with tax ID or empty
- `{{clientVatIdRow}}` - Full `<div>` row with VAT ID or empty
- `{{clientEmailRow}}` - Full `<div>` row with email or empty
- `{{clientPhoneRow}}` - Full `<div>` row with phone or empty

**Invoice Items and Totals:**
- `{{itemsRows}}` - Rendered `<tr>` rows for invoice line items
- `{{subtotal}}` - Subtotal before discounts and tax
- `{{discountTotal}}` - Total discount amount
- `{{taxTotal}}` - Total tax amount
- `{{total}}` - Final total amount

**Additional:**
- `{{notesSection}}` - Full `<div>` section with notes or empty

### Row Placeholders Explained

Placeholders ending with `Row` (e.g., `{{dueDateRow}}`, `{{supplierEmailRow}}`) return either:
- A complete HTML row/div with the data if it exists
- An empty string if the data is not present

This allows conditional rendering without template logic:

```html
<!-- This row will only appear if email is set -->
{{supplierEmailRow}}
```

## PDF Fonts

PDFs are rendered using OpenHTMLToPDF. The application bundles the **DejaVu** font family which supports Latin, Cyrillic, Greek, and other Unicode characters.

### Using Bundled Fonts

```css
body {
    font-family: "DejaVu Sans", sans-serif;
}
```

### Using Custom Web Fonts

Templates can load external fonts via `@font-face` in the inline `<style>` section. Ensure the web font supports your template language (e.g., Cyrillic for Russian/Bulgarian, Greek, etc.). Bundled fonts serve as fallback if the web font fails to load.

```css
@font-face {
    font-family: 'Roboto';
    font-style: normal;
    font-weight: 400;
    src: url('https://fonts.gstatic.com/s/roboto/v30/KFOmCnqEu92Fr1Me5WZLCzYlKw.ttf') format('truetype');
}

body {
    font-family: 'Roboto', 'DejaVu Sans', sans-serif;
}
```

### Available Bundled Fonts

| Font Family              | Weights                                     | Styles          |
| ------------------------ | ------------------------------------------- | --------------- |
| `DejaVu Sans`            | 200 (extra-light), 400 (normal), 700 (bold) | normal, oblique |
| `DejaVu Sans Condensed`  | 400 (normal), 700 (bold)                    | normal, oblique |
| `DejaVu Sans Mono`       | 400 (normal), 700 (bold)                    | normal, oblique |
| `DejaVu Serif`           | 400 (normal), 700 (bold)                    | normal, italic  |
| `DejaVu Serif Condensed` | 400 (normal), 700 (bold)                    | normal, italic  |

## Parties and Localization

- Suppliers and clients share the `parties` table and are distinguished by `role`.
- Soft delete hides parties from lists while preserving invoice history.
- Supplier settings can override locale, currency, date format, invoice prefix, and digit padding.
- Supplier `default_tax_rate` is applied to invoice items that omit tax rate.

## Invoice Numbering and Currency

- Invoice numbers are per supplier and use `max(invoice_number_int) + 1`.
- Optional prefix and digit padding are applied from supplier settings.
- Currency codes are validated against ISO 4217.

## UI Behavior

- `/invoices/new` creates invoices using the selected supplier from the cookie (if present).
- The unpaid toggle is disabled until a due date is provided.
- `/invoices` supports filtering by supplier (dropdown) and searching by number, date, or client. Date search accepts ISO (`YYYY-MM-DD`) or `dd.MM.yyyy`.
- Invoice list sorting includes status; sorting by status toggles `unpaid` and `issued` order.
- `/invoices/{invoiceNumber}` shows status, totals, stored HTML preview, and provides a one-way "Mark as paid" action for unpaid invoices.

## Capabilities

- Manage suppliers and clients with soft delete and uniqueness checks.
- Create and manage invoice templates with custom branding, with a global default and per-supplier defaults.
- Generate invoices with HTML snapshots saved on the invoice record.
- Render invoices to PDF on demand.
- Search and sort invoices by number, date, client, supplier, and status.
- Track invoice status as `unpaid` or `issued` (paid/final).
