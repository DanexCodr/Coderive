// js/router.js - Complete with fixed back button handling and playground route
(function() {
    'use strict';
    
    const routes = {
        '#home': { id: 'homePage', title: 'Coderive · Modern language for Java 7' },
        '#docs': { id: 'docsPage', title: 'Coderive · Documentation' },
        '#playground': { id: 'playgroundPage', title: 'Coderive · Playground' },
        '#editor': { id: 'editorPage', title: 'Coderive · Editor' }
    };
    
    let currentPage = null;
    let isNavigating = false;
    
    function closeDrawer() {
        document.body.classList.remove('drawer-open');
    }
    
    function navigateTo(hash, updateHistory = true) {
        if (isNavigating) return;
        isNavigating = true;
        
        if (hash === '#' || hash === '') {
            window.scrollTo({ top: 0, behavior: 'smooth' });
            isNavigating = false;
            return;
        }
        
        const route = routes[hash] || routes['#home'];
        
        if (currentPage === hash) {
            isNavigating = false;
            return;
        }
        
        document.querySelectorAll('.page').forEach(page => {
            page.classList.remove('active');
        });
        
        const targetPage = document.getElementById(route.id);
        if (targetPage) {
            targetPage.classList.add('active');
            const previousPage = currentPage;
            currentPage = hash;
            document.title = route.title;
            window.scrollTo(0, 0);
            
            setTimeout(() => {
                closeDrawer();
            }, 50);
            
            if (updateHistory) {
                if (hash === '#home' && previousPage === '#docs') {
                    console.log('🔵 Router: DOCS → HOME - replacing entire history');
                    location.replace(hash);
                    isNavigating = false;
                    return;
                } else if (hash === '#home') {
                    history.replaceState(null, route.title, hash);
                    console.log('🔵 Router: → HOME (replace)');
                } else {
                    history.pushState({ page: hash }, route.title, hash);
                    console.log('🟢 Router: → ' + hash + ' (push)');
                }
            }
            
            Coderive.emit('navigated', { page: hash, title: route.title });
            console.log(`🔄 Router: ${previousPage || 'start'} → ${hash}`);
        }
        
        isNavigating = false;
    }
    
    function handleHashChange() {
        if (isNavigating) return;
        navigateTo(window.location.hash || '#home', false);
    }
    
    function handlePopState(event) {
        console.log('⬅️ Back button pressed', event.state);
        
        if (isNavigating) return;
        
        if (event.state === null) {
            console.log('🚪 Router: at root, exiting');
            return;
        }
        
        let targetHash = '#home';
        if (event.state && event.state.page) {
            targetHash = event.state.page;
        }
        
        navigateTo(targetHash, false);
    }
    
    function handleVisibilityChange() {
        if (document.visibilityState === 'visible' && !isNavigating) {
            const hash = window.location.hash || '#home';
            navigateTo(hash, false);
        }
    }
    
    let touchStartX = 0;
    let touchStartY = 0;
    let touchStartTime = 0;
    
    function initTouchHandlers() {
        document.addEventListener('touchstart', (e) => {
            touchStartX = e.touches[0].clientX;
            touchStartY = e.touches[0].clientY;
            touchStartTime = Date.now();
        }, { passive: true });
        
        document.addEventListener('touchend', (e) => {
            if (!touchStartX || isNavigating) return;
            
            const touchEndX = e.changedTouches[0].clientX;
            const touchEndY = e.changedTouches[0].clientY;
            const touchEndTime = Date.now();
            const timeDiff = touchEndTime - touchStartTime;
            
            const diffX = touchEndX - touchStartX;
            const diffY = touchEndY - touchStartY;
            
            if (touchStartX < 30 && diffX > 80 && Math.abs(diffY) < 50 && timeDiff < 300) {
                console.log('👉 Router: edge swipe back detected');
                if (window.location.hash === '#docs' || window.location.hash === '#playground' || window.location.hash === '#editor') {
                    navigateTo('#home', false);
                }
            }
            
            if (diffX > 100 && Math.abs(diffY) < 50 && timeDiff < 300) {
                console.log('👉 Router: swipe right detected');
                if (window.location.hash === '#docs' || window.location.hash === '#playground' || window.location.hash === '#editor') {
                    navigateTo('#home', false);
                }
            }
            
            touchStartX = 0;
        }, { passive: true });
    }
    
    Coderive.whenReady(() => {
        window.addEventListener('hashchange', handleHashChange);
        window.addEventListener('popstate', handlePopState);
        document.addEventListener('visibilitychange', handleVisibilityChange);
        initTouchHandlers();
        
        document.addEventListener('click', (e) => {
            const link = e.target.closest('a[href^="#"]');
            if (!link) return;
            
            if (link.classList.contains('back-to-top')) {
                e.preventDefault();
                window.scrollTo({ top: 0, behavior: 'smooth' });
                return;
            }
            
            if (!link.target) {
                e.preventDefault();
                const hash = link.getAttribute('href');
                console.log('🔗 Link clicked:', hash);
                navigateTo(hash, true);
            }
        });
        
        const initialHash = window.location.hash || '#home';
        history.replaceState(null, document.title, initialHash);
        handleHashChange();
        
        console.log('✅ Router initialized - Home replaces, Docs/Playground/Editor push');
    });
})();
