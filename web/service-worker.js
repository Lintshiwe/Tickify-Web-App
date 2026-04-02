const CACHE_NAME = 'tickify-pwa-v1';
const CORE_ASSETS = [
  '/Tickify-SWP-Web-App/index.html',
  '/Tickify-SWP-Web-App/Login.jsp',
  '/Tickify-SWP-Web-App/UserSelection.jsp',
  '/Tickify-SWP-Web-App/assets/error-popup.js',
  '/Tickify-SWP-Web-App/assets/cookie-consent.js'
];

self.addEventListener('install', (event) => {
  event.waitUntil(caches.open(CACHE_NAME).then((cache) => cache.addAll(CORE_ASSETS)).then(() => self.skipWaiting()));
});

self.addEventListener('activate', (event) => {
  event.waitUntil(caches.keys().then((keys) => Promise.all(keys.map((key) => (key !== CACHE_NAME ? caches.delete(key) : Promise.resolve())))).then(() => self.clients.claim()));
});

self.addEventListener('fetch', (event) => {
  if (event.request.method !== 'GET') {
    return;
  }
  event.respondWith(caches.match(event.request).then((cached) => {
    if (cached) {
      return cached;
    }
    return fetch(event.request)
      .then((response) => {
        const clone = response.clone();
        caches.open(CACHE_NAME).then((cache) => cache.put(event.request, clone)).catch(() => {});
        return response;
      })
      .catch(() => caches.match('/Tickify-SWP-Web-App/index.html'));
  }));
});
