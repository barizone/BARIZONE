// Voci di navigazione condivise da header (desktop), barra in basso e pannello "Altro" (mobile).
export const TABS = [
  { to: '/', label: 'Home', icon: 'home' },
  { to: '/notizie', label: 'Notizie', icon: 'news' },
  { to: '/quartieri', label: 'Quartieri', icon: 'map' },
  { to: '/aziende', label: 'Aziende', icon: 'shop' },
];

export const MORE = [
  { to: '/eventi', label: 'Eventi' },
  { to: '/farmacie', label: 'Farmacie di turno' },
  { to: '/archivio', label: 'Archivio notizie' },
  { to: '/notifiche', label: 'Notifiche' },
  { to: '/iscriviti-news', label: 'Iscriviti alle news' },
  { to: '/promuovi', label: 'Promuovi la tua attività' },
  { to: '/contatti', label: 'Contatti' },
];

export const DESKTOP_NAV = [
  ['/', 'Home'], ['/notizie', 'Notizie'], ['/quartieri', 'Quartieri'], ['/aziende', 'Aziende'],
  ['/iscriviti-news', 'Iscriviti alle news'], ['/archivio', 'Archivio'], ['/eventi', 'Eventi'],
  ['/farmacie', 'Farmacie'], ['/notifiche', 'Notifiche'], ['/promuovi', 'Promuovi'], ['/contatti', 'Contatti'],
];

/** Una voce è attiva anche nelle sottopagine (es. /aziende/123 → Aziende). */
export const isActive = (pathname, to) => (to === '/' ? pathname === '/' : pathname === to || pathname.startsWith(to + '/'));
