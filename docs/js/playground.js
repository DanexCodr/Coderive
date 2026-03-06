(function() {
    'use strict';

    const API_URL = 'https://coderive-api.onrender.com/eval';
    const API_BASE = 'https://coderive-api.onrender.com';
    let history = [];
    let historyIndex = -1;
    let isPending = false;
    let cheerpjReady = false;

    function loadCheerpJ() {
        return new Promise(function(resolve) {
            if (typeof cheerpjInit === 'function') {
                resolve(true);
                return;
            }
            var script = document.createElement('script');
            script.src = 'https://cjrtnc.leaningtech.com/3.0/cj3loader.js';
            script.onload = function() { resolve(true); };
            script.onerror = function() { resolve(false); };
            document.head.appendChild(script);
        });
    }

    function initPlayground() {
        var input = document.getElementById('repl-input');
        input.addEventListener('keydown', handleKeyDown);
        input.focus();

        document.getElementById('clearBtn').addEventListener('click', clearConsole);
        document.getElementById('resetBtn').addEventListener('click', resetSession);

        startConnection();
    }

    async function startConnection() {
        var status = document.getElementById('apiStatus');
        status.textContent = '⏳ Loading...';
        try {
            var loaded = await loadCheerpJ();
            if (!loaded) throw new Error('CheerpJ script failed to load');
            if (!cheerpjReady) {
                await cheerpjInit({ classpath: ['app:///assets/Coderive.jar'] });
                cheerpjReady = true;
            }
            status.textContent = '✅ Ready';
        } catch (e) {
            console.warn('CheerpJ unavailable, trying remote API:', e);
            try {
                var response = await fetch(API_BASE, { method: 'GET' });
                status.textContent = response.ok ? '✅ Connected' : '❌ Offline';
            } catch (e2) {
                status.textContent = '❌ Offline';
            }
        }
    }

    async function evalCode(line) {
        if (cheerpjReady) {
            var javaResult = await cjCall('cod/runner/REPLRunner', 'eval', line);
            return cjStringJavaToJs(javaResult);
        }
        var response = await fetch(API_URL, {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: new URLSearchParams({ code: line })
        });
        return response.text();
    }

    async function submitLine(line) {
        if (!line.trim() || isPending) return;

        history.push(line);
        historyIndex = history.length;

        appendOutput('>> ' + line, 'input');

        var input = document.getElementById('repl-input');
        input.value = '';
        input.disabled = true;
        isPending = true;

        var loadingLine = appendLoadingLine();

        try {
            var result = await evalCode(line);
            removeLoadingLine(loadingLine);
            if (result) {
                result.split('\n').forEach(function(l) {
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
        var output = document.getElementById('repl-output');
        var line = document.createElement('div');
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
        var output = document.getElementById('repl-output');
        var line = document.createElement('div');
        line.className = 'repl-line ' + type;
        line.textContent = text;
        output.appendChild(line);
        output.scrollTop = output.scrollHeight;
    }

    function clearConsole() {
        document.getElementById('repl-output').innerHTML = '';
    }

    async function resetSession() {
        clearConsole();
        if (cheerpjReady) {
            try {
                await cjCall('cod/runner/REPLRunner', 'reset');
                document.getElementById('apiStatus').textContent = '✅ Ready';
            } catch (e) {
                console.warn('Reset failed:', e);
            }
        } else {
            startConnection();
        }
    }

    var playgroundInitialized = false;

    function safeInitPlayground() {
        if (playgroundInitialized) return;
        var input = document.getElementById('repl-input');
        if (!input) return; // partials not yet in DOM
        playgroundInitialized = true;
        initPlayground();
    }

    Coderive.on('partialsLoaded', safeInitPlayground);
    Coderive.whenReady(safeInitPlayground);
})();
