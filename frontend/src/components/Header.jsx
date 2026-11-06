import React, { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { Menu, X, LogOut } from 'lucide-react';
import NewsTicker from './NewsTicker';
import TrafficTicker from './TrafficTicker';
import RadioSoundCityLogo from './RadioSoundCityLogo';
import { DESKTOP_NAV, isActive } from './navigation';
import './Header.css';

/**
 * Header in due parti:
 *  - barra superiore fissa e compatta (logo, radio, iscrizione; menu completo su desktop);
 *  - email e ticker che scorrono via con la pagina, così su telefono non occupano lo schermo.
 * Su mobile la navigazione è nella barra in basso (MobileNav).
 */
// Pagine di moduli e aree riservate: niente barra email né ticker, così il modulo parte subito sotto l'intestazione.
const FOCUSED_PAGES = ['/accedi', '/registrati', '/registra-attivita', '/verifica-email', '/password-dimenticata',
  '/reimposta-password', '/profilo', '/area-attivita', '/login', '/admin', '/gestione-clienti', '/esercente', '/adesioni', '/iscriviti-news'];

export default function Header() {
  const { user, logout, account, accountLogout } = useAuth();
  const [open, setOpen] = useState(false);
  const { pathname } = useLocation();
  const nav = user?.role === 'admin' ? [...DESKTOP_NAV, ['/admin', 'Admin']] : DESKTOP_NAV;
  const links = nav.map(([to, label]) => (
    <Link key={to} to={to} onClick={() => setOpen(false)} aria-current={isActive(pathname, to) ? 'page' : undefined}
      className={`inline-flex items-center min-h-11 px-3 rounded-md font-ui text-sm ${isActive(pathname, to) ? 'font-semibold text-[#D9531E]' : 'text-[#0B2545] hover:bg-[#F8F5EE]'}`}>{label}</Link>
  ));
  return (
    <>
      <header className="site-header">
        <div className="max-w-7xl mx-auto px-3 sm:px-6">
          <div className="barizone-masthead">
            <button type="button" aria-label={open ? 'Chiudi menu' : 'Apri menu'} aria-controls="barizone-menu" aria-expanded={open}
              onClick={() => setOpen(v => !v)} className="masthead-menu">
              {open ? <X size={24} /> : <Menu size={24} />}<span>Menu</span>
            </button>
            <div className="header-brand-group masthead-brands">
              <Link to="/" aria-label="Barizone — homepage" onClick={() => setOpen(false)} className="block shrink-0">
                <img src="/logo-224.webp" alt="Barizone, l’app dei Baresi" width="1254" height="1254" className="masthead-logo" fetchpriority="high" />
              </Link>
              <a className="header-radio-mark" href="https://www.radiosoundcity.net/radio/" target="_blank" rel="noopener noreferrer"
                aria-label="Ascolta ora Radio Sound City Network (apre una nuova scheda)" title="Ascolta ora Radio Sound City Network">
                <RadioSoundCityLogo className="header-radio-image" /><span className="header-radio-caption">Ascolta ora ↗</span>
              </a>
            </div>
            <Link to="/iscriviti-news" onClick={() => setOpen(false)} className="masthead-subscribe" aria-label="Iscriviti alle news"
              aria-current={pathname === '/iscriviti-news' ? 'page' : undefined}><span>Iscriviti</span><span>alle news</span></Link>
          </div>
          <nav id="barizone-menu" aria-label="Menu principale" hidden={!open}
            className={open ? 'grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-1 py-3 border-t border-[#E5DFD3]' : ''}>
            {links}
            {account && <Link to={account.role === 'merchant' ? '/area-attivita' : '/profilo'} onClick={() => setOpen(false)} className="inline-flex items-center min-h-11 px-3 rounded-md font-ui text-sm font-semibold text-[#0B2545]">{account.role === 'merchant' ? 'Area attività' : 'Il mio profilo'}</Link>}
            {!account && !user && <>
              <Link to="/accedi" onClick={() => setOpen(false)} className="inline-flex items-center min-h-11 px-3 rounded-md font-ui text-sm font-semibold text-[#0B2545]">Accedi</Link>
              <Link to="/registrati" onClick={() => setOpen(false)} className="inline-flex items-center min-h-11 px-3 rounded-md font-ui text-sm text-[#0B2545]">Registrati</Link>
              <Link to="/registra-attivita" onClick={() => setOpen(false)} className="inline-flex items-center min-h-11 px-3 rounded-md font-ui text-sm text-[#0B2545]">Registra la tua attività</Link>
            </>}
            {account && <button type="button" onClick={() => { accountLogout(); setOpen(false); }} className="inline-flex items-center gap-2 min-h-11 px-3 rounded-md font-ui text-sm text-left"><LogOut size={18} />Esci</button>}
            {user
              ? <button type="button" onClick={() => { logout(); setOpen(false); }} className="inline-flex items-center gap-2 min-h-11 px-3 rounded-md font-ui text-sm text-left"><LogOut size={18} />Esci (redazione)</button>
              : <Link to="/login" onClick={() => setOpen(false)} className="inline-flex items-center min-h-11 px-3 rounded-md font-ui text-sm text-slate-600">Accesso redazione</Link>}
          </nav>
        </div>
      </header>
      {!FOCUSED_PAGES.includes(pathname) && <div className="header-extras">
        <div className="border-t border-[#E5DFD3] bg-[#F8F5EE] px-4 py-2 text-center text-xs sm:text-sm text-[#0B2545]">
          Comunicazioni e news: <a href="mailto:info@barizone.it" className="font-semibold underline underline-offset-2 break-all hover:text-[#D9531E]">info@barizone.it</a>
        </div>
        <NewsTicker />
        <TrafficTicker />
      </div>}
    </>
  );
}
