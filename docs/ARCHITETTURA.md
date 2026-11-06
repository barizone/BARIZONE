# Architettura

## Flusso di una richiesta

```
Browser ──HTTPS──▶ proxy (Caddy/Cloudflare) ──▶ Spring Boot (porta 8080)
                                                 ├─ OriginCheckFilter   CSRF: Origin = sito
                                                 ├─ AdminSessionFilter  cookie → ruolo ADMIN
                                                 ├─ AdminPageFilter     /admin senza sessione → /login
                                                 ├─ SpaFallbackFilter   rotte React → index.html
                                                 ├─ controller → service → repository (JPA) → PostgreSQL
                                                 └─ integration: Resend, Web Push, Open-Meteo
```

## Scelte tecniche

| Scelta | Motivo |
|---|---|
| Java 21 LTS, Spring Boot 4.1 | Versione stabile più recente al momento del lavoro; virtual threads attivi per l'I/O |
| PostgreSQL + Flyway | Transazioni vere, `text[]` per foto e quartieri, schema versionato |
| `ddl-auto: validate` | Hibernate non modifica mai lo schema: lo modifica solo Flyway |
| Enum con nomi minuscoli | Coincidono con i valori nel DB e nel JSON: nessun converter |
| Sessioni opache con hash in DB | Revocabili al logout; nel DB non c'è il token in chiaro |
| Controllo Origin invece del token CSRF | Stesso modello della versione Worker; il frontend non richiede modifiche |
| Inserimento "prenota-poi-invia" per le push | Più processi in parallelo non inviano mai due volte allo stesso dispositivo |
| Interfacce `EmailGateway`, `WebPushGateway`, `MediaStorage` | Servizi esterni sostituibili e simulabili nei test |
| Frontend nel JAR | Un solo artefatto e un solo origin: cookie `__Host-` e service worker funzionano senza CORS |

## Accessi: utente, attività, admin

| | Utente | Attività | Admin |
|---|---|---|---|
| Archivio | `user_accounts`, ruolo `user` | `user_accounts`, ruolo `merchant`, collegato a una scheda in `company_profiles` | `admin_accounts` |
| Cookie | `__Host-barizone_account` | `__Host-barizone_account` | `__Host-barizone_session` |
| Password | BCrypt (costo 12) | BCrypt (costo 12) | PBKDF2 (compatibile con la versione Worker) |
| Conferma email | sì | sì | no |
| Area | `/profilo` | `/area-attivita` | `/admin` |

**Registrazione attività.** Il modulo richiede ragione sociale, partita IVA (11 cifre con cifra di controllo), codice fiscale
(11 cifre o 16 caratteri con carattere di controllo), sede legale completa, PEC, email, telefono, categoria, quartiere, indirizzo,
descrizione e due dichiarazioni (titolarità, condizioni/privacy). Sono controlli formali: l'esistenza e la titolarità dell'attività
le verifica l'admin. Stati: `pending` (scheda non visibile) → `approved` (pubblicata e segnata come verificata) oppure `rejected`
(con motivo mostrato al titolare, che può correggere e reinviare). Una partita IVA non può avere due registrazioni attive.
Se un'attività approvata cambia ragione sociale, partita IVA o codice fiscale, esce dal sito finché l'admin non la riapprova.

**Sicurezza degli account.** Token di sessione e link email salvati solo come hash; link di conferma validi 48 ore e recupero password 1 ora,
monouso; il recupero chiude tutte le sessioni aperte; risposte identiche per email già registrate o no (non si rivela chi è iscritto);
10 tentativi di accesso errati ogni 15 minuti per coppia email+IP; limite orario di registrazioni, reinvii e recuperi per IP
(le richieste respinte per dati non validi non consumano la quota); password di 10-72 caratteri con almeno una lettera e un numero.

**Adesioni ai gruppi.** Restano possibili senza account (cookie anonimo, utile con il QR in negozio). Con un account, le adesioni
sono collegate all'utente e lo seguono su ogni dispositivo; se un utente accede da un dispositivo con adesioni anonime, quelle passano al suo account.

## Differenze rispetto alla versione Worker

| Area | Prima | Ora |
|---|---|---|
| Invio campagne push | Avanzava solo mentre la pagina admin restava aperta (25 destinatari per chiamata) | Prosegue in background ogni 30 secondi (`PushDispatchJob`); l'API restituisce comunque l'avanzamento |
| Schede aziende | JSON in una colonna di testo | Colonne tipizzate |
| Tipo di campagna | Salvato in `push_config` con chiave `campaign-kind:<id>` | Colonna `kind` |
| Notizie redazionali | Scritte nel codice (`editorial.js`): ogni modifica richiedeva un deploy | Importate in DB all'avvio e modificabili dall'admin; quelle già presenti non vengono sovrascritte |
| Tabelle create al volo dentro gli handler | `CREATE TABLE IF NOT EXISTS` a ogni richiesta | Solo migrazioni Flyway |
| Meteo | Ogni browser chiamava Open-Meteo | `/api/weather` con cache condivisa; in caso di errore serve l'ultimo dato valido (massimo 6 ore) |
| Categorie notizie | 6 (le redazionali "Territorio" e "Mobilità" non erano modificabili) | 8 |
| Cache HTTP | — | `/assets/*` immutabili per 1 anno; HTML e API mai in cache |
| Sessioni scadute | Rimosse solo al login successivo | Pulizia oraria |
| Accesso degli esercenti | Link monouso generato dall'admin per ogni attività | Account con registrazione, conferma email e approvazione admin |
| Account utente | Assente (solo cookie anonimo) | Registrazione, accesso e profilo; il cookie anonimo resta possibile |
| Dati demo | Eventi, ristoranti e azienda "di esempio" nel codice del frontend; flag `is_demo` | Nessun dato demo: colonna `is_demo` rimossa (migrazione V2); la pagina Eventi mostra le notizie reali della categoria "Eventi" |
| Aziende iniziali | — | Sei aziende importate dal vecchio archivio con la migrazione V3, modificabili dall'admin |
| Partita IVA | Solo 11 cifre | Cifra di controllo verificata e unicità tra le schede |

## Compatibilità verificata dai test

- Hash PBKDF2 calcolati con WebCrypto (codice originale) accettati da Java.
- SHA-256 dei token identico.
- Selezione delle notizie identica a `editorialSelection.js` sugli stessi dati e alla stessa data
  (scope `current`, `archive`, `neighborhood`, `all`, filtro per quartiere).
- Chiavi VAPID nel formato del Worker usate dalla libreria Java per firmare e cifrare (aes128gcm).
- Script di migrazione provato su un dump SQLite costruito con le migrazioni Drizzle originali.

## Frontend: interventi per l'uso da telefono

Misure con Playwright su iPhone 13 emulato (schermo 390×664 px):

| Misura | Prima | Dopo |
|---|---|---|
| Area fissa durante lo scorrimento | 226 px (34% dello schermo) | 61 px in alto + 57 px in basso (18%) |
| Elementi toccabili sotto i 44 px | 25 su 44 | 1 (un link dentro una frase, esente secondo WCAG 2.5.8) |
| JavaScript iniziale | 541 KB (170 KB compressi) | 374 KB (121 KB compressi); le altre pagine si scaricano quando servono |
| Logo nell'header | PNG da 971 KB | WebP da 6 KB |

Altri interventi:

- barra di navigazione in basso, nella zona del pollice, con pannello "Altro" che sale dal basso (Esc e tocco sullo sfondo lo chiudono, focus gestito);
- `aria-current` sulla sezione attiva, link "Vai al contenuto", focus visibile;
- campi di testo a 16 px (Safari iOS non fa più zoom quando si tocca un campo), margini per notch e barra Home (`safe-area-inset`);
- skeleton durante il caricamento delle pagine;
- PWA: icone 192, 512 e maskable, icona Apple, scorciatoie, `viewport-fit=cover`;
- service worker: lettura offline delle ultime notizie e del meteo, asset in cache, notifiche invariate;
- già presenti e mantenuti: pausa dei ticker, rispetto di `prefers-reduced-motion`, consenso alle notifiche in due passaggi.
