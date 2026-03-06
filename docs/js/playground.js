(function() {
    'use strict';

    let jvmReady = false;
    let REPLRunnerClass = null;
    const history = [];
    let historyIndex = -1;
    let isPending = false;

    function initPlayground() {
        const input = document.getElementById('repl-input');
        input.addEventListener('keydown', handleKeyDown);

        document.getElementById('clearBtn').addEventListener('click', clearConsole);
        document.getElementById('resetBtn').addEventListener('click', resetSession);

        loadCheerpJ();
    }

    function loadCheerpJ() {
        const status = document.getElementById('apiStatus');
        status.textContent = '⏳ Loading JVM...';

        const input = document.getElementById('repl-input');
        input.disabled = true;

        // Dynamically load CheerpJ from the CDN
        const script = document.createElement('script');
        script.src = 'https://cjrtnc.leaningtech.com/3.0/cj3loader.js';
        script.onload = initJVM;
        script.onerror = function() {
            const status = document.getElementById('apiStatus');
            status.textContent = '❌ Failed';
            appendOutput('Error: Could not load CheerpJ runtime. Check your internet connection.', 'error');
        };
        document.head.appendChild(script);
    }

    async function initJVM() {
        const status = document.getElementById('apiStatus');
        try {
            // Build the JAR path under CheerpJ's /app virtual filesystem.
            // /app maps to the origin root, so we include the page's path prefix.
            const pathname = window.location.pathname;
            const dirPath = pathname.endsWith('/')
                ? pathname
                : pathname.substring(0, pathname.lastIndexOf('/') + 1);
            const jarPath = '/app' + dirPath + 'assets/Coderive.jar';

            try {
                await cheerpjInit();
            } catch (e) {
                throw new Error('Failed to initialise CheerpJ runtime: ' + (e && e.message ? e.message : String(e)));
            }

            let lib;
            try {
                lib = await cheerpjRunLibrary(jarPath);
            } catch (e) {
                throw new Error('Failed to load Coderive.jar (' + jarPath + '): ' + (e && e.message ? e.message : String(e)));
            }

            REPLRunnerClass = await lib.cod.runner.REPLRunner;

            jvmReady = true;
            status.textContent = '✅ Ready';

            const input = document.getElementById('repl-input');
            input.disabled = false;
            input.focus();

            appendOutput('Coderive Playground ready. Type code and press Enter.', 'output');
            appendOutput('Commands: ;help  ;reset', 'output');
        } catch (e) {
            status.textContent = '❌ Error';
            appendOutput('Error initialising JVM: ' + (e && e.message ? e.message : String(e)), 'error');
        }
    }

    async function submitLine(line) {
        if (!line.trim() || isPending || !jvmReady) return;

        history.push(line);
        historyIndex = history.length;

        appendOutput('>> ' + line, 'input');

        const input = document.getElementById('repl-input');
        input.value = '';
        input.disabled = true;
        isPending = true;

        const loadingLine = appendLoadingLine();

        try {
            const result = await REPLRunnerClass.eval(line);
            removeLoadingLine(loadingLine);

            const resultStr = (result !== null && result !== undefined) ? String(result) : '';
            if (resultStr) {
                resultStr.split('\n').forEach(function(l) {
                    if (l.trim()) appendOutput(l, 'output');
                });
            }
        } catch (e) {
            removeLoadingLine(loadingLine);
            appendOutput('Error: ' + (e && e.message ? e.message : String(e)), 'error');
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

    async function resetSession() {
        clearConsole();
        if (jvmReady && REPLRunnerClass) {
            try {
                await REPLRunnerClass.reset();
                appendOutput('State reset.', 'output');
            } catch (e) {
                appendOutput('Warning: could not reset state - ' + (e && e.message ? e.message : String(e)), 'error');
            }
        }
    }

    document.addEventListener('partialsLoaded', initPlayground);
})();
