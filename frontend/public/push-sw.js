/* Barizone service worker: notifiche push + lettura offline delle ultime notizie. */
const CACHE = 'barizone-v2';
const SHELL = ['/', '/manifest.json', '/icon-192.png', '/logo-224.webp'];

self.addEventListener('install', event => {
  event.waitUntil(caches.open(CACHE).then(cache => cache.addAll(SHELL)).then(() => self.skipWaiting()));
});

self.addEventListener('activate', event => {
  event.waitUntil(caches.keys()
    .then(keys => Promise.all(keys.filter(k => k.startsWith('barizone-') && k !== CACHE).map(k => caches.delete(k))))
    .then(() => self.clients.claim()));
});

// Strategie:
//  - pagine: rete, poi copia salvata dell'app (le rotte sono gestite dal router React);
//  - /assets/* (file con hash nel nome, immutabili): cache, poi rete;
//  - /api/news* e /api/weather: rete, poi ultima risposta salvata (lettura offline);
//  - tutto il resto (API riservate, POST, altri siti): solo rete, mai in cache.
self.addEventListener('fetch', event => {
  const request = event.request;
  if (request.method !== 'GET') return;
  const url = new URL(request.url);
  if (url.origin !== self.location.origin) return;

  if (request.mode === 'navigate') {
    event.respondWith(fetch(request).then(response => {
      const copy = response.clone();
      if (response.ok) caches.open(CACHE).then(c => c.put('/', copy));
      return response;
    }).catch(() => caches.match('/')));
    return;
  }
  if (url.pathname.startsWith('/assets/')) {
    event.respondWith(caches.match(request).then(hit => hit || fetch(request).then(response => {
      if (response.ok) { const copy = response.clone(); caches.open(CACHE).then(c => c.put(request, copy)); }
      return response;
    })));
    return;
  }
  if (url.pathname.startsWith('/api/news') || url.pathname === '/api/weather') {
    event.respondWith(fetch(request).then(response => {
      if (response.ok) { const copy = response.clone(); caches.open(CACHE).then(c => c.put(request, copy)); }
      return response;
    }).catch(() => caches.match(request).then(hit => hit || Response.error())));
  }
});

self.addEventListener('push', event => {
  let message;
  try { message = event.data.json(); } catch { message = { title: 'Barizone', body: event.data?.text() || 'Nuova comunicazione' }; }
  event.waitUntil(self.registration.showNotification(message.title || 'Barizone', {
    body: message.body, icon: '/icon-192.png', badge: '/icon-192.png', tag: message.tag, data: { url: message.url || '/notizie' },
  }));
});

self.addEventListener('notificationclick', event => {
  event.notification.close();
  let url;
  try { url = new URL(event.notification.data?.url || '/notizie', self.location.origin); } catch { return; }
  if (url.protocol !== 'https:') return;
  event.waitUntil(clients.openWindow(url.href));
});
