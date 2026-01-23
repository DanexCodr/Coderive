let outputButton = document.getElementById("outputButton");
let codeButton = document.getElementById("codeButton");
let codeTextarea = document.getElementById("code");
let buttonSlider = document.getElementById("buttonSlider");
let headerTitle = document.getElementById("headerTitle");
let currentMode = 'code';
let savedCode = '';

// Side panel elements
let sidePanel = document.getElementById("sidePanel");
let sidePanelToggle = document.getElementById("sidePanelToggle");
let edgeSwipeArea = document.getElementById("edgeSwipeArea");
let mainContent = document.querySelector('.main-content');

// Track original dimensions for restoration
let originalViewportHeight = window.innerHeight;
let originalTextareaHeight = null;
let originalContainerHeight = null;
let isKeyboardVisible = false;
let resizeTimeout = null;
let viewportCheckInterval = null;

// Touch drag variables
let isDragging = false;
let startX = 0;
let currentX = 0;
let isOpen = false;
let panelStartX = 0;

// Enhanced viewport handling for keyboard show/hide
function checkViewportRestored() {
    const currentViewportHeight = window.innerHeight;
    const tolerance = 50;
    
    if (Math.abs(currentViewportHeight - originalViewportHeight) < tolerance) {
        if (isKeyboardVisible) {
            isKeyboardVisible = false;
            restoreOriginalHeights();
            
            setTimeout(() => {
                ensureCursorVisible();
            }, 100);
            
            if (viewportCheckInterval) {
                clearInterval(viewportCheckInterval);
                viewportCheckInterval = null;
            }
        }
    } else if (currentViewportHeight < originalViewportHeight - 100) {
        if (!isKeyboardVisible) {
            isKeyboardVisible = true;
            adjustTextareaToViewport();
        }
    }
}

function adjustTextareaToViewport() {
    if (isPortraitMode() && document.activeElement === codeTextarea) {
        const viewportHeight = window.innerHeight;
        const textareaRect = codeTextarea.getBoundingClientRect();
        
        const availableHeight = viewportHeight - textareaRect.top - 20;
        
        if (availableHeight > 200) {
            codeTextarea.style.height = availableHeight + 'px';
            codeTextarea.parentElement.style.height = availableHeight + 'px';
        }
    }
}

// Touch handlers for edge swipe and panel dragging
function handleTouchStart(e) {
    // Don't allow dragging in landscape mode
    if (!isPortraitMode()) return;
    
    const touch = e.touches[0];
    startX = touch.clientX;
    currentX = startX;
    
    // Store current panel position for dragging
    if (isOpen) {
        panelStartX = 0; // Panel is fully open
    } else {
        panelStartX = -70; // Panel is fully closed
    }
    
    // Edge swipe to open OR drag open panel to close
    if ((touch.clientX <= 20 && !isOpen) || isOpen) {
        isDragging = true;
        mainContent.classList.add('dragging');
        sidePanel.classList.add('dragging');
        e.preventDefault();
    }
}

function handleTouchMove(e) {
    // Don't allow dragging in landscape mode
    if (!isPortraitMode() || !isDragging) return;
    
    const touch = e.touches[0];
    currentX = touch.clientX;
    const deltaX = currentX - startX;
    const screenWidth = window.innerWidth;
    
    if (!isOpen) {
        // Opening from closed state
        if (deltaX > 0) {
            const progress = Math.min(1, deltaX / (screenWidth * 0.7));
            const translateX = progress * 70;
            const panelLeft = -70 + translateX;
            
            // Move main content
            mainContent.style.transform = `translateX(${translateX}%)`;
            // Move side panel
            sidePanel.style.left = `${panelLeft}%`;
            // Update overlay opacity
            updateOverlayOpacity(progress);
        }
    } else {
        // Closing from open state
        if (deltaX < 0) {
            const progress = Math.min(1, Math.abs(deltaX) / (screenWidth * 0.7));
            const translateX = (1 - progress) * 70;
            const panelLeft = -(progress * 70);
            
            // Move main content
            mainContent.style.transform = `translateX(${translateX}%)`;
            // Move side panel
            sidePanel.style.left = `${panelLeft}%`;
            // Update overlay opacity
            updateOverlayOpacity(1 - progress);
        }
    }
    
    e.preventDefault();
}

function handleTouchEnd(e) {
    // Don't allow dragging in landscape mode
    if (!isPortraitMode() || !isDragging) return;
    
    isDragging = false;
    mainContent.classList.remove('dragging');
    sidePanel.classList.remove('dragging');
    
    const deltaX = currentX - startX;
    const screenWidth = window.innerWidth;
    const velocity = Math.abs(deltaX) / 300;
    const threshold = 0.3;
    
    if (!isOpen) {
        const openProgress = deltaX / (screenWidth * 0.7);
        if (openProgress > threshold || velocity > 0.5) {
            openPanel();
        } else {
            closePanel();
        }
    } else {
        const closeProgress = Math.abs(deltaX) / (screenWidth * 0.7);
        if (closeProgress > threshold || velocity > 0.5) {
            closePanel();
        } else {
            openPanel();
        }
    }
    
    e.preventDefault();
}

// Update overlay opacity during drag
function updateOverlayOpacity(progress) {
    const overlay = document.querySelector('.main-content::before');
    if (overlay) {
        document.documentElement.style.setProperty('--overlay-opacity', progress * 0.5);
    }
}

function initializeButtonWidth() {
    const originalWidth = sidePanelToggle.getBoundingClientRect().width;
    sidePanelToggle.style.width = originalWidth + 'px';
    sidePanelToggle.style.textAlign = 'center';
}

function openPanel() {
    isOpen = true;
    sidePanel.classList.add('open');
    document.body.classList.add('panel-open');
    // Reset any inline styles and use CSS classes
    mainContent.style.transform = '';
    sidePanel.style.left = '';
    sidePanelToggle.textContent = strings.ui.buttons.close;
    sidePanelToggle.style.background = '#005a9e';
    
    // Disable textarea when panel is open
    disableTextarea();
    
    // Reset overlay opacity
    document.documentElement.style.setProperty('--overlay-opacity', '0.5');
}

function closePanel() {
    isOpen = false;
    sidePanel.classList.remove('open');
    document.body.classList.remove('panel-open');
    // Reset any inline styles and use CSS classes
    mainContent.style.transform = '';
    sidePanel.style.left = '';
    sidePanelToggle.textContent = strings.ui.buttons.menu;
    sidePanelToggle.style.background = '#007acc';
    
    // Re-enable textarea when panel is closed
    enableTextarea();
    
    // Reset overlay opacity
    document.documentElement.style.setProperty('--overlay-opacity', '0');
}

// Function to disable textarea
function disableTextarea() {
    codeTextarea.disabled = true;
    codeTextarea.style.cursor = 'default';
    codeTextarea.style.opacity = '0.7';
    codeTextarea.blur(); // Remove focus if it was focused
}

// Function to enable textarea
function enableTextarea() {
    codeTextarea.disabled = false;
    codeTextarea.style.cursor = 'text';
    codeTextarea.style.opacity = '1';
}

// Detect device orientation
function isPortraitMode() {
    return window.innerHeight > window.innerWidth;
}

// Update the updateLayoutForOrientation function to be less aggressive
function updateLayoutForOrientation() {
    const isLandscape = window.innerWidth > window.innerHeight && window.innerWidth > 768;
    
    if (isLandscape) {
        document.body.classList.add('landscape-mode');
    } else {
        document.body.classList.remove('landscape-mode');
    }
}

function ensureCursorVisible() {
    // Simple implementation - just scroll to cursor position
    setTimeout(() => {
        codeTextarea.scrollTop = codeTextarea.scrollHeight;
        codeTextarea.scrollLeft = codeTextarea.scrollWidth;
    }, 0);
}

function storeOriginalHeights() {
    if (!originalTextareaHeight) {
        originalTextareaHeight = codeTextarea.style.height;
        originalContainerHeight = codeTextarea.parentElement.style.height;
        originalViewportHeight = window.innerHeight;
    }
}

function restoreOriginalHeights() {
    if (originalTextareaHeight) {
        codeTextarea.style.height = originalTextareaHeight;
        codeTextarea.parentElement.style.height = originalContainerHeight;
    } else {
        codeTextarea.style.height = '100%';
        codeTextarea.parentElement.style.height = '100%';
    }
    codeTextarea.parentElement.style.marginTop = '0';
    codeTextarea.parentElement.style.marginBottom = '0';
}

function toggleSidePanel() {
    // Don't allow toggling in landscape mode
    if (!isPortraitMode()) return;
    
    if (isOpen) {
        closePanel();
    } else {
        openPanel();
    }
}

function initializeSlider() {
    // Set button slider to always be half width (60px since container is 120px)
    buttonSlider.style.width = '60px';
    
    // Initialize position based on current mode
    updateSliderPosition();
}

function updateSliderPosition() {
    if (currentMode === 'code') {
        buttonSlider.style.left = '0';
        buttonSlider.classList.remove('output-active');
        buttonSlider.classList.add('code-active');
    } else {
        buttonSlider.style.left = '50%';
        buttonSlider.classList.remove('code-active');
        buttonSlider.classList.add('output-active');
    }
}

function setMode(mode) {
    currentMode = mode;
    
    if (mode === 'code') {
        codeButton.classList.remove('inactive');
        codeButton.classList.add('active');
        outputButton.classList.remove('active');
        outputButton.classList.add('inactive');
        
        codeTextarea.value = savedCode;
    } else {
        outputButton.classList.remove('inactive');
        outputButton.classList.add('active');
        codeButton.classList.remove('active');
        codeButton.classList.add('inactive');
        
        savedCode = codeTextarea.value;
        // Simulate Coderive output
        codeTextarea.value = simulateCoderiveOutput();
    }
    
    // Update slider position after mode change
    updateSliderPosition();
}

function initializeEventListeners() {
    codeTextarea.addEventListener('focus', function() {
        this.parentElement.style.borderColor = '#007acc';
        storeOriginalHeights();
        setTimeout(function() {
            adjustTextareaToViewport();
            ensureCursorVisible();
        }, 100);
    });

    codeTextarea.addEventListener('blur', function() {
        this.parentElement.style.borderColor = '#333';
        setTimeout(checkViewportRestored, 300);
    });

    codeTextarea.addEventListener('input', function() {
        ensureCursorVisible();
    });

    codeTextarea.addEventListener('click', function() {
        ensureCursorVisible();
    });

    // Click outside to close panel (when overlay is visible)
    document.addEventListener('click', function(e) {
        // Only handle if panel is open in portrait mode
        if (isPortraitMode() && isOpen) {
            const isClickOnPanel = sidePanel.contains(e.target);
            const isClickOnToggle = sidePanelToggle.contains(e.target);
            const isClickOnEdgeSwipe = edgeSwipeArea.contains(e.target);
            
            // If click is on overlay (main content) and not on panel or toggle
            if (!isClickOnPanel && !isClickOnToggle && !isClickOnEdgeSwipe) {
                closePanel();
            }
        }
    });

    // Tab buttons
    codeButton.addEventListener("click", function(e) {
        e.preventDefault();
        e.stopPropagation();
        if (currentMode !== 'code') {
            setMode('code');
        }
    });

    outputButton.addEventListener("click", function(e) {
        e.preventDefault();
        e.stopPropagation();
        if (currentMode !== 'output') {
            setMode('output');
        }
    });

    // Menu button
    sidePanelToggle.addEventListener("click", function(e) {
        e.preventDefault();
        e.stopPropagation();
        toggleSidePanel();
    });

    // Add touch listeners for both edge swipe area AND side panel for dragging
    edgeSwipeArea.addEventListener('touchstart', handleTouchStart, { passive: false });
    sidePanel.addEventListener('touchstart', handleTouchStart, { passive: false });
    
    document.addEventListener('touchmove', handleTouchMove, { passive: false });
    document.addEventListener('touchend', handleTouchEnd, { passive: false });
    document.addEventListener('touchcancel', handleTouchEnd, { passive: false });

    // Close panel on Escape key
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape' && isOpen) {
            closePanel();
        }
    });

    window.addEventListener('resize', function() {
        if (resizeTimeout) {
            clearTimeout(resizeTimeout);
        }
        
        resizeTimeout = setTimeout(function() {
            const currentViewportHeight = window.innerHeight;
            
            if (isKeyboardVisible && !viewportCheckInterval) {
                viewportCheckInterval = setInterval(checkViewportRestored, 100);
            }
            
            if (isPortraitMode() && document.activeElement === codeTextarea) {
                if (currentViewportHeight < originalViewportHeight - 100) {
                    isKeyboardVisible = true;
                    adjustTextareaToViewport();
                }
            }
            
            // Update layout for orientation changes
            updateLayoutForOrientation();
            
            initializeSlider();
            ensureCursorVisible();
        }, 300);
    });

    document.addEventListener('visibilitychange', function() {
        if (!document.hidden) {
            setTimeout(checkViewportRestored, 100);
        }
    });
}

function initializeApp() {
    console.log('Initializing Coderive IDE...');
    initializeButtonWidth();
    initializeSlider();
    initializeContent(); // Initialize dynamic content
    setMode('code');
    adjustTextareaToViewport();
    ensureCursorVisible();
    initializeEventListeners();
    setTimeout(storeOriginalHeights, 500);
    
    // Initial layout setup
    updateLayoutForOrientation();
}

document.addEventListener('DOMContentLoaded', initializeApp);

setTimeout(function() {
    adjustTextareaToViewport();
    ensureCursorVisible();
}, 500);

// ===========================
// CONTENT INITIALIZATION
// ===========================

// Content initialization
function initializeContent() {
    // Set page title
    document.title = strings.ui.titles.coderive_ide;
    
    // Set header title
    headerTitle.textContent = strings.ui.titles.coderive_ide;
    
    // Set button texts
    codeButton.textContent = strings.ui.buttons.code;
    outputButton.textContent = strings.ui.buttons.output;
    
    // Populate menu items
    populateMenuItems();
    
    // Set placeholder text
    codeTextarea.placeholder = strings.ui.placeholders.code_editor;
    
    // Initialize menu button
    sidePanelToggle.textContent = strings.ui.buttons.menu;
}

function populateMenuItems() {
    const filesList = document.getElementById('filesList');
    const projectsList = document.getElementById('projectsList');
    const examplesList = document.getElementById('examplesList');
    
    // Clear existing items
    filesList.innerHTML = '';
    projectsList.innerHTML = '';
    examplesList.innerHTML = '';
    
    // Set section titles
    document.getElementById('filesTitle').textContent = strings.ui.navigation.coderive_files;
    document.getElementById('projectsTitle').textContent = strings.ui.navigation.projects;
    document.getElementById('examplesTitle').textContent = strings.ui.navigation.examples;
    
    // Populate files
    const files = [
        strings.ui.menu_items.main_cod,
        strings.ui.menu_items.utils_cod,
        strings.ui.menu_items.config_cod
    ];
    
    files.forEach(file => {
        const li = document.createElement('li');
        li.textContent = file;
        li.addEventListener('click', () => {
            // Handle file selection
            console.log('Selected file:', file);
            closePanel();
        });
        filesList.appendChild(li);
    });
    
    // Populate projects
    const projects = [
        strings.ui.menu_items.myapp,
        strings.ui.menu_items.demo_project,
        strings.ui.menu_items.test_suite
    ];
    
    projects.forEach(project => {
        const li = document.createElement('li');
        li.textContent = project;
        li.addEventListener('click', () => {
            console.log('Selected project:', project);
            closePanel();
        });
        projectsList.appendChild(li);
    });
    
    // Populate examples
    const examples = [
        strings.ui.menu_items.hello_world,
        strings.ui.menu_items.calculator,
        strings.ui.menu_items.data_structures
    ];
    
    examples.forEach(example => {
        const li = document.createElement('li');
        li.textContent = example;
        li.addEventListener('click', () => {
            console.log('Selected example:', example);
            closePanel();
        });
        examplesList.appendChild(li);
    });
}

// Update the output simulation to use dynamic content
function simulateCoderiveOutput() {
    const now = new Date();
    return strings.ui.placeholders.output_simulation + now.toLocaleTimeString();
}