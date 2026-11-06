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
