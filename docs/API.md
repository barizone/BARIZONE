# API

Tutte le risposte sono JSON con nomi in `snake_case`. Gli errori hanno sempre la forma
`{"detail": "messaggio in italiano"}`.

Ogni richiesta che modifica dati (POST, PUT, DELETE) deve avere l'header `Origin` uguale al sito:
è la protezione CSRF. Le API `/api/admin/**` richiedono la sessione admin (cookie
`__Host-barizone_session`). La specifica OpenAPI completa è su `/api/admin/openapi`, con
interfaccia Swagger su `/api/admin/docs`; entrambe sono riservate all'admin.

## Pubbliche

| Metodo | Percorso | Descrizione |
|---|---|---|
| GET | `/api/news?scope=current\|archive\|neighborhood\|all&neighborhood=&category=&limit=&offset=` | Notizie selezionate per edizione |
| GET | `/api/news/featured` | Come sopra, solo quelle in evidenza |
| GET | `/api/news/{id}` | Dettaglio |
| GET | `/api/news/status` | Stato dell'ultimo controllo redazionale |
| GET | `/api/merchants?quartiere=` | Aziende pubblicate |
| GET | `/api/merchants/spotlight` | Azienda in vetrina oggi |
| GET | `/api/merchants/{id}` | Scheda azienda |
| GET | `/api/media/{id}` | Foto (solo se collegata a un'azienda pubblicata) |
| GET | `/api/contacts` | Email e WhatsApp pubblici |
| GET | `/api/weather` | **Nuovo**: meteo Open-Meteo con cache di 15 minuti |
| GET | `/api/subscribers/config` | Stato del servizio email |
| POST | `/api/subscribers` | Iscrizione alle news (consenso obbligatorio) |
| POST | `/api/subscribers/unsubscribe` | Disiscrizione con token |
| GET | `/api/push/key` | Chiave pubblica VAPID |
| POST / DELETE | `/api/push/subscription` | Attiva o revoca le notifiche del dispositivo |
| POST | `/api/push/status` | Stato del dispositivo |
| POST | `/api/auth/login`, `/api/auth/logout` · GET `/api/auth/me` | Accesso admin |
| POST | `/api/account/register` | Registrazione utente |
| POST | `/api/account/register-business` | Registrazione attività (dati obbligatori + dichiarazioni) |
| POST | `/api/account/verify-email`, `/api/account/resend-verification` | Conferma dell'email |
| POST | `/api/account/login`, `/api/account/logout` · GET `/api/account/me` | Accesso utente o attività |
| POST | `/api/account/forgot-password`, `/api/account/reset-password` | Recupero password |
| GET, PUT | `/api/account/business` | Dati della propria attività (solo account attività) |
| GET | `/sitemap.xml`, `/robots.txt` | SEO |

## Gruppi esercenti (`/api/groups`)

| Metodo | Percorso | Chi |
|---|---|---|
| GET | `/me` | tutti |
| GET | `/state?activity=1` | admin, attività (account approvato e scheda pubblicata) |
| GET | `/shops/{id}` | tutti |
| POST | `/shops/{id}/messages`, `/shops/{id}/messages/{mid}/send` | admin, attività |
| POST | `/subscribe`, `/unsubscribe` · GET `/inbox` | cliente: con account le adesioni lo seguono su ogni dispositivo, senza account valgono sul dispositivo (cookie) |
| GET | `/push/key` · POST/DELETE `/push/subscription` | cliente |

## Admin (`/api/admin`)

| Metodo | Percorso |
|---|---|
| GET, POST | `/news` · PUT `/news/{id}` |
| GET, POST | `/merchants` · PUT `/merchants/{id}` · POST `/merchants/{id}/status?status=` |
| POST | `/media` (multipart, campo `photo`, massimo 1 MB) |
| GET | `/registrations?status=pending\|rejected\|approved` · POST `/registrations/{id}/approve` · POST `/registrations/{id}/reject` (corpo `{"note": "motivo"}`) |
| GET, PUT | `/contacts` |
| GET | `/subscribers` |
| GET, POST | `/push` · PUT `/push/{id}` · POST `/push/test` · POST `/push/{id}/send` |

## Infrastruttura

| Percorso | Accesso |
|---|---|
| `/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness` | pubblico |
| `/actuator/prometheus` | admin |
