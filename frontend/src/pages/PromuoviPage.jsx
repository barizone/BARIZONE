import React from "react";
import { Link } from "react-router-dom";
import { Store } from "lucide-react";

export default function PromuoviPage() {
  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      <div className="border-b border-[#E5DFD3] pb-6">
        <div className="font-mono-accent text-[10px] uppercase tracking-widest text-[#D9531E] flex items-center gap-2"><Store className="w-3 h-3" aria-hidden="true" /> Per gli esercenti</div>
        <h1 className="font-serif-display text-4xl sm:text-5xl font-black text-[#0B2545] tracking-tight mt-1">La tua attività su Barizone</h1>
        <p className="font-body text-slate-600 mt-3">Registra la tua attività con i dati legali: la redazione li verifica e, dopo l’approvazione, la scheda viene pubblicata sul sito.</p>
      </div>

      <ol className="mt-6 space-y-3 font-body list-decimal pl-5">
        <li><strong>Registrati</strong> con i dati dell’attività: ragione sociale, partita IVA, codice fiscale, sede, PEC e contatti.</li>
        <li><strong>Conferma l’email</strong> con il link che ricevi.</li>
        <li><strong>La redazione verifica</strong> i dati. Se qualcosa non torna ti scriviamo il motivo e puoi correggere.</li>
        <li><strong>La scheda è online</strong> con foto, slogan, informazioni e il collegamento “Dove trovarmi”. Puoi invitare i clienti con un QR code dedicato e scrivere loro.</li>
      </ol>

      <div className="flex flex-col sm:flex-row gap-3 mt-8">
        <Link to="/registra-attivita" className="inline-flex items-center justify-center min-h-12 px-5 rounded-md bg-[#0B2545] text-white font-ui font-semibold">Registra la tua attività</Link>
        <Link to="/accedi" className="inline-flex items-center justify-center min-h-12 px-5 rounded-md border border-[#0B2545] text-[#0B2545] font-ui font-semibold">Hai già un account? Accedi</Link>
      </div>

      <div className="mt-8 p-6 bg-[#F8F5EE] border border-[#E5DFD3] rounded-md">
        <h2 className="font-serif-display text-xl font-bold text-[#0B2545]">Hai bisogno di aiuto?</h2>
        <p className="font-body text-sm text-slate-600 mt-2">Per domande sulla registrazione o su altre forme di visibilità, scrivi alla redazione: <a className="underline" href="mailto:info@barizone.it">info@barizone.it</a>.</p>
      </div>
      <Link to="/aziende" className="inline-flex items-center min-h-11 mt-6 font-ui text-sm text-[#D9531E] underline">Esplora le attività</Link>
    </div>
  );
}
