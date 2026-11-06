import React, { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import axios from 'axios';
import { API, useAuth } from '@/context/AuthContext';
import { Alert, Notice, Page, TextField, buttonClass, linkClass, errorText } from '@/components/FormParts';

const safeNext = value => (value && value.startsWith('/') && !value.startsWith('//') ? value : null);

/** Accesso di utenti e attività. L'amministratore usa /login. */
export function AccountLoginPage() {
  const { setAccount } = useAuth();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [unverified, setUnverified] = useState(false);
  const [busy, setBusy] = useState(false);
  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));

  async function submit(e) {
    e.preventDefault();
    setBusy(true); setError(''); setNotice(''); setUnverified(false);
    try {
      const { data } = await axios.post(`${API}/account/login`, form);
      setAccount(data);
      navigate(data.role === 'merchant' ? '/area-attivita' : safeNext(params.get('next')) || '/profilo', { replace: true });
    } catch (err) {
      setError(errorText(err, 'Accesso non riuscito. Riprova.'));
      setUnverified(err.response?.data?.code === 'email_not_verified');
    } finally { setBusy(false); }
  }

  async function resend() {
    setBusy(true);
    try {
      await axios.post(`${API}/account/resend-verification`, { email: form.email });
      setNotice('Se l’indirizzo è in attesa di conferma, ti abbiamo inviato un nuovo link.'); setError(''); setUnverified(false);
    } catch (err) { setError(errorText(err)); } finally { setBusy(false); }
  }

  return (
    <Page title="Accedi" subtitle="Per utenti e attività registrate.">
      <form onSubmit={submit}>
        <TextField label="Email" name="email" type="email" inputMode="email" autoComplete="username" value={form.email} onChange={set} maxLength={254} />
        <TextField label="Password" name="password" type="password" autoComplete="current-password" value={form.password} onChange={set} maxLength={72} />
        <Alert>{error}</Alert><Notice>{notice}</Notice>
        {unverified && <button type="button" onClick={resend} disabled={busy} className={linkClass}>Invia di nuovo il link di conferma</button>}
        <button disabled={busy} className={`${buttonClass} w-full mt-2`}>{busy ? 'Accesso…' : 'Accedi'}</button>
      </form>
      <p className="mt-4"><Link to="/password-dimenticata" className={linkClass}>Password dimenticata?</Link></p>
      <hr className="my-5 border-[#E5DFD3]" />
      <p className="font-body text-sm">Non hai un account?</p>
      <div className="flex flex-col gap-1">
        <Link to="/registrati" className={linkClass}>Registrati come utente</Link>
        <Link to="/registra-attivita" className={linkClass}>Registra la tua attività</Link>
        <Link to="/login" className="underline text-sm text-slate-600 inline-flex items-center min-h-11">Accesso redazione</Link>
      </div>
    </Page>
  );
}

/** Pagina aperta dal link nell'email: conferma l'indirizzo e attiva l'account. */
export function VerifyEmailPage() {
  const [params] = useSearchParams();
  const token = params.get('token') || '';
  const [state, setState] = useState({ status: token ? 'loading' : 'error', message: token ? '' : 'Il collegamento non è valido.' });

  useEffect(() => {
    if (!token) return undefined;
    let active = true;
    axios.post(`${API}/account/verify-email`, { token })
      .then(r => { if (active) setState({ status: 'ok', role: r.data.role }); })
      .catch(e => { if (active) setState({ status: 'error', message: errorText(e, 'Il collegamento non è valido o è scaduto.') }); });
    return () => { active = false; };
  }, [token]);

  return (
    <Page title="Conferma email">
      {state.status === 'loading' && <p role="status">Verifica in corso…</p>}
      {state.status === 'ok' && <>
        <Notice>Indirizzo confermato. Ora puoi accedere.</Notice>
        {state.role === 'merchant' && <p className="font-body text-sm mb-3">La registrazione della tua attività sarà verificata dalla redazione: ti scriveremo appena la scheda sarà pubblicata.</p>}
        <Link to="/accedi" className={`${buttonClass} w-full`}>Vai all’accesso</Link>
      </>}
      {state.status === 'error' && <>
        <Alert>{state.message}</Alert>
        <p className="font-body text-sm">Se il link è scaduto, prova ad accedere: potrai richiederne uno nuovo.</p>
        <Link to="/accedi" className={linkClass}>Vai all’accesso</Link>
      </>}
    </Page>
  );
}

export function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [sent, setSent] = useState(false);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  async function submit(e) {
    e.preventDefault(); setBusy(true); setError('');
    try { await axios.post(`${API}/account/forgot-password`, { email }); setSent(true); } catch (err) { setError(errorText(err)); } finally { setBusy(false); }
  }
  return (
    <Page title="Password dimenticata" subtitle="Ti inviamo un link per sceglierne una nuova.">
      {sent ? <Notice>Se l’indirizzo è registrato, riceverai a breve un’email con il link (valido 1 ora). Controlla anche la cartella spam.</Notice> : (
        <form onSubmit={submit}>
          <TextField label="Email" name="email" type="email" inputMode="email" autoComplete="username" value={email} onChange={(_, v) => setEmail(v)} maxLength={254} />
          <Alert>{error}</Alert>
          <button disabled={busy} className={`${buttonClass} w-full`}>{busy ? 'Invio…' : 'Invia il link'}</button>
        </form>
      )}
      <p className="mt-4"><Link to="/accedi" className={linkClass}>Torna all’accesso</Link></p>
    </Page>
  );
}

export function ResetPasswordPage() {
  const [params] = useSearchParams();
  const token = params.get('token') || '';
  const [password, setPassword] = useState('');
  const [done, setDone] = useState(false);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  async function submit(e) {
    e.preventDefault(); setBusy(true); setError('');
    try { await axios.post(`${API}/account/reset-password`, { token, password }); setDone(true); } catch (err) { setError(errorText(err)); } finally { setBusy(false); }
  }
  return (
    <Page title="Nuova password">
      {done ? <><Notice>Password aggiornata. Per sicurezza abbiamo chiuso gli accessi aperti su altri dispositivi.</Notice><Link to="/accedi" className={`${buttonClass} w-full`}>Accedi</Link></> : (
        <form onSubmit={submit}>
          <TextField label="Nuova password" name="password" type="password" autoComplete="new-password" value={password} onChange={(_, v) => setPassword(v)} maxLength={72} minLength={10}
            hint="Almeno 10 caratteri, con almeno una lettera e un numero." />
          <Alert>{error}</Alert>
          <button disabled={busy || !token} className={`${buttonClass} w-full`}>{busy ? 'Salvataggio…' : 'Salva la password'}</button>
          {!token && <Alert>Il collegamento non è valido.</Alert>}
        </form>
      )}
    </Page>
  );
}
