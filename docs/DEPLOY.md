# Deploy e migrazione

L'hosting non è ancora deciso, quindi il progetto non dipende da nessun fornitore. Serve solo:

- un ambiente che esegua un container Docker (oppure Java 21 con `java -jar barizone.jar`);
- un database PostgreSQL 16 o 17;
- HTTPS davanti all'applicazione. Senza HTTPS i browser rifiutano i cookie `__Host-` e quindi il login.

## Opzione A: un VPS (configurazione già pronta)

Ad esempio Hetzner, OVH o Aruba Cloud, con Docker installato.

```bash
cp .env.example .env    # compila i segreti
docker compose -f docker-compose.prod.yml --env-file .env up -d --build
```

Caddy ottiene e rinnova da solo il certificato HTTPS per `DOMAIN`. Il DNS deve già puntare al server.

**Backup:** pianifica `pg_dump` giornaliero su uno storage esterno al server.

```bash
docker compose -f docker-compose.prod.yml exec db pg_dump -U barizone barizone | gzip > backup-$(date +%F).sql.gz
```

## Opzione B: PaaS (Render, Railway, Fly.io…)

1. Crea un PostgreSQL gestito.
2. Crea un servizio dal `Dockerfile` del repository, oppure dall'immagine `ghcr.io/<owner>/barizone`
   pubblicata dalla CI.
3. Imposta le variabili d'ambiente di `.env.example`. `DATABASE_URL` va in formato JDBC:
   `jdbc:postgresql://host:5432/db`.
4. Configura il controllo di salute su `/actuator/health/readiness`.

Se il PaaS spegne i servizi inattivi, il primo accesso dopo una pausa richiede circa 10 secondi
(avvio della JVM). Per un portale di notizie conviene un piano sempre acceso.

## Cloudflare davanti all'applicazione (consigliato)

Cloudflare può restare come DNS, proxy e WAF anche con il nuovo hosting:

- SSL/TLS in modalità **Full (strict)**;
- regola di rate limiting su `POST /api/auth/login`, che si aggiunge al limite applicativo di 10 tentativi ogni 15 minuti;
- l'applicazione legge l'IP del client da `X-Forwarded-For` (`server.forward-headers-strategy=framework`).
  Non esporre l'applicazione direttamente senza un proxy fidato davanti: altrimenti quell'header può essere falsificato.

## Migrazione dei dati da Cloudflare D1

Da fare una volta sola, in una finestra di manutenzione.

```bash
# 1. Export di D1 (dalla cartella barizone-sites del vecchio repository)
npx wrangler d1 export barizone --remote --output=d1-dump.sql

# 2. Primo avvio della nuova applicazione sul database vuoto: Flyway crea lo schema, poi fermala.

# 3. Conversione e import
python3 -I scripts/migrate_d1_to_postgres.py d1-dump.sql > import.sql
psql "postgresql://barizone:PASSWORD@HOST:5432/barizone" -v ON_ERROR_STOP=1 -f import.sql

# 4. Riavvia l'applicazione e verifica (vedi checklist)
```

Lo script è idempotente e importa tutto in un'unica transazione: se qualcosa fallisce, non resta
nulla di parziale.

**Chiavi VAPID:** lo script migra la riga `vapid`. Non impostare `VAPID_PUBLIC_KEY`/`VAPID_PRIVATE_KEY`
con valori diversi: tutti i dispositivi già iscritti smetterebbero di ricevere notifiche.

**Aziende iniziali:** la migrazione `V3` inserisce all'avvio sei aziende dal vecchio archivio (Sartoria Miglionico,
Caffè del Corso, Libreria Roma, Beauty Studio Poggiofranco, Ciclofficina Japigia, Osteria del Mare Torre), pubblicate ma senza
partita IVA né dati legali, da completare dall'area admin. Se il tuo archivio D1 non le contiene e non le vuoi,
eliminale dall'area admin o dal database prima di aprire il sito. Le aziende segnate come demo in D1 non vengono importate.

**Sessioni:** non vengono migrate. L'admin deve rifare l'accesso (gli account utente e attività non esistono in D1: si registrano di nuovo); i clienti dei gruppi
mantengono il proprio cookie di adesione.

## Checklist di messa in produzione

- [ ] `PUBLIC_ORIGIN` è uguale al dominio reale, in https.
- [ ] `SESSION_SECRET` è casuale e di almeno 32 byte (`openssl rand -hex 32`). In alternativa,
      `ADMIN_PASSWORD` è vuota e si usano solo account nel database.
- [ ] `GET /api/push/key` restituisce la **stessa** chiave pubblica della versione Worker.
- [ ] Il login admin funziona con le credenziali esistenti.
- [ ] `GET /sitemap.xml` usa il dominio corretto.
- [ ] Una notifica di prova dall'area admin arriva su un dispositivo reale.
- [ ] Un'iscrizione di prova riceve l'email (se `NEWS_EMAIL_ENABLED=true`).
- [ ] **Account:** `NEWS_EMAIL_ENABLED=true` e `RESEND_API_KEY` impostati (senza email nessuno riceve il link di conferma e non può accedere).
      Registra un utente di prova, apri il link e accedi; registra un'attività di prova e approvala dall'area admin.
- [ ] **Testi legali:** le caselle di registrazione rimandano a "condizioni di utilizzo e informativa sulla privacy",
      ma nel progetto non esistono pagine con questi testi. Vanno redatti (e collegati alle caselle) prima di aprire le registrazioni.
- [ ] Il backup automatico del database è attivo e un ripristino di prova è riuscito.
- [ ] `/actuator/prometheus` e `/api/admin/docs` rispondono solo all'admin (verificato dai test).
