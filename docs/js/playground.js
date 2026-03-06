(function() {
    'use strict';
    
    const API_URL = 'https://coderive-api.onrender.com/eval';
    let history = [];
    let historyIndex = -1;
    
    function initPlayground() {
        const status = document.getElementById('apiStatus');
        status.textContent = '✅ Connected';
        
        const input = document.getElementById('repl-input');
        input.addEventListener('keydown', handleKeyDown);
        input.focus();
        
        document.getElementById('clearBtn').addEventListener('click', clearConsole);
        document.getElementById('resetBtn').addEventListener('click', resetSession);
    }
    
    async function submitLine(line) {
        if (!line.trim()) return;
        
        history.push(line);
        historyIndex = history.length;
        
        appendOutput('>> ' + line, 'input');
        document.getElementById('repl-input').value = '';
        
        try {
            const response = await fetch(API_URL, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: new URLSearchParams({code: line})
            });
            
            const result = await response.text();
            if (result) {
                result.split('\n').forEach(l => {
                    if (l.trim()) appendOutput(l, 'output');
                });
            }
        } catch (e) {
            appendOutput('Error: ' + e.message, 'error');
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
    }
    
    document.addEventListener('partialsLoaded', initPlayground);
})();
