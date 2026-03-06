// js/loader.js - Loads HTML partials
(function() {
    console.log('Loader: starting...');
    
    async function loadPartial(elementId, partialPath) {
        console.log(`Loader: fetching ${partialPath}`);
        
        try {
            const response = await fetch(partialPath);
            console.log(`Loader: ${partialPath} status:`, response.status);
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }
            
            const html = await response.text();
            console.log(`Loader: ${partialPath} size:`, html.length, 'bytes');
            
            const element = document.getElementById(elementId);
            if (element) {
                element.innerHTML = html;
                console.log(`✅ Loader: loaded ${partialPath} into #${elementId}`);
                console.log(`Loader: #${elementId} now has`, element.children.length, 'children');
                return true;
            } else {
                console.error(`❌ Loader: element #${elementId} not found`);
                return false;
            }
        } catch (error) {
            console.error(`❌ Loader: failed to load ${partialPath}:`, error);
            return false;
        }
    }
    
    async function loadAllPartials() {
        console.log('Loader: loading all partials...');
        
        const loader = document.getElementById('loading-indicator');
        
        const results = await Promise.allSettled([
            loadPartial('drawer-container', 'partials/drawer.html'),
            loadPartial('home-container', 'partials/home.html'),
            loadPartial('docs-container', 'partials/docs.html'),
            loadPartial('playground-container', 'partials/playground.html')
        ]);
        
        const successCount = results.filter(r => r.status === 'fulfilled' && r.value === true).length;
        console.log(`Loader: loaded ${successCount}/4 partials`);
        
        if (loader) {
            loader.style.display = 'none';
            console.log('Loader: hidden loading indicator');
        }
        
        if (successCount === 4) {
            console.log('✅ Loader: all partials loaded successfully');
            if (window.Coderive) {
                Coderive.emit('partialsLoaded', { success: true });
            } else {
                document.dispatchEvent(new CustomEvent('partialsLoaded', { 
                    detail: { success: true } 
                }));
            }
        } else {
            console.error('❌ Loader: some partials failed');
            const errorDisplay = document.getElementById('error-display');
            if (errorDisplay) {
                errorDisplay.innerHTML = `Failed to load ${4 - successCount} partials. Check console.`;
                errorDisplay.style.display = 'block';
            }
        }
    }
    
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', loadAllPartials);
    } else {
        loadAllPartials();
    }
})();