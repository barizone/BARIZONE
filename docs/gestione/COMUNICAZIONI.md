# Barizone — circuito delle comunicazioni
## Come funziona, in parole semplici
1. La persona si iscrive alle news indicando nome, cognome, email e rione; il telefono resta facoltativo.
2. Barizone registra l’iscrizione e mostra subito un ringraziamento. Il ringraziamento via email parte soltanto dopo l’attivazione del servizio di invio.
3. La persona sceglie separatamente se attivare le notifiche sul telefono. L’indirizzo email e il numero non sostituiscono questo passaggio.
4. Le comunicazioni generali di Barizone raggiungono solo i dispositivi consenzienti: tutti quelli abilitati o il pubblico del rione selezionato.
5. La Bottega di Mario scrive dal proprio pannello soltanto agli utenti che hanno aderito al suo gruppo tramite il suo QR o la sua vetrina. Una campagna alla comunità generale viene gestita dall’amministratore, con il consenso previsto per quel pubblico.
6. La persona risponde alla redazione via email o WhatsApp dalla pagina Contatti. Le push non si spediscono da una casella email o da un numero telefonico: arrivano come notifiche di Barizone.

## Dove andare
- Iscrizione: https://barizone.it/iscriviti-news
- Notifiche sul telefono: https://barizone.it/notifiche
- Contatti: https://barizone.it/contatti
- Gestione generale: https://barizone.it/admin
- Numero WhatsApp e procedura: https://barizone.it/admin?sezione=contatti
- Gruppi delle aziende e QR: https://barizone.it/gestione-clienti

## Procedura per l’amministratore
1. Apri Amministrazione → Notifiche e campagne.
2. Scegli “Barizone · ringraziamenti, iniziative e omaggi” oppure “Campagna a pagamento di un’azienda”.
3. Inserisci titolo, testo e collegamento ai dettagli; scegli Tutta Bari o il rione.
4. Salva la bozza. Il salvataggio non invia messaggi.
5. Attiva le notifiche sul tuo dispositivo. Dalla bozza usa “Prova questa notifica solo a me”.
6. Controlla ciò che ricevi, poi usa “Invia campagna” e conferma.
7. Controlla il resoconto: “accettata dal servizio” non equivale a “consegnata” o “letta”.
Per un omaggio indica nella pagina di destinazione cosa si riceve, da chi, dove, entro quando e con quali condizioni. Nessun omaggio è stato inventato o inviato durante il lavoro.

## Procedura per la Bottega di Mario
1. L’amministratore assegna l’accesso alla sola attività.
2. Il cliente usa il QR, aderisce al gruppo e attiva le push.
3. La Bottega prepara la comunicazione nel proprio pannello e la invia al suo gruppo.
4. Il cliente può revocare l’adesione. L’azienda non ottiene automaticamente l’elenco email o telefono generale di Barizone.

## Contatti WhatsApp
L’amministratore può salvare un numero in modalità prova, visibile solo nel pannello. Il numero diventa pubblico soltanto selezionando “Pubblica questo numero come contatto ufficiale di Barizone” e salvando. Il pulsante apre la chat; il messaggio viene inviato dalla persona premendo Invia. Non è un sistema di WhatsApp automatici o di messaggi collettivi.

## Architettura essenziale
Un archivio su Cloudflare D1 conserva iscrizioni, dispositivi abilitati, consensi, gruppi e risultati degli invii. I servizi Worker controllano accessi e destinatari. Il sito e la web app condividono lo stesso pannello e gli stessi dati. Un servizio email invia da info@barizone.it; il servizio push usa il dispositivo abilitato; WhatsApp apre la chat con il contatto configurato.
Quando l’utente attiva le push dal percorso successivo all’iscrizione, il dispositivo viene collegato all’iscrizione; nell’area iscritti compare il numero dei dispositivi collegati. Le vecchie attivazioni non vengono attribuite automaticamente e le adesioni aziendali rimangono separate.

## Cosa è verificato e cosa resta da fare
- Test automatici: 14 gruppi di test superati, inclusi consensi, revoche, isolamento aziende, numero di prova non pubblico, ringraziamento email simulato, campagne proprie senza pagamento e assenza di duplicati.
- Interfaccia: provati iscrizione, ringraziamento, mantenimento del rione, contatto WhatsApp privato e salvataggio della bozza.
- Nessun messaggio di massa o messaggio WhatsApp è stato inviato.
- Al controllo iniziale del sito, confirmation_email_enabled era false: le email reali non erano attive.
- Per le email: verificare il dominio nel proprio account Resend; configurare RESEND_API_KEY come secret e NEWS_EMAIL_ENABLED=true nel Worker. Non modificare i record MX della casella esistente. Dettagli in EMAIL_SETUP.md.
- Dopo la configurazione, fare una nuova iscrizione con un indirizzo controllato e verificare davvero la posta ricevuta.
- La consegna push su Android richiede il test sul telefono con notifiche consentite. Un numero di telefono non è sufficiente per indirizzare una push.

## Timeline operativa proposta
| Quando | Azione | Prova da superare |
| --- | --- | --- |
| Oggi | Pubblicare il percorso guidato, contatti e campagne Barizone | Test automatici e verifica delle pagine |
| Giorno 1, dopo la disponibilità degli accessi | Collegare servizio email e scegliere il numero definitivo | Una email realmente ricevuta e apertura della chat corretta |
| Giorno 2 | Collaudo con 3–5 persone consenzienti a Carrassi e San Pasquale | Ricezione push, rione corretto, revoca e assenza di duplicati |
| Giorno 3 | Primo evento della Bottega di Mario per il suo gruppo | Il messaggio raggiunge solo gli aderenti previsti |
| Giorni 4–7 | Primo ringraziamento Barizone e controllo dei risultati | Resoconto invii, problemi corretti e frequenza sostenibile |
I tempi dipendono dalla configurazione dei servizi e dalla disponibilità dei partecipanti; non sono invii programmati.
