# Barizone · documento funzionale

Ultimo aggiornamento: 6 novembre 2026 · riferito al codice del commit `22ca666` più le modifiche successive elencate in
[Storico degli aggiornamenti](#storico-degli-aggiornamenti).

Questo documento descrive **cosa fa l'applicazione oggi**, come è fatta e dove sono i suoi limiti. Va aggiornato a ogni modifica
che cambia comportamento, architettura, API, dati o flussi utente. Per i dettagli tecnici vedi anche
[ARCHITETTURA.md](ARCHITETTURA.md), [API.md](API.md) e [DEPLOY.md](DEPLOY.md).

---

## 1. Panoramica

**Barizone** è un portale di quartiere per Bari. Obiettivi del prodotto:

1. Dare ai baresi notizie locali **verificabili** (ogni notizia ha una fonte e una data), organizzate per rione.
2. Dare visibilità alle attività commerciali del quartiere, con schede verificate dalla redazione.
3. Permettere alle attività di restare in contatto con i propri clienti (adesione con consenso, comunicazioni push).
4. Funzionare bene da telefono, installabile come app (PWA).

Principio editoriale: **nessun contenuto inventato**. Non ci sono eventi, ristoranti o aziende di esempio; ciò che non è disponibile
viene dichiarato tale (es. farmacie, viabilità).

## 2. Architettura generale

| Livello | Tecnologia | Note |
|---|---|---|
| Frontend | React 19, Vite, Tailwind | Single page application, mobile-first, PWA con service worker |
| Backend | Java 21, Spring Boot 4.1, Spring Security, Spring Data JPA | API REST JSON (`snake_case`); serve anche il frontend |
| Database | PostgreSQL 16+, schema gestito da Flyway (V1–V4) | Hibernate in modalità `validate`: non modifica mai lo schema |
| Servizi esterni | Resend (email), Web Push dei browser (VAPID), Open-Meteo (meteo) | Collegamenti esterni senza chiave: Google Maps, YouTube/Vimeo, ViaMichelin, Ordine dei Farmacisti |
| Distribuzione | Un unico JAR (frontend incluso) / immagine Docker `ghcr.io/barizone/barizone` | CI su GitHub Actions |

Package backend (`it.barizone`): `configuration`, `controller` (`publicapi`, `admin`), `dto`, `model` (`entity`, `enums`),
`repository`, `service`, `integration`, `security`, `scheduler`, `exception`, `util`.

## 3. Ruoli, accessi e permessi

| Ruolo | Come entra | Cookie | Cosa può fare |
|---|---|---|---|
| **Visitatore** | — | — | Leggere notizie, aziende, quartieri; iscriversi alle news; attivare le notifiche; aderire a un'attività (cookie anonimo) |
| **Utente** | `/registrati`, `/accedi` | `__Host-barizone_account` | Come il visitatore, e le adesioni lo seguono su ogni dispositivo |
| **Attività** | `/registra-attivita`, `/accedi` | `__Host-barizone_account` | Gestire i propri dati; dopo l'approvazione, scrivere ai propri aderenti |
| **Admin** | `/login` | `__Host-barizone_session` | Tutto: notizie, aziende, registrazioni, campagne, iscritti, contatti, foto |

Regole trasversali:
- Le sessioni non hanno stato nel server: l'identità arriva dal cookie (`Secure`, `HttpOnly`, `SameSite`) e nel database c'è solo l'hash del token.
- Ogni richiesta che modifica dati deve avere l'header `Origin` uguale al sito (protezione CSRF).
- `/api/admin/**` richiede il ruolo admin; `/api/account/business/**` il ruolo attività. Un cookie non vale per un altro ruolo.
- L'admin ha un archivio separato (`admin_accounts`, PBKDF2, accesso di riserva con `ADMIN_PASSWORD`); utenti e attività sono in `user_accounts` (BCrypt).

## 4. Funzionalità

### 4.1 Notizie

- **Scopo:** pubblicare notizie con fonte; mostrarle per edizione, quartiere, categoria; conservare le versioni precedenti.
- **Flusso utente:** Home (in evidenza), `/notizie` (edizione corrente), `/archivio`, `/notizie/{id}`, `/quartieri` (per rione). L'admin le gestisce in `/admin` → *Notizie*.
- **Componenti:** `NewsController`, `AdminNewsController`, `NewsService`, `NewsSelectionService`, `EditorialSeedService`; pagine `Home`, `NewsList`, `ArticleDetail`, `NeighborhoodNews`, `NewsAdmin`.
- **Regole:**
  - Una notizia pubblicata richiede il collegamento alla **fonte originale**; senza fonte non è mai mostrata al pubblico.
  - Categorie ammesse: Sport, Arte & Cultura, Eventi, Attualità, Cronaca, Mondo, Territorio, Mobilità. Titolo ≤ 200, testo ≤ 30.000 caratteri; link solo `http(s)`.
  - Se una notizia pubblicata cambia nei contenuti, la versione precedente resta in archivio (stato `archived`, non modificabile).
  - Selezione: l'**edizione** è la data più recente tra le notizie in corso; `current` mostra l'edizione corrente e gli eventi non scaduti; `archive` le precedenti, scadute o archiviate; `neighborhood` solo l'edizione corrente di un rione. Dopo 72 ore dall'ultima edizione l'ordine ruota ogni giorno (senza cambiare le date). Gli eventi scaduti (`expires_at`) non tornano mai tra le notizie correnti.
  - Le 17 notizie redazionali del vecchio archivio sono importate all'avvio (una volta; non sovrascrivono modifiche).
- **Dati:** tabella `news` (stato, origine admin/redazionale, quartiere, rioni collegati, scadenza, date).
- **Errori gestiti:** validazioni con messaggi in italiano; modifica di una versione archiviata → 409.
- **Limiti:** il campo `views` non viene incrementato (le visualizzazioni non sono conteggiate); non c'è importazione automatica da RSS; `/api/news/status` legge un file fisso (`editorial/status.json`) che non si aggiorna da solo.

### 4.2 Eventi

- **Scopo:** elenco degli eventi della città.
- **Flusso:** `/eventi`, filtro per quartiere.
- **Comportamento:** non esiste una fonte di eventi dedicata; la pagina mostra le **notizie della categoria "Eventi"** con data e fonte. Se non ce ne sono: "Nessun evento pubblicato".
- **Limiti:** niente calendario, orari o prezzi strutturati; per averli serve un modello dati per gli eventi (vedi proposte).

### 4.3 Aziende e vetrina del giorno

- **Scopo:** schede delle attività del quartiere, con una in vetrina ogni giorno.
- **Flusso:** `/aziende` (ricerca e filtro per quartiere), `/aziende/{id}`, vetrina nella Home.
- **Componenti:** `MerchantController`, `MerchantService`, `SpotlightService`; pagine `QuartieriPage`, `MerchantDetail`, componente `BusinessSpotlight`.
- **Regole:**
  - Sono pubbliche solo le schede con stato `published`.
  - La **vetrina** ruota una volta al giorno (calendario di Bari) tra le schede pubblicate, in ordine di ID; la scelta del giorno è salvata, quindi ricaricare la pagina non la cambia.
  - Il quartiere è normalizzato (accenti, maiuscole, "Bari Vecchia" → "San Nicola / Bari Vecchia").
  - Stati: `draft`, `published`, `suspended`, `archived`.
- **Dati:** `company_profiles` (colonne tipizzate), `business_spotlight`.
- **Aziende iniziali:** sei, importate dalla migrazione V3 (Sartoria Miglionico, Caffè del Corso, Libreria Roma, Beauty Studio Poggiofranco, Ciclofficina Japigia, Osteria del Mare Torre). Sono pubblicate ma **senza partita IVA né dati legali** e non "verificate"; promozioni e piani vengono dal vecchio archivio.
- **Limiti:** nessuna geolocalizzazione né mappa interna (solo link "Dove trovarmi" a Google Maps); nessun contatore di visualizzazioni o clic.

### 4.4 Accesso di utenti e attività

- **Scopo:** identificare utenti e attività in modo sicuro.
- **Flusso:** registrazione → email con link (valido 48 ore) → conferma → accesso. Recupero password: email con link (valido 1 ora) → nuova password.
- **Componenti:** `AccountController`, `AccountService`, `AccountMailer`, `AccountSessionFilter`; pagine `AccountRegister`, `AccountAccess`, `AccountAreas`.
- **Regole:**
  - Password 10–72 caratteri, almeno una lettera e un numero, diversa dall'email. BCrypt costo 12.
  - L'account non entra finché l'email non è confermata (`REQUIRE_EMAIL_VERIFICATION=true`, predefinito).
  - Accesso: 10 tentativi errati ogni 15 minuti per coppia email+IP, poi blocco; stesso messaggio per password errata ed email inesistente.
  - Registrazione, reinvio e recupero: risposta identica per email già registrata o no; limite orario per IP (le richieste respinte per dati non validi non contano).
  - Il recupero password chiude tutte le sessioni aperte. I link sono monouso e salvati come hash.
- **Dati:** `user_accounts`, `account_sessions`, `account_tokens`, `login_attempts`.
- **Errori gestiti:** email non confermata (403, codice `email_not_verified`), account sospeso, troppi tentativi (429), link scaduto.
- **Limiti:** senza servizio email attivo nessuno può confermare l'account; il limite orario è per istanza del server; non esiste 2FA; l'admin non può ancora sospendere o riattivare un account dall'interfaccia (stato `suspended` gestibile solo da database).

### 4.5 Registrazione e verifica delle attività

- **Scopo:** far registrare le attività con dati legali completi e pubblicarle solo dopo una verifica umana.
- **Flusso:** `/registra-attivita` → conferma email → l'attività vede lo stato in `/area-attivita` → l'admin approva o rifiuta in `/admin` → *Registrazioni attività* → email al titolare.
- **Componenti:** `BusinessRegistrationService`, `BusinessValidator`, `ItalianTaxIds`, `AdminRegistrationController`, `BusinessAccountController`; pagine `RegisterBusinessPage`, `BusinessAreaPage`, componente `RegistrationsAdmin`.
- **Regole:**
  - **Obbligatori:** nome, ragione sociale, titolare, partita IVA, codice fiscale, sede legale (indirizzo, CAP, comune, provincia), PEC, email, telefono, categoria, quartiere, indirizzo, descrizione (≥ 20 caratteri) e due dichiarazioni (titolarità; condizioni e privacy).
  - **Partita IVA:** 11 cifre con cifra di controllo. **Codice fiscale:** 11 cifre valide oppure 16 caratteri con carattere di controllo. Sono controlli formali: esistenza e titolarità li verifica l'admin.
  - Una partita IVA non può avere due registrazioni attive (le rifiutate non contano); lo stesso controllo vale per l'inserimento dall'admin.
  - Stati: `pending` (scheda non visibile) → `approved` (pubblicata e segnata verificata) oppure `rejected` (motivo obbligatorio, mostrato al titolare).
  - Modifiche del titolare: cambiare ragione sociale, partita IVA o codice fiscale rimanda la scheda in verifica e la toglie dal sito; una registrazione rifiutata, una volta corretta, torna in attesa.
  - Termini accettati e data sono registrati con la versione (`accounts-v1`).
- **Dati:** `company_profiles` (con `owner_account_id`, `registration_status`, `review_note`, ecc.).
- **Limiti:** il titolare può indicare la foto solo con un collegamento https (le foto le carica l'admin); le caselle di consenso citano condizioni e informativa privacy che **non esistono ancora come pagine**; nessun invio di documenti (visura camerale) per la verifica.

### 4.6 Iscrizione alle news e notifiche

- **Scopo:** restare aggiornati via email e notifiche sul telefono, con consenso esplicito.
- **Flusso:** `/iscriviti-news` (nome, cognome, email, rione, consenso) → email di conferma (Resend) → eventuale attivazione notifiche con scelta separata in `/notifiche`. Disiscrizione con codice personale.
- **Componenti:** `SubscriberController/Service`, `PushController`, `PushSubscriptionService`, `ResendEmailGateway`, `VapidKeyProvider`, `VapidWebPushGateway`, service worker `push-sw.js`.
- **Regole:**
  - Consenso versionato e datato (`barizone-updates-v1` per le news, `marketing-v1` per le notifiche).
  - Campo trappola anti-bot; risposta neutra se l'email è già iscritta.
  - L'iscrizione resta salvata anche se l'invio email fallisce (l'esito è indicato).
  - Notifiche: solo verso servizi push reali (FCM, Mozilla, Apple, Windows), con consenso marketing, quartiere scelto, revocabili. Su iPhone servono iOS 16.4+ e l'app aggiunta alla Home.
  - Le chiavi VAPID sono quelle migrate da D1: cambiarle disattiva tutti i dispositivi iscritti.
- **Dati:** `news_subscribers`, `news_subscriber_consents`, `push_subscriptions`, `app_config` (chiavi VAPID).
- **Limiti:** nessuna newsletter né digest automatico (le email sono solo conferma e avviso alla redazione); le email dipendono da Resend e dal dominio verificato.

### 4.7 Campagne di notifica (admin)

- **Scopo:** inviare notifiche istituzionali o commerciali a un quartiere o a tutta Bari.
- **Flusso:** `/admin` → *Notifiche e campagne*: bozza → prova sul proprio dispositivo → invio con conferma esplicita.
- **Regole:** una campagna **sponsorizzata** parte solo con committente, riferimento dell'accordo, importo e pagamento registrato come pagato; quella `barizone` è gratuita. Titolo ≤ 80, testo ≤ 200, link solo https. Invio a lotti di 25, **senza doppi invii** (ogni consegna è prenotata in modo atomico); i dispositivi che rispondono 404/410 vengono rimossi.
- **Job:** `PushDispatchJob` completa in background gli invii avviati (ogni 30 secondi).
- **Dati:** `push_campaigns`, `push_deliveries`.
- **Limiti:** il pagamento è solo registrato a mano, non incassato online; nessuna pianificazione a orario; nessun tetto di notifiche per utente al giorno.

### 4.8 Gruppi di clienti delle attività

- **Scopo:** far aderire i clienti a un'attività e permetterle di scrivere loro.
- **Flusso:** il cliente apre `/adesioni?azienda=ID` (da QR o link), inserisce un nome e il consenso specifico, attiva le notifiche. L'attività approvata scrive da `/gestione-clienti`.
- **Regole:**
  - Adesione con o senza account; con un account segue l'utente su ogni dispositivo, e le adesioni anonime del dispositivo passano all'account al primo accesso.
  - Può scrivere solo l'admin o l'account di un'attività **approvata e pubblicata**, e solo ai propri aderenti attivi (titolo ≤ 100, messaggio ≤ 500).
  - Consegna a lotti di 5, con stato per destinatario (accettata, non attiva, non riuscita, revocata).
- **Dati:** `merchant_clients`, `merchant_memberships`, `merchant_messages`, `merchant_deliveries`, `merchant_device_subscriptions`.
- **Limiti:** nessuna risposta del cliente (comunicazione a senso unico); i link di accesso monouso per esercenti non esistono più.

### 4.9 Contatti, SEO, meteo e pagine informative

| Funzione | Comportamento | Limiti |
|---|---|---|
| **Contatti** (`/contatti`) | Email della redazione; numero WhatsApp visibile solo se pubblicato dall'admin e valido (con prefisso internazionale) | — |
| **SEO** | `sitemap.xml` generata da notizie e aziende pubblicate; `robots.txt` esclude API e aree riservate | — |
| **Meteo** | `/api/weather` serve i dati di Open-Meteo con cache condivisa di 15 minuti; se il servizio non risponde, l'ultimo dato valido (max 6 ore) | Senza rete verso Open-Meteo mostra "non disponibile" |
| **Viabilità** (barra in alto) | Elenco **fisso** di segnalazioni di ViaMichelin del 4 novembre 2026, valido fino al 5 novembre; dopo la scadenza mostra solo un invito a consultare ViaMichelin | **Non è un feed live**; oggi mostra il messaggio di scadenza |
| **Farmacie** (`/farmacie`) | Tre collegamenti al sito dell'Ordine dei Farmacisti (turni, elenco, aperture prolungate) | Nessun elenco per quartiere né stato "aperta ora" |
| **Quartieri** (`/quartieri`) | Notizie del rione, con storia e foto per 24 rioni | — |
| **Promuovi** (`/promuovi`) | Spiega la registrazione delle attività | — |
| **Media** | Foto JPG/PNG/WebP fino a 1 MB, verifica del formato reale; visibili al pubblico solo se collegate a una scheda pubblicata | Salvate nel database |

### 4.10 Area admin (`/admin`)

Schede: *Notizie*, *Notifiche e campagne*, *Registrazioni attività*, *Schede aziende*, *Dashboard iscritti*, *Contatti e procedura*, più il collegamento a *QR e clienti delle aziende*.
Metriche e documentazione API (`/actuator/prometheus`, `/api/admin/docs`) sono riservate all'admin.

## 5. Modello dati (sintesi)

```
user_accounts ──< account_sessions
      │    └────< account_tokens
      └─ 1:1 ── company_profiles ──< merchant_memberships >── merchant_clients ── (0..1) user_accounts
                      │                      │
                      │                      └──< merchant_deliveries >── merchant_messages
                      └── business_spotlight (data → azienda)
news                      news_subscribers ── news_subscriber_consents
push_subscriptions        push_campaigns ──< push_deliveries
admin_accounts ──< admin_sessions        login_attempts        app_config        media_files
```

## 6. Configurazione e job

- **Variabili d'ambiente:** vedi `.env.example` e README (database, `PUBLIC_ORIGIN`, admin di riserva, `REQUIRE_EMAIL_VERIFICATION`, Resend, VAPID).
- **Job (`scheduler`):** invio in background di campagne e comunicazioni (ogni 30 secondi); pulizia oraria delle sessioni scadute.
- **Cache HTTP:** `/assets/*` immutabili per un anno; HTML e API mai in cache.

## 7. Qualità e verifiche attuali

- 44 test (unitari e di integrazione su PostgreSQL reale, con email e push simulate), eseguiti in CI a ogni push su `main`/`staging` e a ogni pull request.
- Percorso completo dal browser (registrazione attività → approvazione → pubblicazione, utente, separazione dei tre accessi) provato con Playwright su iPhone emulato; **non è ancora uno script nel repository né gira in CI**.
- Non esistono lint né type-check configurati per il frontend (JavaScript senza TypeScript).

## 8. Proposte di miglioramento, in ordine di priorità

| # | Proposta | Beneficio | Impatto | Rischi |
|---|---|---|---|---|
| 1 | **Testi legali** (condizioni, privacy) collegati alle caselle di registrazione | Obbligatorio per aprire le registrazioni; evita contestazioni | Alto, poco lavoro tecnico | Va redatto da un professionista |
| 2 | **Email in produzione** (dominio verificato su Resend) | Senza, nessuno conferma l'account | Alto, poco lavoro | Consegna non garantita (spam) |
| 3 | **Sicurezza admin**: togliere dal repository l'hash della password del titolare, gestire gli account admin da interfaccia, 2FA, alzare le iterazioni PBKDF2 (oggi 100.000) | Riduce il rischio sull'account più potente | Alto | Serve una migrazione controllata delle password |
| 4 | **Completare i dati delle sei aziende** (IVA, ragione sociale, sede) e confermare promozioni e piani | Schede coerenti con le regole di verifica | Alto, lavoro manuale | Promozioni oggi non verificate |
| 5 | **Test end-to-end nel repository e in CI** (Playwright) | Evita regressioni sui flussi principali | Medio-alto | Tempo di CI maggiore |
| 6 | **Protezione dei rami** (nessun push diretto su `main`, CI obbligatoria) | Applica le regole di lavoro in modo automatico | Medio | Nessuno |
| 7 | **Viabilità live o rimozione** della barra | Oggi mostra solo un messaggio di scadenza | Medio | Serve un feed con licenza d'uso |
| 8 | **Foto caricate dalle attività** (storage S3/R2, ridimensionamento) | Schede complete senza passare dall'admin | Medio | Moderazione dei contenuti |
| 9 | **Sospensione e gestione account** dall'admin, con registro delle azioni | Controllo operativo | Medio | — |
| 10 | **Eventi veri** (modello dati, data, luogo, `.ics`) | Calendario utile | Medio | Lavoro editoriale continuo |
| 11 | **Statistiche per attività** (visite, clic su telefono e mappe) | Valore commerciale dei piani | Medio | Privacy: usare conteggi aggregati |
| 12 | **Ricerca full-text** su notizie e aziende | Usabilità | Medio | — |
| 13 | **Pagamenti online** (Stripe) per campagne e piani | Ricavi autonomi | Alto | Obblighi fiscali e di conformità |
| 14 | **Digest settimanale** per quartiere e notifiche automatiche con tetto giornaliero | Fidelizzazione | Medio | Rischio di disturbo se mal tarate |
| 15 | **Limite di richieste globale** (non per istanza) | Protezione con più istanze | Basso oggi | — |
| 16 | **Contare le visualizzazioni** delle notizie | Dati editoriali | Basso | — |
| 17 | **Pulizia del frontend**: dipendenze duplicate (axios/fetch, dayjs/date-fns, swr/react-query), font in locale, lint | Bundle più leggero, manutenzione | Basso | — |

Ordine suggerito: 1 → 2 → 3 → 4 (prima di aprire le registrazioni); 5 e 6 subito dopo, per proteggere il lavoro successivo; poi 7–12 in base al feedback degli utenti; 13–14 quando c'è traffico.

## Storico degli aggiornamenti

| Data | Modifica |
|---|---|
| 6 nov 2026 | Prima stesura. Riflette: backend Java, accessi per utenti/attività/admin, registrazione attività con verifica, rimozione dei dati demo, pagina Promuovi allineata alla registrazione |
