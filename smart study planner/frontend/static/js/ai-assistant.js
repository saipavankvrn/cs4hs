document.addEventListener("DOMContentLoaded", function() {
    alert("AI Assistant JS loaded!");
    const fab = document.getElementById('ai-fab');
    const sidebar = document.getElementById('ai-sidebar');
    const closeBtn = document.getElementById('ai-close-btn');
    const minBtn = document.getElementById('ai-minimize-btn');
    const expandBtn = document.getElementById('ai-expand-btn');
    const newChatBtn = document.getElementById('ai-new-chat-btn');
    const historyToggle = document.getElementById('ai-history-toggle');
    const historyDrawer = document.getElementById('ai-history-drawer');
    const sessionList = document.getElementById('ai-session-list');
    
    const sendBtn = document.getElementById('ai-send-btn');
    const userInput = document.getElementById('ai-user-input');
    const chatHistory = document.getElementById('ai-chat-history');
    const chatTitle = document.getElementById('ai-chat-title');

    // CSRF Token logic if present in meta tags (Spring Security)
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    let activeSessionId = sessionStorage.getItem('activeSessionId') || null;

    // --- State Persistence ---
    function restoreState() {
        const state = localStorage.getItem('aiSidebarState');
        if (state === 'open') { sidebar.classList.remove('hidden'); setTimeout(() => sidebar.classList.add('open'), 10); fab.classList.add('hidden'); }
        else if (state === 'expanded') { sidebar.classList.remove('hidden'); setTimeout(() => sidebar.classList.add('open', 'expanded'), 10); fab.classList.add('hidden'); }
        else if (state === 'minimized') { sidebar.classList.remove('hidden'); setTimeout(() => sidebar.classList.add('open', 'minimized'), 10); fab.classList.add('hidden'); }
        else { sidebar.classList.add('hidden'); fab.classList.remove('hidden'); } // Strict default hidden
        
        if (activeSessionId) {
            loadSession(activeSessionId);
        } else {
            loadSessionsList();
        }
    }
    
    function saveState(state) {
        if (state === 'closed') { localStorage.removeItem('aiSidebarState'); }
        else { localStorage.setItem('aiSidebarState', state); }
    }

    // --- UI Toggles ---
    fab?.addEventListener('click', () => {
        sidebar.classList.remove('hidden');
        setTimeout(() => {
            sidebar.classList.remove('minimized', 'expanded');
            sidebar.classList.add('open');
        }, 10);
        fab.classList.add('hidden');
        saveState('open');
    });
    
    closeBtn?.addEventListener('click', () => {
        sidebar.classList.remove('open', 'expanded', 'minimized');
        historyDrawer.classList.remove('open');
        setTimeout(() => {
            sidebar.classList.add('hidden');
            fab.classList.remove('hidden');
        }, 300); // Wait for transition
        saveState('closed');
    });

    minBtn?.addEventListener('click', () => {
        if(sidebar.classList.contains('minimized')) {
            sidebar.classList.remove('minimized');
            saveState(sidebar.classList.contains('expanded') ? 'expanded' : 'open');
        } else {
            sidebar.classList.add('minimized');
            sidebar.classList.remove('expanded');
            saveState('minimized');
        }
        historyDrawer.classList.remove('open');
    });

    expandBtn?.addEventListener('click', () => {
        sidebar.classList.remove('minimized');
        sidebar.classList.toggle('expanded');
        saveState(sidebar.classList.contains('expanded') ? 'expanded' : 'open');
    });

    historyToggle?.addEventListener('click', () => {
        historyDrawer.classList.toggle('open');
        if (historyDrawer.classList.contains('open')) {
            loadSessionsList();
        }
    });

    newChatBtn?.addEventListener('click', () => {
        startNewChat();
    });

    // --- Network Helpers ---
    function fetchWithCsrf(url, options = {}) {
        if (!options.headers) options.headers = {};
        if (csrfToken && csrfHeader) {
            options.headers[csrfHeader] = csrfToken;
        }
        return fetch(url, options);
    }

    // --- Chat Logic ---
    function startNewChat() {
        chatHistory.innerHTML = '<div class="ai-message ai-bot">Hello! I\'m your AI Tutor. Ask me anything about the current page!</div>';
        chatTitle.innerHTML = '<i class="fas fa-robot"></i> New Chat';
        activeSessionId = null;
        sessionStorage.removeItem('activeSessionId');
        historyDrawer.classList.remove('open');
    }

    function loadSessionsList() {
        fetchWithCsrf('/api/ai/sessions')
        .then(res => res.json())
        .then(sessions => {
            sessionList.innerHTML = '';
            if(sessions.length === 0) {
                sessionList.innerHTML = '<li style="color: gray;">No previous sessions</li>';
                return;
            }
            sessions.forEach(s => {
                const li = document.createElement('li');
                li.innerHTML = `<i class="fas fa-comment-dots"></i> ${s.title}`;
                li.addEventListener('click', () => loadSession(s.id));
                sessionList.appendChild(li);
            });
        }).catch(err => console.error(err));
    }

    function loadSession(id) {
        fetchWithCsrf(`/api/ai/sessions/${id}`)
        .then(res => {
            if(!res.ok) throw new Error('Session not found');
            return res.json();
        })
        .then(messages => {
            chatHistory.innerHTML = '';
            messages.forEach(m => {
                let htmlContent = m.content;
                if(m.role === 'assistant' && typeof marked !== 'undefined') {
                    htmlContent = marked.parse(htmlContent);
                }
                appendMessage(htmlContent, m.role === 'user' ? 'user' : 'bot', m.role === 'assistant');
            });
            activeSessionId = id;
            sessionStorage.setItem('activeSessionId', id);
            historyDrawer.classList.remove('open');
            // Fetch sessions just to update title, but could be optimized
            loadSessionsList(); 
        })
        .catch(err => {
            console.error(err);
            startNewChat();
        });
    }

    function getPageContext() {
        let container = document.querySelector('.main-content') || document.querySelector('main') || document.body;
        let clone = container.cloneNode(true);
        let navs = clone.querySelectorAll('nav, .sidebar, script, style, .ai-sidebar, .ai-fab');
        navs.forEach(n => n.remove());
        return clone.innerText.substring(0, 3000); 
    }

    function appendMessage(text, sender, isHtml = false) {
        const msgDiv = document.createElement('div');
        msgDiv.classList.add('ai-message', sender === 'user' ? 'ai-user' : 'ai-bot');
        if (isHtml) {
            msgDiv.innerHTML = text;
            // apply highlight.js
            if(typeof hljs !== 'undefined') {
                msgDiv.querySelectorAll('pre code').forEach((block) => {
                    hljs.highlightElement(block);
                });
            }
        } else {
            msgDiv.textContent = text;
        }
        chatHistory.appendChild(msgDiv);
        chatHistory.scrollTop = chatHistory.scrollHeight;
        return msgDiv;
    }

    function showTypingIndicator() {
        const msgDiv = document.createElement('div');
        msgDiv.classList.add('ai-message', 'ai-bot');
        msgDiv.id = 'ai-typing';
        msgDiv.innerHTML = '<div class="typing-indicator"><span></span><span></span><span></span></div>';
        chatHistory.appendChild(msgDiv);
        chatHistory.scrollTop = chatHistory.scrollHeight;
    }

    function removeTypingIndicator() {
        const typing = document.getElementById('ai-typing');
        if (typing) typing.remove();
    }

    function sendToAI() {
        const text = userInput.value.trim();
        if (!text) return;

        appendMessage(text, 'user');
        userInput.value = '';
        showTypingIndicator();

        const context = getPageContext();
        
        const payload = {
            userMessage: text,
            pageContext: context
        };
        if(activeSessionId) {
            payload.sessionId = activeSessionId;
        }

        fetchWithCsrf('/api/ai/chat', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        })
        .then(response => response.json())
        .then(data => {
            removeTypingIndicator();
            activeSessionId = data.sessionId;
            sessionStorage.setItem('activeSessionId', activeSessionId);
            chatTitle.innerHTML = `<i class="fas fa-robot"></i> ${data.title}`;
            
            let finalHtml = data.response;
            if (typeof marked !== 'undefined') {
                finalHtml = marked.parse(finalHtml);
            }
            appendMessage(finalHtml, 'bot', true);
        })
        .catch(err => {
            removeTypingIndicator();
            appendMessage('Connection error. Please try again later.', 'bot');
            console.error('AI Error:', err);
        });
    }

    sendBtn?.addEventListener('click', sendToAI);

    userInput?.addEventListener('keypress', function(e) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendToAI();
        }
    });

    // Initialize
    if(typeof marked !== 'undefined') {
        marked.setOptions({
            breaks: true,
            gfm: true
        });
    }
    restoreState();
});
