(function() {
    'use strict';
    
    const API_URL = 'https://coderive-api.onrender.com/eval';
    const API_BASE = 'https://coderive-api.onrender.com';
    let history = [];
    let historyIndex = -1;
    let isPending = false;
    
    function initPlayground() {
        const input = document.getElementById('repl-input');
        input.addEventListener('keydown', handleKeyDown);
        input.focus();
        
        document.getElementById('clearBtn').addEventListener('click', clearConsole);
        document.getElementById('resetBtn').addEventListener('click', resetSession);
        
        checkConnection();
    }
    
    async function checkConnection() {
        const status = document.getElementById('apiStatus');
        status.textContent = '⏳ Connecting...';
        try {
            const response = await fetch(API_BASE, { method: 'GET' });
            if (response.ok) {
                status.textContent = '✅ Connected';
            } else {
                status.textContent = '❌ Offline';
            }
        } catch (e) {
            status.textContent = '❌ Offline';
        }
    }
    
    async function submitLine(line) {
        if (!line.trim() || isPending) return;
        
        history.push(line);
        historyIndex = history.length;
        
        appendOutput('>> ' + line, 'input');
        
        const input = document.getElementById('repl-input');
        input.value = '';
        input.disabled = true;
        isPending = true;
        
        const loadingLine = appendLoadingLine();
        
        try {
            const response = await fetch(API_URL, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: new URLSearchParams({code: line})
            });
            
            removeLoadingLine(loadingLine);
            
            const result = await response.text();
            if (result) {
                result.split('\n').forEach(l => {
                    if (l.trim()) appendOutput(l, 'output');
                });
            }
        } catch (e) {
            removeLoadingLine(loadingLine);
            appendOutput('Error: ' + e.message, 'error');
        } finally {
            isPending = false;
            input.disabled = false;
            input.focus();
        }
    }
    
    function appendLoadingLine() {
        const output = document.getElementById('repl-output');
        const line = document.createElement('div');
        line.className = 'repl-line output repl-loading';
        line.textContent = '...';
        output.appendChild(line);
        output.scrollTop = output.scrollHeight;
        return line;
    }
    
    function removeLoadingLine(line) {
        if (line && line.parentNode) {
            line.parentNode.removeChild(line);
        }
    }
    
    function handleKeyDown(e) {
        if (e.key === 'Enter') {
            submitLine(e.target.value);
        } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            if (historyIndex > 0) {
                historyIndex--;
                e.target.value = history[historyIndex];
            }
        } else if (e.key === 'ArrowDown') {
            e.preventDefault();
            if (historyIndex < history.length - 1) {
                historyIndex++;
                e.target.value = history[historyIndex];
            } else {
                historyIndex = history.length;
                e.target.value = '';
            }
        }
    }
    
    function appendOutput(text, type) {
        const output = document.getElementById('repl-output');
        const line = document.createElement('div');
        line.className = 'repl-line ' + type;
        line.textContent = text;
        output.appendChild(line);
        output.scrollTop = output.scrollHeight;
    }
    
    function clearConsole() {
        document.getElementById('repl-output').innerHTML = '';
    }
    
    function resetSession() {
        clearConsole();
        checkConnection();
    }
    
    document.addEventListener('partialsLoaded', initPlayground);
})();
