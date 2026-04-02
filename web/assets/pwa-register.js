(function () {
  if (!('serviceWorker' in navigator)) {
    return;
  }
  window.addEventListener('load', function () {
    navigator.serviceWorker.register('/Tickify-SWP-Web-App/service-worker.js').catch(function () {
      // Keep PWA registration non-blocking.
    });
  });
})();
