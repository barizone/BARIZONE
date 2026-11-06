# Simulazione condivisa Barizone

Percorso: `/simulazione`. Account demo pubblico `leonardo.demo`, password `BarizoneDemo2026!`.
Usare esclusivamente dati inventati. Questa sessione non concede accesso alle API di amministrazione reali.

1. Creare un’azienda, compilare la vetrina e salvarla. Il server assegna un UUID stabile all’attività.
2. Scaricare il suo QR oppure scansionarlo da un secondo dispositivo. Il QR contiene soltanto il collegamento Barizone con l’identificativo dell’azienda.
3. Iscriversi con un alias e consenso esplicito nel percorso cliente. I dati sono salvati in D1, in tabelle `demo_*` separate dalle attività e dagli iscritti reali.
4. Nel pannello dell’azienda, simulare una notifica. Il server registra la consegna esclusivamente alle adesioni attive di quell’attività al momento dell’invio.
5. Lasciare aperta la pagina cliente: le notifiche vengono aggiornate ogni 5 secondi. Non è un invio Web Push a pagina o browser chiusi.
6. Revocare l’adesione: non vengono create nuove consegne. I messaggi già ricevuti restano consultabili.

L’amministratore demo può generare un collegamento di ingresso esercente, valido una sola volta entro 12 ore. Aprirlo in una finestra privata o su un dispositivo separato per mantenere anche la sessione amministratore. Il server limita quell’accesso alla sola azienda assegnata, anche se si modificano gli ID delle richieste.

La sessione staff scade dopo 12 ore. L’identità cliente usa un cookie HttpOnly sul dispositivo: cancellando i cookie o cambiando browser si simula un nuovo cliente; non esiste ancora recupero tramite email o un account cliente condiviso tra dispositivi.

I dati del precedente laboratorio locale non vengono migrati automaticamente. Le nuove aziende sono create sul server.

Migrazione: `drizzle/20261022_demo_shared.sql`. Verifica: `node tests/demo.mjs`, incluso in `npm test`. La pipeline applica la migrazione prima del deploy.
