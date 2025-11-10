let outputButton = document.getElementById("outputButton");
let codeButton = document.getElementById("codeButton");
let codeTextarea = document.getElementById("code");
let buttonSlider = document.getElementById("buttonSlider");
let lineNumbers = document.getElementById("lineNumbers");
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

// Use the exact same values as CSS
const LINE_HEIGHT = 21;
const TEXTAREA_TOP_PADDING = 15;

// Enhanced viewport handling for keyboard show/hide
function checkViewportRestored() {
    const currentViewportHeight = window.innerHeight;
    const tolerance = 50;
    
    if (Math.abs(currentViewportHeight - originalViewportHeight) < tolerance) {
        if (isKeyboardVisible) {
            isKeyboardVisible = false;
            restoreOriginalHeights();
            
            setTimeout(() => {
                updateCurrentLineHighlight();
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
    sidePanelToggle.textContent = '✕';
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
    sidePanelToggle.textContent = '☰';
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

// Create current line highlight element
let currentLineHighlight = document.createElement('div');
currentLineHighlight.className = 'current-line';
codeTextarea.parentElement.appendChild(currentLineHighlight);

// Detect device orientation
function isPortraitMode() {
    return window.innerHeight > window.innerWidth;
}

// Update layout based on orientation
function updateLayoutForOrientation() {
    if (isPortraitMode()) {
        // Portrait mode - normal behavior
        sidePanel.style.width = '70%';
        sidePanel.style.left = isOpen ? '0' : '-70%';
        mainContent.style.transform = isOpen ? 'translateX(70%)' : '';
        mainContent.style.width = '100%';
        mainContent.style.left = '0';
        sidePanelToggle.style.display = 'block';
        edgeSwipeArea.style.display = 'block';
        
        if (isOpen) {
            document.body.classList.add('panel-open');
            disableTextarea();
        } else {
            document.body.classList.remove('panel-open');
            enableTextarea();
        }
        document.body.classList.remove('landscape-mode');
    } else {
        // Landscape mode - side by side layout
        const landscapeWidth = 35; // 70% of 50% = 35%
        sidePanel.style.width = landscapeWidth + '%';
        sidePanel.style.left = '0';
        mainContent.style.transform = 'none';
        mainContent.style.width = (100 - landscapeWidth) + '%';
        mainContent.style.left = landscapeWidth + '%';
        sidePanelToggle.style.display = 'none';
        edgeSwipeArea.style.display = 'none';
        document.body.classList.add('landscape-mode');
        document.body.classList.remove('panel-open');
        
        // In landscape, textarea should be enabled since panel is always visible
        enableTextarea();
    }
    
    // Update overlay for landscape mode
    if (!isPortraitMode()) {
        document.documentElement.style.setProperty('--overlay-opacity', '0');
    }
    
    // Reinitialize slider to ensure proper button positioning
    setTimeout(initializeSlider, 100);
}

// Update line numbers
function updateLineNumbers() {
    const lines = codeTextarea.value.split('\n').length;
    const currentLines = lineNumbers.childElementCount;
    
    if (lines > currentLines) {
        for (let i = currentLines + 1; i <= lines; i++) {
            const lineNumber = document.createElement('div');
            lineNumber.textContent = i;
            lineNumbers.appendChild(lineNumber);
        }
    } else if (lines < currentLines) {
        while (lineNumbers.childElementCount > lines) {
            lineNumbers.removeChild(lineNumbers.lastChild);
        }
    }
    
    updateLineNumbersScroll();
}

function updateLineNumbersScroll() {
    const scrollTop = codeTextarea.scrollTop;
    lineNumbers.style.transform = `translateY(${-scrollTop}px)`;
}

function getCursorLinePosition() {
    const cursorPosition = codeTextarea.selectionStart;
    const textUpToCursor = codeTextarea.value.substring(0, cursorPosition);
    const lines = textUpToCursor.split('\n');
    const currentLine = lines.length;
    
    const currentLineText = lines[lines.length - 1] || '';
    const cursorInLine = currentLineText.length;
    
    return {
        line: currentLine,
        positionInLine: cursorInLine,
        totalLines: codeTextarea.value.split('\n').length
    };
}

function updateCurrentLineHighlight() {
    const cursorPos = getCursorLinePosition();
    const currentLine = cursorPos.line;
    
    const highlightTop = TEXTAREA_TOP_PADDING + (currentLine - 1) * LINE_HEIGHT - codeTextarea.scrollTop;
    
    currentLineHighlight.style.top = highlightTop + 'px';
    currentLineHighlight.style.height = LINE_HEIGHT + 'px';
}

function ensureCursorVisible() {
    const cursorPos = getCursorLinePosition();
    const currentLine = cursorPos.line;
    
    const cursorTop = (currentLine - 1) * LINE_HEIGHT;
    const textareaHeight = codeTextarea.clientHeight;
    const scrollTop = codeTextarea.scrollTop;
    
    if (cursorTop < scrollTop) {
        codeTextarea.scrollTop = Math.max(0, cursorTop - 10);
    } else if (cursorTop + LINE_HEIGHT > scrollTop + textareaHeight) {
        codeTextarea.scrollTop = cursorTop - textareaHeight + LINE_HEIGHT + 10;
    }
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
        updateLineNumbers();
        updateCurrentLineHighlight();
    } else {
        outputButton.classList.remove('inactive');
        outputButton.classList.add('active');
        codeButton.classList.remove('active');
        codeButton.classList.add('inactive');
        
        savedCode = codeTextarea.value;
        // Simulate Coderive output
        codeTextarea.value = "Coderive Output:\n" + 
                            "Running code...\n" +
                            "Hello, Coderive!\n" +
                            "Execution completed successfully.\n" +
                            "Time: " + new Date().toLocaleTimeString();
        updateLineNumbers();
        updateCurrentLineHighlight();
    }
    
    // Update slider position after mode change
    updateSliderPosition();
}

function initializeEventListeners() {
    codeTextarea.addEventListener('scroll', function() {
        updateLineNumbersScroll();
        updateCurrentLineHighlight();
    });

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
        updateLineNumbers();
        updateCurrentLineHighlight();
        ensureCursorVisible();
    });

    codeTextarea.addEventListener('click', function() {
        setTimeout(updateCurrentLineHighlight, 0);
        setTimeout(updateCurrentLineHighlight, 50);
        ensureCursorVisible();
    });

    codeTextarea.addEventListener('keyup', function() {
        updateCurrentLineHighlight();
        ensureCursorVisible();
    });

    codeTextarea.addEventListener('keydown', function() {
        setTimeout(updateCurrentLineHighlight, 0);
        setTimeout(ensureCursorVisible, 10);
    });

    document.addEventListener('selectionchange', function() {
        if (document.activeElement === codeTextarea) {
            setTimeout(updateCurrentLineHighlight, 0);
        }
    });

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

    sidePanelToggle.addEventListener("click", function(e) {
        e.preventDefault();
        toggleSidePanel();
    });

    // Add touch listeners for both edge swipe area AND side panel for dragging
    edgeSwipeArea.addEventListener('touchstart', handleTouchStart, { passive: false });
    sidePanel.addEventListener('touchstart', handleTouchStart, { passive: false });
    
    document.addEventListener('touchmove', handleTouchMove, { passive: false });
    document.addEventListener('touchend', handleTouchEnd, { passive: false });
    document.addEventListener('touchcancel', handleTouchEnd, { passive: false });

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
    setMode('code');
    updateLineNumbers();
    updateCurrentLineHighlight();
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
    updateCurrentLineHighlight();
}, 500);
