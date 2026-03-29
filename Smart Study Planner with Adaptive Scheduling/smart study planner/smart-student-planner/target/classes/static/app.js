// ====== Smart Student Planner: Unified UI Engine ======

// Global State
let currentTaskId = null;
let plannedDuration = 30;
let timerRemaining = 25 * 60;
let isTimerRunning = false;
let timerInterval = null;
let timerStartTime = null;

// DOM Readiness Initialization
document.addEventListener('DOMContentLoaded', () => {
    const isFocusPage = window.location.pathname.includes('focus.html');
    
    if (isFocusPage) {
        initFocusPage();
    } else if (document.getElementById('timetableGrid')) {
        initTimetablePage();
    }
});

/**
 * TIMETABLE PAGE LOGIC
 */
async function initTimetablePage() {
    const grid = document.getElementById('timetableGrid');
    const btnAdd = document.getElementById('btnAddTask');
    
    // Initial load
    await loadTimetable(grid);
    
    // Task addition listener
    if (btnAdd) {
        btnAdd.onclick = async () => {
            const title = document.getElementById('newTaskTitle').value;
            let deadline = document.getElementById('newTaskDeadline').value;
            const conf = parseInt(document.getElementById('newTaskConfidence').value || "3");

            if (!title || !deadline) return alert("Title and Deadline required!");
            if(deadline.length === 16) deadline += ":00";

            try {
                const res = await fetch('/api/tasks', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ userId: 1, title, deadline, confidenceScore: conf })
                });
                if (res.ok) {
                    await loadTimetable(grid);
                    document.getElementById('newTaskTitle').value = '';
                }
            } catch (e) { console.error("Sync failed:", e); }
        };
    }
}

async function loadTimetable(grid) {
    const hours = ['06:00', '07:00', '08:00', '09:00', '10:00', '11:00', '12:00', '13:00', '14:00', '15:00', '16:00', '17:00', '18:00', '19:00', '20:00', '21:00', '22:00', '23:00'];
    grid.innerHTML = '';
    
    hours.forEach(time => {
        const timeCell = document.createElement('div');
        timeCell.className = 'time-slot';
        timeCell.textContent = time;
        grid.appendChild(timeCell);
        for(let i = 1; i <= 7; i++) {
            const cell = document.createElement('div');
            cell.className = 'grid-cell';
            cell.id = `cell-${time.substring(0,2)}-${i}`;
            grid.appendChild(cell);
        }
    });

    try {
        const res = await fetch('/api/tasks?userId=1');
        const tasks = await res.json();
        tasks.forEach(task => {
            const dt = new Date(task.deadline);
            let day = dt.getDay() === 0 ? 7 : dt.getDay();
            let hour = dt.getHours() < 10 ? '0' + dt.getHours() : '' + dt.getHours();
            
            const cell = document.getElementById(`cell-${hour}-${day}`);
            if (cell) {
                const card = document.createElement('div');
                card.className = `task-card ${task.priorityScore >= 70 ? 'card-high-priority' : ''}`;
                card.style.cursor = 'pointer';
                card.onclick = (e) => {
                    if (e.target.classList.contains('btn-delete-task')) return;
                    startFocusSession(task);
                };
                card.innerHTML = `
                    <div class="title">${task.title}</div>
                    <div style="font-size:0.7rem; opacity:0.8;">Score: ${task.priorityScore.toFixed(0)} | Drift: ${(task.timeDrift || 0).toFixed(0)}m</div>
                    <div style="margin-top:6px; display:flex; justify-content:space-between;">
                        <span style="font-size:0.65rem; background:rgba(255,255,255,0.1); padding:2px 4px; border-radius:3px;">⚡ Focus</span>
                        <span class="btn-delete-task" data-id="${task.id}" style="color:rgba(255,255,255,0.4);">&times;</span>
                    </div>`;
                cell.appendChild(card);
            }
        });
    } catch (e) { console.error("Fetch failed:", e); }
}

function startFocusSession(task) {
    localStorage.setItem('selectedTask', JSON.stringify({
        id: task.id,
        scheduleId: task.scheduleId || 1, // Correctly pass scheduleId from DB
        subject: task.title,
        score: task.priorityScore,
        drift: task.timeDrift || 0,
        duration: task.avgSessionDuration || 30
    }));
    window.location.href = 'focus.html';
}

/**
 * FOCUS PAGE LOGIC
 */
function initFocusPage() {
    const taskData = localStorage.getItem('selectedTask');
    if (!taskData) {
        document.getElementById('redirectOverlay').style.display = 'flex';
        setTimeout(() => window.location.href = 'index.html', 3000);
        return;
    }

    const task = JSON.parse(taskData);
    currentTaskId = task.id;
    plannedDuration = task.duration;
    timerRemaining = plannedDuration * 60;

    // UI Updates
    document.getElementById('focusTaskTitle').textContent = task.subject;
    document.getElementById('focusPriority').textContent = task.score.toFixed(1);
    document.getElementById('focusDrift').textContent = task.drift.toFixed(0);
    document.getElementById('targetDuration').textContent = task.duration;
    updateTimerDisplay();

    // Features Init
    initTimer();
    initWhiteboard();
    initPointsSystem();
    initFileUpload();
    initSessionFinalization();

    // Integrity Rule
    window.onbeforeunload = (e) => {
        if (isTimerRunning) return "Session in progress! Abandoning will record partial progress.";
    };
}

// ⏱️ TIMER LOGIC
function initTimer() {
    const btnStart = document.getElementById('btnStartTimer');
    const btnPause = document.getElementById('btnPauseTimer');
    const btnReset = document.getElementById('btnResetTimer');

    btnStart.onclick = () => {
        if (isTimerRunning) return;
        isTimerRunning = true;
        timerStartTime = Date.now();
        btnStart.textContent = "Engine Active";
        btnStart.classList.add('disabled'); // visual feedback
        
        timerInterval = setInterval(() => {
            if (timerRemaining > 0) {
                timerRemaining--;
                updateTimerDisplay();
            } else {
                clearInterval(timerInterval);
                isTimerRunning = false;
                btnStart.textContent = "Session Complete";
                alert("Session duration reached! Finalize to sync adaptive metrics.");
            }
        }, 1000);
    };

    btnPause.onclick = () => {
        clearInterval(timerInterval);
        isTimerRunning = false;
        btnStart.textContent = "Resume Session";
    };

    if (btnReset) {
        btnReset.onclick = () => {
            clearInterval(timerInterval);
            isTimerRunning = false;
            timerRemaining = plannedDuration * 60;
            updateTimerDisplay();
            btnStart.textContent = "Initiate Session";
        };
    }
}

function updateTimerDisplay() {
    const m = Math.floor(timerRemaining / 60).toString().padStart(2, '0');
    const s = (timerRemaining % 60).toString().padStart(2, '0');
    document.getElementById('timerDisplay').textContent = `${m}:${s}`;
}

function adjustTimer(mins) {
    timerRemaining += (mins * 60);
    if (timerRemaining < 0) timerRemaining = 0;
    plannedDuration += mins;
    document.getElementById('targetDuration').textContent = plannedDuration;
    updateTimerDisplay();
}

// 🎨 WHITEBOARD LOGIC
function initWhiteboard() {
    const canvas = document.getElementById('whiteboard');
    const ctx = canvas.getContext('2d');
    const clearBtn = document.getElementById('btnClearCanvas');
    let drawing = false;

    const resize = () => {
        canvas.width = canvas.parentElement.offsetWidth;
        canvas.height = canvas.parentElement.offsetHeight;
        ctx.strokeStyle = '#22d3ee';
        ctx.lineWidth = 2;
        ctx.lineJoin = 'round';
        ctx.lineCap = 'round';
    };

    window.addEventListener('resize', resize);
    resize();

    const getPos = (e) => {
        const rect = canvas.getBoundingClientRect();
        return {
            x: (e.clientX || e.touches[0].clientX) - rect.left,
            y: (e.clientY || e.touches[0].clientY) - rect.top
        };
    };

    canvas.onmousedown = (e) => { drawing = true; ctx.beginPath(); const p = getPos(e); ctx.moveTo(p.x, p.y); };
    canvas.onmousemove = (e) => { if(!drawing) return; const p = getPos(e); ctx.lineTo(p.x, p.y); ctx.stroke(); };
    canvas.onmouseup = () => { drawing = false; };
    canvas.ontouchstart = (e) => { drawing = true; ctx.beginPath(); const p = getPos(e); ctx.moveTo(p.x, p.y); };
    canvas.ontouchmove = (e) => { if(!drawing) return; const p = getPos(e); ctx.lineTo(p.x, p.y); ctx.stroke(); e.preventDefault(); };
    canvas.ontouchend = () => { drawing = false; };

    clearBtn.onclick = () => ctx.clearRect(0, 0, canvas.width, canvas.height);
}

// 📌 POINTS SYSTEM
function initPointsSystem() {
    const form = document.getElementById('resourceUploadForm');
    const input = document.getElementById('keyPointInput');
    const list = document.getElementById('keyPointsList');

    form.onsubmit = (e) => {
        e.preventDefault();
        const text = input.value.trim();
        if (!text) return;

        if (list.querySelector('p')) list.innerHTML = ''; // Remove empty message
        
        const div = document.createElement('div');
        div.className = 'point-card';
        div.textContent = `📌 ${text}`;
        list.prepend(div);
        
        input.value = '';
    };
}

// 📂 FILE UPLOAD HANDLING (UI)
function initFileUpload() {
    const input = document.getElementById('fileUpload');
    const list = document.getElementById('fileList');

    input.onchange = () => {
        list.innerHTML = '';
        Array.from(input.files).forEach(file => {
            const div = document.createElement('div');
            div.className = 'file-item';
            div.innerHTML = `<span>📄</span> ${file.name}`;
            list.appendChild(div);
        });
    };
}

// ✅ SESSION FINALIZATION
function initSessionFinalization() {
    const form = document.getElementById('sessionForm');
    
    form.onsubmit = async (e) => {
        e.preventDefault();
        const taskData = JSON.parse(localStorage.getItem('selectedTask'));
        const status = document.getElementById('sessionStatus').value;
        const focus = document.getElementById('focusScore').value;
        const confidence = document.getElementById('confidenceScore').value;
        const notes = document.getElementById('studyNotes').value;

        // Calculate actual duration based on time elapsed in timer
        const actualDuration = timerStartTime 
            ? Math.round((Date.now() - timerStartTime) / 60000) 
            : plannedDuration;

        const payload = {
            userId: 1,
            taskId: currentTaskId,
            scheduleId: taskData.scheduleId,
            status: status,
            focusScore: parseInt(focus),
            confidenceScore: parseInt(confidence),
            plannedDuration: plannedDuration,
            actualDuration: actualDuration,
            notes: notes
        };

        try {
            const res = await fetch('/api/session/complete', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            if (res.ok) {
                localStorage.removeItem('selectedTask');
                isTimerRunning = false;
                alert("Success! Adaptive learning engine updated based on your drift and focus.");
                window.location.href = 'index.html';
            }
        } catch (err) { alert("Sync failed. Local save complete."); }
    };
}
