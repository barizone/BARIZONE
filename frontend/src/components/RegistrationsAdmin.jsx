import React, { useCallback, useEffect, useState } from 'react';
import axios from 'axios';
import { API } from '@/context/AuthContext';
import { Alert, Notice, buttonClass, errorText, inputClass } from '@/components/FormParts';

const TABS = [['pending', 'Da verificare'], ['rejected', 'Rifiutate'], ['approved', 'Approvate']];

const ROWS = [
  ['Ragione sociale', 'legal_name'], ['Partita IVA', 'vat_number'], ['Codice fiscale', 'tax_code'], ['Forma giuridica', 'entity_type'],
  ['Titolare', 'owner_name'], ['Sede legale', b => `${b.legal_address}, ${b.postal_code} ${b.city} (${b.province})`], ['PEC', 'pec'],
  ['Email', 'email'], ['Telefono', 'phone'], ['Categoria', 'category'], ['Quartiere', 'quartiere'], ['Indirizzo attività', 'address'], ['Descrizione', 'description'],
];

/** Verifica delle registrazioni: l'admin controlla i dati e approva (pubblica) o rifiuta con un motivo. */
export default function RegistrationsAdmin() {
  const [status, setStatus] = useState('pending');
  const [items, setItems] = useState([]);
  const [notes, setNotes] = useState({});
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState('');

  const load = useCallback(async () => {
    try { setItems((await axios.get(`${API}/admin/registrations`, { params: { status } })).data); setError(''); } catch (e) { setError(errorText(e)); }
  }, [status]);
  useEffect(() => { load(); }, [load]);

  async function decide(id, action) {
    setBusy(id); setError(''); setNotice('');
    try {
      await axios.post(`${API}/admin/registrations/${id}/${action}`, action === 'reject' ? { note: notes[id] || '' } : {});
      setNotice(action === 'approve' ? 'Registrazione approvata: la scheda è pubblicata e il titolare è stato avvisato.' : 'Registrazione rifiutata: il titolare ha ricevuto il motivo.');
      await load();
    } catch (e) { setError(errorText(e)); } finally { setBusy(''); }
  }

  return (
    <section className="max-w-6xl mx-auto px-4 py-6">
      <h2 className="font-serif-display text-2xl font-bold">Registrazioni delle attività</h2>
      <p className="font-body text-sm mt-1">Controlla partita IVA, ragione sociale e PEC prima di approvare. L’approvazione pubblica subito la scheda.</p>
      <div className="flex flex-wrap gap-2 my-4" role="tablist">
        {TABS.map(([key, label]) => (
          <button key={key} role="tab" aria-selected={status === key} onClick={() => setStatus(key)}
            className={`min-h-11 px-4 rounded-md ${status === key ? 'bg-[#0B2545] text-white' : 'border border-[#0B2545]'}`}>{label}</button>
        ))}
      </div>
      <Notice>{notice}</Notice><Alert>{error}</Alert>
      {items.length === 0 && !error && <p className="font-body text-sm py-4">Nessuna registrazione in questo stato.</p>}
      {items.map(({ business: b, account_email: mail, account_name: name }) => (
        <article key={b.id} className="border border-[#E5DFD3] rounded-lg bg-white p-4 mb-4">
          <h3 className="font-serif-display text-xl font-bold">{b.business_name}</h3>
          <p className="font-body text-xs text-slate-600">Account: {name} · {mail} · registrata il {b.registration_at ? new Date(b.registration_at).toLocaleString('it-IT') : '—'}</p>
          <dl className="grid sm:grid-cols-[200px_1fr] gap-x-4 gap-y-1 mt-3 font-body text-sm">
            {ROWS.map(([label, key]) => (
              <React.Fragment key={label}><dt className="font-semibold">{label}</dt><dd className="break-words">{typeof key === 'function' ? key(b) : b[key] || '—'}</dd></React.Fragment>
            ))}
          </dl>
          {b.registration_status === 'rejected' && b.review_note && <p className="font-body text-sm mt-3"><strong>Motivo del rifiuto:</strong> {b.review_note}</p>}
          {b.registration_status === 'pending' && (
            <div className="mt-4">
              <label className="block font-ui text-sm font-semibold">Motivo (obbligatorio solo per rifiutare)
                <textarea rows={2} maxLength={500} value={notes[b.id] || ''} onChange={e => setNotes(n => ({ ...n, [b.id]: e.target.value }))} className={inputClass} />
              </label>
              <div className="flex flex-wrap gap-3 mt-3">
                <button disabled={busy === b.id} onClick={() => decide(b.id, 'approve')} className={buttonClass}>Approva e pubblica</button>
                <button disabled={busy === b.id || !(notes[b.id] || '').trim()} onClick={() => decide(b.id, 'reject')} className="min-h-12 px-5 rounded-md border border-red-700 text-red-800 font-semibold disabled:opacity-40">Rifiuta</button>
              </div>
            </div>
          )}
        </article>
      ))}
    </section>
  );
}
