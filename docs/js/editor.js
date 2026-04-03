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
        var runBtn = document.getElementById('editorRunBtn');
        var clearBtn = document.getElementById('editorClearBtn');
        var resetBtn = document.getElementById('editorResetBtn');

        if (!input || !output || !runBtn || !clearBtn || !resetBtn) return;

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
            setStatus('⏳ Running...');
            runJs(src);
        }

        function runJs(src) {
            if (!window.CodREPLRunner || typeof CodREPLRunner.compileAndRun !== 'function') {
                output.textContent = 'JS runtime unavailable.';
                setStatus('❌ JS runtime unavailable');
                return;
            }
            var result = CodREPLRunner.compileAndRun(src, { reset: false });
            renderOutput(result || '');
            setStatus('✅ Ran with JavaScript runtime');
        }
    }

    function updateHighlight() {
        var input = document.getElementById('editorInput');
        var layer = document.getElementById('editorHighlight');
        if (!input || !layer) return;
        if (window.CoderiveSyntaxHighlighter && typeof CoderiveSyntaxHighlighter.renderTo === 'function') {
            CoderiveSyntaxHighlighter.renderTo(layer, input.value || '', true);
        } else {
            layer.textContent = input.value || '';
        }
        syncHighlightScroll();
    }

    function renderOutput(text) {
        var output = document.getElementById('editorOutput');
        if (!output) return;
        if (window.CoderiveSyntaxHighlighter && typeof CoderiveSyntaxHighlighter.renderTo === 'function') {
            CoderiveSyntaxHighlighter.renderTo(output, text || '', false);
        } else {
            output.textContent = text || '';
        }
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
