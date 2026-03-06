// js/home.js - Using core
(function() {
    async function loadCodeFromGitHub() {
        const codeContent = document.getElementById('codeContent');
        if (!codeContent) return;
        
        const githubUrl = 'https://raw.githubusercontent.com/DanexCodr/Coderive/main/src/main/cod/src/main/test/InteractiveDemo.cod';
        
        try {
            const response = await fetch(githubUrl);
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            
            let code = await response.text();
            const lines = code.split('\n');
            if (lines.length > 30) {
                code = lines.slice(0, 30).join('\n') + '\n\n// ... (truncated)';
            }
            
            codeContent.textContent = code;
            Coderive.emit('codeLoaded');
        } catch (error) {
            Coderive.handleError(error, 'Loading code');
            codeContent.innerHTML = `<span class="error">❌ Failed to load code</span>`;
        }
    }
    
    function initHome() {
        Coderive.updateYear('.currentYear');
        Coderive.fetchVersion();
        loadCodeFromGitHub();
    }
    
    Coderive.on('partialsLoaded', initHome);
    Coderive.whenReady(initHome);
})();