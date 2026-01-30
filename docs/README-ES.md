# <img src="../nullInvoice/src/main/resources/static/images/logo.svg" alt="logo" width="24"> nullInvoice

**nullInvoice** es un microservicio Spring Boot para la **generación y gestión automatizada de facturas** con **plantillas HTML totalmente personalizables**, diseñado para integrarse con tiendas en línea y plataformas SaaS.

## Resumen

Las empresas usan nullInvoice para la generación de facturas después de finalizar las ventas. Los proveedores se configuran una sola vez desde la interfaz web, luego su aplicación llama a la API REST para generar facturas conformes bajo demanda.

**Cómo funciona:**

1. **Configuración**: Configure proveedores en la UI con datos de la empresa, locale, moneda, tasas de impuestos, branding personalizado y plantillas de facturas
2. **Autenticación**: Genere claves API desde el Panel de administrador para un acceso seguro a la API REST
3. **Integración**: Su tienda en línea/SaaS realiza llamadas autenticadas a `/api/v1/invoices/generate` usando el ID del proveedor
4. **Generación**: Las facturas se crean a partir de plantillas HTML totalmente personalizables y se devuelven como JSON o PDF con metadatos en los encabezados
5. **Entrega**: Su aplicación recibe la factura y puede reenviarla al cliente o almacenarla para sus registros

### Flujo de integración típico

```
┌─────────────────┐
│   Cliente       │
│   finaliza      │
│   la compra     │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────────────┐
│  Su plataforma de tienda/SaaS           │
│  ─────────────────────────────────────  │
│  1. Procesar el pago                    │
│  2. Emitir comprobante digital          │
│     (requerido)                         │
│  3. ¿Cliente solicita factura? ──────┐  │
└──────────────────────────────────────┼──┘
                                       │
                                       │ Llamada API
                                       ▼
                        ┌──────────────────────────────┐
                        │  Servicio nullInvoice        │
                        │  ──────────────────────────  │
                        │  POST /api/v1/invoices/      │
                        │       generate               │
                        │                              │
                        │  - Valida ID del proveedor   │
                        │  - Aplica plantilla          │
                        │  - Guarda snapshot HTML      │
                        │  - Genera PDF                │
                        │  - Devuelve la factura       │
                        └──────────────┬───────────────┘
                                       │
                                       │ Respuesta (JSON o PDF)
                                       ▼
┌──────────────────────────────────────────┐
│  Su plataforma de tienda/SaaS            │
│  ──────────────────────────────────────  │
│  - Recibe metadatos JSON O archivo PDF   │
│  - Guarda el número de factura           │
│    para registro                         │
│  - Envía PDF al cliente por correo       │
└──────────────────────────────────────────┘
```

### Funciones clave

**Plantillas totalmente personalizables**

- Las facturas se basan en plantillas HTML definidas por el usuario con CSS en línea
- Las plantillas admiten más de 30 marcadores de posición para proveedor, cliente y datos financieros
- Las plantillas predeterminadas por proveedor permiten branding distinto por unidad de negocio

**Inmutabilidad de documentos**

- Cada factura almacena un **snapshot del HTML procesado** al momento de la generación
- Los cambios en la plantilla no afectan a las facturas ya generadas
- Las facturas pueden regenerarse de forma consistente desde el snapshot guardado
- Los permisos de la base de datos impiden eliminar registros de facturas (cumplimiento financiero)

**Preparado para multi-tenant**

- Configure múltiples proveedores con ajustes independientes (locale, moneda, numeración, branding)
- Los consumidores de la API especifican el ID del proveedor para generar facturas en distintas entidades de negocio

**Entrega flexible**

- Devuelve metadatos de la factura como JSON para archivo (`response_type: number`)
- Devuelve PDF directamente con metadatos en los encabezados para entrega inmediata (`response_type: pdf`)
- Recupere PDFs más tarde en `/api/v1/invoices/{invoiceNumber}/pdf`

**Documentación OpenAPI**

- Documentación API interactiva en `/swagger`
- Especificación OpenAPI JSON en `/openapi`
- Ejemplos completos de solicitudes/respuestas y funcionalidad "try-it-out"

## Avisos importantes

**⚠️ Aviso de seguridad**

nullInvoice incluye **autenticación integrada** (inicio de sesión de UI basado en sesión + autenticación con clave API para endpoints REST). Si bien esto ofrece seguridad por defecto, la aplicación está pensada para despliegues en redes internas/privadas.

**Despliegue recomendado:**

- Detrás de un firewall o VPN
- Dentro de una red privada accesible solo por sus aplicaciones de confianza
- Con HTTPS/TLS habilitado para todas las conexiones
- Detrás de un reverse proxy con limitación de tasa configurada

**Para despliegues en producción se requieren medidas de seguridad adicionales:**

- Habilitar HTTPS/TLS
- Configurar limitación de tasa a nivel de reverse proxy
- Usar contraseñas de administrador seguras
- Rotar claves API regularmente
- Mantener las claves API en un gestor seguro de secretos (HashiCorp Vault, AWS Secrets Manager, etc.)

Consulte la sección [Seguridad y mejores prácticas](#seguridad-y-mejores-practicas) para una lista completa de producción.

**⚠️ Esta aplicación NO es un instrumento contable.**

nullInvoice es una **tubería de generación de facturas** diseñada para crear, almacenar y entregar documentos de facturas. No:

- Hace seguimiento de pagos o estado de pago más allá de las banderas básicas "unpaid/issued"
- Gestiona cuentas por cobrar o por pagar
- Genera reportes financieros o balances
- Se integra con sistemas contables (libros mayores, diarios, etc.)
- Maneja contabilidad, conciliación o declaraciones fiscales

Para una gestión financiera completa, integre nullInvoice con un sistema contable dedicado. Use este servicio para generar documentos de facturas y luego impórtelos en su software contable para seguimiento y cumplimiento.

## Stack

- Java 21, Spring Boot 3.5.3
- MariaDB + JPA
- Thymeleaf (UI)
- OpenHTMLToPDF (PDFBox)
- OpenAPI en `/openapi`, Swagger UI en `/swagger`

## Requisitos previos

**Para despliegue con Docker (recomendado):**

- Docker
- Docker Compose

**Para desarrollo local:**

- Java 21 (JDK - Eclipse Temurin u OpenJDK)
- Maven 3.9+
- MariaDB 10.5+ (o MySQL 8.0+)
- Binario standalone de Tailwind CSS (para construir CSS - desde [GitHub releases](https://github.com/tailwindlabs/tailwindcss/releases))

## Configuración de la base de datos

**Crear la base de datos:**

```sql
CREATE DATABASE nullinvoice CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

**Crear un usuario de aplicación dedicado con permisos restringidos:**

```sql
CREATE USER 'nullinvoice'@'localhost' IDENTIFIED BY 'your_secure_password';

-- Otorgar permisos para operaciones normales y migraciones de esquema
GRANT SELECT, INSERT, UPDATE ON nullinvoice.* TO 'nullinvoice'@'localhost';
GRANT CREATE, ALTER, INDEX, REFERENCES ON nullinvoice.* TO 'nullinvoice'@'localhost';

FLUSH PRIVILEGES;
```

**Para acceso remoto, ajuste el host:**

```sql
CREATE USER 'nullinvoice'@'%' IDENTIFIED BY 'your_secure_password';
GRANT SELECT, INSERT, UPDATE ON nullinvoice.* TO 'nullinvoice'@'%';
GRANT CREATE, ALTER, INDEX, REFERENCES ON nullinvoice.* TO 'nullinvoice'@'%';
FLUSH PRIVILEGES;
```

**Importante: ¿Por qué estos permisos restringidos?**

Las facturas son **documentos financieros inmutables** que nunca deben eliminarse una vez creadas. El usuario de la aplicación está intencionalmente restringido y no puede:

- `DELETE` - no puede eliminar registros
- `DROP` - no puede eliminar tablas ni la base de datos
- `TRUNCATE` - no puede vaciar tablas
- `GRANT` - no puede otorgar permisos a otros

Esto garantiza la integridad de los datos y el cumplimiento de los requisitos de conservación de registros financieros. El permiso `UPDATE` se concede para cambios de estado (p. ej., marcar facturas como pagadas) y soft delete en registros de partes.

### Gestión del esquema

- El esquema de base de datos se gestiona con migraciones **Flyway**
- Esquema inicial: `nullInvoice/src/main/resources/db/migration/V1__initial_schema.sql`
- El esquema se crea/actualiza automáticamente al iniciar la aplicación
- El modo DDL de Hibernate está configurado en `none` (Flyway gestiona todos los cambios de esquema)

## Configuración

Se proporciona un archivo de configuración de ejemplo en `.env.example`. Cópielo a `.env` y ajuste los valores para su entorno.

Variables de entorno (valores predeterminados entre paréntesis):

- `TZ` - **REQUERIDO** zona horaria del sistema (Europe/Sofia)
- `APP_PORT` (8080)
- `DB_HOST` (localhost)
- `DB_PORT` (3306)
- `DB_USER` (nullinvoice)
- `DB_PASSWORD` (vacío)
- `DB_NAME` (nullinvoice)
- `DB_PARAMS` - **REQUERIDO** parámetros JDBC agregados a la URL de conexión

### **IMPORTANTE: Configuración de zona horaria**

**DEBE configurar la zona horaria en DOS lugares:**

1. **`TZ`** variable de entorno - establece la zona horaria del sistema/aplicación
2. **`serverTimezone`** parámetro en `DB_PARAMS` - establece la zona horaria de la conexión a la base de datos

**Ambos valores DEBEN coincidir con la zona horaria del servidor de base de datos** para que las fechas/horas se interpreten y almacenen correctamente.

Configuración de ejemplo:

```bash
TZ=Europe/Sofia
DB_PARAMS=?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Europe/Sofia
```

Zonas horarias comunes: `UTC`, `Europe/London`, `America/New_York`, `Asia/Tokyo`. Consulte la [lista completa](https://en.wikipedia.org/wiki/List_of_tz_database_time_zones).

**Las zonas horarias desalineadas causarán fechas y marcas de tiempo incorrectas en las facturas.**

### Configuración inicial del administrador

En el primer inicio, la aplicación redirige a `/setup` para crear la cuenta de administrador inicial. Esto es obligatorio antes de acceder a cualquier otra funcionalidad.

**Para restablecer y crear una nueva cuenta de administrador (recuperación de emergencia):**

1. Detenga la aplicación
2. Trunque las tablas `users` y `api_keys` en la base de datos
3. Reinicie la aplicación
4. Complete nuevamente el flujo de `/setup`

**Recomendaciones de seguridad:**

- Use una contraseña fuerte para la cuenta de administrador
- Establezca una pista de contraseña (opcional pero recomendado)
- Genere claves API separadas para distintos entornos (dev, staging, prod)
- Revoque las claves API no utilizadas

## Seguridad y mejores prácticas

**Arquitectura de autenticación:**

- **Acceso a UI:** Autenticación basada en sesión con form login
- **Acceso a API:** Autenticación Bearer token sin estado (claves API)
- **Contraseñas:** BCrypt con 10 rondas
- **Claves API:** Formato UUID, hasheadas con BCrypt, se muestran solo una vez al generarlas

**Duración de la sesión:**

Por defecto, las sesiones de UI expiran después de **30 minutos de inactividad** (predeterminado de Spring Boot/Tomcat). Los usuarios serán automáticamente desconectados y redirigidos a la página de inicio de sesión.

Para personalizar la duración de la sesión, agregue lo siguiente a `nullInvoice/src/main/resources/application.yml`:

```yaml
server:
  servlet:
    session:
      timeout: 60m  # Opciones: 15m, 30m, 1h, 2h, etc.
```

Valores de timeout comunes:
- `15m` - 15 minutos (seguridad más estricta)
- `30m` - 30 minutos (predeterminado)
- `1h` - 1 hora (comodidad para usuarios activos)
- `8h` - 8 horas (extendido para sesiones largas)

**Lista de verificación para producción:**

- Habilitar HTTPS/TLS para todas las conexiones
- Usar contraseñas de administrador fuertes
- Generar claves API separadas por aplicación/entorno
- Implementar detrás de firewall o VPN
- Configurar limitación de tasa en el reverse proxy
- Configurar logging y monitoreo
- Revisar regularmente el uso de claves API (timestamp de último uso)
- Revocar inmediatamente claves API no usadas o comprometidas
- Mantener claves API en variables de entorno, nunca en el código
- Usar un gestor seguro de secretos (HashiCorp Vault, AWS Secrets Manager, etc.)

**Protección CSRF:**

- Habilitada para todos los formularios de UI
- Deshabilitada para endpoints API (auth Bearer sin estado)

**Seguridad en el primer inicio:**

- La aplicación no se puede usar hasta crear una cuenta de administrador vía `/setup`
- La página de configuración solo es accesible cuando no existe un administrador
- Tras la configuración, se requiere inicio de sesión para toda la funcionalidad

## Docker Compose

**Imágenes oficiales de Docker:**
Hay imágenes preconstruidas disponibles en [Docker Hub](https://hub.docker.com). Busque la imagen oficial de nullInvoice para omitir el paso de build.

Construir sin caché:

```bash
docker compose build --no-cache
```

Levantar el stack:

```bash
docker compose up -d
```

Detener el stack:

```bash
docker compose down
```

## Desarrollo local (sin Docker)

### Configuración

1. Asegúrese de tener una instancia de MariaDB en ejecución y cree la base de datos. Ver [Configuración de la base de datos](#configuracion-de-la-base-de-datos)

2. Construir Tailwind CSS (requerido antes del primer inicio):

   ```bash
   ./build-tailwind.sh
   ```

3. Construir el proyecto con Maven:

   ```bash
   cd nullInvoice
   mvn clean package
   ```

4. Iniciar la aplicación:

   ```bash
   java -jar target/nullinvoice-0.0.1-SNAPSHOT.jar
   ```

### Uso de NetBeans IDE

El proyecto fue construido con NetBeans y puede abrirse como proyecto Maven con el plugin de Spring Boot.

**Configurar variables de entorno en NetBeans:**

Opción 1 - Vía IDE:

1. Clic derecho en el proyecto >> Properties
2. Ir a Actions >> Run
3. Establecer variables de entorno: `APP_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`

Opción 2 - Editar directamente:

- Modifique `nbactions.xml` en la raíz del proyecto

## Primeros pasos

### Configuración del primer inicio

En el primer acceso, será redirigido a `/setup` para crear la cuenta de administrador inicial:

1. Navegue a `http://localhost:8080` (o su URL configurada)
2. Será redirigido automáticamente a `/setup`
3. Cree una cuenta de administrador con nombre de usuario, contraseña y pista opcional
4. Después del setup, será redirigido a `/login`

**Iniciar sesión:**

- Acceda a la página de inicio de sesión en `/login`
- Use las credenciales de administrador que creó
- La pista de contraseña está disponible mediante el icono de información (si está configurada)

**Panel de administrador:**
Después de iniciar sesión, acceda al panel de administrador desde el menú de usuario:

- Cambiar contraseña de administrador
- Generar claves API para acceso a la API REST
- Revocar claves API
- Ver uso de claves API (timestamp de último uso)

### Configurar su primer proveedor

**Requisitos previos:**

- Configuración inicial completada (cuenta de administrador creada)
- Inicio de sesión con credenciales de administrador

Antes de poder generar facturas vía API, debe configurar al menos un proveedor en la interfaz web. Los proveedores definen datos de la empresa, locale, moneda, tasas de impuestos, branding personalizado (a través de plantillas) y la numeración de facturas para la generación.

1. Inicie sesión en la interfaz web en `http://localhost:8080` (o su puerto configurado)

2. Vaya a Proveedores

3. Cree un nuevo proveedor con los datos de la empresa

4. Configure locale, moneda y ajustes de impuestos

5. Configure preferencias de numeración de facturas (prefijo, relleno)

6. Anote el ID del proveedor para la integración API

7. Genere una clave API desde Administrador > Claves API para acceso a la API REST

**Para ejemplos de configuración de proveedores, vea `example-images` - `en-us` para un ejemplo de EE. UU. o no UE; y `eu-de` para un ejemplo de la UE.**

Una vez configurado, puede establecer una plantilla XHTML y usar el ID del proveedor (mostrado en el menú Editar proveedor) para realizar llamadas a `/api/v1/invoices/generate`.

## Capacidades

- Administrar proveedores y clientes con soft delete y comprobaciones de unicidad.
- Crear y administrar plantillas de facturas con branding personalizado, con un predeterminado global y predeterminados por proveedor.
- Generar facturas con snapshots HTML guardados en el registro de la factura.
- Renderizar facturas a PDF bajo demanda.
- Buscar y ordenar facturas por número, fecha, cliente, proveedor y estado.
- Gestionar el estado de la factura como `unpaid` o `issued` (pagada/final).

## Ciclo de vida y estado de las facturas

- Los valores de estado son `unpaid` y `issued`. `issued` se considera pagada y final.
- La creación de facturas vía API siempre resulta en estado `issued`, y la API no acepta override de estado.
- La creación de facturas vía UI puede marcar una factura como `unpaid` solo cuando se establece una fecha de vencimiento.
- Las facturas impagas pueden marcarse como `issued` desde la página de detalles de la factura.
- Las facturas emitidas no pueden revertirse a `unpaid`.

## Comportamiento de la UI

- `/invoices/new` crea facturas usando el proveedor seleccionado desde la cookie (si existe).
- El interruptor de impagada está deshabilitado hasta que se proporciona una fecha de vencimiento.
- `/invoices` admite filtrado por proveedor (dropdown) y búsqueda por número, fecha o cliente. La búsqueda por fecha acepta ISO (`YYYY-MM-DD`) o `dd.MM.yyyy`.
- La lista de facturas permite ordenar por estado; ordenar por estado alterna el orden de `unpaid` y `issued`.
- `/invoices/{invoiceNumber}` muestra estado, totales, vista previa HTML guardada y ofrece una acción de una sola vía "Marcar como pagada" para facturas impagas.

## Flujo de trabajo de la UI

1) Proveedores: configure primero los datos del proveedor. El perfil del proveedor define locale, moneda, numeración de facturas y tasa de impuesto predeterminada.
2) Plantillas: cree una plantilla de branding y establezca un predeterminado. Use un predeterminado global o uno por proveedor para sobrescribir el global.
3) Clientes (opcional): puede agregar clientes manualmente, pero la generación de facturas también crea/actualiza clientes a partir de los datos ingresados.
4) Seleccionar proveedor activo: elija un proveedor predeterminado en la UI, lo que establece una cookie usada en la creación de facturas.
5) ID de proveedor para la API: abra un proveedor en modo edición y use el ID mostrado en la esquina superior izquierda.
6) Facturas: liste, busque y filtre facturas; abra una factura para revisar detalles y marcar facturas impagas como emitidas/pagadas.
7) Generar factura: ingrese datos del cliente o busque un cliente existente, agregue partidas y establezca impuestos por partida. Si una partida omite el impuesto, se aplica la tasa predeterminada del proveedor.
8) Descuentos y notas: ingrese un descuento fijo o use la calculadora de % de descuento; agregue notas y genere la factura para ver la página de resumen.

La generación de facturas usa un bloqueo de escritura pesimista en el registro del proveedor para evitar condiciones de carrera al calcular el siguiente número de factura. Esto bloquea solicitudes concurrentes para el mismo proveedor hasta asignar el número.

## Autenticación

**Autenticación requerida:** Todos los endpoints de la API requieren:

- Bearer token en el encabezado `Authorization` (recomendado para integraciones)
- Sesión activa (si inició sesión en la interfaz web)

### Autenticación con clave API (recomendado para integraciones externas)

Genere una clave API en el panel de administrador y agréguela en el encabezado `Authorization`:

```bash
curl -H "Authorization: Bearer YOUR_API_KEY_HERE" \
     http://localhost:8080/api/v1/invoices
```

### Autenticación por sesión (para solicitudes iniciadas desde la UI)

Si inició sesión en la interfaz web, su sesión se usa automáticamente para las solicitudes de la API.

**Generar una clave API:**

1. Inicie sesión en la interfaz web
2. Navegue a Administrador (menú de usuario)
3. Desplácese a la sección "Claves API"
4. Ingrese una descripción opcional y haga clic en "Generar clave"
5. **Copie la clave de inmediato** - no se mostrará nuevamente
6. La clave se muestra en formato `Authorization: Bearer {key}`

**Notas de seguridad:**

- Las claves API se almacenan hasheadas en la base de datos (BCrypt)
- Las claves se pueden revocar en cualquier momento desde el panel de administrador
- Se registra el timestamp de último uso para cada clave
- Genere claves separadas para distintas aplicaciones/entornos

## REST API (Base: `/api/v1`)

### Generación de facturas

`POST /api/v1/invoices/generate`

**Autenticación requerida** - incluya el Bearer token en el encabezado `Authorization`.

- Requiere `supplier_id` y `client`.
- `response_type` admite `number` (predeterminado) o `pdf`.
  - `number`: devuelve JSON solo con metadatos de la factura
  - `pdf`: devuelve el PDF directamente con metadatos en los encabezados de respuesta
- El estado siempre es `issued` para facturas generadas por la API.

Ejemplo de solicitud (cliente existente por id):

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

Ejemplo de solicitud (nuevos datos de cliente):

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

Ejemplo de respuesta (response_type: number):

```json
{
  "status": "issued",
  "message": "invoice generated",
  "invoiceNumber": "INV-000001",
  "issueDate": "2026-01-16"
}
```

Ejemplo de solicitud (response_type: pdf para descarga directa de PDF):

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

Ejemplo de respuesta (response_type: pdf):

```
HTTP/1.1 200 OK
Content-Type: application/pdf
Content-Disposition: attachment; filename="INV-000001.pdf"
X-Invoice-Number: INV-000001
X-Invoice-Status: issued
X-Invoice-Issue-Date: 2026-01-16

[PDF binary data]
```

La respuesta PDF incluye metadatos de la factura en encabezados personalizados (`X-Invoice-Number`, `X-Invoice-Status`, `X-Invoice-Issue-Date`), lo que permite a su aplicación guardar los detalles de la factura mientras recibe el PDF directamente.

### Listado y filtrado de facturas

`GET /api/v1/invoices`

**Autenticación requerida** - incluya el Bearer token en el encabezado `Authorization`.

- Filtro opcional: `status=unpaid` o `status=issued`

Ejemplo de solicitud:

```bash
curl -H "Authorization: Bearer YOUR_API_KEY_HERE" \
     http://localhost:8080/api/v1/invoices?status=unpaid
```

Ejemplo de respuesta (filtrado):

```json
[
  { "invoiceNumber": "INV-000002", "status": "unpaid" }
]
```

### Obtención de factura

**Autenticación requerida** - incluya el Bearer token en el encabezado `Authorization`.

Obtener metadatos de la factura:

```bash
curl -H "Authorization: Bearer YOUR_API_KEY_HERE" \
     http://localhost:8080/api/v1/invoices/INV-000001
```

Descargar PDF de la factura:

```bash
curl -H "Authorization: Bearer YOUR_API_KEY_HERE" \
     http://localhost:8080/api/v1/invoices/INV-000001/pdf \
     -o invoice.pdf
```

### Partes

**Autenticación requerida** - incluya el Bearer token en el encabezado `Authorization`.

- `GET /api/v1/parties/client?taxId=...&vatId=...` (requiere uno de taxId/vatId)
- `GET /api/v1/parties/clients/search?q=...` (mínimo 2 caracteres)
- `GET /api/v1/parties/suppliers`

Ejemplo:

```bash
curl -H "Authorization: Bearer YOUR_API_KEY_HERE" \
     http://localhost:8080/api/v1/parties/suppliers
```

### Salud

- `GET /api/v1/health` (sin autenticación)

## Plantillas y PDFs

- Las plantillas viven en `invoice_templates` y deben incluir contenido HTML.
- La generación de facturas requiere una plantilla predeterminada efectiva (por proveedor o global).
- Los proveedores pueden reemplazar el predeterminado global con un predeterminado específico.
- Las facturas generadas almacenan un snapshot HTML para re-renderizado consistente.
- Los PDFs se renderizan desde el snapshot HTML guardado cuando está disponible.

### Requisitos de formato de la plantilla

Las plantillas deben usar formato **XHTML** para un renderizado correcto del PDF:

- Incluir declaración XML: `<?xml version="1.0" encoding="UTF-8"?>`
- Usar namespace XHTML: `<html xmlns="http://www.w3.org/1999/xhtml">`
- Todo el CSS debe ser **en línea** dentro de un `<style>` en la sección `<head>`
- No se admiten hojas de estilo externas para la generación de PDF

Hay plantillas de ejemplo en el directorio `templates/` en 6 idiomas (EN, BG, IT, ES, DE, RU).

### Campos de la plantilla

Las plantillas usan variables `{{placeholder}}`. Si un marcador no está presente en una plantilla, esos datos no se renderizarán en la factura final. El servicio no valida qué marcadores están presentes o ausentes.

Marcadores compatibles:

- `{{invoiceNumber}}`
- `{{issueDate}}`
- `{{dueDateRow}}` (fila `<div>` completa o vacía)
- `{{supplierName}}`
- `{{supplierAddressLine1}}`
- `{{supplierAddressLine2Row}}` (fila `<div>` completa o vacía)
- `{{supplierCityRegionPostal}}`
- `{{supplierCountry}}`
- `{{supplierTaxIdRow}}` (fila `<div>` completa o vacía)
- `{{supplierVatIdRow}}` (fila `<div>` completa o vacía)
- `{{supplierEmailRow}}` (fila `<div>` completa o vacía)
- `{{supplierPhoneRow}}` (fila `<div>` completa o vacía)
- `{{clientName}}`
- `{{clientAddressLine1}}`
- `{{clientAddressLine2Row}}` (fila `<div>` completa o vacía)
- `{{clientCityRegionPostal}}`
- `{{clientCountry}}`
- `{{clientTaxIdRow}}` (fila `<div>` completa o vacía)
- `{{clientVatIdRow}}` (fila `<div>` completa o vacía)
- `{{clientEmailRow}}` (fila `<div>` completa o vacía)
- `{{clientPhoneRow}}` (fila `<div>` completa o vacía)
- `{{itemsRows}}` (filas `<tr>` renderizadas)
- `{{subtotal}}`
- `{{discountTotal}}`
- `{{taxTotal}}`
- `{{total}}`
- `{{notesSection}}` (sección `<div>` completa o vacía)

### Fuentes PDF

Los PDFs se renderizan con OpenHTMLToPDF. La aplicación incluye la familia de fuentes **DejaVu**, que admite caracteres latinos, cirílicos, griegos y otros Unicode.

**Usar las fuentes incluidas:**

```css
body {
    font-family: "DejaVu Sans", sans-serif;
}
```

**Usar fuentes web personalizadas:**
Las plantillas pueden cargar fuentes externas mediante `@font-face` en el `<style>` en línea. Asegúrese de que la fuente soporte el idioma de su plantilla (p. ej., cirílico para ruso/búlgaro, griego, etc.). Las fuentes DejaVu incluidas sirven como fallback si la fuente web no se carga.

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

**Fuentes incluidas disponibles:**

| Font Family              | Weights                                     | Styles          |
| ------------------------ | ------------------------------------------- | --------------- |
| `DejaVu Sans`            | 200 (extra-light), 400 (normal), 700 (bold) | normal, oblique |
| `DejaVu Sans Condensed`  | 400 (normal), 700 (bold)                    | normal, oblique |
| `DejaVu Sans Mono`       | 400 (normal), 700 (bold)                    | normal, oblique |
| `DejaVu Serif`           | 400 (normal), 700 (bold)                    | normal, italic  |
| `DejaVu Serif Condensed` | 400 (normal), 700 (bold)                    | normal, italic  |

## Partes y localización

- Proveedores y clientes comparten la tabla `parties` y se distinguen por `role`.
- Soft delete oculta partes de las listas mientras preserva el historial de facturas.
- La configuración del proveedor puede sobrescribir locale, moneda, formato de fecha, prefijo de factura y relleno de dígitos.
- `default_tax_rate` del proveedor se aplica a partidas que omiten la tasa de impuesto.

## Numeración de facturas y moneda

- Los números de factura son por proveedor y usan `max(invoice_number_int) + 1`.
- El prefijo opcional y el relleno de dígitos se aplican desde la configuración del proveedor.
- Los códigos de moneda se validan contra ISO 4217.

## Pruebas de carga

Se incluye un script para stress test de la API de generación de facturas con solicitudes concurrentes.

Uso:

```bash
API_KEY=your_api_key ./integration-tests/gen-test.sh [SUPPLIER_ID] [COUNT] [BASE_URL]
```

O pase la clave API como cuarto argumento:

```bash
./integration-tests/gen-test.sh [SUPPLIER_ID] [COUNT] [BASE_URL] [API_KEY]
```

Parámetros:

- `SUPPLIER_ID` - ID del proveedor para facturas de prueba (predeterminado: 1)
- `COUNT` - número de solicitudes concurrentes a generar (predeterminado: 20)
- `BASE_URL` - URL base de la aplicación (predeterminado: http://localhost:8080)
- `API_KEY` - su clave API (requerida, puede establecerse como variable de entorno)

Ejemplo:

```bash
API_KEY=abc123-your-key ./integration-tests/gen-test.sh 1 50 http://localhost:8080
```

Esto dispara solicitudes concurrentes de generación de facturas para probar el bloqueo pesimista y el rendimiento general de la API bajo carga.

## Desarrollo frontend (Tailwind CSS)

La UI utiliza Tailwind CSS, que debe reconstruirse cuando se realizan cambios en el CSS.

### Construir Tailwind CSS para producción

1. Descargue el binario standalone de Tailwind CSS desde [GitHub releases](https://github.com/tailwindlabs/tailwindcss/releases)
2. Coloque el binario en el directorio `twbin/` (p. ej., `twbin/tailwindcss-linux-x64`)
3. Ejecute el script de build:

   ```bash
   ./build-tailwind.sh
   ```

Esto reconstruye `nullInvoice/src/main/resources/static/css/tailwind.css` desde el archivo fuente `tailwind-src.css`.

### Alternativa de desarrollo (CDN)

Para desarrollo rápido sin reconstruir Tailwind, descomente el script CDN en `nullInvoice/src/main/resources/templates/fragments/head.html`:

```html
<script src="https://cdn.tailwindcss.com"></script>
```

Recuerde reconstruir Tailwind CSS antes de desplegar en producción.

## Estructura del proyecto

- `nullInvoice/src/main/java` - código de la aplicación
- `nullInvoice/src/main/resources/templates` - plantillas UI (Thymeleaf)
- `nullInvoice/src/main/resources/static` - assets JS/CSS
- `nullInvoice/src/main/resources/db/migration` - migraciones Flyway
- `templates/` - plantillas de facturas de ejemplo en 6 idiomas (EN, BG, IT, ES, DE, RU)
- `integration-tests/` - scripts de pruebas de carga para generación de facturas
- `build-tailwind.sh` - script de conveniencia para construir Tailwind CSS
- `twbin/` - binario standalone de Tailwind CSS (descargar por separado)

## OpenAPI / Swagger

**Documentación API interactiva:**

- Especificación OpenAPI JSON: `/openapi`
- Swagger UI: `/swagger`

**Acceso a Swagger UI:**

1. Inicie sesión en la interfaz web
2. Haga clic en el menú de usuario > "Documentación API"
3. O navegue directamente a `/swagger` (requiere inicio de sesión)

**Probar endpoints en Swagger:**

1. Haga clic en "Authorize" (icono de candado) en la parte superior derecha
2. Ingrese su clave API (genérela en Administrador > Claves API si es necesario)
3. Haga clic en "Authorize"
4. Todas las solicitudes incluirán el Bearer token automáticamente
5. Use "Try it out" para probar endpoints de forma interactiva

**Nota:** Swagger UI requiere autenticación y solo está disponible para administradores con sesión iniciada.

## Internacionalización y contribuciones

**Idiomas de UI compatibles:** La aplicación está completamente internacionalizada con bundles de mensajes:

- Inglés (EN) ✅
- Búlgaro (BG) ✅
- Alemán (DE) ✅
- Español (ES) ✅
- Italiano (IT) ✅
- Ruso (RU) ✅

**Traducciones del README:** Este `README.md` también está disponible en:

- [Búlgaro](README-BG.md)
- [Alemán](README-DE.md)
- [Español](README-ES.md)
- [Italiano](README-IT.md)
- [Ruso](README-RU.md)

Traducción automática proporcionada por Google Gemini.

**Plantillas de facturas:** Se incluyen plantillas de ejemplo para los 6 idiomas anteriores (en el directorio `templates/`)

**Soporte técnico para idiomas adicionales:**
La aplicación puede generar facturas en **cualquier idioma** con el soporte de fuentes adecuado:

- **Árabe** (EAU, Arabia Saudita, etc.) - soporte RTL vía CSS, se requieren plantillas y traducciones UI
- **Asia oriental** (chino, japonés, coreano) - fuentes Unicode compatibles, se requieren plantillas y traducciones UI
- **Hebreo** - soporte RTL vía CSS, se requieren plantillas y traducciones UI
- Cualquier otro idioma basado en Unicode

**Agradecemos contribuciones para:**

- 🌍 Traducciones UI (`messages_{lang}.properties` en `nullInvoice/src/main/resources/`)
- 📄 Plantillas de facturas de ejemplo para su idioma/región (`templates/{lang}/`)
- 🎨 Recomendaciones de fuentes para un renderizado PDF óptimo en su idioma
- 📝 Mejoras y traducciones de documentación

La familia de fuentes DejaVu incluida proporciona amplia cobertura Unicode. Para idiomas que requieren fuentes específicas, use `@font-face` en plantillas de facturas para cargar webfonts con fallback a DejaVu.

## Licencia

Este proyecto está licenciado bajo la **Elastic License 2.0** - consulte el archivo [LICENSE](../LICENSE) para más detalles.

Copyright 2026 nullata

## Licencias de terceros

- Font Awesome Free: `nullInvoice/src/main/resources/static/fontawesome-free-7.1.0-web/LICENSE.txt`
- DejaVu Fonts: `nullInvoice/src/main/resources/fonts/LICENSE.txt`
