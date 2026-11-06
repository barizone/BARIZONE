import React, { useCallback, useEffect, useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { API, useAuth } from '@/context/AuthContext';
import BusinessFields, { EMPTY_BUSINESS } from '@/components/BusinessFields';
import { Alert, Notice, Page, buttonClass, linkClass, errorText } from '@/components/FormParts';

/** Area dell'utente registrato: dati dell'account e attività a cui ha aderito. */
export function ProfilePage() {
  const { account, loading, accountLogout } = useAuth();
  const navigate = useNavigate();
  const [inbox, setInbox] = useState({ memberships: [], messages: [] });
  const [error, setError] = useState('');

  useEffect(() => {
    if (account?.role !== 'user') return;
    axios.get(`${API}/groups/inbox`).then(r => setInbox(r.data)).catch(e => setError(errorText(e)));
  }, [account]);

  if (loading) return null;
  if (!account) return <Navigate to="/accedi?next=%2Fprofilo" replace />;
  if (account.role === 'merchant') return <Navigate to="/area-attivita" replace />;

  const active = inbox.memberships.filter(m => m.active);
  return (
    <Page title={`Ciao ${account.first_name}`} subtitle={account.email}>
      <h2 className="font-serif-display text-xl font-bold text-[#0B2545]">Le tue adesioni</h2>
      <Alert>{error}</Alert>
      {active.length === 0
        ? <p className="font-body text-sm my-3">Non hai ancora aderito a nessuna attività. Scopri le <Link to="/aziende" className={linkClass}>attività del tuo quartiere</Link> o scansiona il QR esposto in negozio.</p>
        : <ul className="my-3">{active.map(m => (
          <li key={m.id} className="border-b border-[#E5DFD3]">
            <Link to={`/adesioni?azienda=${encodeURIComponent(m.shop_id)}`} className="flex items-center justify-between min-h-12 text-[#0B2545] font-semibold">
              <span>{m.shop_name}</span><span aria-hidden="true">›</span>
            </Link>
          </li>))}</ul>}
      <p className="font-body text-sm mt-2">Le comunicazioni ricevute si vedono nella pagina di ciascuna attività. Le notifiche sul telefono si attivano da lì, con il tuo consenso.</p>
      <button type="button" className={`${buttonClass} mt-6 w-full`} onClick={async () => { await accountLogout(); navigate('/'); }}>Esci</button>
    </Page>
  );
}

const STATUS = {
  pending: { tone: 'bg-amber-50 text-amber-950', title: 'Registrazione in verifica', text: 'La redazione sta controllando partita IVA e dati. Appena la scheda sarà pubblicata ti avviseremo per email.' },
  approved: { tone: 'bg-green-50 text-green-900', title: 'Scheda pubblicata', text: 'La tua attività è visibile sul sito.' },
  rejected: { tone: 'bg-red-50 text-red-800', title: 'Servono modifiche', text: 'Correggi i dati indicati qui sotto e invia di nuovo la registrazione.' },
};

/** Area dell'attività: stato della registrazione, modifica dei dati, accesso a clienti e comunicazioni. */
export function BusinessAreaPage() {
  const { account, loading, accountLogout, checkAuth } = useAuth();
  const navigate = useNavigate();
  const [business, setBusiness] = useState(null);
  const [form, setForm] = useState(EMPTY_BUSINESS);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    const { data } = await axios.get(`${API}/account/business`);
    setBusiness(data);
    setForm(Object.fromEntries(Object.keys(EMPTY_BUSINESS).map(k => [k, data[k] ?? ''])));
  }, []);

  useEffect(() => {
    if (account?.role === 'merchant') load().catch(e => setError(errorText(e)));
  }, [account, load]);

  if (loading) return null;
  if (!account) return <Navigate to="/accedi" replace />;
  if (account.role !== 'merchant') return <Navigate to="/profilo" replace />;

  async function save(e) {
    e.preventDefault(); setBusy(true); setError(''); setNotice('');
    try {
      const { data } = await axios.put(`${API}/account/business`, form);
      await load(); await checkAuth();
      setNotice(data.registration_status === 'pending' && business?.registration_status !== 'pending'
        ? 'Dati salvati. Per questa modifica serve una nuova verifica della redazione: la scheda tornerà online dopo l’approvazione.'
        : 'Dati salvati.');
      window.scrollTo({ top: 0 });
    } catch (err) { setError(errorText(err)); } finally { setBusy(false); }
  }

  const status = business ? STATUS[business.registration_status] : null;
  const published = business?.registration_status === 'approved' && business?.status === 'published';

  return (
    <Page wide title={business?.business_name || 'La tua attività'} subtitle={account.email}>
      {status && (
        <section className={`p-4 rounded-md mb-5 ${status.tone}`}>
          <h2 className="font-serif-display text-lg font-bold">{status.title}</h2>
          <p className="font-body text-sm mt-1">{status.text}</p>
          {business.registration_status === 'rejected' && business.review_note && <p className="font-body text-sm mt-2"><strong>Motivo:</strong> {business.review_note}</p>}
        </section>
      )}
      {published && (
        <div className="flex flex-col gap-1 mb-5">
          <Link to={`/aziende/${business.id}`} className={linkClass}>Vedi la scheda pubblicata</Link>
          <Link to="/gestione-clienti" className={linkClass}>QR, clienti e comunicazioni</Link>
        </div>
      )}
      <Notice>{notice}</Notice>
      <Alert>{error}</Alert>
      {business && (
        <form onSubmit={save}>
          <BusinessFields value={form} onChange={setForm} />
          <p className="font-body text-xs text-slate-600 my-3">Se cambi ragione sociale, partita IVA o codice fiscale la scheda esce dal sito finché la redazione non li verifica di nuovo.</p>
          <button disabled={busy} className={`${buttonClass} w-full sm:w-auto`}>{busy ? 'Salvataggio…' : business.registration_status === 'rejected' ? 'Salva e invia di nuovo' : 'Salva le modifiche'}</button>
        </form>
      )}
      <button type="button" className="underline mt-6 inline-flex items-center min-h-11" onClick={async () => { await accountLogout(); navigate('/'); }}>Esci</button>
    </Page>
  );
}
