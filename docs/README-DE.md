# <img src="../nullInvoice/src/main/resources/static/images/logo.svg" alt="logo" width="24"> nullInvoice

**nullInvoice** ist ein Spring Boot Mikroservice für **automatisierte Rechnungserstellung und -verwaltung** mit **vollständig anpassbaren HTML-Vorlagen**, entwickelt zur Integration in Webshops und SaaS-Plattformen.

## Überblick

Unternehmen nutzen nullInvoice für die Rechnungserstellung, nachdem Verkäufe abgeschlossen sind. Lieferanten werden einmalig über die Web-UI konfiguriert, danach ruft Ihre Anwendung die REST API auf, um konforme Rechnungen bei Bedarf zu erstellen.

**So funktioniert es:**

1. **Setup**: Konfigurieren Sie Lieferanten in der UI mit Firmendaten, Locale, Währung, Steuersätzen, individuellem Branding und Rechnungsvorlagen
2. **Authentifizierung**: Generieren Sie API-Schlüssel im Admin-Dashboard für sicheren REST-API-Zugriff
3. **Integration**: Ihr Webshop/SaaS sendet authentifizierte API-Anfragen an `/api/v1/invoices/generate` unter Verwendung der Lieferanten-ID
4. **Erstellung**: Rechnungen werden aus vollständig anpassbaren HTML-Vorlagen erstellt und als JSON oder PDF mit Metadaten-Headern zurückgegeben
5. **Zustellung**: Ihre Anwendung erhält die Rechnung und kann sie an Kunden weiterleiten oder für die Ablage speichern

### Typischer Integrationsablauf

```
┌─────────────────┐
│   Kunde         │
│   schließt      │
│   Kauf ab       │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────────────┐
│  Ihr Webshop/SaaS-Plattform             │
│  ─────────────────────────────────────  │
│  1. Zahlung verarbeiten                 │
│  2. Digitale Quittung ausstellen        │
│     (erforderlich)                      │
│  3. Kunde fordert Rechnung? ─────────┐  │
└──────────────────────────────────────┼──┘
                                       │
                                       │ API-Aufruf
                                       ▼
                        ┌──────────────────────────────┐
                        │  nullInvoice-Service         │
                        │  ──────────────────────────  │
                        │  POST /api/v1/invoices/      │
                        │       generate               │
                        │                              │
                        │  - Prüft Lieferanten-ID      │
                        │  - Wendet Vorlage an         │
                        │  - Speichert HTML-Snapshot   │
                        │  - Generiert PDF             │
                        │  - Gibt Rechnung zurück      │
                        └──────────────┬───────────────┘
                                       │
                                       │ Antwort (JSON oder PDF)
                                       ▼
┌──────────────────────────────────────────┐
│  Ihr Webshop/SaaS-Plattform              │
│  ──────────────────────────────────────  │
│  - Empfängt JSON-Metadaten ODER PDF-Datei│
│  - Speichert Rechnungsnummer für Ablage  │
│  - Sendet PDF per E-Mail an den Kunden   │
└──────────────────────────────────────────┘
```

### Hauptfunktionen

**Vollständig anpassbare Vorlagen**

- Rechnungen basieren auf benutzerdefinierten HTML-Vorlagen mit Inline-CSS
- Vorlagen unterstützen 30+ Platzhalter für Lieferanten-, Kunden- und Finanzdaten
- Standardvorlagen pro Lieferant ermöglichen unterschiedliches Branding je Geschäftseinheit

**Unveränderlichkeit von Dokumenten**

- Jede Rechnung speichert einen **Snapshot der geparsten HTML-Vorlage** zum Zeitpunkt der Erstellung
- Vorlagenänderungen beeinflussen bereits erstellte Rechnungen nicht
- Rechnungen können konsistent aus dem gespeicherten Snapshot neu erstellt werden
- Datenbankberechtigungen verhindern das Löschen von Rechnungsdatensätzen (finanzielle Compliance)

**Multi-Tenant-fähig**

- Konfigurieren Sie mehrere Lieferanten mit unabhängigen Einstellungen (Locale, Währung, Nummerierung, Branding)
- API-Aufrufer geben die Lieferanten-ID an, um Rechnungen unter verschiedenen Geschäftseinheiten zu erstellen

**Flexible Bereitstellung**

- Rückgabe von Rechnungsmetadaten als JSON für die Ablage (`response_type: number`)
- PDF direkt mit Metadaten in den Headern für sofortige Zustellung (`response_type: pdf`)
- PDF später abrufen über `/api/v1/invoices/{invoiceNumber}/pdf`

**OpenAPI-Dokumentation**

- Interaktive API-Dokumentation unter `/swagger`
- OpenAPI-JSON-Spezifikation unter `/openapi`
- Vollständige Request/Response-Beispiele und Try-it-out-Funktionalität

## Wichtige Hinweise

**⚠️ Sicherheitshinweis**

nullInvoice enthält **integrierte Authentifizierung** (sitzungsbasiertes UI-Login + API-Schlüssel-Authentifizierung für REST-Endpunkte). Dies bietet standardmäßig Sicherheit, ist jedoch weiterhin für den Einsatz in internen/privaten Netzwerken gedacht.

**Empfohlene Bereitstellung:**

- Hinter einer Firewall oder einem VPN
- In einem privaten Netzwerk, das nur für Ihre vertrauenswürdigen Anwendungen erreichbar ist
- Mit aktiviertem HTTPS/TLS für alle Verbindungen
- Hinter einem Reverse Proxy mit konfiguriertem Rate Limiting

**Für den Produktivbetrieb sind zusätzliche Sicherheitsmaßnahmen erforderlich:**

- HTTPS/TLS aktivieren
- Rate Limiting auf Reverse-Proxy-Ebene konfigurieren
- Starke Admin-Passwörter verwenden
- API-Schlüssel regelmäßig rotieren
- API-Schlüssel in sicherem Secret Management speichern (HashiCorp Vault, AWS Secrets Manager usw.)

Siehe Abschnitt [Sicherheit & Best Practices](#sicherheit--best-practices) für eine vollständige Checkliste.

**⚠️ Diese Anwendung ist KEIN Buchhaltungsinstrument.**

nullInvoice ist eine **Pipeline zur Rechnungserstellung**, die Rechnungsdokumente erstellt, speichert und bereitstellt. Es:

- Verfolgt keine Zahlungen oder Zahlungsstatus über einfache "unbezahlt/ausgestellt"-Flags hinaus
- Verwaltet keine Forderungen oder Verbindlichkeiten
- Erstellt keine Finanzberichte oder Bilanzen
- Integriert sich nicht in Buchhaltungssysteme (Hauptbücher, Journale usw.)
- Übernimmt keine Buchhaltung, Abstimmung oder Steuererklärungen

Für umfassendes Finanzmanagement integrieren Sie nullInvoice mit einem dedizierten Buchhaltungssystem. Nutzen Sie diesen Dienst zur Rechnungserstellung und importieren Sie die Rechnungen anschließend in Ihre Buchhaltungssoftware für Nachverfolgung und Compliance.

## Stack

- Java 21, Spring Boot 3.5.3
- MariaDB + JPA
- Thymeleaf (UI)
- OpenHTMLToPDF (PDFBox)
- OpenAPI unter `/openapi`, Swagger UI unter `/swagger`

## Voraussetzungen

**Für Docker-Bereitstellung (empfohlen):**

- Docker
- Docker Compose

**Für lokale Entwicklung:**

- Java 21 (JDK - Eclipse Temurin oder OpenJDK)
- Maven 3.9+
- MariaDB 10.5+ (oder MySQL 8.0+)
- Tailwind CSS Standalone-Binary (für den CSS-Build - von [GitHub releases](https://github.com/tailwindlabs/tailwindcss/releases))

## Datenbank einrichten

**Datenbank erstellen:**

```sql
CREATE DATABASE nullinvoice CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

**Dedizierten Anwendungsnutzer mit eingeschränkten Rechten erstellen:**

```sql
CREATE USER 'nullinvoice'@'localhost' IDENTIFIED BY 'your_secure_password';

-- Berechtigungen für normale Operationen und Schema-Migrationen
GRANT SELECT, INSERT, UPDATE ON nullinvoice.* TO 'nullinvoice'@'localhost';
GRANT CREATE, ALTER, INDEX, REFERENCES ON nullinvoice.* TO 'nullinvoice'@'localhost';

FLUSH PRIVILEGES;
```

**Für Remote-Zugriff den Host anpassen:**

```sql
CREATE USER 'nullinvoice'@'%' IDENTIFIED BY 'your_secure_password';
GRANT SELECT, INSERT, UPDATE ON nullinvoice.* TO 'nullinvoice'@'%';
GRANT CREATE, ALTER, INDEX, REFERENCES ON nullinvoice.* TO 'nullinvoice'@'%';
FLUSH PRIVILEGES;
```

**Wichtig: Warum diese eingeschränkten Rechte?**

Rechnungen sind **unveränderliche Finanzdokumente** und dürfen nach dem Erstellen niemals gelöscht werden. Der Anwendungsnutzer ist bewusst eingeschränkt und darf nicht:

- `DELETE` - kann keine Datensätze löschen
- `DROP` - kann keine Tabellen oder Datenbank löschen
- `TRUNCATE` - kann keine Tabellen leeren
- `GRANT` - kann keine Berechtigungen vergeben

Das stellt Datenintegrität und Compliance mit Anforderungen zur Finanzarchivierung sicher. Das `UPDATE`-Recht wird für Statusänderungen (z. B. als bezahlt markieren) und Soft Deletes für Parteien-Datensätze benötigt.

### Schema-Verwaltung

- Das Datenbankschema wird durch **Flyway**-Migrationen verwaltet
- Initiales Schema: `nullInvoice/src/main/resources/db/migration/V1__initial_schema.sql`
- Das Schema wird beim Start automatisch erstellt/aktualisiert
- Hibernate DDL-Modus ist auf `none` gesetzt (Flyway verwaltet alle Schemaänderungen)

## Konfiguration

Eine Beispiel-Konfigurationsdatei liegt unter `.env.example`. Kopieren Sie diese nach `.env` und passen Sie die Werte an Ihre Umgebung an.

Umgebungsvariablen (Standardwerte in Klammern):

- `TZ` - **ERFORDERLICH** System-Zeitzone (Europe/Sofia)
- `APP_PORT` (8080)
- `DB_HOST` (localhost)
- `DB_PORT` (3306)
- `DB_USER` (nullinvoice)
- `DB_PASSWORD` (leer)
- `DB_NAME` (nullinvoice)
- `DB_PARAMS` - **ERFORDERLICH** JDBC-Parameter, die an die Verbindungs-URL angehängt werden

### **WICHTIG: Zeitzonen-Konfiguration**

**Sie MÜSSEN die Zeitzone an ZWEI Stellen setzen:**

1. **`TZ`** Umgebungsvariable - setzt die System-/Anwendungszeitzone
2. **`serverTimezone`** Parameter in `DB_PARAMS` - setzt die Zeitzone der Datenbankverbindung

**Beide Werte MÜSSEN mit der Zeitzone des Datenbankservers übereinstimmen**, damit Datums-/Zeitwerte korrekt interpretiert und gespeichert werden.

Beispielkonfiguration:

```bash
TZ=Europe/Sofia
DB_PARAMS=?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Europe/Sofia
```

Häufige Zeitzonenwerte: `UTC`, `Europe/London`, `America/New_York`, `Asia/Tokyo`. Siehe [vollständige Liste](https://en.wikipedia.org/wiki/List_of_tz_database_time_zones).

**Nicht übereinstimmende Zeitzonen führen zu falschen Rechnungsdaten und Zeitstempeln.**

### Erste Admin-Einrichtung

Beim ersten Start wird die Anwendung auf `/setup` umleiten, um das initiale Admin-Konto zu erstellen. Dies ist erforderlich, bevor andere Funktionen genutzt werden können.

**Zurücksetzen und neues Admin-Konto erstellen (Notfall):**

1. Anwendung stoppen
2. Tabellen `users` und `api_keys` in der Datenbank leeren
3. Anwendung neu starten
4. Den `/setup`-Flow erneut abschließen

**Sicherheitsempfehlungen:**

- Starke Passwörter für das Admin-Konto verwenden
- Passwort-Hinweis setzen (optional, aber empfohlen)
- Separate API-Schlüssel für unterschiedliche Umgebungen generieren (dev, staging, prod)
- Nicht verwendete API-Schlüssel widerrufen

## Sicherheit & Best Practices

**Authentifizierungsarchitektur:**

- **UI-Zugriff:** Sitzungsbasierte Authentifizierung mit Form-Login
- **API-Zugriff:** Stateless Bearer-Token-Authentifizierung (API-Schlüssel)
- **Passwörter:** BCrypt-Hash mit 10 Runden
- **API-Schlüssel:** UUID-Format, BCrypt-gehasht, werden nur einmal angezeigt

**Sitzungsdauer:**

Standardmäßig laufen UI-Sitzungen nach **30 Minuten Inaktivität** ab (Spring Boot/Tomcat Standard). Benutzer werden automatisch abgemeldet und zur Anmeldeseite weitergeleitet.

Um die Sitzungsdauer anzupassen, fügen Sie Folgendes zu `nullInvoice/src/main/resources/application.yml` hinzu:

```yaml
server:
  servlet:
    session:
      timeout: 60m  # Optionen: 15m, 30m, 1h, 2h, usw.
```

Gängige Timeout-Werte:
- `15m` - 15 Minuten (strengere Sicherheit)
- `30m` - 30 Minuten (Standard)
- `1h` - 1 Stunde (Komfort für aktive Benutzer)
- `8h` - 8 Stunden (erweitert für lange Sitzungen)

**Checkliste für den Produktivbetrieb:**

- HTTPS/TLS für alle Verbindungen aktivieren
- Starke Admin-Passwörter verwenden
- Separate API-Schlüssel pro Anwendung/Umgebung erstellen
- Hinter Firewall oder VPN betreiben
- Rate Limiting auf Reverse-Proxy-Ebene konfigurieren
- Logging und Monitoring einrichten
- API-Schlüssel-Nutzung regelmäßig prüfen (Zeitstempel der letzten Nutzung)
- Nicht verwendete oder kompromittierte API-Schlüssel sofort widerrufen
- API-Schlüssel in Umgebungsvariablen halten, niemals im Code
- Sicheres Secret Management verwenden (HashiCorp Vault, AWS Secrets Manager usw.)

**CSRF-Schutz:**

- Aktiviert für alle UI-Formulare
- Deaktiviert für API-Endpunkte (stateless Bearer-Token-Auth)

**Sicherheit beim ersten Start:**

- Die Anwendung ist unbenutzbar, bis ein Admin-Konto über `/setup` erstellt wurde
- Die Setup-Seite ist nur erreichbar, wenn noch kein Admin existiert
- Nach dem Setup ist Login für alle Funktionen erforderlich

## Docker Compose

**Offizielle Docker-Images:**
Vorgefertigte Images sind auf [Docker Hub](https://hub.docker.com) verfügbar. Suchen Sie nach dem offiziellen nullInvoice-Image, um den Build-Schritt zu überspringen.

Build ohne Cache:

```bash
docker compose build --no-cache
```

Stack starten:

```bash
docker compose up -d
```

Stack stoppen:

```bash
docker compose down
```

## Lokale Entwicklung (ohne Docker)

### Setup

1. Stellen Sie sicher, dass eine MariaDB-Instanz läuft und erstellen Sie die Datenbank. Siehe [Datenbank einrichten](#datenbank-einrichten)

2. Tailwind CSS bauen (vor dem ersten Start erforderlich):

   ```bash
   ./build-tailwind.sh
   ```

3. Projekt mit Maven bauen:

   ```bash
   cd nullInvoice
   mvn clean package
   ```

4. Anwendung starten:

   ```bash
   java -jar target/nullinvoice-0.0.1-SNAPSHOT.jar
   ```

### Verwendung von NetBeans IDE

Das Projekt wurde mit NetBeans erstellt und kann als Maven-Projekt mit dem Spring-Boot-Plugin geöffnet werden.

**Umgebungsvariablen in NetBeans setzen:**

Option 1 - Über die IDE:

1. Rechtsklick auf das Projekt >> Properties
2. Zu Actions >> Run navigieren
3. Umgebungsvariablen setzen: `APP_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`

Option 2 - Direkt bearbeiten:

- `nbactions.xml` im Projekt-Root anpassen

## Erste Schritte

### Ersteinrichtung beim ersten Start

Beim ersten Zugriff werden Sie zu `/setup` weitergeleitet, um das initiale Admin-Konto zu erstellen:

1. `http://localhost:8080` öffnen (oder Ihre konfigurierte URL)
2. Sie werden automatisch zu `/setup` weitergeleitet
3. Admin-Konto mit Benutzername, Passwort und optionalem Passwort-Hinweis erstellen
4. Nach dem Setup werden Sie zu `/login` weitergeleitet

**Anmeldung:**

- Anmeldeseite unter `/login` aufrufen
- Die erstellten Admin-Zugangsdaten verwenden
- Passwort-Hinweis ist über das Info-Icon verfügbar (falls konfiguriert)

**Admin-Dashboard:**
Nach der Anmeldung finden Sie das Admin-Dashboard im Benutzermenü:

- Admin-Passwort ändern
- API-Schlüssel für REST-API-Zugriff generieren
- API-Schlüssel widerrufen
- API-Schlüssel-Nutzung anzeigen (Zeitstempel der letzten Nutzung)

### Ersten Lieferanten einrichten

**Voraussetzungen:**

- Ersteinrichtung abgeschlossen (Admin-Konto erstellt)
- Mit Admin-Zugangsdaten angemeldet

Bevor Sie Rechnungen über die API erstellen können, müssen Sie mindestens einen Lieferanten über die Web-UI konfigurieren. Lieferanten definieren Firmendaten, Locale, Währung, Steuersätze, individuelles Branding (über Vorlagen) und die Rechnungsnummerierung für die Rechnungserstellung.

1. In die Web-UI unter `http://localhost:8080` einloggen (oder Ihren konfigurierten Port)

2. Zu Lieferanten navigieren

3. Neuen Lieferanten mit Firmendaten erstellen

4. Locale, Währung und Steuereinstellungen konfigurieren

5. Einstellungen für die Rechnungsnummerierung setzen (Präfix, Stellen)

6. Lieferanten-ID für die API-Integration notieren

7. API-Schlüssel unter Admin > API-Schlüssel für REST-API-Zugriff generieren

**Beispiele für die Lieferanteneinrichtung finden Sie unter `docs/example-images` - `en-us` für ein US- oder Nicht-EU-Beispiel und `eu-de` für ein EU-Beispiel.**

Nach der Konfiguration können Sie eine XHTML-Vorlage einrichten und die Lieferanten-ID (oben links in der Lieferantenbearbeitung) verwenden, um API-Aufrufe an `/api/v1/invoices/generate` zu senden.

## Funktionen

- Lieferanten und Kunden mit Soft Delete und Eindeutigkeitsprüfungen verwalten.
- Rechnungsvorlagen mit individuellem Branding erstellen und verwalten, mit globalem Standard und Lieferanten-Standard.
- Rechnungen erstellen, wobei HTML-Snapshots im Rechnungseintrag gespeichert werden.
- Rechnungen bei Bedarf als PDF rendern.
- Rechnungen nach Nummer, Datum, Kunde, Lieferant und Status suchen und sortieren.
- Rechnungsstatus als `unpaid` oder `issued` (bezahlt/endgültig) verwalten.

## Rechnungslebenszyklus und Status

- Statuswerte sind `unpaid` und `issued`. `issued` gilt als bezahlt und endgültig.
- Die Erstellung einer Rechnung über die API führt immer zu `issued`, und die API akzeptiert keine Status-Overrides.
- Die Erstellung über die UI kann eine Rechnung nur dann als `unpaid` markieren, wenn ein Fälligkeitsdatum gesetzt ist.
- Unbezahlte Rechnungen können auf der Rechnungsdetailseite als `issued` markiert werden.
- Ausgestellte Rechnungen können nicht wieder auf unbezahlt zurückgesetzt werden.

## UI-Verhalten

- `/invoices/new` erstellt Rechnungen mit dem im Cookie ausgewählten Lieferanten (falls vorhanden).
- Der Schalter für `unpaid` ist deaktiviert, bis ein Fälligkeitsdatum gesetzt ist.
- `/invoices` unterstützt Filterung nach Lieferant (Dropdown) und Suche nach Nummer, Datum oder Kunde. Die Datumssuche akzeptiert ISO (`YYYY-MM-DD`) oder `dd.MM.yyyy`.
- Die Rechnungsliste kann nach Status sortiert werden; die Sortierung nach Status wechselt die Reihenfolge von `unpaid` und `issued`.
- `/invoices/{invoiceNumber}` zeigt Status, Summen, gespeicherte HTML-Vorschau und bietet eine Einweg-Aktion „Als bezahlt markieren“ für unbezahlte Rechnungen.

## UI-Workflow

1) Lieferanten: Zuerst Lieferantendaten einrichten. Das Lieferantenprofil bestimmt Locale, Währung, Rechnungsnummerierung und Standardsteuersatz.
2) Vorlagen: Vorlage für individuelles Branding erstellen und als Standard festlegen. Globalen Standard verwenden oder einen Lieferanten-Standard setzen, um den globalen Standard zu überschreiben.
3) Kunden (optional): Kunden können manuell hinzugefügt werden, aber die Rechnungserstellung legt Kunden auch automatisch an/aktualisiert sie.
4) Aktiven Lieferanten auswählen: Standardlieferant in der UI auswählen, wodurch ein Cookie gesetzt wird, der bei der Rechnungserstellung verwendet wird.
5) Lieferanten-ID für die API: Lieferanten im Bearbeitungsmodus öffnen und die Lieferanten-ID oben links verwenden.
6) Rechnungen: Rechnungen auflisten, suchen und filtern; Rechnung öffnen, um Details zu prüfen und unbezahlte Rechnungen als ausgestellt/bezahlt zu markieren.
7) Rechnung erstellen: Kundendaten eingeben oder einen vorhandenen Kunden suchen, Positionen hinzufügen und den Steuersatz pro Position setzen. Fehlt bei einer Position der Steuersatz, gilt der Standardsteuersatz des Lieferanten.
8) Rabatte und Notizen: Einen pauschalen Rabatt eingeben oder den Rabatt-%-Rechner verwenden; Notizen hinzufügen und die Rechnung erstellen, um die Übersichtsseite zu sehen.

Die Rechnungserstellung verwendet eine pessimistische Schreibsperre auf dem Lieferanten-Datensatz, um Race Conditions bei der Berechnung der nächsten Rechnungsnummer zu vermeiden. Dies blockiert konkurrierende Anfragen für denselben Lieferanten, bis die Nummer vergeben ist.

## Authentifizierung

**Authentifizierung erforderlich:** Alle API-Endpunkte erfordern entweder:

- Bearer-Token im `Authorization`-Header (empfohlen für Integrationen)
- Aktive Sitzung (wenn Sie über die Web-UI eingeloggt sind)

### API-Schlüssel-Authentifizierung (empfohlen für externe Integrationen)

Generieren Sie einen API-Schlüssel im Admin-Dashboard und fügen Sie ihn in den `Authorization`-Header ein:

```bash
curl -H "Authorization: Bearer YOUR_API_KEY_HERE" \
     http://localhost:8080/api/v1/invoices
```

### Sitzungsbasierte Authentifizierung (für UI-initierte Anfragen)

Wenn Sie über die Web-UI eingeloggt sind, wird Ihre Sitzung automatisch für API-Anfragen genutzt.

**API-Schlüssel generieren:**

1. In die Web-UI einloggen
2. Zu Admin (Benutzermenü) navigieren
3. Zum Abschnitt "API-Schlüssel" scrollen
4. Optional eine Beschreibung eingeben und auf „Schlüssel generieren“ klicken
5. **Schlüssel sofort kopieren** - er wird nicht erneut angezeigt
6. Der Schlüssel wird im Format `Authorization: Bearer {key}` angezeigt

**Sicherheitshinweise:**

- API-Schlüssel sind in der Datenbank gehasht (BCrypt)
- Schlüssel können jederzeit im Admin-Dashboard widerrufen werden
- Zeitstempel der letzten Nutzung wird pro Schlüssel erfasst
- Separate Schlüssel für verschiedene Anwendungen/Umgebungen generieren

## REST API (Basis: `/api/v1`)

### Rechnungserstellung

`POST /api/v1/invoices/generate`

**Authentifizierung erforderlich** - Bearer-Token im `Authorization`-Header angeben.

- Erfordert `supplier_id` und `client`.
- `response_type` unterstützt `number` (Standard) oder `pdf`.
  - `number`: gibt JSON nur mit Rechnungsmetadaten zurück
  - `pdf`: liefert das PDF direkt mit Metadaten in den Response-Headern
- Status ist immer `issued` für API-erstellte Rechnungen.

Beispielanfrage (bestehender Kunde per ID):

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

Beispielanfrage (neue Kundendaten):

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

Beispielantwort (response_type: number):

```json
{
  "status": "issued",
  "message": "invoice generated",
  "invoiceNumber": "INV-000001",
  "issueDate": "2026-01-16"
}
```

Beispielanfrage (response_type: pdf für direkten PDF-Download):

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

Beispielantwort (response_type: pdf):

```
HTTP/1.1 200 OK
Content-Type: application/pdf
Content-Disposition: attachment; filename="INV-000001.pdf"
X-Invoice-Number: INV-000001
X-Invoice-Status: issued
X-Invoice-Issue-Date: 2026-01-16

[PDF binary data]
```

Die PDF-Antwort enthält Rechnungsmetadaten in benutzerdefinierten Response-Headern (`X-Invoice-Number`, `X-Invoice-Status`, `X-Invoice-Issue-Date`), sodass Ihre Anwendung die Rechnungsdetails speichern kann, während sie die PDF-Datei direkt erhält.

### Rechnungen auflisten und filtern

`GET /api/v1/invoices`

**Authentifizierung erforderlich** - Bearer-Token im `Authorization`-Header angeben.

- Optionaler Filter: `status=unpaid` oder `status=issued`

Beispielanfrage:

```bash
curl -H "Authorization: Bearer YOUR_API_KEY_HERE" \
     http://localhost:8080/api/v1/invoices?status=unpaid
```

Beispielantwort (gefiltert):

```json
[
  { "invoiceNumber": "INV-000002", "status": "unpaid" }
]
```

### Rechnung abrufen

**Authentifizierung erforderlich** - Bearer-Token im `Authorization`-Header angeben.

Rechnungsmetadaten abrufen:

```bash
curl -H "Authorization: Bearer YOUR_API_KEY_HERE" \
     http://localhost:8080/api/v1/invoices/INV-000001
```

Rechnung als PDF herunterladen:

```bash
curl -H "Authorization: Bearer YOUR_API_KEY_HERE" \
     http://localhost:8080/api/v1/invoices/INV-000001/pdf \
     -o invoice.pdf
```

### Parteien

**Authentifizierung erforderlich** - Bearer-Token im `Authorization`-Header angeben.

- `GET /api/v1/parties/client?taxId=...&vatId=...` (erfordert taxId oder vatId)
- `GET /api/v1/parties/clients/search?q=...` (mindestens 2 Zeichen)
- `GET /api/v1/parties/suppliers`

Beispiel:

```bash
curl -H "Authorization: Bearer YOUR_API_KEY_HERE" \
     http://localhost:8080/api/v1/parties/suppliers
```

### Health

- `GET /api/v1/health` (keine Authentifizierung erforderlich)

## Vorlagen und PDFs

- Vorlagen befinden sich in `invoice_templates` und müssen HTML-Inhalt enthalten.
- Die Rechnungserstellung erfordert eine wirksame Standardvorlage (lieferantenspezifisch oder global).
- Lieferanten können den globalen Standard mit einer lieferantenspezifischen Vorlage überschreiben.
- Erstellte Rechnungen speichern einen HTML-Snapshot für konsistentes Re-Rendering.
- PDFs werden aus dem gespeicherten HTML-Snapshot gerendert, sofern vorhanden.

### Formatanforderungen für Vorlagen

Vorlagen müssen im **XHTML**-Format vorliegen, damit PDFs korrekt gerendert werden:

- XML-Deklaration hinzufügen: `<?xml version="1.0" encoding="UTF-8"?>`
- XHTML-Namespace verwenden: `<html xmlns="http://www.w3.org/1999/xhtml">`
- Sämtliches CSS muss **inline** in einem `<style>`-Tag im `<head>` stehen
- Externe Stylesheets werden für die PDF-Erstellung nicht unterstützt

Beispielvorlagen sind im Verzeichnis `templates/` in 6 Sprachen verfügbar (EN, BG, IT, ES, DE, RU).

### Vorlagenfelder

Vorlagen verwenden `{{placeholder}}`-Variablen. Wenn ein Platzhalter in der Vorlage fehlt, werden diese Daten in der finalen Rechnung nicht gerendert. Der Dienst validiert nicht, welche Platzhalter vorhanden oder fehlend sind.

Unterstützte Platzhalter:

- `{{invoiceNumber}}`
- `{{issueDate}}`
- `{{dueDateRow}}` (komplette `<div>`-Zeile oder leer)
- `{{supplierName}}`
- `{{supplierAddressLine1}}`
- `{{supplierAddressLine2Row}}` (komplette `<div>`-Zeile oder leer)
- `{{supplierCityRegionPostal}}`
- `{{supplierCountry}}`
- `{{supplierTaxIdRow}}` (komplette `<div>`-Zeile oder leer)
- `{{supplierVatIdRow}}` (komplette `<div>`-Zeile oder leer)
- `{{supplierEmailRow}}` (komplette `<div>`-Zeile oder leer)
- `{{supplierPhoneRow}}` (komplette `<div>`-Zeile oder leer)
- `{{clientName}}`
- `{{clientAddressLine1}}`
- `{{clientAddressLine2Row}}` (komplette `<div>`-Zeile oder leer)
- `{{clientCityRegionPostal}}`
- `{{clientCountry}}`
- `{{clientTaxIdRow}}` (komplette `<div>`-Zeile oder leer)
- `{{clientVatIdRow}}` (komplette `<div>`-Zeile oder leer)
- `{{clientEmailRow}}` (komplette `<div>`-Zeile oder leer)
- `{{clientPhoneRow}}` (komplette `<div>`-Zeile oder leer)
- `{{itemsRows}}` (gerenderte `<tr>`-Zeilen)
- `{{subtotal}}`
- `{{discountTotal}}`
- `{{taxTotal}}`
- `{{total}}`
- `{{notesSection}}` (kompletter `<div>`-Abschnitt oder leer)

### PDF-Schriften

PDFs werden mit OpenHTMLToPDF gerendert. Die Anwendung bündelt die Schriftfamilie **DejaVu**, die Latein, Kyrillisch, Griechisch und andere Unicode-Zeichen unterstützt.

**Verwendung der gebündelten Schriften:**

```css
body {
    font-family: "DejaVu Sans", sans-serif;
}
```

**Verwendung benutzerdefinierter Webfonts:**
Vorlagen können externe Fonts über `@font-face` im Inline-`<style>`-Abschnitt laden. Stellen Sie sicher, dass der Webfont Ihre Sprache unterstützt (z. B. Kyrillisch für Russisch/Bulgarisch, Griechisch usw.). Gebündelte DejaVu-Schriften dienen als Fallback, falls der Webfont nicht geladen werden kann.

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

**Verfügbare gebündelte Schriften:**

| Font Family              | Weights                                     | Styles          |
| ------------------------ | ------------------------------------------- | --------------- |
| `DejaVu Sans`            | 200 (extra-light), 400 (normal), 700 (bold) | normal, oblique |
| `DejaVu Sans Condensed`  | 400 (normal), 700 (bold)                    | normal, oblique |
| `DejaVu Sans Mono`       | 400 (normal), 700 (bold)                    | normal, oblique |
| `DejaVu Serif`           | 400 (normal), 700 (bold)                    | normal, italic  |
| `DejaVu Serif Condensed` | 400 (normal), 700 (bold)                    | normal, italic  |

## Parteien und Lokalisierung

- Lieferanten und Kunden teilen sich die Tabelle `parties` und werden über `role` unterschieden.
- Soft Delete blendet Parteien aus Listen aus, während die Rechnungshistorie erhalten bleibt.
- Lieferanteneinstellungen können Locale, Währung, Datumsformat, Rechnungspräfix und Stellenzahl überschreiben.
- Der Lieferantenwert `default_tax_rate` wird auf Positionen ohne Steuersatz angewendet.

## Rechnungsnummerierung und Währung

- Rechnungsnummern sind lieferantenspezifisch und verwenden `max(invoice_number_int) + 1`.
- Optionales Präfix und Stellenzahl werden aus den Lieferanteneinstellungen angewendet.
- Währungscodes werden gegen ISO 4217 validiert.

## Lasttest

Ein Skript ist enthalten, um die API zur Rechnungserstellung mit parallelen Anfragen zu belasten.

Verwendung:

```bash
API_KEY=your_api_key ./integration-tests/gen-test.sh [SUPPLIER_ID] [COUNT] [BASE_URL]
```

Oder den API-Schlüssel als 4. Argument übergeben:

```bash
./integration-tests/gen-test.sh [SUPPLIER_ID] [COUNT] [BASE_URL] [API_KEY]
```

Parameter:

- `SUPPLIER_ID` - Lieferanten-ID für Testrechnungen (Standard: 1)
- `COUNT` - Anzahl paralleler Anfragen für die Rechnungserstellung (Standard: 20)
- `BASE_URL` - Basis-URL der Anwendung (Standard: http://localhost:8080)
- `API_KEY` - Ihr API-Schlüssel (erforderlich, kann als Umgebungsvariable gesetzt werden)

Beispiel:

```bash
API_KEY=abc123-your-key ./integration-tests/gen-test.sh 1 50 http://localhost:8080
```

Dies sendet parallele Anfragen zur Rechnungserstellung, um die pessimistische Sperre und die Gesamtleistung der API unter Last zu testen.

## Frontend-Entwicklung (Tailwind CSS)

Die UI verwendet Tailwind CSS, das bei Änderungen am CSS neu gebaut werden muss.

### Tailwind CSS für Produktion bauen

1. Tailwind CSS Standalone-Binary von [GitHub releases](https://github.com/tailwindlabs/tailwindcss/releases) herunterladen
2. Binary in das Verzeichnis `twbin/` legen (z. B. `twbin/tailwindcss-linux-x64`)
3. Build-Skript ausführen:

   ```bash
   ./build-tailwind.sh
   ```

Dies baut `nullInvoice/src/main/resources/static/css/tailwind.css` aus der Quelldatei `tailwind-src.css`.

### Entwicklungsalternative (CDN)

Für schnelle Entwicklung ohne Rebuild kommentieren Sie das CDN-Skript in `nullInvoice/src/main/resources/templates/fragments/head.html` aus:

```html
<script src="https://cdn.tailwindcss.com"></script>
```

Denken Sie daran, Tailwind CSS vor dem Produktiv-Deployment neu zu bauen.

## Projektstruktur

- `nullInvoice/src/main/java` - Anwendungscode
- `nullInvoice/src/main/resources/templates` - UI-Vorlagen (Thymeleaf)
- `nullInvoice/src/main/resources/static` - JS/CSS-Assets
- `nullInvoice/src/main/resources/db/migration` - Flyway-Migrationen
- `templates/` - Beispiel-Rechnungsvorlagen in 6 Sprachen (EN, BG, IT, ES, DE, RU)
- `integration-tests/` - Lasttest-Skripte für die Rechnungserstellung
- `build-tailwind.sh` - Hilfsskript zum Bauen von Tailwind CSS
- `twbin/` - Tailwind CSS Standalone-Binary (separat herunterladen)

## OpenAPI / Swagger

**Interaktive API-Dokumentation:**

- OpenAPI-JSON-Spezifikation: `/openapi`
- Swagger UI: `/swagger`

**Zugriff auf Swagger UI:**

1. In die Web-UI einloggen
2. Benutzermenü > "API Docs" klicken
3. Oder direkt `/swagger` aufrufen (Login erforderlich)

**Endpunkte in Swagger testen:**

1. Auf den "Authorize"-Button (Schlosssymbol) oben rechts klicken
2. API-Schlüssel eingeben (bei Bedarf unter Admin > API-Schlüssel erzeugen)
3. "Authorize" klicken
4. Alle Anfragen enthalten nun automatisch den Bearer-Token
5. "Try it out" verwenden, um Endpunkte interaktiv zu testen

**Hinweis:** Die Swagger UI erfordert Authentifizierung und ist nur für eingeloggte Admin-Nutzer verfügbar.

## Internationalisierung & Mitwirken

**Unterstützte UI-Sprachen:** Die Anwendung ist vollständig internationalisiert mit Message Bundles:

- Englisch (EN) ✅
- Deutsch (DE) ✅
- Bulgarisch (BG) ✅
- Spanisch (ES) ✅
- Italienisch (IT) ✅
- Russisch (RU) ✅

**README-Übersetzungen:** Dieses `README.md` ist auch verfügbar in:

- [Bulgarisch](README-BG.md)
- [Deutsch](README-DE.md)
- [Spanisch](README-ES.md)
- [Italienisch](README-IT.md)
- [Russisch](README-RU.md)

Maschinelle Übersetzung bereitgestellt von Google Gemini.

**Rechnungsvorlagen:** Beispielvorlagen sind für alle 6 Sprachen oben enthalten (im Verzeichnis `templates/`)

**Technische Unterstützung für weitere Sprachen:**
Die Anwendung kann Rechnungen in **jeder Sprache** mit passender Schriftunterstützung erzeugen:

- **Arabisch** (VAE, Saudi-Arabien usw.) - RTL-Unterstützung per CSS, benötigt Vorlagen & UI-Übersetzungen
- **Ostasiatische Sprachen** (Chinesisch, Japanisch, Koreanisch) - Unicode-Schriften unterstützt, benötigt Vorlagen & UI-Übersetzungen
- **Hebräisch** - RTL-Unterstützung per CSS, benötigt Vorlagen & UI-Übersetzungen
- Jede andere Unicode-basierte Sprache

**Wir freuen uns über Beiträge zu:**

- 🌍 UI-Übersetzungen (`messages_{lang}.properties` in `nullInvoice/src/main/resources/`)
- 📄 Beispiel-Rechnungsvorlagen für Ihre Sprache/Region (`templates/{lang}/`)
- 🎨 Schrift-Empfehlungen für optimales PDF-Rendering in Ihrer Sprache
- 📝 Dokumentationsverbesserungen und Übersetzungen

Die gebündelte Schriftfamilie DejaVu unterstützt umfangreiche Unicode-Abdeckung. Für Sprachen, die spezielle Schriften benötigen, verwenden Sie `@font-face` in Rechnungsvorlagen, um Webfonts zu laden und mit DejaVu als Fallback abzusichern.

## Lizenz

Dieses Projekt ist unter der **Elastic License 2.0** lizenziert - siehe Datei [LICENSE](LICENSE) für Details.

Copyright 2026 nullata

## Lizenzen Dritter

- Font Awesome Free: `nullInvoice/src/main/resources/static/fontawesome-free-7.1.0-web/LICENSE.txt`
- DejaVu Fonts: `nullInvoice/src/main/resources/fonts/LICENSE.txt`
