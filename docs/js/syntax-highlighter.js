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

    function render(input) {
        var src = input == null ? '' : String(input);
        var tokenize = global.CoderiveLanguage && global.CoderiveLanguage.tokenize;
        if (!tokenize) return escapeHtml(src);

        try {
            var tokens = tokenize(src) || [];
            var html = '';
            var cursor = 0;

            for (var i = 0; i < tokens.length; i++) {
                var token = tokens[i];
                if (!token || token.type === 'EOF') continue;

                var raw = token.text == null ? '' : String(token.text);
                if (!raw.length) continue;

                var foundAt = src.indexOf(raw, cursor);
                if (foundAt === -1) continue;

                if (foundAt > cursor) {
                    html += escapeHtml(src.slice(cursor, foundAt));
                }

                var kind = kindForToken(token);
                html += '<span class="syn-' + kind + '">' + escapeHtml(raw) + '</span>';
                cursor = foundAt + raw.length;
            }

            if (cursor < src.length) {
                html += escapeHtml(src.slice(cursor));
            }
            return html;
        } catch (e) {
            return escapeHtml(src);
        }
    }

    global.CoderiveSyntaxHighlighter = {
        render: render
    };
})(typeof window !== 'undefined' ? window : this);
