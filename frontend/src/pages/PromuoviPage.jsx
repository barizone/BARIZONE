import React from "react";
import { Link } from "react-router-dom";
import { Store } from "lucide-react";

export default function PromuoviPage() {
  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      <div className="border-b border-[#E5DFD3] pb-6">
        <div className="font-mono-accent text-[10px] uppercase tracking-widest text-[#D9531E] flex items-center gap-2"><Store className="w-3 h-3"/> Per gli esercenti</div>
        <h1 className="font-serif-display text-4xl sm:text-5xl font-black text-[#0B2545] tracking-tight mt-1">La tua attività su Barizone</h1>
        <p className="font-body text-slate-600 mt-3">Le schede delle aziende sono compilate e pubblicate dall’amministratore di Barizone dopo un accordo con l’esercente. Nessuna registrazione o attivazione automatica.</p>
      </div>
      <p className="mt-5">Foto, slogan, informazioni dell’attività e collegamento “Dove trovarmi”. Ogni esercente potrà invitare i propri clienti tramite un QR code dedicato.</p>
      <Link to="/gestione-clienti" className="inline-block mt-5 underline">Gestione QR, clienti e notifiche →</Link>
      <div className="mt-8 p-6 bg-[#F8F5EE] border border-[#E5DFD3] rounded-md">
        <h2 className="font-serif-display text-xl font-bold text-[#0B2545]">Richiedi informazioni</h2>
        <p className="font-body text-sm text-slate-600 mt-2">Per valutare l’adesione e pubblicare la vetrina della tua attività, contatta la redazione.</p>
      </div>
      <Link to="/aziende" className="inline-block mt-6 font-ui text-sm text-[#D9531E] underline">Esplora le attività</Link>
    </div>
  );
}


