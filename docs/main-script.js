// Initialize content for main page
document.addEventListener('DOMContentLoaded', function() {
    initializeMainContent();
    setupCopyButtons();
    setupEventListeners();
    setupSmoothScrolling();
    handlePageLoad(); // Initialize page load handling
});

// GitHub repository information
const GITHUB_REPO = {
    owner: 'DanexCodr',
    repo: 'Coderive',
    branch: 'main',
    demoPath: 'src/main/cod/src/main/test/InteractiveDemo.cod'
};

// Function to fetch file from GitHub
async function fetchGitHubFile(path) {
    const url = `https://raw.githubusercontent.com/${GITHUB_REPO.owner}/${GITHUB_REPO.repo}/${GITHUB_REPO.branch}/${path}`;
    try {
        const response = await fetch(url);
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        return await response.text();
    } catch (error) {
        console.error('Error fetching file from GitHub:', error);
        return null;
    }
}

function initializeMainContent() {
    // Set page title and header
    document.title = strings.ui.titles.main_page;
    document.getElementById('mainHeader').textContent = strings.ui.titles.coderive_language;
    
    // Set tagline with "Learn More..." link - ADDED DOT AFTER CAPABILITIES
    const tagline = document.getElementById('tagline');
    if (tagline) {
        tagline.innerHTML = strings.ui.titles.tagline + '. <span class="learn-more-link">Learn More...</span>';
    }
    
    // Set landscape tagline as well - ADDED DOT AFTER CAPABILITIES
    const landscapeTagline = document.querySelector('.landscape-header .tagline');
    if (landscapeTagline) {
        landscapeTagline.innerHTML = strings.ui.titles.tagline + '. <span class="learn-more-link">Learn More...</span>';
    }
    
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
    document.getElementById('tryDescription').textContent = strings.ui.messages.try_description;
    document.getElementById('copyright').textContent = strings.ui.messages.copyright;
    document.getElementById('builtWith').textContent = strings.ui.messages.footer_built_with;
    
    // Populate features with VIEWPAGER
    createViewPager();
    
    // Populate code examples (async)
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

// VIEWPAGER Implementation
let viewPagerCurrentIndex = 0;
let viewPagerIsDragging = false;
let viewPagerStartX = 0;
let viewPagerCurrentX = 0;
let viewPagerTotalSlides = strings.content.features.length;

function createViewPager() {
    const featuresSection = document.getElementById('features');
    const container = featuresSection.querySelector('.container');
    
    // Create viewpager HTML structure WITH CONSISTENT CONTENT CONTAINER
    container.innerHTML = `
        <h2 class="section-title" id="featuresTitle">${strings.ui.labels.features}</h2>
        <div class="viewpager-container">
            <div class="viewpager-wrapper">
                <div class="viewpager">
                    ${strings.content.features.map((feature, index) => `
                        <div class="viewpager-slide" data-index="${index}">
                            <div class="viewpager-content">
                                <div class="feature-icon">${feature.icon}</div>
                                <h3>${feature.name}</h3>
                                <p>${feature.description}</p>
                            </div>
                        </div>
                    `).join('')}
                </div>
                
                <div class="viewpager-controls">
                    <button class="viewpager-nav prev-btn" aria-label="Previous feature">
                        <svg width="20" height="20" viewBox="0 0 24 24">
                            <path d="M15.41 7.41L14 6l-6 6 6 6 1.41-1.41L10.83 12z"/>
                        </svg>
                    </button>
                    
                    <div class="viewpager-indicators">
                        ${strings.content.features.map((_, index) => `
                            <button class="viewpager-indicator ${index === 0 ? 'active' : ''}" 
                                    data-index="${index}" 
                                    aria-label="Go to feature ${index + 1}">
                            </button>
                        `).join('')}
                    </div>
                    
                    <button class="viewpager-nav next-btn" aria-label="Next feature">
                        <svg width="20" height="20" viewBox="0 0 24 24">
                            <path d="M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"/>
                        </svg>
                    </button>
                </div>
            </div>
        </div>
    `;
    
    // Initialize viewpager
    setupViewPager();
}

function setupViewPager() {
    const viewpager = document.querySelector('.viewpager');
    const prevBtn = document.querySelector('.viewpager-nav.prev-btn');
    const nextBtn = document.querySelector('.viewpager-nav.next-btn');
    const indicators = document.querySelectorAll('.viewpager-indicator');
    
    // Set initial position
    updateViewPagerPosition();
    
    // Navigation buttons - FIXED: No rubber band effect at boundaries
    prevBtn.addEventListener('click', () => {
        if (viewPagerCurrentIndex > 0) {
            goToSlide(viewPagerCurrentIndex - 1);
        }
        // REMOVED: Rubber band effect at start
    });
    
    nextBtn.addEventListener('click', () => {
        if (viewPagerCurrentIndex < viewPagerTotalSlides - 1) {
            goToSlide(viewPagerCurrentIndex + 1);
        }
        // REMOVED: Rubber band effect at end
    });
    
    // Indicators
    indicators.forEach(indicator => {
        indicator.addEventListener('click', () => {
            const targetIndex = parseInt(indicator.dataset.index);
            if (targetIndex !== viewPagerCurrentIndex) {
                goToSlide(targetIndex);
            }
        });
    });
    
    // Touch events for mobile swipe - UPDATED WITH BETTER TOUCH HANDLING
    viewpager.addEventListener('touchstart', handleTouchStart, { passive: true });
    viewpager.addEventListener('touchmove', handleTouchMove, { passive: true });
    viewpager.addEventListener('touchend', handleTouchEnd);
    
    // Mouse events for desktop drag
    viewpager.addEventListener('mousedown', handleMouseDown);
    viewpager.addEventListener('mousemove', handleMouseMove);
    viewpager.addEventListener('mouseup', handleMouseUp);
    viewpager.addEventListener('mouseleave', handleMouseUp);
}

function goToSlide(targetIndex) {
    if (targetIndex < 0 || targetIndex >= viewPagerTotalSlides) return;
    
    // Snap animation
    const currentSlide = document.querySelector(`.viewpager-slide[data-index="${viewPagerCurrentIndex}"]`);
    const direction = targetIndex > viewPagerCurrentIndex ? 'next' : 'prev';
    
    // Apply snap effect to current slide
    applySnapEffect(currentSlide, direction);
    
    // Update position
    viewPagerCurrentIndex = targetIndex;
    updateViewPagerPosition();
    updateViewPagerIndicators();
}

function updateViewPagerPosition() {
    const viewpager = document.querySelector('.viewpager');
    const slideWidth = 100; // percentage
    const translateX = -viewPagerCurrentIndex * slideWidth;
    
    viewpager.style.transform = `translateX(${translateX}%) translateZ(0)`;
    viewpager.style.transition = 'transform 0.3s cubic-bezier(0.4, 0, 0.2, 1)';
}

function updateViewPagerIndicators() {
    const indicators = document.querySelectorAll('.viewpager-indicator');
    indicators.forEach((indicator, index) => {
        if (index === viewPagerCurrentIndex) {
            indicator.classList.add('active');
        } else {
            indicator.classList.remove('active');
        }
    });
}

// SNAP EFFECTS
function applySnapEffect(slide, direction) {
    slide.classList.remove('snap-left', 'snap-right');
    
    // Force reflow
    void slide.offsetWidth;
    
    if (direction === 'next') {
        slide.classList.add('snap-right');
    } else {
        slide.classList.add('snap-left');
    }
    
    // Remove class after animation
    setTimeout(() => {
        slide.classList.remove('snap-left', 'snap-right');
    }, 300);
}

// TOUCH HANDLERS - UPDATED WITH BETTER TOUCH DETECTION
function handleTouchStart(e) {
    // Check if touch is inside a code element
    const isCodeElement = e.target.closest('.code-example, .code-example-content, .command, pre, code');
    if (isCodeElement) {
        // Don't start ViewPager dragging if inside code area
        return;
    }
    
    viewPagerIsDragging = true;
    viewPagerStartX = e.touches[0].clientX;
    viewPagerCurrentX = viewPagerStartX;
    
    const viewpager = document.querySelector('.viewpager');
    viewpager.style.transition = 'none';
}

function handleTouchMove(e) {
    if (!viewPagerIsDragging) return;
    
    // Check if touch moved to a code element during drag
    const touch = e.touches[0];
    const elementAtTouch = document.elementFromPoint(touch.clientX, touch.clientY);
    const isCodeElement = elementAtTouch.closest('.code-example, .code-example-content, .command, pre, code');
    
    if (isCodeElement) {
        // Cancel drag if moved to code element
        handleTouchEnd();
        return;
    }
    
    viewPagerCurrentX = e.touches[0].clientX;
    const diff = viewPagerCurrentX - viewPagerStartX;
    
    // Apply drag transform with resistance at boundaries
    const viewpager = document.querySelector('.viewpager');
    const slideWidth = window.innerWidth;
    const baseTranslate = -viewPagerCurrentIndex * 100;
    
    // Calculate drag with rubber band effect at boundaries
    let dragPercent = (diff / slideWidth) * 100;
    
    // Add resistance at boundaries
    if ((viewPagerCurrentIndex === 0 && diff > 0) || 
        (viewPagerCurrentIndex === viewPagerTotalSlides - 1 && diff < 0)) {
        dragPercent *= 0.3; // Reduced movement at boundaries
    }
    
    const translateX = baseTranslate + dragPercent;
    viewpager.style.transform = `translateX(${translateX}%) translateZ(0)`;
}

function handleTouchEnd() {
    if (!viewPagerIsDragging) return;
    
    viewPagerIsDragging = false;
    const viewpager = document.querySelector('.viewpager');
    viewpager.style.transition = 'transform 0.3s cubic-bezier(0.4, 0, 0.2, 1)';
    
    const diff = viewPagerCurrentX - viewPagerStartX;
    const slideWidth = window.innerWidth;
    const threshold = slideWidth * 0.1; // 10% threshold
    
    // Determine if we should change slide
    if (Math.abs(diff) > threshold) {
        if (diff > 0 && viewPagerCurrentIndex > 0) {
            // Swipe right - go previous
            goToSlide(viewPagerCurrentIndex - 1);
        } else if (diff < 0 && viewPagerCurrentIndex < viewPagerTotalSlides - 1) {
            // Swipe left - go next
            goToSlide(viewPagerCurrentIndex + 1);
        } else {
            // At boundary - just snap back (NO RUBBER BAND)
            updateViewPagerPosition();
        }
    } else {
        // Not enough movement - snap back to current
        updateViewPagerPosition();
    }
}

// MOUSE HANDLERS (for desktop)
function handleMouseDown(e) {
    // Check if click is inside a code element
    const isCodeElement = e.target.closest('.code-example, .code-example-content, .command, pre, code');
    if (isCodeElement) {
        return;
    }
    
    viewPagerIsDragging = true;
    viewPagerStartX = e.clientX;
    viewPagerCurrentX = viewPagerStartX;
    
    const viewpager = document.querySelector('.viewpager');
    viewpager.style.transition = 'none';
    viewpager.style.cursor = 'grabbing';
}

function handleMouseMove(e) {
    if (!viewPagerIsDragging) return;
    
    viewPagerCurrentX = e.clientX;
    const diff = viewPagerCurrentX - viewPagerStartX;
    
    const viewpager = document.querySelector('.viewpager');
    const slideWidth = viewpager.clientWidth;
    const baseTranslate = -viewPagerCurrentIndex * 100;
    
    let dragPercent = (diff / slideWidth) * 100;
    
    // Add resistance at boundaries
    if ((viewPagerCurrentIndex === 0 && diff > 0) || 
        (viewPagerCurrentIndex === viewPagerTotalSlides - 1 && diff < 0)) {
        dragPercent *= 0.3;
    }
    
    const translateX = baseTranslate + dragPercent;
    viewpager.style.transform = `translateX(${translateX}%) translateZ(0)`;
}

function handleMouseUp() {
    if (!viewPagerIsDragging) return;
    
    viewPagerIsDragging = false;
    const viewpager = document.querySelector('.viewpager');
    viewpager.style.transition = 'transform 0.3s cubic-bezier(0.4, 0, 0.2, 1)';
    viewpager.style.cursor = 'grab';
    
    const diff = viewPagerCurrentX - viewPagerStartX;
    const slideWidth = viewpager.clientWidth;
    const threshold = slideWidth * 0.1;
    
    if (Math.abs(diff) > threshold) {
        if (diff > 0 && viewPagerCurrentIndex > 0) {
            goToSlide(viewPagerCurrentIndex - 1);
        } else if (diff < 0 && viewPagerCurrentIndex < viewPagerTotalSlides - 1) {
            goToSlide(viewPagerCurrentIndex + 1);
        } else {
            // At boundary - just snap back (NO RUBBER BAND)
            updateViewPagerPosition();
        }
    } else {
        updateViewPagerPosition();
    }
}

async function populateCodeExamples() {
    // Show loading indicator
    const demoExample = document.getElementById('demoExample');
    
    demoExample.innerHTML = '<div class="loading-indicator">Loading InteractiveDemo.cod from GitHub...</div>';
    
    // Fetch the InteractiveDemo.cod file from GitHub
    const demoCode = await fetchGitHubFile(GITHUB_REPO.demoPath);
    
    // Clear loading indicator
    demoExample.innerHTML = '';
    
    if (demoCode) {
        // Create code element with fetched content
        const demoPre = document.createElement('pre');
        const demoCodeElement = document.createElement('code');
        demoCodeElement.className = 'code-example-content';
        demoCodeElement.textContent = demoCode;
        
        // Add a note that this is from GitHub
        const noteElement = document.createElement('div');
        noteElement.className = 'code-note';
        noteElement.textContent = '📁 Full InteractiveDemo.cod file from GitHub repository';
        demoExample.appendChild(noteElement);
        
        demoPre.appendChild(demoCodeElement);
        demoExample.appendChild(demoPre);
    } else {
        // Show error message if fetch fails
        demoExample.innerHTML = '<div class="code-note error">❌ Failed to load code from GitHub. Please check your connection and try again.</div>';
    }
    
    // Language features section remains the same
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
                user-select: text;
                -webkit-user-select: text;
                touch-action: pan-y;
            }
            
            /* Style the pre element for better wrapping */
            .code-example pre {
                max-width: 100%;
                overflow-x: auto;
                touch-action: pan-y;
            }
            
            /* Add some color to the wrapper */
            .code-example {
                background: linear-gradient(145deg, #0d1117, #161b22);
                border: 1px solid #58a6ff;
                padding: 5px;
                position: relative;
                touch-action: pan-y;
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
            
            /* Learn More link styling */
            .learn-more-link {
                color: #007acc;
                text-decoration: underline;
                font-weight: 500;
                cursor: pointer;
                transition: color 0.3s ease;
            }
            
            .learn-more-link:hover {
                color: #005a9e;
                text-decoration: underline;
            }
            
            /* Loading indicator */
            .loading-indicator {
                color: var(--text-secondary);
                text-align: center;
                padding: 2rem;
                font-style: italic;
            }
            
            /* Code note */
            .code-note {
                font-size: 0.8rem;
                color: #58a6ff;
                margin-bottom: 0.5rem;
                padding: 0.25rem 0.5rem;
                background: rgba(88, 166, 255, 0.1);
                border-radius: 4px;
                display: inline-block;
            }
            
            .code-note.error {
                color: #f97583;
                background: rgba(249, 117, 131, 0.1);
            }
            
            /* Mobile touch fixes */
            @media (max-width: 768px) {
                .code-example,
                .code-example *,
                .command,
                .command-container,
                .command * {
                    touch-action: pan-y !important;
                    -webkit-overflow-scrolling: touch !important;
                }
                
                body {
                    touch-action: pan-y !important;
                    overflow-y: scroll !important;
                }
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
        
        // Apply mobile touch fixes
        document.body.style.touchAction = 'pan-y';
        document.body.style.overflowY = 'scroll';
        
        // Make all code elements touch-friendly
        const codeElements = document.querySelectorAll('.code-example, .code-example *, .command, .command-container');
        codeElements.forEach(el => {
            el.style.touchAction = 'pan-y';
            el.style.userSelect = 'text';
            el.style.webkitUserSelect = 'text';
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
        document.body.style.touchAction = 'pan-y';
    }
    
    // Add smooth scroll behavior
    const scrollableElements = document.querySelectorAll('.code-example, .command');
    scrollableElements.forEach(el => {
        el.style.WebkitOverflowScrolling = 'touch';
        el.style.touchAction = 'pan-y';
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
    
    // Apply initial touch fixes
    setTimeout(() => {
        if ('ontouchstart' in window) {
            // Apply touch fixes after page loads
            const codeElements = document.querySelectorAll('.code-example, .command');
            codeElements.forEach(el => {
                el.style.touchAction = 'pan-y';
                el.style.webkitUserSelect = 'text';
                el.style.userSelect = 'text';
            });
        }
    }, 1000);
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
        document.body.style.touchAction = 'pan-y';
        
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
            
            // Re-apply touch fixes after orientation change
            if ('ontouchstart' in window) {
                const codeElements = document.querySelectorAll('.code-example, .command');
                codeElements.forEach(el => {
                    el.style.touchAction = 'pan-y';
                });
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
        document.addEventListener('touchmove', function(e) {
            // Allow default touch behavior for scrolling
            // Only prevent default if we're handling a specific gesture
            const isCodeElement = e.target.closest('.code-example, .command, pre, code');
            if (!isCodeElement) {
                // Allow default scroll behavior
                return;
            }
        }, { passive: true });
        
        // Add touch event listener to allow scrolling through code areas
        document.addEventListener('touchstart', function(e) {
            // Ensure code areas allow text selection
            const codeElement = e.target.closest('.code-example, .code-example-content, .command');
            if (codeElement) {
                codeElement.style.userSelect = 'text';
                codeElement.style.webkitUserSelect = 'text';
            }
        }, { passive: true });
    }
    
    // Initial orientation setup
    updateLayoutForOrientation();
    setTimeout(updateLeftSideWidth, 100); // Initial width calculation
    
    // Initial smooth scrolling setup
    setTimeout(setupSmoothScrolling, 500);
    
    // Add global touch handler to ensure scrolling works
    document.addEventListener('touchmove', function(e) {
        // Always allow touch move for scrolling
        // Don't prevent default unless absolutely necessary
    }, { passive: true });
}