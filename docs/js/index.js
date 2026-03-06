// js/index.js - Bootstrap loader
(function() {
    console.log('Bootstrap: starting...');
    
    const coreScript = document.createElement('script');
    coreScript.src = 'js/core.js';
    coreScript.onload = function() {
        console.log('✅ Core loaded, initializing app...');
        
        const scripts = [
            'js/loader.js',
            'js/router.js',
            'js/menu.js',
            'js/home.js',
            'js/docs.js',
            'js/playground.js'
        ];
        
        function loadNext(index) {
            if (index >= scripts.length) {
                console.log('✅ All modules loaded');
                return;
            }
            
            const script = document.createElement('script');
            script.src = scripts[index];
            script.onload = () => {
                console.log(`✅ Loaded ${scripts[index]}`);
                loadNext(index + 1);
            };
            script.onerror = (e) => {
                console.error(`❌ Failed to load ${scripts[index]}:`, e);
                loadNext(index + 1);
            };
            document.body.appendChild(script);
        }
        
        loadNext(0);
    };
    
    coreScript.onerror = function(e) {
        console.error('❌ Failed to load core.js:', e);
        const errorDisplay = document.getElementById('error-display');
        if (errorDisplay) {
            errorDisplay.innerHTML = 'Failed to load core.js';
            errorDisplay.style.display = 'block';
        }
    };
    
    document.head.appendChild(coreScript);
})();