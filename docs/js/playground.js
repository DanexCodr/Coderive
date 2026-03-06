(function() {
    'use strict';

    let jvmReady = false;
    let REPLRunnerClass = null;
    const replHistory = [];
    let historyIndex = -1;
    let isPending = false;
    let playgroundInitialized = false;

    // ── Pre-load CheerpJ as early as possible ────────────────────────────
    // The download starts immediately when this module is parsed, in parallel
    // with anything else the page is doing. initJVM() just awaits the promise.
    let cheerpJLoadPromise = new Promise(function(resolve, reject) {
        var script = document.createElement('script');
        script.src = 'https://cjrtnc.leaningtech.com/3.0/cj3loader.js';
        script.onload = resolve;
        script.onerror = function() {
            reject(new Error('Could not load CheerpJ runtime. Check your internet connection.'));
        };
        document.head.appendChild(script);
    });

    // ── Overlay helpers ───────────────────────────────────────────────────

    function setStep(n, state) {
        var el    = document.getElementById('jvm-step-' + n);
        var check = document.getElementById('jvm-step-' + n + '-check');
        if (!el) return;
        el.className = 'jvm-step ' + state;
        if (state === 'done')   { check.textContent = '✓'; }
        else if (state === 'active') { check.textContent = '…'; }
        else if (state === 'error')  { check.textContent = '✗'; }
        else                         { check.textContent = ''; }
    }

    function setProgress(done) {
        var fill = document.getElementById('jvm-progress-fill');
        var lbl  = document.getElementById('jvm-progress-label');
        var bar  = document.getElementById('jvm-progressbar');
        if (fill) fill.style.width = ((done / 4) * 100) + '%';
        if (lbl)  lbl.textContent  = done + ' / 4';
        if (bar)  bar.setAttribute('aria-valuenow', done);
    }

    function hideOverlay() {
        var overlay = document.getElementById('jvm-overlay');
        if (!overlay) return;
        overlay.classList.add('jvm-overlay-out');
        // remove from layout after transition
        overlay.addEventListener('transitionend', function onEnd() {
            overlay.removeEventListener('transitionend', onEnd);
            overlay.style.display = 'none';
        });
    }

    function showOverlayError(msg) {
        var el = document.getElementById('jvm-overlay-error');
        if (el) {
            el.textContent = msg;
            el.style.display = 'block';
        }
    }

    function markActiveStepError() {
        for (var i = 1; i <= 4; i++) {
            var el = document.getElementById('jvm-step-' + i);
            if (el && el.classList.contains('active')) {
                setStep(i, 'error');
                return;
            }
        }
    }

    // ── Playground init ───────────────────────────────────────────────────

    function initPlayground() {
        if (playgroundInitialized) return;
        playgroundInitialized = true;

        var input = document.getElementById('repl-input');
        input.addEventListener('keydown', handleKeyDown);
        document.getElementById('clearBtn').addEventListener('click', clearConsole);
        document.getElementById('resetBtn').addEventListener('click', resetSession);

        // Step 1 is already done (partials loaded before we got here)
        setStep(1, 'done');
        setStep(2, 'active');  // CheerpJ download started at module parse time
        setProgress(1);

        initJVM();
    }

    async function initJVM() {
        var status = document.getElementById('apiStatus');
        try {
            var pathname = window.location.pathname;
            var dirPath  = pathname.endsWith('/')
                ? pathname
                : pathname.substring(0, pathname.lastIndexOf('/') + 1);
            var jarPath  = '/app' + dirPath + 'assets/Coderive.jar';

            // Step 2 — wait for CheerpJ script (pre-loading already in flight)
            try {
                await cheerpJLoadPromise;
            } catch (e) {
                throw new Error('Failed to download CheerpJ runtime: ' +
                    (e && e.message ? e.message : String(e)));
            }
            setStep(2, 'done');
            setProgress(2);

            // Step 3 — initialise CheerpJ / JVM
            setStep(3, 'active');
            try {
                await cheerpjInit();
            } catch (e) {
                throw new Error('Failed to initialise CheerpJ: ' +
                    (e && e.message ? e.message : String(e)));
            }
            setStep(3, 'done');
            setProgress(3);

            // Step 4 — load Coderive.jar and obtain the runner class
            setStep(4, 'active');
            var lib;
            try {
                lib = await cheerpjRunLibrary(jarPath);
            } catch (e) {
                throw new Error('Failed to load Coderive.jar (' + jarPath + '): ' +
                    (e && e.message ? e.message : String(e)));
            }
            REPLRunnerClass = await lib.cod.runner.REPLRunner;
            setStep(4, 'done');
            setProgress(4);

            jvmReady = true;
            if (status) status.textContent = '✅ Ready';

            // Brief pause so the user can see "4 / 4" before the overlay fades
            await new Promise(function(r) { setTimeout(r, 350); });
            hideOverlay();

            var input = document.getElementById('repl-input');
            input.disabled = false;
            input.focus();

            appendOutput('Coderive Playground ready. Type code and press Enter.', 'output');
            appendOutput('Commands: ;help  ;reset', 'output');

        } catch (e) {
            if (status) status.textContent = '❌ Error';
            markActiveStepError();
            showOverlayError('Error: ' + (e && e.message ? e.message : String(e)));
            appendOutput('Error initialising JVM: ' + (e && e.message ? e.message : String(e)), 'error');
        }
    }

    // ── REPL logic ────────────────────────────────────────────────────────

    async function submitLine(line) {
        if (!line.trim() || isPending || !jvmReady) return;

        replHistory.push(line);
        historyIndex = replHistory.length;

        appendOutput('>> ' + line, 'input');

        var input = document.getElementById('repl-input');
        input.value = '';
        input.disabled = true;
        isPending = true;

        var loadingLine = appendLoadingLine();

        try {
            var result = await REPLRunnerClass.eval(line);
            removeLoadingLine(loadingLine);

            var resultStr = (result !== null && result !== undefined) ? String(result) : '';
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
        if (jvmReady && REPLRunnerClass) {
            try {
                await REPLRunnerClass.reset();
                appendOutput('State reset.', 'output');
            } catch (e) {
                appendOutput('Warning: could not reset state - ' +
                    (e && e.message ? e.message : String(e)), 'error');
            }
        }
    }

    document.addEventListener('partialsLoaded', initPlayground);
    if (window.Coderive) {
        Coderive.on('partialsLoaded', initPlayground);
        Coderive.whenReady(function() {
            // Partials may have loaded before this module; initialize immediately if so.
            if (document.getElementById('jvm-overlay') && !playgroundInitialized) {
                initPlayground();
            }
        });
    }
})();
