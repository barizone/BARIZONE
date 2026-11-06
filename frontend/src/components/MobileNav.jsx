import React, { useEffect, useRef, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Home, Newspaper, MapPin, Store, Menu, X, LogOut, LogIn, Shield, User, Building2, UserPlus } from 'lucide-react';
import { useAuth } from '@/context/AuthContext';
import { TABS, MORE, isActive } from './navigation';
import './MobileNav.css';

const ICONS = { home: Home, news: Newspaper, map: MapPin, shop: Store };

/**
 * Navigazione mobile: barra fissa in basso (zona del pollice) con le 4 sezioni principali
 * e un pannello "Altro" che sale dal basso. Visibile solo sotto i 768 px.
 */
export default function MobileNav() {
  const { pathname } = useLocation();
  const { user, logout, account, accountLogout } = useAuth();
  const [open, setOpen] = useState(false);
  const sheetRef = useRef(null);
  const triggerRef = useRef(null);

  useEffect(() => setOpen(false), [pathname]);

  useEffect(() => {
    if (!open) return undefined;
    const previous = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    sheetRef.current?.querySelector('a,button')?.focus();
    const onKey = e => { if (e.key === 'Escape') setOpen(false); };
    document.addEventListener('keydown', onKey);
    return () => {
      document.body.style.overflow = previous;
      document.removeEventListener('keydown', onKey);
      triggerRef.current?.focus();
    };
  }, [open]);

  const moreActive = MORE.some(m => isActive(pathname, m.to)) || ['/admin', '/login', '/accedi', '/registrati', '/registra-attivita', '/profilo', '/area-attivita'].includes(pathname);

  return (
    <>
      {open && <div className="mobile-sheet-backdrop" onClick={() => setOpen(false)} aria-hidden="true" />}
      <div
        ref={sheetRef}
        id="mobile-more"
        role="dialog"
        aria-modal="true"
        aria-label="Altre sezioni"
        className={`mobile-sheet${open ? ' is-open' : ''}`}
        hidden={!open}
      >
        <div className="mobile-sheet-handle" aria-hidden="true" />
        <nav aria-label="Altre sezioni">
          <ul>
            {MORE.map(item => (
              <li key={item.to}>
                <Link to={item.to} aria-current={isActive(pathname, item.to) ? 'page' : undefined}>{item.label}</Link>
              </li>
            ))}
            {account && (
              <li><Link to={account.role === 'merchant' ? '/area-attivita' : '/profilo'}>
                {account.role === 'merchant' ? <Building2 size={18} aria-hidden="true" /> : <User size={18} aria-hidden="true" />}
                {account.role === 'merchant' ? 'Area attività' : 'Il mio profilo'}</Link></li>
            )}
            {!account && !user && (<>
              <li><Link to="/accedi"><LogIn size={18} aria-hidden="true" />Accedi</Link></li>
              <li><Link to="/registrati"><UserPlus size={18} aria-hidden="true" />Registrati</Link></li>
              <li><Link to="/registra-attivita"><Building2 size={18} aria-hidden="true" />Registra la tua attività</Link></li>
            </>)}
            {user?.role === 'admin' && (
              <li><Link to="/admin" aria-current={pathname === '/admin' ? 'page' : undefined}><Shield size={18} aria-hidden="true" />Area amministrazione</Link></li>
            )}
            {account && <li><button type="button" onClick={() => { accountLogout(); setOpen(false); }}><LogOut size={18} aria-hidden="true" />Esci</button></li>}
            <li>
              {user
                ? <button type="button" onClick={() => { logout(); setOpen(false); }}><LogOut size={18} aria-hidden="true" />Esci (redazione)</button>
                : <Link to="/login"><Shield size={18} aria-hidden="true" />Accesso redazione</Link>}
            </li>
          </ul>
        </nav>
        <a className="mobile-sheet-mail" href="mailto:info@barizone.it">Scrivici: info@barizone.it</a>
      </div>

      <nav className="mobile-tabbar" aria-label="Navigazione principale">
        {TABS.map(({ to, label, icon }) => {
          const Icon = ICONS[icon];
          const active = isActive(pathname, to);
          return (
            <Link key={to} to={to} className={active ? 'is-active' : ''} aria-current={active ? 'page' : undefined}>
              <Icon size={22} aria-hidden="true" strokeWidth={active ? 2.4 : 1.8} />
              <span>{label}</span>
            </Link>
          );
        })}
        <button
          ref={triggerRef}
          type="button"
          className={open || moreActive ? 'is-active' : ''}
          aria-expanded={open}
          aria-controls="mobile-more"
          onClick={() => setOpen(v => !v)}
        >
          {open ? <X size={22} aria-hidden="true" /> : <Menu size={22} aria-hidden="true" />}
          <span>Altro</span>
        </button>
      </nav>
    </>
  );
}
