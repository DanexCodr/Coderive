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
    }
    
    Coderive.on('partialsLoaded', initDocs);
    Coderive.whenReady(initDocs);
})();