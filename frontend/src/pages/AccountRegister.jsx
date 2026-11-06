import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import axios from 'axios';
import { API } from '@/context/AuthContext';
import BusinessFields, { EMPTY_BUSINESS } from '@/components/BusinessFields';
import { Alert, Notice, Page, TextField, buttonClass, linkClass, errorText } from '@/components/FormParts';

const PASSWORD_HINT = 'Almeno 10 caratteri, con almeno una lettera e un numero.';

function Sent({ outcome, business }) {
  const failed = outcome.email_status === 'failed' || outcome.email_status === 'disabled';
  return (
    <>
      <Notice>
        {outcome.verification_required
          ? 'Registrazione ricevuta. Ti abbiamo inviato un’email: apri il link per confermare l’indirizzo e attivare l’account (valido 48 ore).'
          : 'Registrazione completata. Ora puoi accedere.'}
      </Notice>
      {failed && <Alert>Non siamo riusciti a inviare l’email. Prova ad accedere: potrai richiedere un nuovo link, oppure scrivi a info@barizone.it.</Alert>}
      {business && <p className="font-body text-sm my-3">La redazione verificherà partita IVA e dati prima di pubblicare la scheda: ti avviseremo per email.</p>}
      <Link to="/accedi" className={`${buttonClass} w-full`}>Vai all’accesso</Link>
    </>
  );
}

export function RegisterUserPage() {
  const [form, setForm] = useState({ first_name: '', last_name: '', email: '', password: '', accept_terms: false });
  const [error, setError] = useState('');
  const [outcome, setOutcome] = useState(null);
  const [busy, setBusy] = useState(false);
  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));
  async function submit(e) {
    e.preventDefault(); setBusy(true); setError('');
    try { setOutcome((await axios.post(`${API}/account/register`, form)).data); } catch (err) { setError(errorText(err)); } finally { setBusy(false); }
  }
  return (
    <Page title="Registrati" subtitle="Con un account ritrovi le tue adesioni alle attività su ogni dispositivo.">
      {outcome ? <Sent outcome={outcome} /> : (
        <form onSubmit={submit}>
          <TextField label="Nome" name="first_name" value={form.first_name} onChange={set} maxLength={80} autoComplete="given-name" />
          <TextField label="Cognome" name="last_name" value={form.last_name} onChange={set} maxLength={80} autoComplete="family-name" />
          <TextField label="Email" name="email" type="email" inputMode="email" autoComplete="username" value={form.email} onChange={set} maxLength={254} />
          <TextField label="Password" name="password" type="password" autoComplete="new-password" value={form.password} onChange={set} maxLength={72} minLength={10} hint={PASSWORD_HINT} />
          <label className="flex items-start gap-3 font-body text-sm my-4 min-h-11">
            <input type="checkbox" required checked={form.accept_terms} onChange={e => set('accept_terms', e.target.checked)} className="mt-1 h-5 w-5 shrink-0" />
            <span>Accetto le condizioni di utilizzo e confermo di aver letto l’informativa sulla privacy di Barizone.</span>
          </label>
          <Alert>{error}</Alert>
          <button disabled={busy} className={`${buttonClass} w-full`}>{busy ? 'Invio…' : 'Crea l’account'}</button>
        </form>
      )}
      <p className="mt-4 font-body text-sm">Hai già un account? <Link to="/accedi" className={linkClass}>Accedi</Link></p>
    </Page>
  );
}

export function RegisterBusinessPage() {
  const [account, setAccount] = useState({ first_name: '', last_name: '', email: '', password: '' });
  const [business, setBusiness] = useState(EMPTY_BUSINESS);
  const [declarations, setDeclarations] = useState({ accept_terms: false, represents_business: false });
  const [error, setError] = useState('');
  const [outcome, setOutcome] = useState(null);
  const [busy, setBusy] = useState(false);
  const setA = (k, v) => setAccount(a => ({ ...a, [k]: v }));
  const setD = (k, v) => setDeclarations(d => ({ ...d, [k]: v }));

  async function submit(e) {
    e.preventDefault(); setBusy(true); setError('');
    try {
      setOutcome((await axios.post(`${API}/account/register-business`, { ...account, ...declarations, business })).data);
      window.scrollTo({ top: 0 });
    } catch (err) { setError(errorText(err)); } finally { setBusy(false); }
  }

  return (
    <Page wide title="Registra la tua attività" subtitle="Compila tutti i dati richiesti: la redazione verifica la partita IVA e pubblica la scheda sul sito.">
      {outcome ? <Sent outcome={outcome} business /> : (
        <form onSubmit={submit} noValidate={false}>
          <fieldset className="mb-6">
            <legend className="font-serif-display text-xl font-bold text-[#0B2545] mb-3">Il tuo account</legend>
            <div className="grid sm:grid-cols-2 gap-x-3">
              <TextField label="Nome" name="first_name" value={account.first_name} onChange={setA} maxLength={80} autoComplete="given-name" />
              <TextField label="Cognome" name="last_name" value={account.last_name} onChange={setA} maxLength={80} autoComplete="family-name" />
            </div>
            <TextField label="Email di accesso" name="email" type="email" inputMode="email" autoComplete="username" value={account.email} onChange={setA} maxLength={254} />
            <TextField label="Password" name="password" type="password" autoComplete="new-password" value={account.password} onChange={setA} maxLength={72} minLength={10} hint={PASSWORD_HINT} />
          </fieldset>

          <BusinessFields value={business} onChange={setBusiness} />

          <fieldset className="my-6">
            <legend className="font-serif-display text-xl font-bold text-[#0B2545] mb-3">Dichiarazioni</legend>
            <label className="flex items-start gap-3 font-body text-sm mb-3 min-h-11">
              <input type="checkbox" required checked={declarations.represents_business} onChange={e => setD('represents_business', e.target.checked)} className="mt-1 h-5 w-5 shrink-0" />
              <span>Dichiaro di essere il titolare o il legale rappresentante dell’attività, o di essere autorizzato a registrarla, e che i dati inseriti sono veritieri.</span>
            </label>
            <label className="flex items-start gap-3 font-body text-sm min-h-11">
              <input type="checkbox" required checked={declarations.accept_terms} onChange={e => setD('accept_terms', e.target.checked)} className="mt-1 h-5 w-5 shrink-0" />
              <span>Accetto le condizioni di utilizzo e confermo di aver letto l’informativa sulla privacy di Barizone.</span>
            </label>
          </fieldset>

          <Alert>{error}</Alert>
          <button disabled={busy} className={`${buttonClass} w-full sm:w-auto`}>{busy ? 'Invio…' : 'Invia la registrazione'}</button>
        </form>
      )}
      <p className="mt-6 font-body text-sm">Hai già un account? <Link to="/accedi" className={linkClass}>Accedi</Link></p>
    </Page>
  );
}
