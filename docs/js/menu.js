// js/menu.js - Handle menu buttons with dynamic filtering
(function() {
    'use strict';
    
    function initMenu() {
        console.log('Menu: initializing...');
        
        const homeDrawerToggle = document.getElementById('drawerToggle');
        const docsDrawerToggle = document.getElementById('docsDrawerToggle');
        const playgroundDrawerToggle = document.getElementById('playgroundDrawerToggle');
        const editorDrawerToggle = document.getElementById('editorDrawerToggle');
        const drawerClose = document.getElementById('drawerClose');
        const drawerOverlay = document.getElementById('drawerOverlay');
        
        function openDrawer() {
            document.body.classList.add('drawer-open');
            updateMenuItems();
            Coderive.emit('drawerOpened');
        }
        
        function closeDrawer() {
            document.body.classList.remove('drawer-open');
            Coderive.emit('drawerClosed');
        }
        
        if (homeDrawerToggle) {
            homeDrawerToggle.addEventListener('click', openDrawer);
            console.log('Menu: home toggle found');
        }
        
        if (docsDrawerToggle) {
            docsDrawerToggle.addEventListener('click', openDrawer);
            console.log('Menu: docs toggle found');
        }
        
        if (playgroundDrawerToggle) {
            playgroundDrawerToggle.addEventListener('click', openDrawer);
            console.log('Menu: playground toggle found');
        }

        if (editorDrawerToggle) {
            editorDrawerToggle.addEventListener('click', openDrawer);
            console.log('Menu: editor toggle found');
        }
        
        if (drawerClose) {
            drawerClose.addEventListener('click', closeDrawer);
        }
        
        if (drawerOverlay) {
            drawerOverlay.addEventListener('click', closeDrawer);
        }
        
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && document.body.classList.contains('drawer-open')) {
                closeDrawer();
            }
        });
        
        Coderive.on('navigated', function(data) {
            console.log('Menu: navigation detected, closing then updating...');
            closeDrawer();
            setTimeout(() => {
                updateMenuItems();
                console.log('Menu: items updated after close');
            }, 300);
        });
        
        Coderive.updateYear('#drawerYear');
        console.log('✅ Menu initialized');
    }
    
    function updateMenuItems() {
        const activePage = document.querySelector('.page.active');
        if (!activePage) return;
        
        const pageId = activePage.id;
        console.log('Menu: updating items for page:', pageId);
        
        const navLinks = document.querySelectorAll('.drawer-item a[data-route]');
        
        navLinks.forEach(link => {
            const route = link.getAttribute('data-route');
            
            if ((pageId === 'homePage' && route === 'home') ||
                (pageId === 'docsPage' && route === 'docs') ||
                (pageId === 'playgroundPage' && route === 'playground') ||
                (pageId === 'editorPage' && route === 'editor')) {
                link.style.display = 'none';
                link.closest('.drawer-item').style.display = 'none';
            } else {
                link.style.display = 'block';
                link.closest('.drawer-item').style.display = 'block';
            }
        });
    }
    
    Coderive.on('partialsLoaded', initMenu);
    Coderive.whenReady(initMenu);
})();
