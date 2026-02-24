// Populate drawer content
document.addEventListener('DOMContentLoaded', function() {
    populateDrawerContent();
});

function populateDrawerContent() {
    const drawerContent = document.getElementById('drawerContent');
    if (!drawerContent) return;
    
    // Determine current page for active state
    const currentPage = window.location.pathname.split('/').pop() || 'index.html';
    
    drawerContent.innerHTML = `
        <div class="drawer-section">
            <h4 class="drawer-section-title">Navigation</h4>
            <div class="drawer-btn-group">
                <button class="drawer-btn ${currentPage === 'index.html' ? 'active' : ''}" onclick="navigateTo('index.html')">
                    <span class="drawer-btn-icon">🏠</span>
                    <span class="drawer-btn-text">Home</span>
                </button>
                <button class="drawer-btn ${currentPage === 'examples.html' ? 'active' : ''}" onclick="navigateTo('examples.html')">
                    <span class="drawer-btn-icon">📚</span>
                    <span class="drawer-btn-text">Code Examples</span>
                </button>
                <button class="drawer-btn ${currentPage === 'editor.html' ? 'active' : ''}" onclick="navigateTo('editor.html')">
                    <span class="drawer-btn-icon">✏️</span>
                    <span class="drawer-btn-text">Online Editor</span>
                </button>
            </div>
        </div>
        
        <div class="drawer-section">
            <h4 class="drawer-section-title">Quick Links</h4>
            <div class="drawer-btn-group">
                <button class="drawer-btn" onclick="window.open('https://github.com/DanexCodr/Coderive', '_blank')">
                    <span class="drawer-btn-icon">📦</span>
                    <span class="drawer-btn-text">GitHub Repository</span>
                </button>
                <button class="drawer-btn" onclick="document.getElementById('drawerToggle').click(); window.location.href='index.html#features'">
                    <span class="drawer-btn-icon">⭐</span>
                    <span class="drawer-btn-text">Features</span>
                </button>
                <button class="drawer-btn" onclick="document.getElementById('drawerToggle').click(); window.location.href='index.html#getting-started'">
                    <span class="drawer-btn-icon">🚀</span>
                    <span class="drawer-btn-text">Getting Started</span>
                </button>
            </div>
        </div>
        
        <div class="drawer-section">
            <h4 class="drawer-section-title">Resources</h4>
            <div class="drawer-btn-group">
                <button class="drawer-btn" onclick="window.open('https://github.com/DanexCodr/Coderive/wiki', '_blank')">
                    <span class="drawer-btn-icon">📖</span>
                    <span class="drawer-btn-text">Documentation</span>
                </button>
                <button class="drawer-btn" onclick="window.location.href='mailto:danisonnunez001@gmail.com'">
                    <span class="drawer-btn-icon">📧</span>
                    <span class="drawer-btn-text">Contact</span>
                </button>
            </div>
        </div>
    `;
}

// Navigation helper
function navigateTo(url) {
    // Close drawer if open
    if (document.body.classList.contains('drawer-visible')) {
        document.getElementById('drawerToggle').click();
    }
    // Navigate
    window.location.href = url;
}