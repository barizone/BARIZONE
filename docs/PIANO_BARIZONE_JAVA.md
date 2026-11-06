# Barizone: riscrittura del backend in Java/Spring Boot e revisione UX mobile

Documento di progetto · 6 novembre 2026 · basato sull'analisi di `bari-hub-main.zip`

> **Stato di attuazione (aggiornato a fine lavoro).** Le fasi 1-7 sono realizzate. Rispetto allo stack proposto:
> - **Realizzato come previsto:** Java 21, Spring Boot 4.1.2, Maven, JPA/Hibernate, PostgreSQL, Flyway, Spring Security, springdoc, Actuator/Prometheus, Caffeine (configurata ma poco usata), web-push + BouncyCastle, Docker, CI GitHub Actions.
> - **Semplificato:** MapStruct → metodi statici `from()` nei DTO (pochi mapping, nessuna dipendenza in più); Resilience4j → timeout espliciti sul client HTTP e ultimo dato valido per il meteo; Bucket4j → limite di login persistito in DB, come nella versione Worker; Testcontainers e WireMock → PostgreSQL reale (servizio nella CI) e gateway simulati nei test, perché nell'ambiente di sviluppo Docker non era disponibile.
> - **Solo predisposto:** storage S3/R2 per le foto (c'è l'interfaccia `MediaStorage`, implementata solo su DB); ricerca full-text e tutte le nuove idee della sezione 5.
> - **Non fatto:** consolidamento delle dipendenze duplicate del frontend (axios/fetch, dayjs/date-fns, swr/react-query) e font Google ospitati in locale.
> - **Frontend:** rimossi 41 componenti UI mai usati; misure prima/dopo in `docs/ARCHITETTURA.md`.

---

## 1. Cosa c'è oggi (analisi verificata del codice)

Il repository contiene **due versioni**. Il README principale lo dichiara: è attiva solo `barizone-sites/`. `frontend/` e `backend/` (FastAPI + MongoDB) sono la versione precedente e non vengono pubblicate.

### Versione attiva: `barizone-sites/`

| Livello | Tecnologia | Note |
|---|---|---|
| Runtime | Cloudflare Worker (JavaScript, ESM) | 15 moduli in `server/` |
| Database | Cloudflare D1 (SQLite) | migrazioni Drizzle in `drizzle/` |
| Frontend | React 19 + Vite + Tailwind + shadcn/ui | servito come asset statico dallo stesso Worker |
| Deploy | GitHub Actions su runner self-hosted → `wrangler deploy` | ambienti `staging` e `production` |

### Funzioni realmente operative (da conservare 1:1)

| Area | Endpoint | Cosa fa |
|---|---|---|
| Autenticazione admin | `POST /api/auth/login`, `POST /api/auth/logout`, `GET /api/auth/me` | Due modalità: account in DB (PBKDF2-SHA256, 100.000 iterazioni, sessione opaca con hash SHA-256) oppure password singola da secret (`ADMIN_PASSWORD` + JWT HS256). Blocco dopo 10 tentativi in 15 minuti per coppia username+IP. Cookie `__Host-barizone_session`, HttpOnly, Secure, SameSite=Lax, 12 ore. |
| Notizie | `GET /api/news`, `/api/news/featured`, `/api/news/{id}`, `/api/news/status`; admin `GET/POST/PUT /api/admin/news` | Bozze e pubblicazione. Quando un articolo pubblicato cambia, la versione precedente viene archiviata. Le 17 notizie "editoriali" sono scritte direttamente nel codice (`editorial.js`). Regole di selezione per edizione, quartiere e scadenza (`expires_at`); dopo 72 ore dall'ultima edizione l'ordine ruota ogni giorno. |
| Aziende | `GET /api/merchants`, `/api/merchants/{id}`, `/api/merchants/spotlight`; admin `/api/admin/merchants[/{id}[/status]]` | Scheda completa (P.IVA, PEC, sede legale…), stati `draft/published/suspended/archived`. Ogni giorno, con fuso orario Europe/Rome, un'azienda diversa è "in vetrina", a rotazione persistita in DB. |
| Media | `POST /api/admin/media`, `GET /api/media/{id}` | Foto JPG/PNG/WebP fino a 1 MB, con controllo dei magic bytes. Salvate come BLOB in D1. Il pubblico vede solo le foto collegate ad aziende pubblicate. |
| Iscritti news | `POST /api/subscribers`, `/api/subscribers/unsubscribe`, `GET /api/subscribers/config`, admin `GET /api/admin/subscribers` | Consenso versionato (`barizone-updates-v1`), honeypot anti-bot (campo `website`), disiscrizione con token. |
| Email | API **Resend** (`/emails/batch`) | Due email per ogni iscrizione: conferma all'iscritto e avviso alla redazione. Chiave di idempotenza, timeout 8 s. Attiva solo con `NEWS_EMAIL_ENABLED=true`. |
| Notifiche push | `/api/push/key`, `/api/push/subscription` (POST/DELETE), `/api/push/status`; admin `/api/admin/push`, `/test`, `/{id}`, `/{id}/send` | Web Push con chiavi **VAPID** generate e salvate in DB. Accetta solo endpoint dei servizi FCM, Mozilla, Apple e Windows. Consenso marketing versionato. Campagne con committente, contratto, importo e pagamento: una campagna sponsorizzata parte solo con pagamento registrato. Invio a lotti di 25 con deduplica delle consegne. Le sottoscrizioni che rispondono 404/410 vengono rimosse. |
| Gruppi esercenti | `/api/groups/*` | L'admin genera un link di accesso monouso per l'esercente (valido 12 ore). I clienti aderiscono a un'attività (cookie anonimo, 30 giorni). L'esercente invia comunicazioni agli aderenti via push, a lotti di 5. |
| Contatti | `GET /api/contacts`, `PUT /api/admin/contacts` | Numero WhatsApp pubblicabile e link `wa.me`. |
| SEO | `/sitemap.xml`, `/robots.txt` | Sitemap generata dal DB. |
| Sicurezza trasversale | — | Controllo dell'header `Origin` su ogni richiesta che modifica dati (protezione CSRF). Cookie `__Host-`. `Cache-Control: no-store` sulle API. |

### Servizi esterni da mantenere

| Servizio | Dove | Uso |
|---|---|---|
| Resend API | backend | email transazionali |
| Web Push (FCM, Mozilla, Apple, WNS) | backend | notifiche |
| Open-Meteo | frontend | meteo attuale e previsioni a 7 giorni per Bari |
| YouTube-nocookie / Vimeo (`dnt=1`) | frontend | video nelle notizie |
| Google Maps (solo link di ricerca, senza chiave API) | frontend | indirizzi delle aziende |
| Ordine Farmacisti Bari-BAT | frontend | link ai turni ufficiali |
| ViaMichelin, Radio Sound City, social | frontend | link esterni |

### Permessi richiesti all'utente (da mantenere)

- **Notifiche push**: richieste solo dopo un'azione esplicita e un consenso marketing separato, con scelta del quartiere. Revocabili.
- **Iscrizione news**: consenso esplicito, versionato e registrato con data.
- **Adesione a un esercente**: consenso specifico per singola attività.
- Nessuna geolocalizzazione e nessun cookie di tracciamento.

### Codice morto da non migrare

- `server/demo.js` e le tabelle `demo_*`: `/api/demo` risponde già `410 Gone`.
- Le tabelle `user_profiles` e `merchant_access` non sono usate da nessun modulo attivo.
- Le cartelle radice `frontend/`, `backend/`, `.emergent/`: versione precedente.

---

## 2. Vincolo critico da decidere prima di iniziare

**Java non gira su Cloudflare Workers.** Riscrivere il backend in Spring Boot significa uscire dall'hosting attuale. Le conseguenze:

| Aspetto | Oggi | Dopo |
|---|---|---|
| Hosting | Worker serverless (piano gratuito/economico, scala a zero) | Un server JVM sempre acceso: VPS (es. Hetzner, circa 5-10 €/mese) oppure PaaS (Fly.io, Render, Railway) |
| Database | D1 (SQLite gestito) | PostgreSQL (gestito o in un container) |
| Dati esistenti | in D1 | da esportare (`wrangler d1 export`) e importare con uno script di migrazione una tantum |
| Deploy | `wrangler deploy` | immagine Docker → server |
| CDN e WAF | inclusi | Cloudflare resta davanti come DNS, proxy, cache e rate limiting (gratuito) |

**Vantaggi reali del passaggio:** dominio tipizzato, transazioni vere, scheduler e job in background (oggi gli invii push procedono solo quando l'admin richiama l'API, un lotto alla volta), test di integrazione su un database reale, nessun limite di CPU per richiesta, codice manutenibile da sviluppatori Java.

**Svantaggi reali:** costo fisso mensile, un server da mantenere (aggiornamenti, backup), avvio a freddo della JVM se si usa un PaaS che scala a zero.

---

## 3. Architettura proposta

### Stack

| Componente | Scelta | Perché |
|---|---|---|
| Linguaggio | **Java 21 LTS** (compatibile con Java 25 LTS) | Record, sealed types, pattern matching, virtual threads. Java 21 è il JDK disponibile qui per compilare e testare; il passaggio a Java 25 cambia una sola proprietà. |
| Framework | **Spring Boot 4.1.2** (ultima stabile su Maven Central oggi; 4.2 è ancora RC) | Standard di mercato, supporto di lungo periodo |
| Build | **Maven** (wrapper `mvnw` incluso) | Richiesto. Il build del frontend è integrato nello stesso build con `frontend-maven-plugin`. |
| Persistenza | **Spring Data JPA + Hibernate** | Richiesto |
| Database | **PostgreSQL 17** | JSONB per campi flessibili, ricerca full-text in italiano, affidabile |
| Migrazioni | **Flyway** | Schema versionato, ripetibile, applicato all'avvio |
| Sicurezza | **Spring Security 7** | Filtro di sessione custom compatibile con i cookie e gli hash attuali |
| Validazione | **Jakarta Bean Validation** | Stesse regole e stessi messaggi in italiano di oggi |
| Errori | `@RestControllerAdvice` + `ProblemDetail` (RFC 9457) | Il formato `{"detail": "..."}` atteso dal frontend coincide con il campo `detail` di ProblemDetail |
| HTTP client | `RestClient` di Spring + Resilience4j (retry, circuit breaker) | Per Resend e Open-Meteo |
| Web Push | `nl.martijndwars:web-push` 5.1.2 + BouncyCastle | Implementazione Java standard di VAPID e della cifratura aes128gcm |
| Mapping | MapStruct | Conversione entity ↔ DTO senza codice ripetitivo |
| Cache | Caffeine | Meteo, vetrina del giorno, sitemap |
| Rate limiting | Bucket4j | Login, iscrizioni, upload |
| Documentazione API | springdoc-openapi (Swagger UI solo per l'admin) | Contratto API esplicito |
| Osservabilità | Spring Boot Actuator + Micrometer (Prometheus) + log JSON | Health check, metriche, log strutturati |
| Test | JUnit 5, Spring Boot Test, MockMvc, **Testcontainers** (PostgreSQL), WireMock (Resend, push) | I 15 test Node attuali diventano test di integrazione Java |
| Container | Dockerfile multi-stage + `docker-compose.yml` (app + Postgres) | Ambiente locale uguale alla produzione |
| CI/CD | GitHub Actions: build → test → immagine su GHCR → deploy | Sostituisce il workflow Wrangler |

### Organizzazione del repository

```
barizone/
├── pom.xml                         # build unico: backend + frontend
├── mvnw, .mvn/
├── docker-compose.yml              # postgres + app per lo sviluppo locale
├── Dockerfile
├── .github/workflows/ci.yml
├── docs/                           # architettura, API, deploy, migrazione dati
├── frontend/                       # React + Vite (ex barizone-sites/client)
└── src/
    ├── main/java/it/barizone/
    │   ├── BarizoneApplication.java
    │   ├── config/          # SecurityConfig, WebConfig, CacheConfig, SchedulingConfig,
    │   │                    # OpenApiConfig, BarizoneProperties (@ConfigurationProperties)
    │   ├── controller/
    │   │   ├── publicapi/   # NewsController, MerchantController, PushController,
    │   │   │                # SubscriberController, ContactController, MediaController,
    │   │   │                # GroupController, SeoController, AuthController
    │   │   └── admin/       # AdminNewsController, AdminMerchantController,
    │   │                    # AdminPushController, AdminSubscriberController, ...
    │   ├── dto/
    │   │   ├── request/     # record immutabili con validazione
    │   │   └── response/
    │   ├── model/
    │   │   ├── entity/      # News, CompanyProfile, PushSubscription, PushCampaign,
    │   │   │                # PushDelivery, NewsSubscriber, AdminAccount, AdminSession,
    │   │   │                # MerchantSession, MerchantClient, Membership, ...
    │   │   └── enums/       # NewsStatus, NewsCategory, CompanyStatus, District, ...
    │   ├── repository/      # interfacce Spring Data JPA
    │   ├── service/         # logica di dominio, transazioni
    │   │   └── selection/   # NewsSelectionService, SpotlightService (algoritmi attuali)
    │   ├── integration/     # ResendEmailClient, WebPushSender, VapidKeyProvider
    │   ├── security/        # SessionAuthenticationFilter, OriginCsrfFilter,
    │   │                    # Pbkdf2PasswordVerifier, LoginAttemptService, TokenHasher
    │   ├── scheduler/       # PushDispatchJob, SessionCleanupJob
    │   ├── mapper/          # MapStruct
    │   ├── exception/       # GlobalExceptionHandler, eccezioni di dominio
    │   └── util/
    ├── main/resources/
    │   ├── application.yml, application-dev.yml, application-prod.yml
    │   ├── db/migration/    # V1__schema.sql, V2__editorial_seed.sql, ...
    │   └── editorial/       # notizie redazionali in JSON (oggi dentro editorial.js)
    └── test/java/it/barizone/  # stessa struttura a package
```

Una struttura **a strati** (controller, service, repository, model) come richiesto. Dentro `service/` i sottopackage restano per dominio, così che il progetto possa crescere senza diventare un unico blocco.

### Compatibilità garantita

1. **Stesso contratto API**: stessi path, stessi JSON (`snake_case`, campo `detail` negli errori), stessi codici HTTP. Il frontend esistente funziona senza modifiche alle chiamate.
2. **Stesse password admin**: verifica PBKDF2-SHA256 con salt esadecimale e iterazioni lette dal DB, quindi l'hash di `leonardo.favia` già presente nella migrazione continua a funzionare. Resta anche la modalità di fallback `ADMIN_PASSWORD`.
3. **Stesse chiavi VAPID**: la coppia di chiavi salvata in `push_config` viene migrata così com'è. **È indispensabile**: con chiavi nuove tutti i dispositivi già iscritti smetterebbero di ricevere notifiche.
4. **Stesso origin**: Spring Boot serve sia il frontend compilato sia le API, quindi cookie `__Host-`, controllo `Origin` e service worker funzionano come oggi.
5. **Stessi consensi**: versioni `marketing-v1` e `barizone-updates-v1` con le date originali.

### Miglioramenti tecnici al backend (oltre la traduzione)

| Problema attuale | Soluzione |
|---|---|
| `company_profiles.data` è un JSON in testo: nessun vincolo, nessun indice | Colonne vere per i campi principali; JSONB solo per `photos` |
| Il tipo di campagna (`kind`) è salvato in `push_config` come chiave `campaign-kind:{id}` | Colonna `kind` nella tabella `push_campaigns` |
| L'invio push avanza solo quando l'admin richiama l'API, 25 destinatari per volta | Job asincrono con coda in DB: l'admin conferma una volta e l'invio prosegue da solo, con avanzamento visibile |
| Foto salvate come BLOB nel database | Interfaccia `MediaStorage` con due implementazioni: DB (per compatibilità) e storage S3-compatibile (Cloudflare R2, che ha egress gratuito). Ridimensionamento e conversione in WebP. |
| Le notizie "editoriali" sono codice JavaScript: aggiornarle richiede un deploy | Importate in DB come notizie normali, modificabili dall'admin |
| Tabelle create a runtime (`CREATE TABLE IF NOT EXISTS` dentro gli handler) | Solo migrazioni Flyway |
| Rate limit del login implementato a mano | Bucket4j, esteso anche a iscrizioni e upload |
| Nessuna metrica | Actuator: health, metriche, consegne push riuscite e fallite |

---

## 4. Frontend: revisione UX mobile-first

Si mantiene React + Vite + Tailwind (funziona, è moderno, e riscriverlo non porterebbe valore). Interventi previsti:

### Navigazione
- **Barra di navigazione in basso** (bottom tab bar) su mobile, con 5 voci al massimo: Home, Notizie, Quartieri, Aziende, Altro. Resta raggiungibile con il pollice; l'header in alto si riduce a logo e azioni.
- Menu "Altro" in un bottom sheet (`vaul` è già tra le dipendenze): Eventi, Farmacie, Contatti, Notifiche, Iscriviti.
- Quartiere preferito memorizzato, che filtra automaticamente Home e Notizie.

### Ergonomia del tocco
- Aree toccabili di almeno **48×48 px** (Material Design) o 44×44 pt (Apple HIG); spaziatura di almeno 8 px tra un elemento e l'altro.
- Azione principale di ogni schermata in basso, nell'area raggiungibile con il pollice.
- Margini con `env(safe-area-inset-*)` per notch e barra home di iOS.

### Leggibilità
- Testo base a 16 px (sotto i 16 px Safari iOS fa zoom automatico quando si tocca un campo), interlinea 1,5, righe di 60-75 caratteri.
- Contrasto **WCAG 2.2 AA** (4,5:1 per il testo): da verificare sulla palette terracotta/crema.
- I ticker a scorrimento rispettano `prefers-reduced-motion` e hanno un pulsante di pausa. *Correzione: nella prima stesura c'era scritto che "scorrono senza sosta"; era sbagliato, il codice esistente gestiva già entrambe le cose.*

### Prestazioni (Core Web Vitals su rete 4G)
- Code splitting per pagina (`React.lazy`) e `loading="lazy"` sulle immagini, con `srcset` e WebP.
- Skeleton al posto degli spinner; nessuno spostamento del layout durante il caricamento (CLS < 0,1).
- Obiettivi: LCP < 2,5 s, INP < 200 ms.
- Il meteo passa dal backend con cache di 15 minuti: una sola richiesta a Open-Meteo per tutti gli utenti invece di una per visitatore.

### PWA
- Manifest completo: icone 192, 512 e *maskable* (oggi c'è una sola icona 1254 px), `shortcuts` verso Notizie e Quartieri.
- Service worker con cache "stale-while-revalidate" delle ultime notizie, così sono leggibili offline.
- Invito a installare l'app mostrato solo dopo un uso ripetuto, mai al primo accesso.

### Moduli e permessi
- Tastiere giuste (`inputmode="email"`, `type="tel"`) e `autocomplete` corretti.
- Errori mostrati accanto al campo, con i dati inseriti conservati (il backend già lo promette nei messaggi).
- **Richiesta di permesso in due fasi** per le notifiche: prima una schermata dell'app che spiega il valore, poi la richiesta del browser. Se l'utente rifiuta il prompt nativo, il browser non lo ripropone più: per questo la richiesta nativa deve arrivare solo quando l'utente ha già detto sì.
- Su iOS le notifiche web funzionano solo con l'app installata sulla schermata Home (iOS 16.4 o successivo): serve una guida dedicata.

### Accessibilità
- Navigazione completa da tastiera, focus visibile, etichette ARIA sulle icone, testo alternativo sulle foto dei rioni, gerarchia dei titoli coerente.

### Pulizia
- Rimozione dei componenti shadcn non usati (oggi sono 46 file in `components/ui/`) e di dipendenze duplicate (`axios` e `fetch`, `dayjs` e `date-fns`, `swr` e `react-query`): bundle più leggero.

---

## 5. Nuove idee per rafforzare l'app

Ordinate per rapporto valore/sforzo. Tra parentesi: cosa sbloccano.

### Priorità alta (rafforzano ciò che esiste)
1. **Pagamenti reali con Stripe** (ricavi). Oggi pagamenti di campagne e piani sono solo "registrati" a mano. Stripe Checkout con webhook rende il flusso esercente autonomo: sceglie il piano, paga, la scheda diventa attivabile.
2. **Area esercente self-service** (scala commerciale). L'esercente modifica la propria scheda, vede le statistiche (visualizzazioni, clic su telefono, Maps e WhatsApp) e invia comunicazioni. Le statistiche sono l'argomento di vendita dei piani a pagamento.
3. **Notifiche automatiche per quartiere** (fidelizzazione). Quando esce una notizia del rione scelto, una push automatica, con un tetto massimo per giorno per non infastidire. Il consenso esiste già.
4. **Digest email settimanale per quartiere** (fidelizzazione). Resend è già integrato e gli iscritti hanno già un quartiere.
5. **Ricerca full-text** su notizie e aziende (usabilità). PostgreSQL `tsvector` con dizionario italiano, senza servizi esterni.

### Priorità media (nuovo valore per gli utenti)
6. **Mappa delle attività e dei quartieri** con OpenStreetMap + Leaflet: niente chiave API e niente costi, a differenza di Google Maps.
7. **Agenda eventi reale**: oggi gli eventi sono esempi locali. Eventi inseriti da admin ed esercenti, con moderazione, esportazione `.ics` per il calendario del telefono e pulsante "aggiungi al calendario".
8. **Tessera fedeltà digitale con QR**: la libreria QR è già nel progetto. Il cliente aderente mostra un QR, l'esercente lo scansiona e accumula timbri. Rafforza i gruppi esercenti esistenti.
9. **Segnalazioni dei cittadini** (buche, allagamenti, rifiuti) con foto e quartiere, moderate e pubblicate come dati aggregati. Si lega bene a notizie come quella di Santo Spirito presente oggi.
10. **Import RSS assistito** (era nella versione precedente): le fonti locali finiscono in una coda di bozze che la redazione approva. Si mantiene la regola attuale: niente pubblicazione senza fonte e senza revisione umana.

### Priorità bassa o da verificare
11. **Orari dei bus AMTAB**: utile solo se esiste un feed GTFS pubblico. **Non l'ho verificato**: va controllato prima di prometterlo.
12. **Farmacie di turno in app**: oggi ci sono i link ufficiali. Mostrare i dati in app richiede una fonte strutturata e affidabile; senza, meglio restare sui link (lo dice già il README).
13. **Analytics rispettosi della privacy** (Umami o Plausible, senza cookie): misurare cosa leggono gli utenti senza banner di consenso aggiuntivi.

---

## 6. Piano di lavoro

| Fase | Contenuto | Verifica |
|---|---|---|
| 1 | Scaffold Maven, configurazione, Docker Compose, Flyway V1 (schema completo) | `mvn verify` verde, app avviata, `/actuator/health` UP |
| 2 | Sicurezza: login admin (DB e fallback secret), sessioni, filtro Origin, rate limit | Test: hash PBKDF2 esistente accettato, blocco dopo 10 tentativi, CSRF respinto |
| 3 | Notizie, selezione editoriale, archiviazione delle versioni, SEO | Test che replicano `tests/news.mjs`, `editorial-selection.mjs`, `seo.mjs` |
| 4 | Aziende, vetrina del giorno, media, contatti | Replica di `merchants.mjs`, `business-visibility.mjs`, `media.mjs`, `contacts.mjs` |
| 5 | Iscritti + Resend, push + VAPID, campagne, gruppi esercenti | Replica di `subscribers*.mjs`, `push.mjs`, `live-groups.mjs` (servizi esterni simulati con WireMock) |
| 6 | Frontend: integrazione nel build Maven + revisione UX mobile | Screenshot da telefono (Playwright, iPhone e Android), Lighthouse mobile |
| 7 | Script di migrazione dei dati da D1 a PostgreSQL, CI/CD, documentazione di deploy | Import di prova su un export D1 |
| 8 | Nuove funzioni scelte dalla sezione 5 | Una alla volta, dopo la tua approvazione |

Il codice verrà caricato su un branch del repository `barizone/BARIZONE` (oggi vuoto), che è quello a cui ho accesso in scrittura. Nessun deploy in produzione da parte mia.

---

## 7. Decisioni che servono da te

1. **Hosting**: accetti di lasciare Cloudflare Workers per un server JVM (VPS o PaaS)? Senza questo passaggio un backend Java non è possibile.
2. **Database**: PostgreSQL (consigliato) oppure MySQL/MariaDB se hai già un hosting che lo offre?
3. **Repository**: devo pubblicare il nuovo progetto in `barizone/BARIZONE`?
4. **Nuove funzioni**: quali idee della sezione 5 vuoi nella prima versione? Le fasi 1-7 replicano e migliorano quello che esiste; le nuove funzioni si aggiungono dopo.
