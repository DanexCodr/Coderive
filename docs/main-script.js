// Initialize content for main page
document.addEventListener('DOMContentLoaded', function() {
    initializeMainContent();
    setupCopyButtons();
    setupEventListeners();
    setupSmoothScrolling();
    handlePageLoad(); // Initialize page load handling
});

function initializeMainContent() {
    // Set page title and header
    document.title = strings.ui.titles.main_page;
    document.getElementById('mainHeader').textContent = strings.ui.titles.coderive_language;
    document.getElementById('tagline').textContent = strings.ui.titles.tagline;
    
    // Set button texts for regular header
    document.getElementById('getStartedBtn').textContent = strings.ui.buttons.get_started;
    document.getElementById('tryEditorBtn').textContent = strings.ui.buttons.try_online_editor;
    document.getElementById('githubBtn').textContent = strings.ui.buttons.view_on_github;
    document.getElementById('openEditorBtn').textContent = strings.ui.buttons.open_online_editor;
    
    // Set button texts for landscape header (40/60 layout)
    const landscapeGetStartedBtn = document.getElementById('landscapeGetStartedBtn');
    const landscapeTryEditorBtn = document.getElementById('landscapeTryEditorBtn');
    const landscapeGithubBtn = document.getElementById('landscapeGithubBtn');
    
    if (landscapeGetStartedBtn) {
        landscapeGetStartedBtn.textContent = strings.ui.buttons.get_started;
    }
    if (landscapeTryEditorBtn) {
        landscapeTryEditorBtn.textContent = strings.ui.buttons.try_online_editor;
    }
    if (landscapeGithubBtn) {
        landscapeGithubBtn.textContent = strings.ui.buttons.view_on_github;
        landscapeGithubBtn.href = strings.metadata.github;
    }
    
    // Set section titles
    document.getElementById('featuresTitle').textContent = strings.ui.labels.features;
    document.getElementById('examplesTitle').textContent = strings.ui.labels.language_examples;
    document.getElementById('runnersTitle').textContent = strings.ui.labels.execution_runners;
    document.getElementById('techTitle').textContent = strings.ui.labels.technical_innovations;
    document.getElementById('tryTitle').textContent = strings.ui.labels.try_online;
    document.getElementById('gettingStartedTitle').textContent = strings.ui.labels.getting_started;
    
    // Set dynamic text content
    document.getElementById('demoTitle').textContent = strings.ui.labels.demo_title;
    document.getElementById('loopsTitle').textContent = strings.ui.labels.loops_title;
    document.getElementById('tryDescription').textContent = strings.ui.messages.try_description;
    document.getElementById('copyright').textContent = strings.ui.messages.copyright;
    document.getElementById('builtWith').textContent = strings.ui.messages.footer_built_with;
    
    // Populate features
    populateFeatures();
    
    // Populate code examples
    populateCodeExamples();
    
    // Populate runners
    populateRunners();
    
    // Populate technical innovations
    populateTechnicalInnovations();
    
    // Populate getting started
    populateGettingStarted();
    
    // Populate footer
    populateFooter();
}

function populateFeatures() {
    const featuresGrid = document.getElementById('featuresGrid');
    featuresGrid.innerHTML = '';
    
    strings.content.features.forEach(feature => {
        const card = document.createElement('div');
        card.className = 'feature-card';
        card.innerHTML = `
            <div class="feature-icon">${feature.icon}</div>
            <h3>${feature.name}</h3>
            <p>${feature.description}</p>
        `;
        featuresGrid.appendChild(card);
    });
}

function populateCodeExamples() {
    // Interactive demo
    const demoExample = document.getElementById('demoExample');
    demoExample.innerHTML = '';
    const demoPre = document.createElement('pre');
    const demoCode = document.createElement('code');
    demoCode.className = 'code-example-content';
    // Just set textContent instead of innerHTML - NO HIGHLIGHTING
    demoCode.textContent = strings.content.code_examples.interactive_demo.code;
    demoPre.appendChild(demoCode);
    demoExample.appendChild(demoPre);
    
    // Smart for-loops
    const loopsExample = document.getElementById('loopsExample');
    loopsExample.innerHTML = '';
    const loopsPre = document.createElement('pre');
    const loopsCode = document.createElement('code');
    loopsCode.className = 'code-example-content';
    // Just set textContent instead of innerHTML - NO HIGHLIGHTING
    loopsCode.textContent = strings.content.code_examples.smart_for_loops.code;
    loopsPre.appendChild(loopsCode);
    loopsExample.appendChild(loopsPre);
    
    // Language features
    const languageFeatures = document.getElementById('languageFeatures');
    languageFeatures.innerHTML = '';
    
    const features = [
        { title: "Multi-Return Slots", desc: "Declare multiple return values with ~| syntax and assign with ~ operator" },
        { title: "Smart For-Loops", desc: "Support for multiplicative (*2), divisive (/2), and compound (i+=1) steps" },
        { title: "Flexible Control Flow", desc: "Support for both else-if and elif syntax with clean block structure" },
        { title: "Array Support", desc: "Native array literals with indexing and modification capabilities" }
    ];
    
    features.forEach(feature => {
        const item = document.createElement('div');
        item.className = 'feature-item';
        item.innerHTML = `
            <h4>${feature.title}</h4>
            <p>${feature.desc}</p>
        `;
        languageFeatures.appendChild(item);
    });
}

function populateRunners() {
    const runnersGrid = document.getElementById('runnersGrid');
    runnersGrid.innerHTML = '';
    
    strings.content.runners.forEach(runner => {
        const card = document.createElement('div');
        card.className = 'runner-card';
        
        const featuresList = runner.features.map(feature => 
            `<li>${feature}</li>`
        ).join('');
        
        card.innerHTML = `
            <h3>${runner.name}</h3>
            <p>${runner.description}</p>
            <ul>${featuresList}</ul>
            <div class="command-container">
                <div class="command">${runner.command}</div>
                <button class="copy-btn">${strings.ui.buttons.copy}</button>
            </div>
        `;
        runnersGrid.appendChild(card);
    });
}

function populateTechnicalInnovations() {
    const techGrid = document.getElementById('techGrid');
    techGrid.innerHTML = '';
    
    strings.content.technical_innovations.forEach(innovation => {
        const card = document.createElement('div');
        card.className = 'feature-card';
        card.innerHTML = `
            <h3>${innovation.title}</h3>
            <p>${innovation.description}</p>
        `;
        techGrid.appendChild(card);
    });
}

function populateGettingStarted() {
    const gettingStartedGrid = document.getElementById('gettingStartedGrid');
    gettingStartedGrid.innerHTML = '';
    
    strings.content.getting_started.forEach(step => {
        const card = document.createElement('div');
        card.className = 'feature-card';
        
        let content = `
            <h3>${step.step}</h3>
            <p>${step.description}</p>
        `;
        
        if (step.command) {
            content += `
                <div class="command-container">
                    <div class="command">${step.command}</div>
                    <button class="copy-btn">${strings.ui.buttons.copy}</button>
                </div>
            `;
        }
        
        card.innerHTML = content;
        
        if (step.code_example) {
            const codeContainer = document.createElement('div');
            codeContainer.className = 'code-example';
            codeContainer.style.margin = '1rem 0';
            codeContainer.style.padding = '1rem';
            
            const pre = document.createElement('pre');
            const code = document.createElement('code');
            code.className = 'code-example-content';
            // Just set textContent instead of innerHTML - NO HIGHLIGHTING
            code.textContent = step.code_example;
            
            pre.appendChild(code);
            codeContainer.appendChild(pre);
            card.appendChild(codeContainer);
        }
        
        gettingStartedGrid.appendChild(card);
    });
}

function populateFooter() {
    document.getElementById('footerLinks').innerHTML = '';
    
    Object.entries(strings.content.footer.links).forEach(([key, text]) => {
        let href = '#';        
        if (key === 'github') href = strings.metadata.github;
        else if (key === 'contact') href = `mailto:${strings.metadata.contact_email}`;
        else if (key === 'features') href = '#features';
        else if (key === 'documentation') href = '#getting-started';
        
        const link = document.createElement('a');
        link.href = href;
        link.textContent = text;
        document.getElementById('footerLinks').appendChild(link);
    });
}

// Add CSS class for code styling (without highlighting)
function addCodeStyles() {
    if (!document.querySelector('#code-styles')) {
        const style = document.createElement('style');
        style.id = 'code-styles';
        style.textContent = `
            .code-example-content {
                font-family: 'JetBrains Mono', 'Fira Code', 'Courier New', monospace;
                font-size: clamp(0.8rem, 3vw, 0.9rem);
                line-height: 1.4;
                display: block;
                max-width: 100%;
                color: #c9d1d9;
            }
            
            /* Style the pre element for better wrapping */
            .code-example pre {
                max-width: 100%;
                overflow-x: auto;
            }
            
            /* Add some color to the wrapper */
            .code-example {
                background: linear-gradient(145deg, #0d1117, #161b22);
                border: 1px solid #58a6ff;
                padding: 5px;
                position: relative;
            }
            
            /* Add a subtle corner accent */
            .code-example::after {
                content: '';
                position: absolute;
                bottom: 0;
                right: 0;
                background: linear-gradient(135deg, transparent 50%, #007acc 50%);
                border-radius: 0 0 8px 0;
            }
        `;
        document.head.appendChild(style);
    }
}

// Copy to clipboard function (updated to use dynamic strings)
function copyToClipboard(button) {
    const commandContainer = button.parentElement;
    const commandElement = commandContainer.querySelector('.command');
    const textToCopy = commandElement.textContent || commandElement.innerText;
    
    const textarea = document.createElement('textarea');
    textarea.value = textToCopy;
    textarea.style.position = 'fixed';
    textarea.style.opacity = '0';
    document.body.appendChild(textarea);
    
    textarea.select();
    textarea.setSelectionRange(0, 99999);
    
    try {
        const successful = document.execCommand('copy');
        if (successful) {
            const originalText = button.textContent;
            button.textContent = strings.ui.buttons.copied;
            button.classList.add('copied');
            
            setTimeout(() => {
                button.textContent = originalText;
                button.classList.remove('copied');
            }, 2000);
        }
    } catch (err) {
        console.error('Failed to copy text: ', err);
    }
    
    document.body.removeChild(textarea);
}

function setupCopyButtons() {
    const copyButtons = document.querySelectorAll('.copy-btn');
    copyButtons.forEach(button => {
        button.addEventListener('click', function() {
            copyToClipboard(this);
        });
    });
    
    // Re-setup after dynamic content loads
    setTimeout(setupCopyButtons, 100);
}

// Function to detect mobile device
function isMobileDevice() {
    // Method 1: Check user agent for common mobile patterns
    const userAgent = navigator.userAgent.toLowerCase();
    const isMobileUA = /android|webos|iphone|ipad|ipod|blackberry|iemobile|opera mini/i.test(userAgent);
    
    // Method 2: Check screen width and touch capability
    const hasTouch = 'ontouchstart' in window || navigator.maxTouchPoints > 0;
    const isSmallScreen = window.innerWidth <= 768;
    
    // Method 3: Check device pixel ratio and screen orientation
    const isHighDPR = window.devicePixelRatio > 1;
    
    // Combine checks - more reliable
    return (isMobileUA || (hasTouch && isSmallScreen)) && isHighDPR;
}

// Function to apply mobile-specific styles
function applyMobileStyles() {
    const isMobile = isMobileDevice();
    const debugElement = document.getElementById('deviceDebug');
    
    if (isMobile) {
        document.body.classList.add('mobile-device');
        document.body.classList.remove('desktop-device');
        if (debugElement) {
            debugElement.textContent = 'Mobile: Yes';
            debugElement.style.display = 'block';
        }
        
        // Additional mobile-specific adjustments
        const topLeftContainer = document.querySelector('.top-left-container');
        if (topLeftContainer) {
            // Make touch targets larger for mobile
            const menuButton = topLeftContainer.querySelector('.side-panel-toggle');
            if (menuButton) {
                menuButton.style.padding = '12px 16px';
                menuButton.style.fontSize = '18px';
            }
        }
        
        // Adjust font sizes for better readability on mobile
        const body = document.body;
        body.style.fontSize = '16px'; // Minimum readable size for mobile
        
        // Add mobile-specific class to sections for potential future styling
        const sections = document.querySelectorAll('section');
        sections.forEach(section => {
            section.classList.add('mobile-section');
        });
    } else {
        document.body.classList.add('desktop-device');
        document.body.classList.remove('mobile-device');
        if (debugElement) {
            debugElement.textContent = 'Mobile: No';
            debugElement.style.display = 'block';
        }
    }
    
    // Auto-hide debug after 3 seconds
    if (debugElement) {
        setTimeout(() => {
            debugElement.style.display = 'none';
        }, 3000);
    }
}

// Function to update left side width for landscape mode
function updateLeftSideWidth() {
    if (document.body.classList.contains('landscape-mode')) {
        const leftSide = document.querySelector('.landscape-left');
        if (leftSide) {
            const width = leftSide.offsetWidth;
            document.documentElement.style.setProperty('--left-side-width', width + 'px');
        }
    }
}

// Setup smooth scrolling
function setupSmoothScrolling() {
    // Remove any existing event listeners that might interfere
    document.removeEventListener('touchmove', preventDefaultScroll);
    
    // Ensure body has proper scrolling
    if (!document.body.classList.contains('landscape-mode')) {
        document.body.style.overflowY = 'auto';
        document.body.style.height = 'auto';
        document.body.style.minHeight = '100vh';
    }
    
    // Add smooth scroll behavior
    const scrollableElements = document.querySelectorAll('.code-example, .command');
    scrollableElements.forEach(el => {
        el.style.WebkitOverflowScrolling = 'touch';
    });
}

// Prevent default scroll handler (if needed)
function preventDefaultScroll(e) {
    e.preventDefault();
}

// ============================================
// DRAWER FUNCTIONALITY
// ============================================

// Drawer variables
let drawerVisible = true; // Start with drawer visible on desktop
let drawer = document.getElementById('mainDrawer');
let drawerToggle = document.getElementById('drawerToggle');
let drawerOverlay = document.getElementById('drawerOverlay');

// Function to handle page load
function handlePageLoad() {
    // Add loaded class after a short delay to prevent initial animation
    setTimeout(() => {
        document.body.classList.add('loaded');
    }, 500);
    
    // Remove any conflicting inline styles
    const topLeftContainer = document.querySelector('.top-left-container');
    if (topLeftContainer) {
        topLeftContainer.style.transform = '';
        topLeftContainer.style.transition = '';
    }
}

// Initialize drawer based on screen size
function initializeDrawer() {
    const isSmallScreen = window.innerWidth <= 600;
    const topLeftContainer = document.querySelector('.top-left-container');
    
    // Reset position first
    if (topLeftContainer) {
        topLeftContainer.style.transform = '';
        topLeftContainer.style.transition = '';
    }
    
    if (isSmallScreen) {
        // On small screens, start with drawer hidden
        drawerVisible = false;
        document.body.classList.remove('drawer-visible');
        drawerToggle.textContent = '☰';
        drawerToggle.setAttribute('aria-label', 'Open navigation menu');
        drawerToggle.style.background = '#007acc';
    } else {
        // On larger screens, start with drawer hidden
        drawerVisible = false;
        document.body.classList.remove('drawer-visible');
        drawerToggle.textContent = '☰';
        drawerToggle.setAttribute('aria-label', 'Open navigation menu');
        drawerToggle.style.background = '#007acc';
    }
}

// Toggle drawer function
function toggleDrawer() {
    drawerVisible = !drawerVisible;
    
    if (drawerVisible) {
        document.body.classList.add('drawer-visible');
        drawerToggle.textContent = '✕';
        drawerToggle.setAttribute('aria-label', 'Close navigation menu');
        drawerToggle.style.background = '#005a9e';
    } else {
        document.body.classList.remove('drawer-visible');
        drawerToggle.textContent = '☰';
        drawerToggle.setAttribute('aria-label', 'Open navigation menu');
        drawerToggle.style.background = '#007acc';
    }
    
    // Ensure top-left container gets the proper classes
    const topLeftContainer = document.querySelector('.top-left-container');
    if (topLeftContainer) {
        // Force reflow to ensure animation triggers
        topLeftContainer.offsetHeight;
    }
    
    localStorage.setItem('drawerState', drawerVisible ? 'open' : 'closed');
}

// Update the updateLayoutForOrientation function
function updateLayoutForOrientation() {
    const isLandscape = window.innerWidth > window.innerHeight && window.innerWidth > 768;
    
    if (isLandscape) {
        document.body.classList.add('landscape-mode');
        document.body.style.overflow = 'hidden';
        
        // Hide drawer toggle by default in landscape
        drawerToggle.style.display = 'none';
        
        // Close drawer when entering landscape mode
        if (drawerVisible) {
            toggleDrawer();
        }
    } else {
        document.body.classList.remove('landscape-mode');
        document.body.style.overflow = 'auto';
        document.body.style.height = 'auto';
        document.body.style.minHeight = '100vh';
        
        // Show toggle button in portrait
        drawerToggle.style.display = 'flex';
    }
}

// Setup event listeners
function setupEventListeners() {
    // Add code styles
    addCodeStyles();
    
    // Apply mobile styles
    applyMobileStyles();
    
    // Initialize drawer
    initializeDrawer();
    
    // Drawer toggle
    drawerToggle.addEventListener('click', toggleDrawer);
    
    // Close drawer when clicking overlay (mobile only)
    drawerOverlay.addEventListener('click', function() {
        if (window.innerWidth <= 600) {
            toggleDrawer();
        }
    });
    
    // Close drawer on Escape key
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape' && drawerVisible && window.innerWidth <= 600) {
            toggleDrawer();
        }
    });
    
    // Handle viewport changes
    window.addEventListener('orientationchange', function() {
        setTimeout(function() {
            window.dispatchEvent(new Event('resize'));
            applyMobileStyles(); // Re-apply on orientation change
            setupSmoothScrolling(); // Re-setup scrolling
            
            // Update drawer state based on new orientation
            initializeDrawer();
            
            // Reset drawer position
            const topLeftContainer = document.querySelector('.top-left-container');
            if (topLeftContainer) {
                topLeftContainer.style.transform = '';
                topLeftContainer.style.transition = '';
            }
        }, 100);
    });

    window.addEventListener('resize', function() {
        setupSmoothScrolling();
        updateLayoutForOrientation();
        updateLeftSideWidth();
        applyMobileStyles(); // Re-apply on resize
        
        // Update drawer on resize
        initializeDrawer();
        
        // Reset drawer position
        const topLeftContainer = document.querySelector('.top-left-container');
        if (topLeftContainer) {
            topLeftContainer.style.transform = '';
            topLeftContainer.style.transition = '';
        }
    });

    // Initialize smooth scrolling for touch devices
    if ('ontouchstart' in window) {
        // Use passive event listeners for better performance
        document.addEventListener('touchmove', function() {}, { passive: true });
    }
    
    // Initial orientation setup
    updateLayoutForOrientation();
    setTimeout(updateLeftSideWidth, 100); // Initial width calculation
    
    // Initial smooth scrolling setup
    setTimeout(setupSmoothScrolling, 500);
}