# Gestione effettiva di aziende, QR e clienti

- `/login` e `/admin`: account amministratore riservato, vetrine e iscritti alle news.
- L’account `leonardo.favia` usa una password generata fuori dal repository. In D1 è presente soltanto il relativo hash PBKDF2 con sale casuale; le sessioni sono token opachi, conservati nel browser in cookie HttpOnly e verificati sul server. La password non è quella dell’area demo.
- Il modulo aziende permette di contrassegnare una scheda come fittizia per le verifiche. La scheda segue lo stesso percorso di salvataggio, pubblicazione, foto, QR e adesioni delle altre. La natura fittizia è visibile nella vetrina.
- `/gestione-clienti`: attività pubblicate, QR personale, accesso dell’esercente, adesioni e comunicazioni. L’ID nel QR coincide con quello della vetrina reale.
- `/adesioni?azienda=ID`: percorso cliente, consenso specifico all’attività, revoca, comunicazioni e attivazione facoltativa delle notifiche sul dispositivo.
- `/esercente`: ingresso personale assegnato dall’amministratore, utilizzabile una volta entro 12 ore. La sessione è limitata alla sola azienda.
- Gli invii scrivono comunicazioni persistenti e tentano il Web Push esclusivamente per le adesioni attive dotate di una sottoscrizione valida. L’accettazione del servizio push non dimostra la lettura o la ricezione sul dispositivo.
- Il pannello distingue push accettate, dispositivi senza notifiche attive ed errori. Il completamento a lotti non reinvia le consegne già elaborate.
- I dati di verifica creati nella precedente area demo restano nelle tabelle dedicate e non vengono automaticamente pubblicati come attività effettive. `/simulazione` rimanda al pannello reale; le API demo pubbliche sono disattivate.

La raccolta delle iscrizioni news resta distinta dalle adesioni alle attività. L’invio automatico di email news non viene attivato da questa modifica.

Migrazioni: `20261023_live_accounts_groups.sql`, poi `20261024_owner_account.sql`.
