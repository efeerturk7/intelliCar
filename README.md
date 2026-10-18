# 🚗 intelliCar - AI-Ready & Event-Driven Car Marketplace API

![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Containerized-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Apache Kafka](https://img.shields.io/badge/Apache_Kafka-Event_Driven-231F20?style=for-the-badge&logo=apachekafka&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-Caching-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-JWT_Auth-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)

**intelliCar**, ikinci el araç alım-satım ekosistemi için tasarlanmış; asenkron olay güdümlü (event-driven) mimariye sahip, yüksek performanslı önbellekleme kullanan ve yakında eklenecek **Spring AI** danışmanı ile akıllı değerleme sunacak kurumsal bir REST API platformudur.

Platform; **Docker Compose** üzerinde izole PostgreSQL ve Redis katmanları, **Apache Kafka** ile dağıtık asenkron iş akışları, işlem güvenliği (transaction synchronization) ve modern **Spring Security 6 (JWT & Refresh Token Rotation)** mekanizmaları ile production-ready standartlarda kurgulanmıştır.

---

## 🏗 Sistem Mimarisi & Akış Şeması

```text
[ İstemci (Swagger / Frontend) ]
              │
              ▼
   [ JWT Filtresi & Güvenlik ]
              │
              ▼
  [ RestCarController / Offer Service ] ────(Okuma İsteği)───► [ Redis Cache ]
              │                                                     │ (Miss)
              │ (Veri Kaydı)                                        ▼
              ├──────────────────────────────────────────────► [ PostgreSQL ]
              │
              ▼ (Transaction Başarılıysa / afterCommit)
     [ KafkaEventPublisher ]
              │
              ▼
       [ Apache Kafka ]
        ├── offer-events
        ├── car-view-events
        └── car-status-events
              │
              ├────────► [ CarStatusConsumer ] ──────► Bekleyen Teklifleri İptal Et (EXPIRED)
              ├────────► [ CarViewConsumer ]   ──────► İlan Görüntülenme Sayacını Artır (+1)
              └────────► [ OfferNotificationConsumer ] ► Asenkron Alıcı/Satıcı Bildirimleri
```

---

## 🚀 Öne Çıkan Teknik Yetkinlikler

### 1. 📨 Asenkron Olay Güdümlü Mimari (Apache Kafka)
Sistemdeki ağır iş yüklerini ve durum güncellemelerini ana HTTP thread'ini bloklamadan çözmek üzere Apache Kafka entegre edilmiştir:

* **Transaction-Aware Event Publishing:** `KafkaEventPublisher`, Spring'in `TransactionSynchronizationManager` mekanizmasını dinler. Bir ilan veya teklif veritabanına başarıyla yazılıp transaction `commit` edilmeden Kafka'ya mesaj gönderilmez (`afterCommit`). Böylece veritabanı rollback olursa hayalet (phantom) event'lerin kuyruğa düşmesi engellenir.
* **Akıllı Teklif Yaşam Döngüsü (Status Cascade):** Bir araç satıldığında veya pasife alındığında (`SOLD`, `INACTIVE`), `CarStatusConsumer` devreye girerek bu ilana ait askıdaki tüm bekleyen teklifleri (`PENDING`) topluca zaman aşımına uğratır (`EXPIRED`).
* **Sayaç ve İzleme Ayrımı (Decoupled Analytics):** Kullanıcı ilan detayına girdiğinde HTTP yanıtı anında döner; görüntülenme sayacı `CarViewConsumer` tarafından asenkron olarak arka planda PostgreSQL üzerinde artırılır.
* **Bildirim Servisi (Notification Consumer):** Yeni bir teklif verildiğinde veya satıcı teklife yanıt döndüğünde (`ACCEPTED`, `REJECTED`), alıcı ve satıcı bildirimleri `offer-events` topic'i üzerinden asenkron yönetilir.

### 2. ⚡ Önbellekleme Stratejisi (Redis Caching)
* **Okuma Optimizasyonu (`@Cacheable`):** En sık sorgulanan araç detayları ve ilan filtreleme verileri RAM üzerinde önbelleğe alınarak PostgreSQL üzerindeki I/O yükü minimize edilir.
* **Cache Invalidation (`@CacheEvict`):** İlan güncellendiğinde veya silindiğinde ilgili Redis anahtarları otomatik olarak temizlenerek veri tutarsızlığı (stale data) engellenir.
* **JSON Serileştirme:** `GenericJackson2JsonRedisSerializer` kullanılarak veriler okunabilir JSON formatında tutulur.

### 3. 🛡️ Güvenlik ve Kimlik Doğrulama
* **Stateless JWT & Refresh Token Rotation:** Kısa ömürlü Access Token ve veritabanı destekli Refresh Token ile güvenli oturum yönetimi sağlanır.
* **Korumalı Uç Noktalar:** Yetkisiz ilan güncelleme/silme girişimleri servis seviyesinde doğrulanır; satıcı yalnızca kendi ilanları üzerinde değişiklik yapabilir (`@AuthenticationPrincipal`).

### 4. 🐳 Konteynerizasyon (Docker & Docker Compose)
* **Tek Komutla Dağıtım:** PostgreSQL ve Redis servisleri `docker-compose.yml` aracılığıyla izole bir bridge network üzerinde ayağa kaldırılır.
* **Kalıcı Depolama:** `postgres_data` ve `redis_data` volume'leri ile konteyner silinse dahi veri kaybı önlenir.

---

## 🔮 Gelecek Yol Haritası: Spring AI Entegrasyonu (Roadmap)

`intelliCar`, yakın zamanda **Spring AI** entegrasyonu ile zenginleştirilerek akıllı bir danışman katmanına kavuşacaktır:

1. **Doğal Dille Araç Arama (Semantic Smart Search):**
   * Kullanıcılar karmaşık filtre kutularıyla uğraşmak yerine serbest metinle arama yapabilecektir.
   * *Örnek Girdi:* `"Elimde 900 bin TL bütçe var, az yakan, otomatik vites bir aile aracı öner."`
   * Spring AI Function Calling / Structured Output modelleri bu cümleyi parse ederek sistemdeki `CarFilterRequest` kriterlerine dönüştürecektir.
2. **Akıllı Piyasa Değerleme Asistanı (Smart Price Valuation):**
   * Satıcı aracının teknik verilerini (model, yıl, kilometre, hasar durumu) girdiğinde sistem, RAG (Retrieval-Augmented Generation) yaklaşımıyla veri tabanındaki emsal ilanları tarayacaktır.
   * Satıcıya piyasa trendlerine uygun gerçekçi bir fiyat aralığı (örneğin *1.150.000 TL - 1.200.000 TL*) tavsiye edilecektir.

---

## 🛠️ Teknoloji Yığını (Tech Stack)

| Kategori | Teknoloji | Açıklama |
| :--- | :--- | :--- |
| **Backend Core** | Java 17, Spring Boot 3.x | RESTful API, Service & Repository Mimarisi |
| **Mesajlaşma & Kuyruk** | Apache Kafka, Spring Kafka | Asenkron olay akışı ve bildirimler |
| **Önbellek (Cache)** | Redis 7 | Sık okunan veriler için yüksek hızlı önbellekleme |
| **Veritabanı & ORM** | PostgreSQL 15, Spring Data JPA / Hibernate | İlişkisel veri modeli ve transaction yönetimi |
| **Güvenlik** | Spring Security 6, JWT | Token tabanlı kimlik doğrulama & yetkilendirme |
| **Yapay Zeka (Upcoming)** | Spring AI | LLM entegrasyonu, semantik arama ve değerleme |
| **Altyapı & Test** | Docker, Docker Compose, Swagger UI / OpenAPI | Servis orkestrasyonu ve API test dokümantasyonu |

---

## ⚙️ Yerel Ortamda Çalıştırma (Local Setup)

### 1. Depoyu Klonlayın
```bash
git clone https://github.com/efeerturk7/intelliCar.git
cd intelliCar
```

### 2. Altyapı Servislerini (PostgreSQL & Redis) Başlatın
```bash
docker compose up -d postgres redis
```

### 3. Uygulamayı Çalıştırın
IDE üzerinden `IntelliCarApplication.java` dosyasını çalıştırabilir veya terminalden derleyebilirsiniz:
```bash
./mvnw clean spring-boot:run
```

### 4. API Dokümantasyonuna Erişin
Tarayıcınızdan Swagger UI arayüzüne gidin:
```text
http://localhost:8080/swagger-ui/index.html
```

---

### 👨‍💻 Geliştirici
**Bahadır Efe ERTÜRK** - Backend Developer

[![LinkedIn](https://img.shields.io/badge/LinkedIn-Connect-0077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/efeerturk7/)
[![GitHub](https://img.shields.io/badge/GitHub-Follow-100000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/efeerturk7)