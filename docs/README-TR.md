![nullInvoice logosu](../docs/nullinvoice.png)

# <img src="../nullInvoice/src/main/resources/static/images/logo.svg" alt="logo" width="24"> nullInvoice

**nullInvoice**, e-ticaret mağazaları ve SaaS platformları ile entegrasyon için tasarlanmış, **tamamen özelleştirilebilir HTML şablonlarına** sahip, **otomatik fatura oluşturma ve yönetimi** için geliştirilmiş bir Spring Boot mikroservisidir.

> **📖 Genişletilmiş Dokümantasyon:** Dağıtım, API referansı, şablonlar, yapılandırma ve geliştirme hakkında ayrıntılı kılavuzlar için aşağıdaki [Genişletilmiş Dokümantasyon](extended/) bölümüne bakın.

## Genel Bakış

İşletmeler, satışlar tamamlandıktan sonra fatura kesme işlemlerini yönetmek için nullInvoice'u kullanır. Tedarikçiler web arayüzü üzerinden bir kez yapılandırılır, ardından uygulamanız isteğe bağlı olarak mevzuata uygun faturaları otomatik olarak oluşturmak için REST API'yi çağırır.

**Nasıl çalışır:**

1. **Kurulum**: Tedarikçileri arayüzde şirket detayları, dil/bölge (locale), para birimi, vergi oranları, özel markalama ve fatura şablonları ile yapılandırın
2. **Kimlik Doğrulama**: Güvenli REST API erişimi için Yönetici kontrol panelinden API anahtarları oluşturun
3. **Entegrasyon**: E-ticaret mağazanız veya SaaS uygulamanız, tedarikçi kimliğini (supplier_id) kullanarak `/api/v1/invoices/generate` adresine kimliği doğrulanmış API çağrıları yapar
4. **Oluşturma**: Faturalar tamamen özelleştirilebilir HTML şablonlarından oluşturulur ve üst bilgi üstverileriyle (metadata) birlikte JSON veya PDF olarak döndürülür
5. **Teslimat**: Uygulamanız faturayı alır ve müşterilere iletebilir veya kayıtları için saklayabilir

### Tipik Entegrasyon Akışı

```
┌─────────────────┐
│   Müşteri       │
│   satın almayı  │
│   tamamlar      │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────────────┐
│  E-Ticaret / SaaS Platformunuz          │
│  ─────────────────────────────────────  │
│  1. Ödemeyi işle                        │
│  2. Dijital makbuz düzenle (zorunlu)   │
│  3. Müşteri fatura talep etti mi? ────┐ │
└──────────────────────────────────────┼──┘
                                       │
                                       │ API Çağrısı
                                       ▼
                        ┌──────────────────────────────┐
                        │  nullInvoice Servisi         │
                        │  ──────────────────────────  │
                        │  POST /api/v1/invoices/      │
                        │       generate               │
                        │                              │
                        │  - Tedarikçi ID doğrular     │
                        │  - Özel şablonu uygular      │
                        │  - HTML anlık görüntüsünü    │
                        │    saklar                    │
                        │  - PDF oluşturur             │
                        │  - Faturayı döndürür         │
                        └──────────────┬───────────────┘
                                       │
                                       │ Yanıt (JSON veya PDF)
                                       ▼
┌──────────────────────────────────────────┐
│  E-Ticaret / SaaS Platformunuz           │
│  ──────────────────────────────────────  │
│  - JSON üstverisini VEYA PDF dosyasını   │
│    alır                                  │
│  - Fatura numarasını kayıtlar için       │
│    saklar                                │
│  - PDF'i e-posta ile müşteriye iletir    │
└──────────────────────────────────────────┘
```

## Temel Özellikler

- **Tamamen Özelleştirilebilir Şablonlar** - Satır içi CSS ve 30'dan fazla yer tutucu içeren HTML şablonları
- **Doküman Değişmezliği** - HTML anlık görüntüleri (snapshots), geriye dönük değişiklikleri önler (finansal uyumluluk)
- **Çoklu Kiracıya Uygun (Multi-tenant)** - Bağımsız ayarlara sahip birden fazla tedarikçi
- **Esnek Teslimat** - JSON üstverisi veya doğrudan PDF döndürme
- **Asenkron Oluşturma Kuyruğu** - Yüksek işlem hacimli entegrasyonlar için durum sorgulamalı isteğe bağlı kuyruk yapısı
- **OpenAPI Dokümantasyonu** - `/swagger` adresinde etkileşimli API dokümantasyonu

## Hızlı Başlangıç

### Önkoşullar

**Docker dağıtımı için (önerilen):**
- Docker
- Docker Compose

**Yerel geliştirme için:**
- Java 21 (JDK - Eclipse Temurin veya OpenJDK)
- Maven 3.9+
- MariaDB 10.5+ (veya MySQL 8.0+)

### Docker Dağıtımı

Hazır derlenmiş görüntüler [Docker Hub](https://hub.docker.com/r/nullata/nullinvoice) üzerinde mevcuttur.

```bash
docker compose up -d
```

Eksiksiz kurulum talimatları için [Dağıtım Kılavuzu](extended/DEPLOYMENT.md) belgesine bakın.

### Yapılandırma

Temel çevre değişkenleri (environment variables):

| Değişken | Zorunlu | Açıklama |
|----------|----------|-------------|
| `TZ` | **Evet** | Sistem saat dilimi (örn. `Europe/Istanbul`) |
| `DB_HOST` | **Evet** | Veritabanı sunucusu |
| `DB_USER` | **Evet** | Veritabanı kullanıcı adı |
| `DB_PASSWORD` | **Evet** | Veritabanı şifresi |
| `DB_NAME` | **Evet** | Veritabanı adı |
| `DB_PARAMS` | **Evet** | `serverTimezone` dahil JDBC parametreleri |

**Örnek:**
```bash
TZ=Europe/Istanbul
DB_HOST=localhost
DB_USER=nullinvoice
DB_PASSWORD=guvenli_sifreniz
DB_NAME=nullinvoice
DB_PARAMS=?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Europe/Istanbul
```

Tüm seçenekler için [Yapılandırma Referansı](extended/CONFIGURATION.md) belgesine bakın.

### İlk Çalıştırma

`http://localhost:8080` adresine ilk erişimde:

1. Yönetici hesabını oluşturmak için `/setup` sayfasına yönlendirileceksiniz
2. Kurulumdan sonra `/login` sayfasından giriş yapın
3. Web arayüzünde ilk tedarikçinizi yapılandırın
4. Yönetim > API Anahtarları bölümünden bir API anahtarı oluşturun
5. API üzerinden fatura oluşturmaya başlayın!

## API Hızlı Örnek

Bir fatura oluşturun ve PDF olarak indirin:

```bash
curl -X POST http://localhost:8080/api/v1/invoices/generate \
  -H "Authorization: Bearer API_ANAHTARINIZ" \
  -H "Content-Type: application/json" \
  -d '{
    "response_type": "pdf",
    "supplier_id": 1,
    "client": {
      "name": "Örnek Şirket Ltd.",
      "addressLine1": "Atatürk Cad. No: 100",
      "city": "İstanbul",
      "country": "TR"
    },
    "items": [
      {"description": "Danışmanlık Hizmeti", "quantity": 1, "unit_price": 1000, "tax_rate": 0.20}
    ]
  }' -o fatura.pdf
```

Tam dokümantasyon için [API Referansı](extended/API.md) belgesine bakın.

## Önemli Uyarılar

### ⚠️ Güvenlik Uyarısı

nullInvoice **yerleşik kimlik doğrulama** içerir (oturum tabanlı kullanıcı arayüzü girişi + REST uç noktaları için API anahtarı kimlik doğrulaması). Uygulama **iç/özel ağ dağıtımı** için tasarlanmıştır.

**Önerilen dağıtım:**
- Güvenlik duvarı veya VPN arkasında
- Yalnızca güvenilir uygulamalar tarafından erişilebilen özel bir ağ içinde
- Tüm bağlantılar için HTTPS/TLS etkinleştirilmiş olarak
- Hız sınırlaması (rate limiting) yapılandırılmış bir ters proxy (reverse proxy) arkasında

Canlı ortam kontrol listesi için [Yapılandırma > Güvenlik](extended/CONFIGURATION.md#security--best-practices) bölümüne bakın.

### ⚠️ Muhasebe Yazılımı Değildir

nullInvoice, fatura belgeleri oluşturmak, saklamak ve teslim etmek için tasarlanmış bir **fatura oluşturma hattıdır**. Aşağıdaki işlemleri yapmaz:

- Temel "ödenmedi/düzenlendi" durumlarının ötesinde ödemeleri veya ödeme durumlarını takip etmez
- Alacak veya borç hesaplarını yönetmez
- Finansal raporlar veya bilançolar oluşturmaz
- Muhasebe sistemleriyle (defter-i kebir, yevmiye vb.) entegre olmaz
- Defter tutma, mutabakat veya vergi beyanı işlemlerini yürütmez

Kapsamlı finansal yönetim için nullInvoice'u özel bir muhasebe yazılımı ile entegre edin.

## Genişletilmiş Dokümantasyon

Konulara göre düzenlenmiş ayrıntılı kılavuzlar:

- **[Dağıtım Kılavuzu](extended/DEPLOYMENT.md)** - Veritabanı kurulumu, Docker, yerel geliştirme, ilk çalıştırma yapılandırması
- **[API Referansı](extended/API.md)** - Örneklerle eksiksiz REST API dokümantasyonu
- **[Şablon Özelleştirme](extended/TEMPLATES.md)** - HTML şablonları, yer tutucular, yazı tipleri, PDF dönüştürme
- **[Yapılandırma](extended/CONFIGURATION.md)** - Ortam değişkenleri, güvenlik, oturum yönetimi
- **[Geliştirme Kılavuzu](extended/DEVELOPMENT.md)** - Katkıda bulunma, derleme, test etme, uluslararasılaştırma

## Teknoloji Yığını

- Java 21, Spring Boot 3.5.3
- MariaDB + JPA
- Thymeleaf (Kullanıcı Arayüzü)
- OpenHTMLToPDF (PDFBox)
- `/openapi` adresinde OpenAPI, `/swagger` adresinde Swagger UI

## Uluslararasılaştırma

**Desteklenen Arayüz Dilleri:** İngilizce, Bulgarca, Almanca, İspanyolca, İtalyanca, Rusça, Türkçe

**README Çevirileri:**
- [Bulgarca](README-BG.md)
- [Almanca](README-DE.md)
- [İspanyolca](README-ES.md)
- [İtalyanca](README-IT.md)
- [Rusça](README-RU.md)
- [Türkçe](README-TR.md)

**Fatura Şablonları:** 7 dilin tümü için örnek şablonlar [`templates/`](../templates/) dizininde mevcuttur

## Lisans

Bu proje **Elastic License 2.0** kapsamında lisanslanmıştır - detaylar için [LICENSE](../LICENSE) dosyasına bakın.

Telif Hakkı 2026 nullata
