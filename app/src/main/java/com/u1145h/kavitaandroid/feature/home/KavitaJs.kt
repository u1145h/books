package com.u1145h.kavitaandroid.feature.home

/** JavaScript injected into the embedded Kavita web UI. */
object KavitaJs {

    /** Name of the `@JavascriptInterface` object exposed to the page. */
    const val OBJECT_NAME = "KavitaAndroid"

    /**
     * Evaluated after each page load: polls for the Kavita auth token and
     * reports it to the native side so OkHttp-backed sync can authenticate.
     */
    const val SESSION_SYNC = """
        (function() {
            var api = window.KavitaAndroid;
            if (!api) return;
            var reported = false;
            function extract() {
                try {
                    var token = localStorage.getItem('kavita_token')
                        || localStorage.getItem('token')
                        || localStorage.getItem('jwt')
                        || '';
                    if (!token && document.cookie) {
                        var m = document.cookie.match(/(?:^|;\s*)token=([^;]+)/);
                        token = m ? m[1] : '';
                    }
                    var username = localStorage.getItem('kavita_username') || '';
                    if (token && !reported) {
                        reported = true;
                        api.onSession(JSON.stringify({ token: token, username: username }));
                    }
                } catch (e) {}
            }
            var n = 0;
            var timer = setInterval(function() {
                extract();
                if (++n >= 120) clearInterval(timer);
            }, 1000);
        })();
    """

    /**
     * Evaluated after each page load: reads the webpage body's computed
     * background color so the native status/navigation bar strip can match it.
     */
    const val BODY_COLOR = """
        (function() {
            var api = window.KavitaAndroid;
            if (!api) return;
            try {
                var b = getComputedStyle(document.body).backgroundColor;
                if (b && b !== 'rgba(0, 0, 0, 0)' && b !== 'transparent') {
                    api.onBodyColor(b);
                }
            } catch (e) {}
        })();
    """

    /**
     * Injected into Kavita Web App to detect item action menus, inject the
     * "Offline Download" button right below "Download", show a tick mark (✓)
     * if downloaded, and handle native downloads.
     */
    const val OFFLINE_INJECT = """
        (function() {
            var api = window.KavitaAndroid;
            if (!api) return;

            if (!document.getElementById('kavita-offline-style')) {
                var style = document.createElement('style');
                style.id = 'kavita-offline-style';
                style.textContent = `
                    .offline-download-btn {
                        display: flex !important;
                        align-items: center !important;
                        justify-content: space-between !important;
                        width: 100% !important;
                        margin-top: 6px !important;
                        padding: 10px 16px !important;
                        border-radius: 8px !important;
                        border: 1px solid #198754 !important;
                        background-color: rgba(25, 135, 84, 0.1) !important;
                        color: #ffffff !important;
                        font-weight: 500 !important;
                        font-size: 15px !important;
                        cursor: pointer !important;
                        transition: all 0.2s ease-in-out !important;
                        box-sizing: border-box !important;
                    }
                    .offline-download-btn:hover, .offline-download-btn:active {
                        background-color: rgba(25, 135, 84, 0.3) !important;
                    }
                    .offline-tick-mark {
                        color: #20c997 !important;
                        font-weight: bold !important;
                        font-size: 18px !important;
                        margin-left: 8px !important;
                    }
                    .offline-status-badge {
                        font-size: 13px !important;
                        opacity: 0.85 !important;
                    }
                `;
                document.head.appendChild(style);
            }

            function checkAndInjectActionMenu() {
                var buttons = Array.from(document.querySelectorAll('button, a, .action-item, .btn'));
                buttons.forEach(function(btn) {
                    var txt = (btn.textContent || '').trim();
                    if ((txt === 'Download' || (txt.indexOf('Download') === 0 && txt.length < 15)) && 
                        !btn.classList.contains('offline-download-btn') && !btn.dataset.offlineInjected) {
                        
                        btn.dataset.offlineInjected = 'true';
                        var parent = btn.parentElement;
                        if (!parent || parent.querySelector('.offline-download-btn')) return;

                        // Find closest card, row, or modal container for context
                        var container = btn.closest('.modal-content, .card, [data-chapter-id], [data-series-id], [data-volume-id], app-chapter-item, app-volume-item, app-series-detail, app-action-menu') || document.body;
                        
                        // Extract Title
                        var titleEl = container.querySelector('.modal-title, .card-title, .series-title, .chapter-title, h1, h2, h3, h4, h5, .title');
                        var title = titleEl ? titleEl.textContent.trim() : '';
                        if (!title || title === 'Actions' || title === 'Books' || title === 'Home (Kavita)') {
                            var altTitle = document.querySelector('h1, h2, .series-title, .chapter-title')?.textContent?.trim();
                            if (altTitle && altTitle !== 'Books' && altTitle !== 'Home (Kavita)') title = altTitle;
                        }
                        if (!title) title = 'Downloaded Book';

                        // Extract IDs from DOM dataset
                        var chapterId = parseInt(container.dataset?.chapterId || container.getAttribute('chapter-id') || '0', 10);
                        var seriesId = parseInt(container.dataset?.seriesId || container.getAttribute('series-id') || '0', 10);
                        var volumeId = parseInt(container.dataset?.volumeId || container.getAttribute('volume-id') || '0', 10);

                        // Extract IDs from URL path
                        var path = window.location.pathname;
                        var chapMatch = path.match(/\/(?:chapter|reader\/chapter)\/(\d+)/i);
                        if (!chapterId && chapMatch) chapterId = parseInt(chapMatch[1], 10);

                        var serMatch = path.match(/\/series\/(\d+)/i);
                        if (!seriesId && serMatch) seriesId = parseInt(serMatch[1], 10);

                        var volMatch = path.match(/\/volume\/(\d+)/i);
                        if (!volumeId && volMatch) volumeId = parseInt(volMatch[1], 10);

                        // Check for direct href link containing /api/Download/
                        var downloadUrl = btn.getAttribute('href') || btn.dataset.url || null;
                        if (!downloadUrl) {
                            var linkWithUrl = container.querySelector('a[href*="/api/Download/"]');
                            if (linkWithUrl) downloadUrl = linkWithUrl.getAttribute('href');
                        }

                        var offlineBtn = document.createElement('button');
                        offlineBtn.className = 'offline-download-btn';
                        
                        var isDownloaded = chapterId > 0 && api.isChapterDownloaded ? api.isChapterDownloaded(chapterId) : false;
                        
                        offlineBtn.innerHTML = '<span>Offline Download</span> ' + 
                            (isDownloaded ? '<span class="offline-tick-mark">✓</span>' : '<span class="offline-status-badge">⬇</span>');

                        offlineBtn.addEventListener('click', function(e) {
                            e.preventDefault();
                            e.stopPropagation();

                            var payload = {
                                chapterId: chapterId > 0 ? chapterId : null,
                                seriesId: seriesId > 0 ? seriesId : null,
                                volumeId: volumeId > 0 ? volumeId : null,
                                title: title,
                                downloadUrl: downloadUrl
                            };

                            offlineBtn.innerHTML = '<span>Offline Download</span> <span class="offline-status-badge">Downloading...</span>';
                            api.downloadOffline(JSON.stringify(payload));
                        });

                        if (btn.nextSibling) {
                            parent.insertBefore(offlineBtn, btn.nextSibling);
                        } else {
                            parent.appendChild(offlineBtn);
                        }
                    }
                });
            }

            window.onOfflineStatusChanged = function(chapterId, status) {
                var btns = document.querySelectorAll('.offline-download-btn');
                btns.forEach(function(btn) {
                    if (status === 'downloaded') {
                        btn.innerHTML = '<span>Offline Download</span> <span class="offline-tick-mark">✓</span>';
                    } else if (status === 'error') {
                        btn.innerHTML = '<span>Offline Download</span> <span style="color:#ff6b6b;">Error ✖</span>';
                    } else if (status === 'downloading') {
                        btn.innerHTML = '<span>Offline Download</span> <span class="offline-status-badge">Downloading...</span>';
                    }
                });
            };

            var observer = new MutationObserver(function() {
                checkAndInjectActionMenu();
            });
            observer.observe(document.body, { childList: true, subtree: true });
            checkAndInjectActionMenu();
        })();
    """

    /**
     * Injected into Kavita Web App to insert "Offline Mode" and "App Settings"
     * options into the mobile navigation drawer/sidebar.
     */
    const val SIDEBAR_INJECT = """
        (function() {
            var api = window.KavitaAndroid;
            if (!api) return;

            if (!document.getElementById('kavita-sidebar-style')) {
                var style = document.createElement('style');
                style.id = 'kavita-sidebar-style';
                style.textContent = `
                    .kavita-custom-nav-item {
                        display: flex !important;
                        align-items: center !important;
                        padding: 12px 16px !important;
                        color: #e0e0e0 !important;
                        font-size: 15px !important;
                        font-weight: 500 !important;
                        cursor: pointer !important;
                        text-decoration: none !important;
                        transition: background 0.15s ease !important;
                        border-radius: 6px !important;
                        margin: 2px 8px !important;
                    }
                    .kavita-custom-nav-item:hover, .kavita-custom-nav-item:active {
                        background-color: rgba(255, 255, 255, 0.1) !important;
                    }
                    .kavita-custom-nav-icon {
                        margin-right: 12px !important;
                        font-size: 18px !important;
                        width: 24px !important;
                        text-align: center !important;
                    }
                    .kavita-sidebar-divider {
                        height: 1px !important;
                        background-color: rgba(255, 255, 255, 0.15) !important;
                        margin: 10px 12px !important;
                    }
                `;
                document.head.appendChild(style);
            }

            function injectSidebarOptions() {
                var sidebar = document.querySelector('app-side-nav, .side-nav-content, .sidebar-nav, .nav-list, ul.nav, nav');
                if (!sidebar) return;
                if (document.getElementById('kavita-custom-offline-mode')) return;

                var items = Array.from(sidebar.querySelectorAll('li, a, .nav-item, .nav-link'));
                var targetNode = null;
                items.forEach(function(item) {
                    var txt = (item.textContent || '').trim();
                    if (txt.indexOf('Light Novel') !== -1 || txt.indexOf('Manga') !== -1 || txt.indexOf('Browse People') !== -1) {
                        targetNode = item.closest('li') || item;
                    }
                });

                var divider = document.createElement('div');
                divider.className = 'kavita-sidebar-divider';

                var offlineItem = document.createElement('div');
                offlineItem.id = 'kavita-custom-offline-mode';
                offlineItem.className = 'kavita-custom-nav-item';
                offlineItem.innerHTML = '<span class="kavita-custom-nav-icon">📶</span> <span>Offline Mode</span>';
                offlineItem.addEventListener('click', function(e) {
                    e.preventDefault();
                    e.stopPropagation();
                    if (api.openOfflineMode) api.openOfflineMode();
                });

                var settingsItem = document.createElement('div');
                settingsItem.id = 'kavita-custom-app-settings';
                settingsItem.className = 'kavita-custom-nav-item';
                settingsItem.innerHTML = '<span class="kavita-custom-nav-icon">⚙️</span> <span>App Settings</span>';
                settingsItem.addEventListener('click', function(e) {
                    e.preventDefault();
                    e.stopPropagation();
                    if (api.openAppSettings) api.openAppSettings();
                });

                if (targetNode && targetNode.parentElement) {
                    var parent = targetNode.parentElement;
                    var next = targetNode.nextSibling;
                    if (next) {
                        parent.insertBefore(divider, next);
                        parent.insertBefore(offlineItem, next);
                        parent.insertBefore(settingsItem, next);
                    } else {
                        parent.appendChild(divider);
                        parent.appendChild(offlineItem);
                        parent.appendChild(settingsItem);
                    }
                } else {
                    sidebar.appendChild(divider);
                    sidebar.appendChild(offlineItem);
                    sidebar.appendChild(settingsItem);
                }
            }

            var observer = new MutationObserver(function() {
                injectSidebarOptions();
            });
            observer.observe(document.body, { childList: true, subtree: true });
            injectSidebarOptions();
        })();
    """
}

