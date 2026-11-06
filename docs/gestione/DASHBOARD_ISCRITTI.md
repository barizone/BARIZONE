# Dashboard iscritti e consenso agli aggiornamenti

Percorso amministratore: `/admin?sezione=iscritti`.

La dashboard legge l'archivio news tramite l'API riservata già esistente. Mostra iscrizioni totali, attive e revocate, nuove iscrizioni oggi/7/30 giorni (calendario Europe/Rome), iscritti con push collegate e numero dei dispositivi collegati. Ricerca e filtri combinabili per recapiti, quartiere, stato, dispositivo e date del consenso. Esportazione CSV dei risultati filtrati, con protezione dalle formule nei dati esportati e paginazione di 25 righe.

I totali riguardano soltanto l'archivio news, non gli account esercenti o le push anonime. La presenza di un dispositivo non dimostra che il browser consenta ancora le notifiche, né prova consegna o lettura. Il consenso è distinto dal permesso tecnico del dispositivo.

Il modulo di iscrizione presenta una sola casella esplicita per newsletter/email e push con notizie, iniziative e offerte. La versione `barizone-updates-v1` e la data sono registrate nella tabella `news_subscriber_consents`, creata dal gestore insieme all'archivio già esistente. I consensi precedenti non sono estesi automaticamente. Dopo una nuova iscrizione, un pulsante permette di chiedere il permesso al dispositivo e collegare la sottoscrizione push al token restituito dal server. Le richieste duplicate non restituiscono il token e non modificano il consenso precedente.

La revoca tramite il token dell'iscrizione annulla lo stato email e rimuove le push collegate alla stessa iscrizione in un'unica transazione. Non agisce sui dispositivi anonimi o su iscrizioni alle singole aziende.

Questa modifica unisce il consenso, non l'invio delle campagne. Il codice esistente invia l'email di benvenuto (se il servizio è configurato) e gestisce campagne push. Un compositore che invii lo stesso contenuto via newsletter e push richiede un ulteriore sviluppo. Nessuna comunicazione è inviata da dashboard, filtri o esportazione.

Verifiche: `npm test`, `npm run build`, prova browser della dashboard con dati inventati. Le consegne email/push reali e i dati di produzione richiedono verifica nell'area amministratore; non sono dimostrati dai test simulati.
