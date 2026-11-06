# Barizone

Portale di quartiere per Bari: notizie, aziende, vetrina del giorno, iscrizioni alle news,
notifiche push e gruppi esercenti. Versione 2: backend **Java 21 + Spring Boot 4**, database
**PostgreSQL**, frontend **React 19 + Vite** ottimizzato per mobile, distribuiti come un unico JAR.

Riscrive la versione Cloudflare Worker + D1 (repository `bari-hub`) mantenendo **lo stesso contratto API**:
stessi percorsi, stesso JSON, stessi messaggi, stesse password admin, stesse chiavi VAPID.

## Avvio rapido

Requisiti: Java 21, Maven 3.9 e PostgreSQL 16+ (oppure Docker).

```bash
docker compose up --build
```

Apri http://localhost:8080. Senza Docker:

```bash
createdb barizone   # utente/password predefiniti: barizone/barizone
mvn spring-boot:run
```

Il frontend viene compilato da Maven (scarica da solo Node.js). Per lavorare solo sul frontend,
con ricarica a caldo:

```bash
cd frontend && npm ci --legacy-peer-deps && npm start
```

## Test

```bash
createdb barizone_test
mvn verify
```

I test di integrazione usano un PostgreSQL reale (`TEST_DATABASE_URL`, predefinito
`jdbc:postgresql://localhost:5432/barizone_test`). Email e notifiche push sono simulate.
La selezione delle notizie è verificata contro i risultati del codice JavaScript originale.

## Struttura

```
src/main/java/it/barizone/
├── configuration/   Sicurezza HTTP, cache, client HTTP, scheduler, proprietà tipizzate
├── controller/
│   ├── publicapi/   API pubbliche (/api/news, /api/merchants, /api/push, /api/groups, …)
│   └── admin/       API riservate (/api/admin/**)
├── dto/             Record di richiesta e risposta (JSON snake_case)
├── model/
│   ├── entity/      Entità JPA
│   └── enums/       Stati, quartieri, categorie
├── repository/      Spring Data JPA
├── service/         Logica di dominio e transazioni
├── integration/     Servizi esterni: Resend, Web Push/VAPID, Open-Meteo, storage foto
├── security/        Sessioni via cookie, controllo Origin (CSRF), PBKDF2
├── scheduler/       Invio push in background, pulizia sessioni
├── exception/       Errori in formato {"detail": "..."}
└── util/
src/main/resources/
├── db/migration/    Schema Flyway
└── editorial/       Notizie redazionali importate all'avvio
frontend/            React + Vite + Tailwind
scripts/             Migrazione dati D1 → PostgreSQL
docs/                Architettura, deploy, API, guide operative
```

## Configurazione

Tutto si configura con variabili d'ambiente (vedi `.env.example`): lo stesso artefatto gira su
VPS, PaaS o Kubernetes.

| Variabile | Uso |
|---|---|
| `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` | PostgreSQL (JDBC) |
| `PUBLIC_ORIGIN` | Origin pubblico (es. `https://barizone.it`): sitemap, link email, CSRF |
| `ALLOWED_ORIGINS` | Altri origin ammessi, separati da virgola |
| `ADMIN_PASSWORD`, `SESSION_SECRET`, `ADMIN_EMAIL` | Accesso admin di riserva (come nella versione Worker) |
| `NEWS_EMAIL_ENABLED`, `RESEND_API_KEY` | Email di conferma iscrizione |
| `VAPID_PUBLIC_KEY`, `VAPID_PRIVATE_KEY` | Facoltative: se vuote, chiavi migrate in DB o generate al primo avvio |
| `PORT` | Porta HTTP (predefinita 8080) |

## Documentazione

- [docs/ARCHITETTURA.md](docs/ARCHITETTURA.md): scelte tecniche e differenze dalla versione Worker
- [docs/DEPLOY.md](docs/DEPLOY.md): hosting, migrazione dati da D1, checklist di produzione
- [docs/API.md](docs/API.md): elenco degli endpoint
- [docs/PIANO_BARIZONE_JAVA.md](docs/PIANO_BARIZONE_JAVA.md): documento di progetto e nuove idee
- [docs/gestione/](docs/gestione/): guide operative per redazione e gestione clienti
