// Initialize content for main page
document.addEventListener('DOMContentLoaded', function() {
    initializeMainContent();
    setupCopyButtons();
    setupEventListeners();
    setupSmoothScrolling();
    handlePageLoad();
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
    document.title = strings.ui.titles.main_page;
    document.getElementById('mainHeader').textContent = strings.ui.titles.coderive_language;
    
    const tagline = document.getElementById('tagline');
    if (tagline) {
        tagline.innerHTML = strings.ui.titles.tagline + '. <span class="learn-more-link">Learn More...</span>';
    }
    
    const landscapeTagline = document.querySelector('.landscape-header .tagline');
    if (landscapeTagline) {
        landscapeTagline.innerHTML = strings.ui.titles.tagline + '. <span class="learn-more-link">Learn More...</span>';
    }
    
    document.getElementById('getStartedBtn').textContent = strings.ui.buttons.get_started;
    document.getElementById('tryEditorBtn').textContent = strings.ui.buttons.try_online_editor;
    document.getElementById('githubBtn').textContent = strings.ui.buttons.view_on_github;
    document.getElementById('openEditorBtn').textContent = strings.ui.buttons.open_online_editor;
    
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
    
    document.getElementById('featuresTitle').textContent = strings.ui.labels.features;
    document.getElementById('examplesTitle').textContent = strings.ui.labels.language_examples;
    document.getElementById('runnersTitle').textContent = strings.ui.labels.execution_runners;
    document.getElementById('techTitle').textContent = strings.ui.labels.technical_innovations;
    document.getElementById('tryTitle').textContent = strings.ui.labels.try_online;
    document.getElementById('gettingStartedTitle').textContent = strings.ui.labels.getting_started;
    
    document.getElementById('demoTitle').textContent = strings.ui.labels.demo_title;
    document.getElementById('tryDescription').textContent = strings.ui.messages.try_description;
    document.getElementById('copyright').textContent = strings.ui.messages.copyright;
    document.getElementById('builtWith').textContent = strings.ui.messages.footer_built_with;
    
    createViewPager();
    populateCodeExamples();
    populateRunners();
    populateTechnicalInnovations();
    populateGettingStarted();
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
    
    setupViewPager();
}

function setupViewPager() {
    const viewpager = document.querySelector('.viewpager');
    const prevBtn = document.querySelector('.viewpager-nav.prev-btn');
    const nextBtn = document.querySelector('.viewpager-nav.next-btn');
    const indicators = document.querySelectorAll('.viewpager-indicator');
    
    updateViewPagerPosition();
    
    prevBtn.addEventListener('click', () => {
        if (viewPagerCurrentIndex > 0) {
            goToSlide(viewPagerCurrentIndex - 1);
        }
    });
    
    nextBtn.addEventListener('click', () => {
        if (viewPagerCurrentIndex < viewPagerTotalSlides - 1) {
            goToSlide(viewPagerCurrentIndex + 1);
        }
    });
    
    indicators.forEach(indicator => {
        indicator.addEventListener('click', () => {
            const targetIndex = parseInt(indicator.dataset.index);
            if (targetIndex !== viewPagerCurrentIndex) {
                goToSlide(targetIndex);
            }
        });
    });
    
    viewpager.addEventListener('touchstart', handleTouchStart, { passive: true });
    viewpager.addEventListener('touchmove', handleTouchMove, { passive: true });
    viewpager.addEventListener('touchend', handleTouchEnd);
    
    viewpager.addEventListener('mousedown', handleMouseDown);
    viewpager.addEventListener('mousemove', handleMouseMove);
    viewpager.addEventListener('mouseup', handleMouseUp);
    viewpager.addEventListener('mouseleave', handleMouseUp);
}

function goToSlide(targetIndex) {
    if (targetIndex < 0 || targetIndex >= viewPagerTotalSlides) return;
    
    const currentSlide = document.querySelector(`.viewpager-slide[data-index="${viewPagerCurrentIndex}"]`);
    const direction = targetIndex > viewPagerCurrentIndex ? 'next' : 'prev';
    
    applySnapEffect(currentSlide, direction);
    
    viewPagerCurrentIndex = targetIndex;
    updateViewPagerPosition();
    updateViewPagerIndicators();
}

function updateViewPagerPosition() {
    const viewpager = document.querySelector('.viewpager');
    const slideWidth = 100;
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

function applySnapEffect(slide, direction) {
    slide.classList.remove('snap-left', 'snap-right');
    void slide.offsetWidth;
    
    if (direction === 'next') {
        slide.classList.add('snap-right');
    } else {
        slide.classList.add('snap-left');
    }
    
    setTimeout(() => {
        slide.classList.remove('snap-left', 'snap-right');
    }, 300);
}

function handleTouchStart(e) {
    const isCodeElement = e.target.closest('.code-example, .code-example-content, .command, pre, code');
    if (isCodeElement) return;
    
    viewPagerIsDragging = true;
    viewPagerStartX = e.touches[0].clientX;
    viewPagerCurrentX = viewPagerStartX;
    
    const viewpager = document.querySelector('.viewpager');
    viewpager.style.transition = 'none';
}

function handleTouchMove(e) {
    if (!viewPagerIsDragging) return;
    
    const touch = e.touches[0];
    const elementAtTouch = document.elementFromPoint(touch.clientX, touch.clientY);
    const isCodeElement = elementAtTouch.closest('.code-example, .code-example-content, .command, pre, code');
    
    if (isCodeElement) {
        handleTouchEnd();
        return;
    }
    
    viewPagerCurrentX = e.touches[0].clientX;
    const diff = viewPagerCurrentX - viewPagerStartX;
    
    const viewpager = document.querySelector('.viewpager');
    const slideWidth = window.innerWidth;
    const baseTranslate = -viewPagerCurrentIndex * 100;
    
    let dragPercent = (diff / slideWidth) * 100;
    
    if ((viewPagerCurrentIndex === 0 && diff > 0) || 
        (viewPagerCurrentIndex === viewPagerTotalSlides - 1 && diff < 0)) {
        dragPercent *= 0.3;
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
    const threshold = slideWidth * 0.1;
    
    if (Math.abs(diff) > threshold) {
        if (diff > 0 && viewPagerCurrentIndex > 0) {
            goToSlide(viewPagerCurrentIndex - 1);
        } else if (diff < 0 && viewPagerCurrentIndex < viewPagerTotalSlides - 1) {
            goToSlide(viewPagerCurrentIndex + 1);
        } else {
            updateViewPagerPosition();
        }
    } else {
        updateViewPagerPosition();
    }
}

function handleMouseDown(e) {
    const isCodeElement = e.target.closest('.code-example, .code-example-content, .command, pre, code');
    if (isCodeElement) return;
    
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
            updateViewPagerPosition();
        }
    } else {
        updateViewPagerPosition();
    }
}

async function populateCodeExamples() {
    const demoExample = document.getElementById('demoExample');
    
    demoExample.innerHTML = '<div class="loading-indicator">Loading InteractiveDemo.cod from GitHub...</div>';
    
    const demoCode = await fetchGitHubFile(GITHUB_REPO.demoPath);
    
    demoExample.innerHTML = '';
    
    if (demoCode) {
        const codeContainer = document.createElement('div');
        codeContainer.className = 'code-preview-container';
        
        const lines = demoCode.split('\n');
        const previewLines = lines.slice(0, 18);
        const truncatedCode = previewLines.join('\n') + '\n\n// ...';
        
        const demoPre = document.createElement('pre');
        const demoCodeElement = document.createElement('code');
        demoCodeElement.className = 'code-example-content';
        demoCodeElement.textContent = truncatedCode;
        
        const fadeContainer = document.createElement('div');
        fadeContainer.className = 'code-fade-container';
        
        demoPre.appendChild(demoCodeElement);
        codeContainer.appendChild(demoPre);
        codeContainer.appendChild(fadeContainer);
        demoExample.appendChild(codeContainer);
        
        const seeMoreContainer = document.createElement('div');
        seeMoreContainer.className = 'see-more-container';
        seeMoreContainer.innerHTML = `
            <a href="examples.html" class="see-more-link">
                See more code examples
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M5 12h14M12 5l7 7-7 7"/>
                </svg>
            </a>
        `;
        
        demoExample.appendChild(seeMoreContainer);
        
    } else {
        demoExample.innerHTML = `
            <div class="code-note error">❌ Failed to load code from GitHub.</div>
            <div class="see-more-container">
                <a href="examples.html" class="see-more-link">
                    See more code examples
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M5 12h14M12 5l7 7-7 7"/>
                    </svg>
                </a>
            </div>
        `;
    }
    
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
            
            .code-example pre {
                max-width: 100%;
                overflow-x: auto;
                touch-action: pan-y;
            }
            
            .code-example {
                background: linear-gradient(145deg, #0d1117, #161b22);
                border: 1px solid #58a6ff;
                padding: 5px;
                position: relative;
                touch-action: pan-y;
            }
            
            .code-example::after {
                content: '';
                position: absolute;
                bottom: 0;
                right: 0;
                background: linear-gradient(135deg, transparent 50%, #007acc 50%);
                border-radius: 0 0 8px 0;
            }
            
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
            
            .loading-indicator {
                color: var(--text-secondary);
                text-align: center;
                padding: 2rem;
                font-style: italic;
            }
            
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
            
            .code-preview-container {
                position: relative;
                max-height: 400px;
                overflow: hidden;
            }
            
            .code-fade-container {
                position: absolute;
                bottom: 0;
                left: 0;
                right: 0;
                height: 100px;
                background: linear-gradient(to bottom, transparent, #0d1117);
                pointer-events: none;
            }
            
            .see-more-container {
                text-align: center;
                margin-top: 1.5rem;
                padding-top: 0.5rem;
            }
            
            .see-more-link {
                display: inline-flex;
                align-items: center;
                gap: 0.5rem;
                color: var(--accent-primary);
                text-decoration: none;
                font-weight: 500;
                font-size: 1rem;
                padding: 0.75rem 1.5rem;
                border-radius: 4px;
                background: rgba(0, 122, 204, 0.1);
                transition: all 0.3s ease;
                border: 1px solid transparent;
            }
            
            .see-more-link:hover {
                background: rgba(0, 122, 204, 0.15);
                border-color: var(--accent-primary);
                transform: translateY(-2px);
            }
            
            .see-more-link svg {
                transition: transform 0.3s ease;
            }
            
            .see-more-link:hover svg {
                transform: translateX(4px);
            }
            
            @media (max-width: 768px) {
                .see-more-link {
                    padding: 0.6rem 1.2rem;
                    font-size: 0.95rem;
                }
            }
            
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
    
    setTimeout(setupCopyButtons, 100);
}

function isMobileDevice() {
    const userAgent = navigator.userAgent.toLowerCase();
    const isMobileUA = /android|webos|iphone|ipad|ipod|blackberry|iemobile|opera mini/i.test(userAgent);
    const hasTouch = 'ontouchstart' in window || navigator.maxTouchPoints > 0;
    const isSmallScreen = window.innerWidth <= 768;
    const isHighDPR = window.devicePixelRatio > 1;
    
    return (isMobileUA || (hasTouch && isSmallScreen)) && isHighDPR;
}

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
        
        const topLeftContainer = document.querySelector('.top-left-container');
        if (topLeftContainer) {
            const menuButton = topLeftContainer.querySelector('.side-panel-toggle');
            if (menuButton) {
                menuButton.style.padding = '12px 16px';
                menuButton.style.fontSize = '18px';
            }
        }
        
        const body = document.body;
        body.style.fontSize = '16px';
        
        const sections = document.querySelectorAll('section');
        sections.forEach(section => {
            section.classList.add('mobile-section');
        });
        
        document.body.style.touchAction = 'pan-y';
        document.body.style.overflowY = 'scroll';
        
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
    
    if (debugElement) {
        setTimeout(() => {
            debugElement.style.display = 'none';
        }, 3000);
    }
}

function updateLeftSideWidth() {
    if (document.body.classList.contains('landscape-mode')) {
        const leftSide = document.querySelector('.landscape-left');
        if (leftSide) {
            const width = leftSide.offsetWidth;
            document.documentElement.style.setProperty('--left-side-width', width + 'px');
        }
    }
}

function setupSmoothScrolling() {
    document.removeEventListener('touchmove', preventDefaultScroll);
    
    if (!document.body.classList.contains('landscape-mode')) {
        document.body.style.overflowY = 'auto';
        document.body.style.height = 'auto';
        document.body.style.minHeight = '100vh';
        document.body.style.touchAction = 'pan-y';
    }
    
    const scrollableElements = document.querySelectorAll('.code-example, .command');
    scrollableElements.forEach(el => {
        el.style.WebkitOverflowScrolling = 'touch';
        el.style.touchAction = 'pan-y';
    });
}

function preventDefaultScroll(e) {
    e.preventDefault();
}

let drawerVisible = true;
let drawer = document.getElementById('mainDrawer');
let drawerToggle = document.getElementById('drawerToggle');
let drawerOverlay = document.getElementById('drawerOverlay');

function handlePageLoad() {
    setTimeout(() => {
        document.body.classList.add('loaded');
    }, 500);
    
    const topLeftContainer = document.querySelector('.top-left-container');
    if (topLeftContainer) {
        topLeftContainer.style.transform = '';
        topLeftContainer.style.transition = '';
    }
    
    setTimeout(() => {
        if ('ontouchstart' in window) {
            const codeElements = document.querySelectorAll('.code-example, .command');
            codeElements.forEach(el => {
                el.style.touchAction = 'pan-y';
                el.style.webkitUserSelect = 'text';
                el.style.userSelect = 'text';
            });
        }
    }, 1000);
}

function initializeDrawer() {
    const isSmallScreen = window.innerWidth <= 600;
    const topLeftContainer = document.querySelector('.top-left-container');
    
    if (topLeftContainer) {
        topLeftContainer.style.transform = '';
        topLeftContainer.style.transition = '';
    }
    
    if (isSmallScreen) {
        drawerVisible = false;
        document.body.classList.remove('drawer-visible');
        drawerToggle.textContent = '☰';
        drawerToggle.setAttribute('aria-label', 'Open navigation menu');
        drawerToggle.style.background = '#007acc';
    } else {
        drawerVisible = false;
        document.body.classList.remove('drawer-visible');
        drawerToggle.textContent = '☰';
        drawerToggle.setAttribute('aria-label', 'Open navigation menu');
        drawerToggle.style.background = '#007acc';
    }
}

function toggleDrawer() {
    drawerVisible = !drawerVisible;
    
    if (drawerVisible) {
        document.body.classList.add('drawer-visible');
        drawerToggle.textContent = '✕';
        drawerToggle.setAttribute('aria-label', 'Close navigation menu');
        drawerToggle.style.background = '#005a9e';
        
        // CRITICAL: When opening drawer, fix the body and ensure content can scroll
        if (window.innerWidth <= 768) {
            document.body.style.position = 'fixed';
            document.body.style.width = '100%';
            document.body.style.height = '100%';
            document.body.style.overflow = 'hidden';
            
            const landscapeRight = document.querySelector('.landscape-right');
            if (landscapeRight) {
                landscapeRight.style.overflowY = 'auto';
                landscapeRight.style.WebkitOverflowScrolling = 'touch';
                landscapeRight.style.height = '100%';
                landscapeRight.style.position = 'relative';
            }
        }
    } else {
        document.body.classList.remove('drawer-visible');
        drawerToggle.textContent = '☰';
        drawerToggle.setAttribute('aria-label', 'Open navigation menu');
        drawerToggle.style.background = '#007acc';
        
        // CRITICAL: When closing drawer, restore body scrolling
        if (window.innerWidth <= 768) {
            document.body.style.position = '';
            document.body.style.width = '';
            document.body.style.height = '';
            document.body.style.overflow = '';
            
            const landscapeRight = document.querySelector('.landscape-right');
            if (landscapeRight) {
                landscapeRight.style.overflowY = '';
                landscapeRight.style.WebkitOverflowScrolling = '';
                landscapeRight.style.height = '';
                landscapeRight.style.position = '';
            }
        }
    }
    
    const topLeftContainer = document.querySelector('.top-left-container');
    if (topLeftContainer) {
        topLeftContainer.offsetHeight;
    }
    
    localStorage.setItem('drawerState', drawerVisible ? 'open' : 'closed');
}

function updateLayoutForOrientation() {
    const isLandscape = window.innerWidth > window.innerHeight && window.innerWidth > 768;
    
    if (isLandscape) {
        document.body.classList.add('landscape-mode');
        document.body.style.overflow = 'hidden';
        
        drawerToggle.style.display = 'none';
        
        if (drawerVisible) {
            toggleDrawer();
        }
    } else {
        document.body.classList.remove('landscape-mode');
        document.body.style.overflow = 'auto';
        document.body.style.height = 'auto';
        document.body.style.minHeight = '100vh';
        document.body.style.touchAction = 'pan-y';
        
        drawerToggle.style.display = 'flex';
    }
}

function setupEventListeners() {
    addCodeStyles();
    applyMobileStyles();
    initializeDrawer();
    
    drawerToggle.addEventListener('click', toggleDrawer);
    
    drawerOverlay.addEventListener('click', function() {
        if (window.innerWidth <= 600) {
            toggleDrawer();
        }
    });
    
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape' && drawerVisible && window.innerWidth <= 600) {
            toggleDrawer();
        }
    });
    
    window.addEventListener('orientationchange', function() {
        setTimeout(function() {
            window.dispatchEvent(new Event('resize'));
            applyMobileStyles();
            setupSmoothScrolling();
            initializeDrawer();
            
            const topLeftContainer = document.querySelector('.top-left-container');
            if (topLeftContainer) {
                topLeftContainer.style.transform = '';
                topLeftContainer.style.transition = '';
            }
            
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
        applyMobileStyles();
        initializeDrawer();
        
        const topLeftContainer = document.querySelector('.top-left-container');
        if (topLeftContainer) {
            topLeftContainer.style.transform = '';
            topLeftContainer.style.transition = '';
        }
    });

    if ('ontouchstart' in window) {
        document.addEventListener('touchmove', function(e) {
            const isCodeElement = e.target.closest('.code-example, .command, pre, code');
            if (!isCodeElement) {
                return;
            }
        }, { passive: true });
        
        document.addEventListener('touchstart', function(e) {
            const codeElement = e.target.closest('.code-example, .code-example-content, .command');
            if (codeElement) {
                codeElement.style.userSelect = 'text';
                codeElement.style.webkitUserSelect = 'text';
            }
        }, { passive: true });
    }
    
    updateLayoutForOrientation();
    setTimeout(updateLeftSideWidth, 100);
    setTimeout(setupSmoothScrolling, 500);
    
    document.addEventListener('touchmove', function(e) {
    }, { passive: true });
}