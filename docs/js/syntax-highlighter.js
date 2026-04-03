(function(global) {
    'use strict';

    function escapeHtml(text) {
        return String(text)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;');
    }

    function kindForToken(token) {
        if (!token) return 'plain';
        switch (token.type) {
            case 'KEYWORD': return 'kw';
            case 'INT_LIT':
            case 'FLOAT_LIT': return 'num';
            case 'TEXT_LIT':
            case 'INTERPOL': return 'str';
            case 'BOOL_LIT': return 'bool';
            case 'ID': return 'id';
            case 'SYMBOL': return 'sym';
            case 'INVALID': return 'err';
            default: return 'plain';
        }
    }

    function toSegments(input) {
        var sourceText = input === null || input === undefined ? '' : String(input);
        var tokenize = global.CoderiveLanguage && global.CoderiveLanguage.tokenize;
        if (!tokenize) return [{ kind: 'plain', text: sourceText }];

        try {
            var tokens = tokenize(sourceText) || [];
            var segments = [];
            var cursor = 0;

            for (var i = 0; i < tokens.length; i++) {
                var token = tokens[i];
                if (!token || token.type === 'EOF') continue;

                var raw = token.text === null || token.text === undefined ? '' : String(token.text);
                if (!raw.length) continue;

                var foundAt = sourceText.indexOf(raw, cursor);
                if (foundAt === -1) continue;

                if (foundAt > cursor) {
                    segments.push({ kind: 'plain', text: sourceText.slice(cursor, foundAt) });
                }

                var kind = kindForToken(token);
                segments.push({ kind: kind, text: raw });
                cursor = foundAt + raw.length;
            }

            if (cursor < sourceText.length) {
                segments.push({ kind: 'plain', text: sourceText.slice(cursor) });
            }
            return segments;
        } catch (e) {
            return [{ kind: 'plain', text: sourceText }];
        }
    }

    function render(input) {
        var segments = toSegments(input);
        var html = '';
        for (var i = 0; i < segments.length; i++) {
            var seg = segments[i];
            if (seg.kind === 'plain') {
                html += escapeHtml(seg.text);
            } else {
                html += '<span class="syn-' + seg.kind + '">' + escapeHtml(seg.text) + '</span>';
            }
        }
        return html;
    }

    function renderTo(element, input, addCaretSpace) {
        if (!element) return;
        var segments = toSegments(input);
        element.textContent = '';
        for (var i = 0; i < segments.length; i++) {
            var seg = segments[i];
            if (seg.kind === 'plain') {
                element.appendChild(document.createTextNode(seg.text));
            } else {
                var span = document.createElement('span');
                span.className = 'syn-' + seg.kind;
                span.textContent = seg.text;
                element.appendChild(span);
            }
        }
        if (addCaretSpace) {
            var caret = document.createElement('span');
            caret.className = 'syn-caret-space';
            caret.textContent = ' ';
            element.appendChild(caret);
        }
    }

    global.CoderiveSyntaxHighlighter = {
        render: render,
        renderTo: renderTo
    };
})(typeof window !== 'undefined' ? window : this);
