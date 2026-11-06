# Visibilità delle vetrine e banner giornaliero

Salvare una scheda crea una bozza. Per mostrarla ai visitatori usare “Salva e pubblica” oppure “Pubblica in galleria”. Il pannello indica lo stato in italiano, i campi mancanti e il collegamento “Vedi nel rione”. Nessuna attesa rende pubblica una bozza.

Le vetrine pubblicate compaiono nella pagina del rione, sotto il selettore e prima delle notizie. I nomi dei rioni vengono normalizzati anche per le schede esistenti. Le pagine controllano gli aggiornamenti ogni 30 secondi; ricaricando si vede subito il nuovo stato.

In cima alla home, prima di Barizone News, il banner “Vetrina del giorno” mostra una sola attività pubblicata. La scelta è registrata in D1 per la data locale Europe/Rome: un ricaricamento o l’aggiunta di altre aziende non cambia l’attività durante la giornata. A mezzanotte passa alla successiva attività disponibile; con una sola attività resta quella. Una scheda sospesa o archiviata viene sostituita senza aspettare il giorno seguente.

Se non ci sono attività pubblicate, il banner pubblico non mostra esempi inventati. L’amministratore vede un invito a pubblicare la prima scheda. Le risposte online del catalogo non vengono più sostituite dal vecchio riempitivo statico.

Migrazione: `20261025_business_spotlight.sql`. Verifica: `node tests/business-visibility.mjs`.
