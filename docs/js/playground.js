(function() {
    'use strict';

    const replHistory = [];
    let historyIndex = -1;
    let playgroundInitialized = false;

    // ── Playground init ───────────────────────────────────────────────────

    function initPlayground() {
        if (playgroundInitialized) return;
        playgroundInitialized = true;

        var input = document.getElementById('repl-input');
        if (!input) return;

        input.addEventListener('keydown', handleKeyDown);
        document.getElementById('clearBtn').addEventListener('click', clearConsole);
        document.getElementById('resetBtn').addEventListener('click', resetSession);

        // The JS interpreter is already loaded synchronously — no waiting needed.
        var status = document.getElementById('apiStatus');
        if (status) status.textContent = '✅ Ready';

        input.disabled = false;
        input.focus();

        appendOutput('Coderive Playground ready. Type code and press Enter.', 'output');
        appendOutput('Commands: ;help  ;reset', 'output');
    }

    // ── REPL logic ────────────────────────────────────────────────────────

    function submitLine(line) {
        if (!line.trim()) return;

        replHistory.push(line);
        historyIndex = replHistory.length;

        appendOutput('>> ' + line, 'input');

        var input = document.getElementById('repl-input');
        input.value = '';

        try {
            var result = CodREPLRunner.eval(line);
            var resultStr = (result !== null && result !== undefined) ? String(result) : '';
            if (resultStr) {
                resultStr.split('\n').forEach(function(l) {
                    appendOutput(l, 'output');
                });
            }
        } catch (e) {
            appendOutput('Error: ' + (e && e.message ? e.message : String(e)), 'error');
        }

        input.focus();
    }

    function handleKeyDown(e) {
        if (e.key === 'Enter') {
            submitLine(e.target.value);
        } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            if (historyIndex > 0) {
                historyIndex--;
                e.target.value = replHistory[historyIndex];
            }
        } else if (e.key === 'ArrowDown') {
            e.preventDefault();
            if (historyIndex < replHistory.length - 1) {
                historyIndex++;
                e.target.value = replHistory[historyIndex];
            } else {
                historyIndex = replHistory.length;
                e.target.value = '';
            }
        }
    }

    function appendOutput(text, type) {
        var output = document.getElementById('repl-output');
        if (!output) return;
        var line = document.createElement('div');
        line.className = 'repl-line ' + type;
        line.textContent = text;
        output.appendChild(line);
        output.scrollTop = output.scrollHeight;
    }

    function clearConsole() {
        var output = document.getElementById('repl-output');
        if (output) output.innerHTML = '';
    }

    function resetSession() {
        clearConsole();
        try {
            CodREPLRunner.reset();
            appendOutput('State reset.', 'output');
        } catch (e) {
            appendOutput('Warning: could not reset state - ' +
                (e && e.message ? e.message : String(e)), 'error');
        }
    }

    document.addEventListener('partialsLoaded', initPlayground);
    if (window.Coderive) {
        Coderive.on('partialsLoaded', initPlayground);
        Coderive.whenReady(function() {
            if (document.getElementById('repl-input') && !playgroundInitialized) {
                initPlayground();
            }
        });
    }
})();
