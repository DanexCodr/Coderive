/* coi-serviceworker - Enables SharedArrayBuffer on GitHub Pages by
 * adding Cross-Origin-Opener-Policy and Cross-Origin-Embedder-Policy
 * headers via a service worker. Required for CheerpJ (WebAssembly JVM).
 *
 * Based on the pattern from https://github.com/gzuidhof/coi-serviceworker
 */

self.addEventListener('install', function() {
    self.skipWaiting();
});

self.addEventListener('activate', function(event) {
    event.waitUntil(self.clients.claim());
});

self.addEventListener('fetch', function(event) {
    var request = event.request;

    // Skip non-GET requests and opaque requests that would break with added headers
    if (request.cache === 'only-if-cached' && request.mode !== 'same-origin') {
        return;
    }

    // For navigation requests (HTML pages), add COOP/COEP headers
    event.respondWith(
        fetch(request).then(function(response) {
            // Only modify responses we can read (same-origin or CORS)
            if (response.type === 'opaque') {
                return response;
            }

            var headers = new Headers(response.headers);
            headers.set('Cross-Origin-Opener-Policy', 'same-origin');
            headers.set('Cross-Origin-Embedder-Policy', 'credentialless');

            return new Response(response.body, {
                status: response.status,
                statusText: response.statusText,
                headers: headers
            });
        }).catch(function(err) {
            // Log the error and fall through to the browser's default fetch handling
            console.warn('coi-serviceworker: fetch failed, retrying without isolation headers:', err);
            return fetch(request);
        })
    );
});
