import React from 'react';

const links = [
  ['Turni delle farmacie di Bari', 'https://www.ordinefarmacistibaribat.it/turni-farmacie/farmacie-in-bari/', 'Consulta il calendario ufficiale e controlla la data del turno.'],
  ['Elenco delle farmacie e recapiti', 'https://www.ordinefarmacistibaribat.it/cittadino/elenco-delle-farmacie/', 'Seleziona il comune di Bari per trovare indirizzi, telefoni e mappe.'],
  ['Aperture prolungate', 'https://www.ordinefarmacistibaribat.it/turni-farmacie/estensione-orario/', 'Seleziona Bari e controlla il periodo di validità delle indicazioni.'],
];

export default function Pharmacies() {
  return <div className="max-w-5xl mx-auto px-4 py-10 text-[#0B2545]">
    <h1 className="font-serif-display text-4xl font-bold">Farmacie di Bari</h1>
    <p className="mt-4">Turni, indirizzi e aperture pubblicati dall’Ordine Interprovinciale dei Farmacisti di Bari e Barletta-Andria-Trani.</p>
    <p className="mt-3 text-sm text-slate-600">Collegamenti verificati l’8 ottobre 2026. La suddivisione delle farmacie per quartiere non è ancora disponibile su Barizone. Turno di servizio e orario ordinario sono informazioni diverse.</p>
    <div className="grid md:grid-cols-3 gap-5 mt-8">{links.map(([title,url,description]) => <article key={url} className="bg-white border border-[#E5DFD3] rounded-md p-6"><h2 className="font-serif-display text-2xl font-bold">{title}</h2><p className="mt-4">{description}</p><a href={url} target="_blank" rel="noopener noreferrer" className="inline-block mt-5 underline font-semibold">Consulta la fonte ufficiale</a></article>)}</div>
    <p className="mt-8">Prima di recarti in farmacia, verifica telefonicamente disponibilità e modalità di accesso, soprattutto durante il turno notturno.</p>
  </div>;
}
