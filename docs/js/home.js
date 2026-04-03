// js/home.js - Using core
(function() {
    const REPO_API_BASE = 'https://api.github.com/repos/DanexCodr/Coderive/contents';
    const REPO_RAW_BASE = 'https://raw.githubusercontent.com/DanexCodr/Coderive/main';
    const REPO_BLOB_BASE = 'https://github.com/DanexCodr/Coderive/blob/main';

    function encodePath(path) {
        return String(path || '')
            .split('/')
            .filter(Boolean)
            .map(function(part) { return encodeURIComponent(part); })
            .join('/');
    }

    function buildRawUrl(path) {
        return REPO_RAW_BASE + '/' + encodePath(path);
    }

    function buildBlobUrl(path) {
        return REPO_BLOB_BASE + '/' + encodePath(path);
    }

    function compareEntryNames(a, b) {
        return String(a.name || '').localeCompare(String(b.name || ''));
    }

    async function listDirectory(path) {
        const encodedPath = encodePath(path);
        const response = await fetch(REPO_API_BASE + '/' + encodedPath);
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        return await response.json();
    }

    async function findFirstCodPath() {
        // Some repository layouts place generated/curated examples in this nested path.
        // If it does not exist, traversal falls back to the broader src/main root.
        const preferredRoot = 'src/main/cod/src/main/src/cod';
        const fallbackRoot = 'src/main/cod/src/main';
        const roots = [preferredRoot, fallbackRoot];

        for (let i = 0; i < roots.length; i++) {
            const found = await findFirstCodPathInRoot(roots[i]);
            if (found) return found;
        }
        return null;
    }

    async function findFirstCodPathInRoot(rootPath) {
        let entries;
        try {
            entries = await listDirectory(rootPath);
        } catch (error) {
            return null;
        }
        if (!Array.isArray(entries)) return null;

        const sorted = entries.slice().sort(compareEntryNames);
        for (let i = 0; i < sorted.length; i++) {
            const entry = sorted[i];
            if (!entry || !entry.type || !entry.path) continue;
            if (entry.type === 'file' && /\.cod$/i.test(entry.name || '')) {
                return entry.path;
            }
        }
        for (let i = 0; i < sorted.length; i++) {
            const entry = sorted[i];
            if (!entry || entry.type !== 'dir' || !entry.path) continue;
            const nested = await findFirstCodPathInRoot(entry.path);
            if (nested) return nested;
        }
        return null;
    }

    function renderHighlightedCode(codeContent, code) {
        if (window.CoderiveSyntaxHighlighter && typeof CoderiveSyntaxHighlighter.renderTo === 'function') {
            CoderiveSyntaxHighlighter.renderTo(codeContent, code, false);
            return;
        }
        codeContent.textContent = code;
    }

    async function loadCodeFromGitHub() {
        const codeContent = document.getElementById('codeContent');
        if (!codeContent) return;
        const filePathEl = document.getElementById('homeFilePath');
        const viewFullLink = document.getElementById('homeViewFullLink');
        
        try {
            const codPath = await findFirstCodPath();
            if (!codPath) throw new Error('No .cod file found');

            const response = await fetch(buildRawUrl(codPath));
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            
            let code = await response.text();
            const lines = code.split('\n');
            if (lines.length > 30) {
                code = lines.slice(0, 30).join('\n') + '\n\n// ... (truncated)';
            }

            renderHighlightedCode(codeContent, code);
            if (filePathEl) filePathEl.textContent = codPath;
            if (viewFullLink) viewFullLink.href = buildBlobUrl(codPath);
            Coderive.emit('codeLoaded');
        } catch (error) {
            Coderive.handleError(error, 'Loading code');
            codeContent.innerHTML = `<span class="error">❌ Failed to load code</span>`;
            if (filePathEl) filePathEl.textContent = 'Failed to resolve .cod file';
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
