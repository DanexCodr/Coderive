// js/docs.js - Using core
(function() {
    function initDocs() {
        const backToTop = document.querySelector('.back-to-top');
        if (backToTop) {
            backToTop.addEventListener('click', (e) => {
                e.preventDefault();
                window.scrollTo({ top: 0, behavior: 'smooth' });
                Coderive.emit('backToTopClicked');
            });
        }

        const input = document.getElementById('docsSearchInput');
        const meta = document.getElementById('docsSearchMeta');
        const sections = Array.from(document.querySelectorAll('#docsPage .docs-section.searchable-content'));
        if (!input || sections.length === 0) return;

        const updateResults = () => {
            const query = input.value.trim().toLowerCase();
            let visibleCount = 0;

            sections.forEach((section) => {
                const text = (section.textContent || '').toLowerCase();
                const matched = query === '' || text.includes(query);
                section.classList.toggle('hidden-by-search', !matched);
                section.classList.toggle('search-hit', matched && query !== '');
                if (matched) visibleCount++;
            });

            if (meta) {
                meta.textContent = query === ''
                    ? 'Showing all sections'
                    : `Found ${visibleCount} matching section${visibleCount === 1 ? '' : 's'}`;
            }
        };

        input.addEventListener('input', updateResults);
        updateResults();
    }
    
    Coderive.on('partialsLoaded', initDocs);
    Coderive.whenReady(initDocs);
})();
