(function() {
    'use strict';

    var initialized = false;
    var DEFAULT_SOURCE = [
        'unit editor_demo',
        '',
        'share main() {',
        '    numbers := [1 to 5]',
        '    sum := 0',
        '    for n of numbers {',
        '        sum = sum + n',
        '    }',
        '    out("sum = {sum}")',
        '}'
    ].join('\n');

    function initEditor() {
        if (initialized) return;
        initialized = true;

        var input = document.getElementById('editorInput');
        var output = document.getElementById('editorOutput');
        var runtime = document.getElementById('editorRuntime');
        var runBtn = document.getElementById('editorRunBtn');
        var clearBtn = document.getElementById('editorClearBtn');
        var resetBtn = document.getElementById('editorResetBtn');

        if (!input || !output || !runtime || !runBtn || !clearBtn || !resetBtn) return;

        input.value = DEFAULT_SOURCE;
        updateHighlight();

        runBtn.addEventListener('click', runProgram);
        clearBtn.addEventListener('click', function() { output.textContent = ''; });
        resetBtn.addEventListener('click', function() {
            if (window.CodREPLRunner) {
                CodREPLRunner.reset();
                setStatus('✅ JS state reset');
            }
        });

        input.addEventListener('input', updateHighlight);
        input.addEventListener('scroll', syncHighlightScroll);

        function runProgram() {
            var src = input.value || '';
            var selected = runtime.value;
            setStatus('⏳ Running...');

            if (selected === 'java7') {
                runJava7(src);
            } else {
                runJs(src);
            }
        }

        function runJs(src) {
            if (!window.CodREPLRunner || typeof CodREPLRunner.compileAndRun !== 'function') {
                output.textContent = 'JS runtime unavailable.';
                setStatus('❌ JS runtime unavailable');
                return;
            }
            var result = CodREPLRunner.compileAndRun(src, { reset: false });
            output.innerHTML = window.CoderiveSyntaxHighlighter
                ? CoderiveSyntaxHighlighter.render(result || '')
                : String(result || '');
            setStatus('✅ Ran with JavaScript runtime');
        }

        function runJava7(src) {
            fetch('https://coderive-repl.onrender.com/eval', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
                body: new URLSearchParams({ code: src }).toString()
            }).then(function(res) {
                if (!res.ok) throw new Error('HTTP ' + res.status);
                return res.text();
            }).then(function(text) {
                output.innerHTML = window.CoderiveSyntaxHighlighter
                    ? CoderiveSyntaxHighlighter.render(text || '')
                    : String(text || '');
                setStatus('✅ Ran with Java 7 server runtime');
            }).catch(function() {
                var fallback = '';
                if (window.CodREPLRunner && typeof CodREPLRunner.compileAndRun === 'function') {
                    fallback = CodREPLRunner.compileAndRun(src, { reset: false });
                }
                output.innerHTML = window.CoderiveSyntaxHighlighter
                    ? CoderiveSyntaxHighlighter.render(fallback || '')
                    : String(fallback || '');
                setStatus('⚠ Java 7 server unavailable — used JS fallback');
            });
        }
    }

    function updateHighlight() {
        var input = document.getElementById('editorInput');
        var layer = document.getElementById('editorHighlight');
        if (!input || !layer) return;
        layer.innerHTML = window.CoderiveSyntaxHighlighter
            ? CoderiveSyntaxHighlighter.render(input.value || '') + '<span class="syn-caret-space"> </span>'
            : (input.value || '');
        syncHighlightScroll();
    }

    function syncHighlightScroll() {
        var input = document.getElementById('editorInput');
        var layer = document.getElementById('editorHighlight');
        if (!input || !layer) return;
        layer.scrollTop = input.scrollTop;
        layer.scrollLeft = input.scrollLeft;
    }

    function setStatus(text) {
        var status = document.getElementById('editorStatus');
        if (status) status.textContent = text;
    }

    document.addEventListener('partialsLoaded', initEditor);
    if (window.Coderive) {
        Coderive.on('partialsLoaded', initEditor);
        Coderive.whenReady(function() {
            if (document.getElementById('editorInput') && !initialized) {
                initEditor();
            }
        });
    }
})();
