// js/core.js - Complete with no dependencies
window.Coderive = (function() {
    'use strict';
    
    console.log('Core: initializing...');
    
    // Event system
    const events = {};
    
    function on(event, callback) {
        if (!events[event]) events[event] = [];
        events[event].push(callback);
        console.log(`Core: registered listener for '${event}'`);
    }
    
    function emit(event, data) {
        console.log(`Core: emitting '${event}'`, data);
        
        // Call JS listeners
        if (events[event]) {
            events[event].forEach(cb => {
                try {
                    cb(data);
                } catch (e) {
                    console.error(`Core: error in ${event} listener:`, e);
                }
            });
        }
        
        // Dispatch DOM event
        document.dispatchEvent(new CustomEvent(`coderive:${event}`, { detail: data }));
    }
    
    // DOM Ready utilities
    function whenReady(callback) {
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => {
                console.log('Core: DOM ready, calling callback');
                callback();
            });
        } else {
            console.log('Core: DOM already ready, calling callback');
            callback();
        }
    }
    
    // Wait for element
    function waitForElement(selector, callback, maxAttempts = 20) {
        let attempts = 0;
        
        function check() {
            const element = document.querySelector(selector);
            if (element) {
                console.log(`Core: found element ${selector}`);
                callback(element);
                return true;
            }
            
            attempts++;
            if (attempts < maxAttempts) {
                setTimeout(check, 100);
            } else {
                console.warn(`Core: element ${selector} not found after ${maxAttempts} attempts`);
            }
            return false;
        }
        
        check();
    }
    
    // Script loader
    function loadScript(src) {
        return new Promise((resolve, reject) => {
            const script = document.createElement('script');
            script.src = src;
            script.onload = resolve;
            script.onerror = reject;
            document.body.appendChild(script);
        });
    }
    
    // Year updater
    function updateYear(selector = '.currentYear') {
        document.querySelectorAll(selector).forEach(span => {
            span.textContent = new Date().getFullYear();
        });
    }
    
    // Error handler
    function handleError(error, context = '') {
        const message = context ? `${context}: ${error.message}` : error.message;
        console.error(`❌ Core error: ${message}`, error);
        // Could show user-friendly message
    }

    // Version fetcher — reads the VERSION file from GitHub and updates all
    // elements with the class "coderive-version".
    async function fetchVersion() {
        const versionUrl = 'https://raw.githubusercontent.com/DanexCodr/Coderive/main/VERSION';
        try {
            const response = await fetch(versionUrl);
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            const version = (await response.text()).trim();
            console.log(`Core: fetched version ${version}`);
            document.querySelectorAll('.coderive-version').forEach(function(el) {
                el.textContent = 'v' + version;
            });
            return version;
        } catch (error) {
            console.warn('Core: failed to fetch VERSION file:', error);
            return null;
        }
    }
    
    console.log('✅ Core initialized');
    
    // Public API
    return {
        on,
        emit,
        whenReady,
        waitForElement,
        loadScript,
        updateYear,
        handleError,
        fetchVersion
    };
})();

// Notify that core is ready
document.dispatchEvent(new Event('coreReady'));