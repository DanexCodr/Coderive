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

    function classifyIdentifier(sourceText, token, tokenStart, tokenEnd) {
        if (!token || token.type !== 'ID') return null;
        var text = token.text === null || token.text === undefined ? '' : String(token.text);
        if (!text) return null;

        var prev = sourceText.slice(0, tokenStart);
        if (/(^|[\s{};])(?:share|local|builtin)\s+$/.test(prev)) {
            var after = sourceText.slice(tokenEnd);
            if (/^\s*\{/.test(after)) return 'class';
        }
        return null;
    }

    function findTokenStart(sourceText, token, cursor) {
        var raw = token.text === null || token.text === undefined ? '' : String(token.text);
        if (!raw.length) return -1;

        var i = cursor;
        while (i <= sourceText.length - raw.length) {
            if (sourceText.slice(i, i + raw.length) !== raw) {
                i++;
                continue;
            }
            if (token.type === 'ID') {
                var prev = i > 0 ? sourceText.charAt(i - 1) : '';
                var next = i + raw.length < sourceText.length ? sourceText.charAt(i + raw.length) : '';
                if (/[\w]/.test(prev) || /[\w]/.test(next)) {
                    i++;
                    continue;
                }
            }
            return i;
        }
        return -1;
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

                var foundAt = findTokenStart(sourceText, token, cursor);
                if (foundAt === -1) continue;

                if (foundAt > cursor) {
                    segments.push({ kind: 'plain', text: sourceText.slice(cursor, foundAt) });
                }

                var kind = kindForToken(token);
                if (kind === 'id') {
                    var refined = classifyIdentifier(sourceText, token, foundAt, foundAt + raw.length);
                    if (refined) kind = refined;
                }
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
        renderTo: renderTo,
        registerTheme: function(theme) {
            var styleId = 'coderive-syntax-theme';
            var styleEl = document.getElementById(styleId);
            if (!styleEl) {
                styleEl = document.createElement('style');
                styleEl.id = styleId;
                document.head.appendChild(styleEl);
            }
            var css = '';
            if (theme && typeof theme === 'object') {
                Object.keys(theme).forEach(function(tokenKind) {
                    var color = theme[tokenKind];
                    if (!color) return;
                    css += '.syn-' + tokenKind + ' { color: ' + color + '; }\n';
                });
            }
            styleEl.textContent = css;
            return true;
        }
    };
})(typeof window !== 'undefined' ? window : this);
