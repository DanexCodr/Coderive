// Initialize examples page
document.addEventListener('DOMContentLoaded', function() {
    initializeExamples();
    setupEventListeners();
    setupDrawer();
});

// GitHub repository information
const GITHUB_REPO = {
    owner: 'DanexCodr',
    repo: 'Coderive',
    branch: 'main',
    examplesPath: 'src/main/cod/src/main/test/'
};

// Store fetched files
let exampleFiles = [];
let currentFile = null;

async function initializeExamples() {
    // Fetch list of .cod files from the directory
    await fetchExampleFiles();
    
    // Setup tab bar
    renderTabs();
    
    // Load first file (InteractiveDemo.cod)
    if (exampleFiles.length > 0) {
        await loadFile(exampleFiles[0]);
    }
}

async function fetchExampleFiles() {
    const tabBar = document.getElementById('tabBar');
    const codeDisplay = document.getElementById('codeDisplay');
    
    try {
        // For now, we'll define known test files
        exampleFiles = [
            { name: 'InteractiveDemo.cod', path: 'InteractiveDemo.cod' },
            { name: 'SimpleTest.cod', path: 'SimpleTest.cod' },
            { name: 'ArrayTest.cod', path: 'ArrayTest.cod' },
            { name: 'LoopTest.cod', path: 'LoopTest.cod' },
            { name: 'PolicyTest.cod', path: 'PolicyTest.cod' },
            { name: 'ConstructorTest.cod', path: 'ConstructorTest.cod' }
        ];
        
    } catch (error) {
        console.error('Error setting up examples:', error);
        codeDisplay.innerHTML = '<div class="error-message">❌ Failed to load examples list. Please try again later.</div>';
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
        
        // Add file icon based on name
        let icon = '📄';
        if (file.name.includes('Interactive')) icon = '🎮';
        else if (file.name.includes('Simple')) icon = '👋';
        else if (file.name.includes('Array')) icon = '📊';
        else if (file.name.includes('Loop')) icon = '🔄';
        else if (file.name.includes('Policy')) icon = '⚖️';
        else if (file.name.includes('Constructor')) icon = '🏗️';
        
        tabBtn.innerHTML = `
            <span class="file-icon">${icon}</span>
            <span>${file.name}</span>
        `;
        
        tabBtn.addEventListener('click', () => {
            // Remove active class from all tabs
            document.querySelectorAll('.tab-btn').forEach(btn => {
                btn.classList.remove('active');
            });
            
            // Add active class to clicked tab
            tabBtn.classList.add('active');
            
            // Load the file
            loadFile(file);
        });
        
        tabBar.appendChild(tabBtn);
    });
}

async function loadFile(file) {
    const codeDisplay = document.getElementById('codeDisplay');
    const fileInfo = document.getElementById('fileInfo');
    
    currentFile = file;
    
    // Show loading state
    codeDisplay.innerHTML = '<div class="loading-indicator">Loading file from GitHub...</div>';
    
    // Update file info
    fileInfo.innerHTML = `
        <span class="file-path">📁 ${GITHUB_REPO.examplesPath}${file.path}</span>
        <span>Fetching from GitHub repository...</span>
    `;
    
    // Fetch file from GitHub
    const url = `https://raw.githubusercontent.com/${GITHUB_REPO.owner}/${GITHUB_REPO.repo}/${GITHUB_REPO.branch}/${GITHUB_REPO.examplesPath}${file.path}`;
    
    try {
        const response = await fetch(url);
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const code = await response.text();
        
        // Update file info with success
        fileInfo.innerHTML = `
            <span class="file-path">📁 ${GITHUB_REPO.examplesPath}${file.path}</span>
            <span>✅ Loaded from GitHub</span>
        `;
        
        // Display code
        codeDisplay.innerHTML = `
            <pre><code class="code-example-content">${escapeHtml(code)}</code></pre>
        `;
        
    } catch (error) {
        console.error('Error fetching file:', error);
        
        // Update file info with error
        fileInfo.innerHTML = `
            <span class="file-path">📁 ${GITHUB_REPO.examplesPath}${file.path}</span>
            <span style="color: #f97583;">❌ Failed to load</span>
        `;
        
        codeDisplay.innerHTML = `
            <div class="error-message">
                ❌ Failed to load ${file.name} from GitHub.<br>
                Please check your connection and try again.
            </div>
        `;
    }
}

// Helper function to escape HTML special characters
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Setup drawer navigation
function setupDrawer() {
    const drawerToggle = document.getElementById('drawerToggle');
    const drawerOverlay = document.getElementById('drawerOverlay');
    const drawer = document.getElementById('mainDrawer');
    
    drawerToggle.addEventListener('click', toggleDrawer);
    drawerOverlay.addEventListener('click', toggleDrawer);
    
    // Close on escape key
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

// Setup event listeners (orientation changes, etc)
function setupEventListeners() {
    // Handle orientation changes
    window.addEventListener('orientationchange', function() {
        setTimeout(() => {
            window.dispatchEvent(new Event('resize'));
        }, 100);
    });

    window.addEventListener('resize', function() {
        updateLayoutForOrientation();
    });

    // Initial orientation setup
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

// Add CSS for code styling
(function addCodeStyles() {
    if (!document.querySelector('#examples-code-styles')) {
        const style = document.createElement('style');
        style.id = 'examples-code-styles';
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
            
            /* Mobile touch fixes */
            @media (max-width: 768px) {
                .code-display,
                .code-display * {
                    touch-action: pan-y !important;
                    -webkit-overflow-scrolling: touch !important;
                }
            }
        `;
        document.head.appendChild(style);
    }
})();