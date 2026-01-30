# <img src="../nullInvoice/src/main/resources/static/images/logo.svg" alt="logo" width="24"> nullInvoice

**nullInvoice** è un microservizio Spring Boot per la **generazione e gestione automatizzata delle fatture** con **modelli HTML completamente personalizzabili**, progettato per l'integrazione con webstore e piattaforme SaaS.

## Panoramica

Le aziende usano nullInvoice per gestire la generazione delle fatture dopo che le vendite sono state completate. I fornitori vengono configurati una sola volta tramite la UI web, poi la sua applicazione chiama la REST API per generare fatture conformi on-demand.

**Come funziona:**

1. **Configurazione**: Configuri i fornitori nella UI con dati aziendali, locale, valuta, aliquote fiscali, branding personalizzato e modelli di fattura
2. **Autenticazione**: Generi chiavi API dalla Dashboard amministratore per un accesso sicuro alla REST API
3. **Integrazione**: Il suo webstore/SaaS effettua chiamate API autenticate a `/api/v1/invoices/generate` usando l'ID del fornitore
4. **Generazione**: Le fatture vengono create da modelli HTML completamente personalizzabili e restituite come JSON o PDF con metadati negli header
5. **Consegna**: La sua applicazione riceve la fattura e può inoltrarla ai clienti o conservarla per archivio

### Flusso di integrazione tipico

```
┌─────────────────┐
│   Cliente       │
│   completa      │
│   l'acquisto    │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────────────┐
│  La sua piattaforma Webstore/SaaS       │
│  ─────────────────────────────────────  │
│  1. Elabora il pagamento                │
│  2. Emette ricevuta digitale            │
│     (obbligatoria)                      │
│  3. Il cliente richiede fattura? ────┐  │
└──────────────────────────────────────┼──┘
                                       │
                                       │ Chiamata API
                                       ▼
                        ┌──────────────────────────────┐
                        │  Servizio nullInvoice        │
                        │  ──────────────────────────  │
                        │  POST /api/v1/invoices/      │
                        │       generate               │
                        │                              │
                        │  - Valida ID fornitore       │
                        │  - Applica modello           │
                        │  - Salva snapshot HTML       │
                        │  - Genera PDF                │
                        │  - Restituisce la fattura    │
                        └──────────────┬───────────────┘
                                       │
                                       │ Risposta (JSON o PDF)
                                       ▼
┌──────────────────────────────────────────┐
│  La sua piattaforma Webstore/SaaS        │
│  ──────────────────────────────────────  │
│  - Riceve metadati JSON O file PDF       │
│  - Salva il numero fattura per archivio  │
│  - Invia il PDF al cliente via email     │
└──────────────────────────────────────────┘
```

### Funzionalità principali

**Modelli completamente personalizzabili**

- Le fatture si basano su modelli HTML definiti dall'utente con CSS inline
- I modelli supportano oltre 30 placeholder per dati di fornitore, cliente e finanziari
- I modelli predefiniti per fornitore consentono branding diverso per unità di business

**Immutabilità dei documenti**

- Ogni fattura memorizza uno **snapshot dell'HTML parsato** al momento della generazione
- Le modifiche ai modelli non influenzano le fatture già generate
- Le fatture possono essere rigenerate in modo coerente dallo snapshot salvato
- Le autorizzazioni del database impediscono l'eliminazione dei record delle fatture (compliance finanziaria)

**Pronto per multi-tenant**

- Configuri più fornitori con impostazioni indipendenti (locale, valuta, numerazione, branding)
- I chiamanti API specificano l'ID del fornitore per generare fatture per diverse entità aziendali

**Consegna flessibile**

- Restituisce i metadati della fattura come JSON per archiviazione (`response_type: number`)
- Restituisce il PDF direttamente con metadati negli header per consegna immediata (`response_type: pdf`)
- Recupero PDF successivo tramite `/api/v1/invoices/{invoiceNumber}/pdf`

**Documentazione OpenAPI**

- Documentazione API interattiva disponibile su `/swagger`
- Specifica OpenAPI JSON su `/openapi`
- Esempi completi di request/response e funzionalità try-it-out

## Avvisi importanti

**⚠️ Avviso di sicurezza**

nullInvoice include **autenticazione integrata** (login UI basato su sessione + autenticazione con chiave API per gli endpoint REST). Anche se offre sicurezza di default, l'applicazione è comunque destinata a deployment in reti interne/private.

**Deploy consigliato:**

- Dietro un firewall o VPN
- All'interno di una rete privata accessibile solo dalle sue applicazioni fidate
- Con HTTPS/TLS abilitato per tutte le connessioni
- Dietro un reverse proxy con rate limiting configurato

**Per i deploy in produzione sono necessarie misure di sicurezza aggiuntive:**

- Abilitare HTTPS/TLS
- Configurare il rate limiting a livello di reverse proxy
- Usare password amministratore robuste
- Ruotare regolarmente le chiavi API
- Conservare le chiavi API in un sistema sicuro di gestione segreti (HashiCorp Vault, AWS Secrets Manager, ecc.)

Veda la sezione [Sicurezza e best practice](#sicurezza-e-best-practice) per una checklist completa di produzione.

**⚠️ Questa applicazione NON è uno strumento contabile.**

nullInvoice è una **pipeline di generazione fatture** progettata per creare, archiviare e consegnare documenti di fattura. Non:

- Traccia pagamenti o stati di pagamento oltre i flag base "unpaid/issued"
- Gestisce crediti o debiti
- Genera report finanziari o bilanci
- Si integra con sistemi contabili (registri, giornali, ecc.)
- Gestisce contabilità, riconciliazioni o dichiarazioni fiscali

Per una gestione finanziaria completa, integri nullInvoice con un sistema contabile dedicato. Usi questo servizio per generare i documenti di fattura, poi li importi nel suo software contabile per tracciamento e compliance.

## Stack

- Java 21, Spring Boot 3.5.3
- MariaDB + JPA
- Thymeleaf (UI)
- OpenHTMLToPDF (PDFBox)
- OpenAPI su `/openapi`, Swagger UI su `/swagger`

## Prerequisiti

**Per deployment Docker (consigliato):**

- Docker
- Docker Compose

**Per sviluppo locale:**

- Java 21 (JDK - Eclipse Temurin o OpenJDK)
- Maven 3.9+
- MariaDB 10.5+ (o MySQL 8.0+)
- Binario standalone di Tailwind CSS (per build CSS - da [GitHub releases](https://github.com/tailwindlabs/tailwindcss/releases))

## Configurazione database

**Creare il database:**

```sql
CREATE DATABASE nullinvoice CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

**Creare un utente applicazione dedicato con permessi limitati:**

```sql
CREATE USER 'nullinvoice'@'localhost' IDENTIFIED BY 'your_secure_password';

-- Conceda i permessi per operazioni normali e migrazioni dello schema
GRANT SELECT, INSERT, UPDATE ON nullinvoice.* TO 'nullinvoice'@'localhost';
GRANT CREATE, ALTER, INDEX, REFERENCES ON nullinvoice.* TO 'nullinvoice'@'localhost';

FLUSH PRIVILEGES;
```

**Per accesso remoto, regoli l'host:**

```sql
CREATE USER 'nullinvoice'@'%' IDENTIFIED BY 'your_secure_password';
GRANT SELECT, INSERT, UPDATE ON nullinvoice.* TO 'nullinvoice'@'%';
GRANT CREATE, ALTER, INDEX, REFERENCES ON nullinvoice.* TO 'nullinvoice'@'%';
FLUSH PRIVILEGES;
```

**Importante: perché questi permessi limitati?**

Le fatture sono **documenti finanziari immutabili** che non devono mai essere eliminate dopo la creazione. L'utente dell'applicazione è intenzionalmente limitato e non può:

- `DELETE` - non può eliminare record
- `DROP` - non può eliminare tabelle o database
- `TRUNCATE` - non può svuotare tabelle
- `GRANT` - non può concedere permessi ad altri

Questo garantisce l'integrità dei dati e la conformità ai requisiti di conservazione dei record finanziari. Il permesso `UPDATE` è concesso per i cambi di stato (es. segnare fatture come pagate) e soft delete sui record delle parti.

### Gestione dello schema

- Lo schema del database è gestito da migrazioni **Flyway**
- Schema iniziale: `nullInvoice/src/main/resources/db/migration/V1__initial_schema.sql`
- Lo schema viene creato/aggiornato automaticamente all'avvio dell'applicazione
- La modalità DDL di Hibernate è impostata su `none` (Flyway gestisce tutti i cambi di schema)

## Configurazione

Un file di configurazione di esempio è fornito in `.env.example`. Lo copi in `.env` e adatti i valori per il suo ambiente.

Variabili d'ambiente (valori predefiniti tra parentesi):

- `TZ` - **RICHIESTO** fuso orario di sistema (Europe/Sofia)
- `APP_PORT` (8080)
- `DB_HOST` (localhost)
- `DB_PORT` (3306)
- `DB_USER` (nullinvoice)
- `DB_PASSWORD` (vuoto)
- `DB_NAME` (nullinvoice)
- `DB_PARAMS` - **RICHIESTO** parametri JDBC aggiunti all'URL di connessione

### **IMPORTANTE: Configurazione fuso orario**

**DEVE impostare il fuso orario in DUE punti:**

1. **`TZ`** variabile d'ambiente - imposta il fuso orario di sistema/applicazione
2. **`serverTimezone`** parametro in `DB_PARAMS` - imposta il fuso orario della connessione al database

**Entrambi i valori DEVONO corrispondere al fuso orario del server database** per interpretare e salvare correttamente date e orari.

Esempio di configurazione:

```bash
TZ=Europe/Sofia
DB_PARAMS=?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Europe/Sofia
```

Fusi orari comuni: `UTC`, `Europe/London`, `America/New_York`, `Asia/Tokyo`. Veda [la lista completa](https://en.wikipedia.org/wiki/List_of_tz_database_time_zones).

**Fusi orari non allineati causeranno date e timestamp errati nelle fatture.**

### Configurazione iniziale amministratore

Al primo avvio, l'applicazione reindirizza a `/setup` per creare l'account amministratore iniziale. Questo è obbligatorio prima di accedere a qualsiasi altra funzionalità.

**Per reimpostare e creare un nuovo account amministratore (recupero di emergenza):**

1. Arresti l'applicazione
2. Trunchi le tabelle `users` e `api_keys` nel database
3. Riavvii l'applicazione
4. Completi nuovamente il flusso `/setup`

**Raccomandazioni di sicurezza:**

- Usi una password forte per l'account amministratore
- Imposti un suggerimento password (opzionale ma consigliato)
- Generi chiavi API separate per diversi ambienti (dev, staging, prod)
- Revochi le chiavi API inutilizzate

## Sicurezza e best practice

**Architettura di autenticazione:**

- **Accesso UI:** Autenticazione basata su sessione con form login
- **Accesso API:** Autenticazione Bearer token stateless (chiavi API)
- **Password:** BCrypt con 10 round
- **Chiavi API:** Formato UUID, hashate con BCrypt, mostrate una sola volta alla generazione

**Durata della sessione:**

Di default, le sessioni UI scadono dopo **30 minuti di inattività** (default di Spring Boot/Tomcat). Gli utenti verranno automaticamente disconnessi e reindirizzati alla pagina di login.

Per personalizzare la durata della sessione, aggiunga il seguente codice a `nullInvoice/src/main/resources/application.yml`:

```yaml
server:
  servlet:
    session:
      timeout: 60m  # Opzioni: 15m, 30m, 1h, 2h, ecc.
```

Valori di timeout comuni:
- `15m` - 15 minuti (sicurezza più rigorosa)
- `30m` - 30 minuti (predefinito)
- `1h` - 1 ora (comodità per utenti attivi)
- `8h` - 8 ore (esteso per sessioni lunghe)

**Checklist per la produzione:**

- Abilitare HTTPS/TLS per tutte le connessioni
- Usare password amministratore robuste
- Generare chiavi API separate per ogni applicazione/ambiente
- Deploy dietro firewall o VPN
- Configurare il rate limiting a livello di reverse proxy
- Configurare logging e monitoraggio
- Rivedere regolarmente l'uso delle chiavi API (timestamp dell'ultimo utilizzo)
- Revocare immediatamente chiavi API inutilizzate o compromesse
- Conservare le chiavi API nelle variabili d'ambiente, mai nel codice
- Usare un sistema sicuro di gestione segreti (HashiCorp Vault, AWS Secrets Manager, ecc.)

**Protezione CSRF:**

- Abilitata per tutti i form UI
- Disabilitata per gli endpoint API (auth Bearer stateless)

**Sicurezza al primo avvio:**

- L'applicazione è inutilizzabile finché non viene creato un account amministratore tramite `/setup`
- La pagina di setup è accessibile solo quando non esiste un amministratore
- Dopo il setup è richiesto il login per tutte le funzionalità

## Docker Compose

**Immagini Docker ufficiali:**
Sono disponibili immagini precompilate su [Docker Hub](https://hub.docker.com). Cerchi l'immagine ufficiale di nullInvoice per saltare il build.

Build senza cache:

```bash
docker compose build --no-cache
```

Avvii lo stack:

```bash
docker compose up -d
```

Arresti lo stack:

```bash
docker compose down
```

## Sviluppo locale (senza Docker)

### Configurazione

1. Si assicuri di avere un'istanza MariaDB in esecuzione e crei il database. Veda [Configurazione database](#configurazione-database)

2. Build di Tailwind CSS (necessario prima del primo avvio):

   ```bash
   ./build-tailwind.sh
   ```

3. Build del progetto con Maven:

   ```bash
   cd nullInvoice
   mvn clean package
   ```

4. Avvii l'applicazione:

   ```bash
   java -jar target/nullinvoice-0.0.1-SNAPSHOT.jar
   ```

### Uso di NetBeans IDE

Il progetto è stato costruito con NetBeans e può essere aperto come progetto Maven con il plugin Spring Boot.

**Impostare le variabili d'ambiente in NetBeans:**

Opzione 1 - Tramite IDE:

1. Clic destro sul progetto >> Properties
2. Navigare in Actions >> Run
3. Impostare le variabili d'ambiente: `APP_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`

Opzione 2 - Modifica diretta:

- Modifichi `nbactions.xml` nella root del progetto

## Per iniziare

### Configurazione al primo avvio

Al primo accesso verrà reindirizzato a `/setup` per creare l'account amministratore iniziale:

1. Apra `http://localhost:8080` (o l'URL configurato)
2. Verrà reindirizzato automaticamente a `/setup`
3. Crei un account amministratore con nome utente, password e suggerimento opzionale
4. Dopo il setup verrà reindirizzato a `/login`

**Accedi:**

- Acceda alla pagina di login su `/login`
- Usi le credenziali amministratore che ha creato
- Il suggerimento password è disponibile tramite l'icona info (se configurato)

**Dashboard amministratore:**
Dopo il login, acceda alla dashboard amministratore dal menu utente:

- Cambiare la password amministratore
- Generare chiavi API per l'accesso alla REST API
- Revocare chiavi API
- Visualizzare l'uso delle chiavi API (timestamp dell'ultimo utilizzo)

### Configurare il primo fornitore

**Prerequisiti:**

- Configurazione iniziale completata (account amministratore creato)
- Login con credenziali amministratore

Prima di poter generare fatture via API, deve configurare almeno un fornitore tramite la UI web. I fornitori definiscono dati aziendali, locale, valuta, aliquote fiscali, branding personalizzato (tramite modelli) e numerazione fatture per la generazione.

1. Effettui il login nella UI web su `http://localhost:8080` (o la porta configurata)

2. Vada a Fornitori

3. Crei un nuovo fornitore con i dati aziendali

4. Configuri locale, valuta e impostazioni fiscali

5. Imposti le preferenze di numerazione fatture (prefisso, padding)

6. Annoti l'ID fornitore per l'integrazione API

7. Generi una chiave API da Amministratore > Chiavi API per l'accesso alla REST API

**Per esempi di configurazione del fornitore, veda `example-images` - `en-us` per un esempio USA o non UE; e `eu-de` per un esempio UE.**

Una volta configurato, può impostare un modello XHTML e usare l'ID fornitore (mostrato nel menu Modifica fornitore) per effettuare chiamate a `/api/v1/invoices/generate`.

## Capacità

- Gestire fornitori e clienti con soft delete e controlli di unicità.
- Creare e gestire modelli di fattura con branding personalizzato, con un predefinito globale e predefiniti per fornitore.
- Generare fatture con snapshot HTML salvati nel record della fattura.
- Renderizzare fatture in PDF su richiesta.
- Cercare e ordinare fatture per numero, data, cliente, fornitore e stato.
- Gestire lo stato della fattura come `unpaid` o `issued` (pagata/finale).

## Ciclo di vita e stato delle fatture

- I valori di stato sono `unpaid` e `issued`. `issued` è considerato pagata e finale.
- La creazione di una fattura via API produce sempre lo stato `issued` e l'API non accetta override dello stato.
- La creazione via UI può contrassegnare una fattura come `unpaid` solo quando è impostata una data di scadenza.
- Le fatture non pagate possono essere contrassegnate come `issued` dalla pagina dettagli fattura.
- Le fatture emesse non possono essere riportate a `unpaid`.

## Comportamento UI

- `/invoices/new` crea fatture usando il fornitore selezionato dal cookie (se presente).
- Il toggle per `unpaid` è disabilitato finché non viene fornita una data di scadenza.
- `/invoices` supporta il filtro per fornitore (dropdown) e la ricerca per numero, data o cliente. La ricerca per data accetta ISO (`YYYY-MM-DD`) o `dd.MM.yyyy`.
- L'elenco fatture include l'ordinamento per stato; l'ordinamento per stato alterna `unpaid` e `issued`.
- `/invoices/{invoiceNumber}` mostra stato, totali, anteprima HTML salvata e offre un'azione unidirezionale "Segna come pagata" per le fatture non pagate.

## Flusso di lavoro UI

1) Fornitori: configuri prima i dati del fornitore. Il profilo fornitore guida locale, valuta, numerazione fatture e aliquota fiscale predefinita.
2) Modelli: crei un modello per il branding personalizzato e imposti un predefinito. Usi un predefinito globale o uno specifico per fornitore per sovrascrivere la scelta globale.
3) Clienti (opzionale): può aggiungere clienti manualmente, ma la generazione fatture crea/aggiorna anche i clienti dai dati inseriti.
4) Selezioni il fornitore attivo: scelga un fornitore predefinito nella UI, che imposta un cookie usato durante la creazione delle fatture.
5) ID fornitore per API: apra un fornitore in modalità modifica e usi l'ID mostrato nell'angolo in alto a sinistra della pagina.
6) Fatture: elenchi, cerchi e filtri le fatture; apra una fattura per rivedere i dettagli e contrassegnare le fatture non pagate come emesse/pagate.
7) Generare fattura: inserisca i dati del cliente o cerchi un cliente esistente, aggiunga voci e imposti l'imposta per voce. Se una voce omette l'imposta, si applica l'aliquota predefinita del fornitore.
8) Sconti e note: inserisca uno sconto fisso o usi il calcolatore % di sconto; aggiunga note e generi la fattura per vedere la pagina di riepilogo.

La generazione delle fatture usa un lock di scrittura pessimista sul record del fornitore per evitare race condition nel calcolo del prossimo numero fattura. Questo blocca richieste concorrenti per lo stesso fornitore fino all'assegnazione del numero.

## Autenticazione

**Autenticazione richiesta:** Tutti gli endpoint API richiedono:

- Bearer token nell'header `Authorization` (consigliato per le integrazioni)
- Sessione attiva (se è loggato tramite UI web)

### Autenticazione con chiave API (consigliata per integrazioni esterne)

Generi una chiave API dalla dashboard amministratore e la includa nell'header `Authorization`:

```bash
curl -H "Authorization: Bearer YOUR_API_KEY_HERE" \
     http://localhost:8080/api/v1/invoices
```

### Autenticazione di sessione (per richieste avviate dalla UI)

Se è loggato tramite la UI web, la sua sessione viene usata automaticamente per le richieste API.

**Generare una chiave API:**

1. Effettui il login nella UI web
2. Navighi a Amministratore (menu utente)
3. Scorra alla sezione "Chiavi API"
4. Inserisca una descrizione opzionale e clicchi "Genera chiave"
5. **Copi immediatamente la chiave** - non verrà mostrata di nuovo
6. La chiave viene mostrata nel formato `Authorization: Bearer {key}`

**Note sulla sicurezza:**

- Le chiavi API sono hashate nel database (BCrypt)
- Le chiavi possono essere revocate in qualsiasi momento dalla dashboard amministratore
- Per ogni chiave è tracciato il timestamp dell'ultimo utilizzo
- Generi chiavi separate per diverse applicazioni/ambienti

## REST API (Base: `/api/v1`)

### Generazione fatture

`POST /api/v1/invoices/generate`

**Autenticazione richiesta** - includa il Bearer token nell'header `Authorization`.

- Richiede `supplier_id` e `client`.
- `response_type` supporta `number` (predefinito) o `pdf`.
  - `number`: restituisce JSON solo con metadati della fattura
  - `pdf`: restituisce il PDF direttamente con metadati negli header di risposta
- Lo stato è sempre `issued` per le fatture generate via API.

Esempio di richiesta (cliente esistente per id):

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

Esempio di richiesta (nuovi dati cliente):

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

Esempio di risposta (response_type: number):

```json
{
  "status": "issued",
  "message": "invoice generated",
  "invoiceNumber": "INV-000001",
  "issueDate": "2026-01-16"
}
```

Esempio di richiesta (response_type: pdf per download diretto del PDF):

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

Esempio di risposta (response_type: pdf):

```
HTTP/1.1 200 OK
Content-Type: application/pdf
Content-Disposition: attachment; filename="INV-000001.pdf"
X-Invoice-Number: INV-000001
X-Invoice-Status: issued
X-Invoice-Issue-Date: 2026-01-16

[PDF binary data]
```

La risposta PDF include i metadati della fattura negli header di risposta personalizzati (`X-Invoice-Number`, `X-Invoice-Status`, `X-Invoice-Issue-Date`), consentendo alla sua applicazione di salvare i dettagli della fattura mentre riceve il PDF direttamente.

### Elenco e filtro fatture

`GET /api/v1/invoices`

**Autenticazione richiesta** - includa il Bearer token nell'header `Authorization`.

- Filtro opzionale: `status=unpaid` o `status=issued`

Esempio di richiesta:

```bash
curl -H "Authorization: Bearer YOUR_API_KEY_HERE" \
     http://localhost:8080/api/v1/invoices?status=unpaid
```

Esempio di risposta (filtrato):

```json
[
  { "invoiceNumber": "INV-000002", "status": "unpaid" }
]
```

### Recupero fattura

**Autenticazione richiesta** - includa il Bearer token nell'header `Authorization`.

Recupero metadati fattura:

```bash
curl -H "Authorization: Bearer YOUR_API_KEY_HERE" \
     http://localhost:8080/api/v1/invoices/INV-000001
```

Download PDF fattura:

```bash
curl -H "Authorization: Bearer YOUR_API_KEY_HERE" \
     http://localhost:8080/api/v1/invoices/INV-000001/pdf \
     -o invoice.pdf
```

### Parti

**Autenticazione richiesta** - includa il Bearer token nell'header `Authorization`.

- `GET /api/v1/parties/client?taxId=...&vatId=...` (richiede uno tra taxId/vatId)
- `GET /api/v1/parties/clients/search?q=...` (minimo 2 caratteri)
- `GET /api/v1/parties/suppliers`

Esempio:

```bash
curl -H "Authorization: Bearer YOUR_API_KEY_HERE" \
     http://localhost:8080/api/v1/parties/suppliers
```

### Health

- `GET /api/v1/health` (nessuna autenticazione richiesta)

## Modelli e PDF

- I modelli si trovano in `invoice_templates` e devono includere contenuto HTML.
- La generazione delle fatture richiede un modello predefinito effettivo (specifico per fornitore o globale).
- I fornitori possono sovrascrivere il predefinito globale con un modello specifico.
- Le fatture generate memorizzano uno snapshot HTML per un re-render coerente.
- I PDF vengono renderizzati dallo snapshot HTML salvato quando disponibile.

### Requisiti del formato del modello

I modelli devono essere in formato **XHTML** per un rendering PDF corretto:

- Includere la dichiarazione XML: `<?xml version="1.0" encoding="UTF-8"?>`
- Usare il namespace XHTML: `<html xmlns="http://www.w3.org/1999/xhtml">`
- Tutto il CSS deve essere **inline** dentro un tag `<style>` nella sezione `<head>`
- I fogli di stile esterni non sono supportati per la generazione PDF

Sono disponibili modelli di esempio nella directory `templates/` in 6 lingue (EN, BG, IT, ES, DE, RU).

### Campi del modello

I modelli usano variabili `{{placeholder}}`. Se un placeholder è omesso dal modello, quei dati non verranno renderizzati nella fattura finale. Il servizio non valida quali placeholder siano presenti o mancanti.

Placeholder supportati:

- `{{invoiceNumber}}`
- `{{issueDate}}`
- `{{dueDateRow}}` (riga `<div>` completa o vuota)
- `{{supplierName}}`
- `{{supplierAddressLine1}}`
- `{{supplierAddressLine2Row}}` (riga `<div>` completa o vuota)
- `{{supplierCityRegionPostal}}`
- `{{supplierCountry}}`
- `{{supplierTaxIdRow}}` (riga `<div>` completa o vuota)
- `{{supplierVatIdRow}}` (riga `<div>` completa o vuota)
- `{{supplierEmailRow}}` (riga `<div>` completa o vuota)
- `{{supplierPhoneRow}}` (riga `<div>` completa o vuota)
- `{{clientName}}`
- `{{clientAddressLine1}}`
- `{{clientAddressLine2Row}}` (riga `<div>` completa o vuota)
- `{{clientCityRegionPostal}}`
- `{{clientCountry}}`
- `{{clientTaxIdRow}}` (riga `<div>` completa o vuota)
- `{{clientVatIdRow}}` (riga `<div>` completa o vuota)
- `{{clientEmailRow}}` (riga `<div>` completa o vuota)
- `{{clientPhoneRow}}` (riga `<div>` completa o vuota)
- `{{itemsRows}}` (righe `<tr>` renderizzate)
- `{{subtotal}}`
- `{{discountTotal}}`
- `{{taxTotal}}`
- `{{total}}`
- `{{notesSection}}` (sezione `<div>` completa o vuota)

### Font PDF

I PDF sono renderizzati con OpenHTMLToPDF. L'applicazione include la famiglia di font **DejaVu**, che supporta latino, cirillico, greco e altri caratteri Unicode.

**Usare i font inclusi:**

```css
body {
    font-family: "DejaVu Sans", sans-serif;
}
```

**Usare font web personalizzati:**
I modelli possono caricare font esterni tramite `@font-face` nella sezione `<style>` inline. Si assicuri che il font web supporti la lingua del modello (es. cirillico per russo/bulgaro, greco, ecc.). I font DejaVu inclusi fungono da fallback se il font web non si carica.

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

**Font inclusi disponibili:**

| Font Family              | Weights                                     | Styles          |
| ------------------------ | ------------------------------------------- | --------------- |
| `DejaVu Sans`            | 200 (extra-light), 400 (normal), 700 (bold) | normal, oblique |
| `DejaVu Sans Condensed`  | 400 (normal), 700 (bold)                    | normal, oblique |
| `DejaVu Sans Mono`       | 400 (normal), 700 (bold)                    | normal, oblique |
| `DejaVu Serif`           | 400 (normal), 700 (bold)                    | normal, italic  |
| `DejaVu Serif Condensed` | 400 (normal), 700 (bold)                    | normal, italic  |

## Parti e localizzazione

- Fornitori e clienti condividono la tabella `parties` e sono distinti da `role`.
- Il soft delete nasconde le parti dagli elenchi preservando la cronologia delle fatture.
- Le impostazioni del fornitore possono sovrascrivere locale, valuta, formato data, prefisso fattura e padding delle cifre.
- `default_tax_rate` del fornitore si applica alle voci che omettono l'aliquota.

## Numerazione fatture e valuta

- I numeri di fattura sono per fornitore e usano `max(invoice_number_int) + 1`.
- Il prefisso opzionale e il padding delle cifre sono applicati dalle impostazioni del fornitore.
- I codici valuta sono validati contro ISO 4217.

## Test di carico

È incluso uno script per stress test dell'API di generazione fatture con richieste concorrenti.

Uso:

```bash
API_KEY=your_api_key ./integration-tests/gen-test.sh [SUPPLIER_ID] [COUNT] [BASE_URL]
```

Oppure passi la chiave API come quarto argomento:

```bash
./integration-tests/gen-test.sh [SUPPLIER_ID] [COUNT] [BASE_URL] [API_KEY]
```

Parametri:

- `SUPPLIER_ID` - ID fornitore per fatture di test (predefinito: 1)
- `COUNT` - numero di richieste concorrenti da generare (predefinito: 20)
- `BASE_URL` - URL base dell'applicazione (predefinito: http://localhost:8080)
- `API_KEY` - la sua chiave API (richiesta, può essere impostata come variabile d'ambiente)

Esempio:

```bash
API_KEY=abc123-your-key ./integration-tests/gen-test.sh 1 50 http://localhost:8080
```

Questo invia richieste concorrenti di generazione fatture per testare il lock pessimista e le prestazioni complessive dell'API sotto carico.

## Sviluppo frontend (Tailwind CSS)

La UI usa Tailwind CSS, che deve essere ricostruito quando vengono apportate modifiche al CSS.

### Build Tailwind CSS per la produzione

1. Scarichi il binario standalone di Tailwind CSS da [GitHub releases](https://github.com/tailwindlabs/tailwindcss/releases)
2. Inserisca il binario nella directory `twbin/` (es. `twbin/tailwindcss-linux-x64`)
3. Esegua lo script di build:

   ```bash
   ./build-tailwind.sh
   ```

Questo ricostruisce `nullInvoice/src/main/resources/static/css/tailwind.css` dal file sorgente `tailwind-src.css`.

### Alternativa di sviluppo (CDN)

Per sviluppo rapido senza rebuild, decommenti lo script CDN in `nullInvoice/src/main/resources/templates/fragments/head.html`:

```html
<script src="https://cdn.tailwindcss.com"></script>
```

Ricordi di ricostruire Tailwind CSS prima del deploy in produzione.

## Struttura del progetto

- `nullInvoice/src/main/java` - codice applicativo
- `nullInvoice/src/main/resources/templates` - modelli UI (Thymeleaf)
- `nullInvoice/src/main/resources/static` - asset JS/CSS
- `nullInvoice/src/main/resources/db/migration` - migrazioni Flyway
- `templates/` - modelli di fattura di esempio in 6 lingue (EN, BG, IT, ES, DE, RU)
- `integration-tests/` - script di test di carico per la generazione fatture
- `build-tailwind.sh` - script di comodo per buildare Tailwind CSS
- `twbin/` - binario standalone di Tailwind CSS (scaricare separatamente)

## OpenAPI / Swagger

**Documentazione API interattiva:**

- Specifica OpenAPI JSON: `/openapi`
- Swagger UI: `/swagger`

**Accesso alla Swagger UI:**

1. Effettui il login nella UI web
2. Clicchi sul menu utente > "Documentazione API"
3. Oppure navighi direttamente su `/swagger` (richiede login)

**Testare gli endpoint in Swagger:**

1. Clicchi il pulsante "Authorize" (icona lucchetto) in alto a destra
2. Inserisca la sua chiave API (generata da Amministratore > Chiavi API, se necessario)
3. Clicchi "Authorize"
4. Tutte le richieste includeranno automaticamente il Bearer token
5. Usi "Try it out" per testare gli endpoint in modo interattivo

**Nota:** Swagger UI richiede autenticazione ed è disponibile solo per amministratori loggati.

## Internazionalizzazione e contributi

**Lingue UI supportate:** L'applicazione è completamente internazionalizzata con message bundle:

- Inglese (EN) ✅
- Bulgaro (BG) ✅
- Tedesco (DE) ✅
- Spagnolo (ES) ✅
- Italiano (IT) ✅
- Russo (RU) ✅

**Traduzioni README:** Questo `README.md` è disponibile anche in:

- [Bulgaro](README-BG.md)
- [Tedesco](README-DE.md)
- [Spagnolo](README-ES.md)
- [Italiano](README-IT.md)
- [Russo](README-RU.md)

Traduzione automatica fornita da Google Gemini.

**Modelli di fattura:** Modelli di esempio inclusi per tutte le 6 lingue sopra (nella directory `templates/`)

**Supporto tecnico per lingue aggiuntive:**
L'applicazione può generare fatture in **qualsiasi lingua** con un adeguato supporto di font:

- **Arabo** (EAU, Arabia Saudita, ecc.) - supporto RTL tramite CSS, servono modelli e traduzioni UI
- **Asia orientale** (cinese, giapponese, coreano) - font Unicode supportati, servono modelli e traduzioni UI
- **Ebraico** - supporto RTL tramite CSS, servono modelli e traduzioni UI
- Qualsiasi altra lingua basata su Unicode

**Accogliamo contributi per:**

- 🌍 Traduzioni UI (`messages_{lang}.properties` in `nullInvoice/src/main/resources/`)
- 📄 Modelli di fattura di esempio per la sua lingua/regione (`templates/{lang}/`)
- 🎨 Raccomandazioni di font per un rendering PDF ottimale nella sua lingua
- 📝 Miglioramenti e traduzioni della documentazione

La famiglia di font DejaVu inclusa supporta ampia copertura Unicode. Per lingue che richiedono font specifici, usi `@font-face` nei modelli di fattura per caricare webfont con fallback a DejaVu.

## Licenza

Questo progetto è rilasciato sotto **Elastic License 2.0** - veda il file [LICENSE](../LICENSE) per i dettagli.

Copyright 2026 nullata

## Licenze di terze parti

- Font Awesome Free: `nullInvoice/src/main/resources/static/fontawesome-free-7.1.0-web/LICENSE.txt`
- DejaVu Fonts: `nullInvoice/src/main/resources/fonts/LICENSE.txt`
