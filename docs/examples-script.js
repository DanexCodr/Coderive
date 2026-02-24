document.addEventListener('DOMContentLoaded', function() {
    initializeExamples();
    setupEventListeners();
    setupDrawer();
});

const GITHUB_REPO = {
    owner: 'DanexCodr',
    repo: 'Coderive',
    branch: 'main',
    examplesPath: 'src/main/cod/src/main/test/'
};

let exampleFiles = [];
let currentFile = null;

async function initializeExamples() {
    const codeDisplay = document.getElementById('codeDisplay');
    const tabBar = document.getElementById('tabBar');
    const fileInfo = document.getElementById('fileInfo');
    
    tabBar.innerHTML = '<div class="loading-indicator">Loading examples list...</div>';
    codeDisplay.innerHTML = '';
    fileInfo.innerHTML = '';
    
    await fetchExampleFiles();
}

async function fetchExampleFiles() {
    const tabBar = document.getElementById('tabBar');
    const codeDisplay = document.getElementById('codeDisplay');
    const fileInfo = document.getElementById('fileInfo');
    
    try {
        const apiUrl = `https://api.github.com/repos/${GITHUB_REPO.owner}/${GITHUB_REPO.repo}/contents/${GITHUB_REPO.examplesPath}?ref=${GITHUB_REPO.branch}`;
        
        const response = await fetch(apiUrl);
        
        if (!response.ok) {
            throw new Error(`GitHub API error: ${response.status}`);
        }
        
        const contents = await response.json();
        
        exampleFiles = contents
            .filter(item => item.type === 'file' && item.name.endsWith('.cod'))
            .map(item => ({
                name: item.name,
                path: item.path,
                url: item.download_url,
                size: item.size
            }))
            .sort((a, b) => a.name.localeCompare(b.name));
        
        if (exampleFiles.length === 0) {
            tabBar.innerHTML = '<div class="error-message">No .cod files found.</div>';
            return;
        }
        
        renderTabs();
        
        if (exampleFiles.length > 0) {
            await loadFile(exampleFiles[0]);
        }
        
    } catch (error) {
        console.error('Error fetching file list:', error);
        
        tabBar.innerHTML = `
            <div class="error-message">
                ❌ Failed to load file list.<br>
                <span style="font-size: 0.9rem; margin-top: 0.5rem; display: block;">
                    Please check your connection and try again.
                </span>
            </div>
        `;
        codeDisplay.innerHTML = '';
        fileInfo.innerHTML = '';
    }
}

function renderTabs() {
    const tabBar = document.getElementById('tabBar');
    tabBar.innerHTML = '';
    
    exampleFiles.forEach((file, index) => {
        const tabBtn = document.createElement('button');
        tabBtn.className = `tab-btn ${index === 0 ? 'active' : ''}`;
        tabBtn.setAttribute('data-path', file.path);
        tabBtn.setAttribute('data-name', file.name);
        tabBtn.setAttribute('data-url', file.url);
        
        const icon = '📄';
        
        tabBtn.innerHTML = `
            <span class="file-icon">${icon}</span>
            <span class="file-name">${file.name}</span>
            <span class="file-size">${formatFileSize(file.size)}</span>
        `;
        
        tabBtn.addEventListener('click', () => {
            document.querySelectorAll('.tab-btn').forEach(btn => {
                btn.classList.remove('active');
            });
            tabBtn.classList.add('active');
            loadFile(file);
        });
        
        tabBar.appendChild(tabBtn);
    });
}

async function loadFile(file) {
    const codeDisplay = document.getElementById('codeDisplay');
    const fileInfo = document.getElementById('fileInfo');
    
    currentFile = file;
    
    codeDisplay.innerHTML = '<div class="loading-indicator">Loading...</div>';
    
    fileInfo.innerHTML = `
        <span class="file-path">📁 ${file.path}</span>
        <span class="file-size-badge">${formatFileSize(file.size)}</span>
        <span class="fetch-status">Fetching...</span>
    `;
    
    try {
        const response = await fetch(file.url);
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const code = await response.text();
        
        fileInfo.innerHTML = `
            <span class="file-path">📁 ${file.path}</span>
            <span class="file-size-badge">${formatFileSize(file.size)}</span>
            <span class="fetch-status success">✅ Loaded</span>
        `;
        
        codeDisplay.innerHTML = `
            <pre><code class="code-example-content">${escapeHtml(code)}</code></pre>
        `;
        
    } catch (error) {
        console.error('Error fetching file:', error);
        
        fileInfo.innerHTML = `
            <span class="file-path">📁 ${file.path}</span>
            <span class="file-size-badge">${formatFileSize(file.size)}</span>
            <span class="fetch-status error">❌ Failed</span>
        `;
        
        codeDisplay.innerHTML = `
            <div class="error-message">
                ❌ Failed to load file.<br>
                <span style="font-size: 0.9rem; margin-top: 0.5rem; display: block;">
                    Please check your connection and try again.
                </span>
            </div>
        `;
    }
}

function formatFileSize(bytes) {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function setupDrawer() {
    const drawerToggle = document.getElementById('drawerToggle');
    const drawerOverlay = document.getElementById('drawerOverlay');
    
    drawerToggle.addEventListener('click', toggleDrawer);
    drawerOverlay.addEventListener('click', toggleDrawer);
    
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && document.body.classList.contains('drawer-visible')) {
            toggleDrawer();
        }
    });
}

function toggleDrawer() {
    document.body.classList.toggle('drawer-visible');
    const drawerToggle = document.getElementById('drawerToggle');
    
    if (document.body.classList.contains('drawer-visible')) {
        drawerToggle.textContent = '✕';
        drawerToggle.setAttribute('aria-label', 'Close navigation menu');
    } else {
        drawerToggle.textContent = '☰';
        drawerToggle.setAttribute('aria-label', 'Open navigation menu');
    }
}

function setupEventListeners() {
    window.addEventListener('orientationchange', function() {
        setTimeout(() => {
            window.dispatchEvent(new Event('resize'));
        }, 100);
    });

    window.addEventListener('resize', function() {
        updateLayoutForOrientation();
    });

    updateLayoutForOrientation();
}

function updateLayoutForOrientation() {
    const isLandscape = window.innerWidth > window.innerHeight && window.innerWidth > 768;
    
    if (isLandscape) {
        document.body.classList.add('landscape-mode');
        document.body.style.overflow = 'hidden';
    } else {
        document.body.classList.remove('landscape-mode');
        document.body.style.overflow = 'auto';
    }
}

(function addStyles() {
    if (!document.querySelector('#examples-styles')) {
        const style = document.createElement('style');
        style.id = 'examples-styles';
        style.textContent = `
            .code-example-content {
                font-family: 'JetBrains Mono', 'Fira Code', 'Courier New', monospace;
                font-size: clamp(0.85rem, 3vw, 0.95rem);
                line-height: 1.5;
                display: block;
                color: #c9d1d9;
                user-select: text;
                -webkit-user-select: text;
            }
            
            .file-size {
                font-size: 0.75rem;
                color: var(--text-secondary);
                margin-left: 0.5rem;
                opacity: 0.7;
            }
            
            .file-size-badge {
                font-size: 0.75rem;
                background: rgba(255, 255, 255, 0.1);
                padding: 0.2rem 0.5rem;
                border-radius: 4px;
                color: var(--text-secondary);
            }
            
            .fetch-status {
                font-size: 0.85rem;
            }
            
            .fetch-status.success {
                color: #4caf50;
            }
            
            .fetch-status.error {
                color: #f97583;
            }
            
            .tab-btn {
                display: flex;
                align-items: center;
                gap: 0.5rem;
            }
            
            .file-name {
                flex: 1;
            }
            
            @media (max-width: 768px) {
                .file-size {
                    display: none;
                }
            }
        `;
        document.head.appendChild(style);
    }
})();