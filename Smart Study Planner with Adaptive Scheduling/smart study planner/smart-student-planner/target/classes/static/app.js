// ====== Smart Student Planner: Core UI Logic ======

// DOM Elements
const timetableGrid = document.getElementById('timetableGrid');
const focusScore = document.getElementById('focusScore');
const focusVal = document.getElementById('focusVal');
const confidenceScore = document.getElementById('confidenceScore');
const confidenceVal = document.getElementById('confidenceVal');
const sessionForm = document.getElementById('sessionForm');
const whiteboard = document.getElementById('whiteboard');

// 1. Dynamic REST API Grid Mapping (MySQL Integration)
const btnAddTask = document.getElementById('btnAddTask');

if (timetableGrid) {
    const hours = ['06:00', '07:00', '08:00', '09:00', '10:00', '11:00', '12:00', '13:00', '14:00', '15:00', '16:00', '17:00', '18:00', '19:00', '20:00', '21:00', '22:00', '23:00'];
    
    // Draw the structural bones initially
    const initializeGrid = () => {
        timetableGrid.innerHTML = '';
        hours.forEach(time => {
            const timeCell = document.createElement('div');
            timeCell.className = 'time-slot';
            timeCell.textContent = time;
            timetableGrid.appendChild(timeCell);
            
            for(let i = 1; i <= 7; i++) {
                const cell = document.createElement('div');
                cell.className = 'grid-cell';
                cell.id = `cell-${time.substring(0,2)}-${i}`; // id mapping e.g: cell-08-1
                timetableGrid.appendChild(cell);
            }
        });
    };

    // Auto-fetch Tasks from the Java endpoint
    const loadRealtimeTasks = async () => {
        try {
            const response = await fetch('/api/tasks?userId=1'); // Pull user 1
            if (!response.ok) return;
            const tasks = await response.json();
            
            initializeGrid(); // Clear to prevent duplicates
            
            tasks.forEach((task) => {
                // Parse the actual deadline from the database
                const dt = new Date(task.deadline);
                
                // Map JS Day (0=Sun, 1=Mon...) to Grid Day (1=Mon, 2=Tue, ..., 7=Sun)
                let day = dt.getDay();
                if (day === 0) day = 7; 
                
                // Map Hour to 2-digit string (e.g. 08, 14)
                let hour = dt.getHours();
                const hourSlot = hour < 10 ? '0' + hour : '' + hour;
                
                const targetCell = document.getElementById(`cell-${hourSlot}-${day}`);
                
                if(targetCell) {
                    const highPriority = task.priorityScore >= 70;
                    const card = document.createElement('div');
                    card.className = `task-card ${highPriority ? 'card-high-priority' : ''}`;
                    card.innerHTML = `
                        <div style="display:flex; justify-content:space-between; align-items:flex-start;">
                            <div class="title" style="word-break:break-word; max-width:85%;">${task.title}</div>
                            <span class="btn-delete-task" data-id="${task.id}" style="cursor:pointer; font-size:1.2rem; line-height:1; font-weight:bold; opacity:0.7; transition:0.2s;" onmouseover="this.style.color='#ef4444'; this.style.opacity='1'" onmouseout="this.style.color=''; this.style.opacity='0.7'" title="Delete Subject">&times;</span>
                        </div>
                        <div style="font-size:0.8rem; margin-top:2px; opacity:0.8;">
                            Priority: <b>${task.priorityScore}</b><br/>Missed Slots: ${task.missedCount}
                        </div>
                    `;
                    targetCell.appendChild(card);
                }
            });
        } catch (e) {
            console.error("Connection isolated. Ensure Spring Boot is resolving on :8080");
            initializeGrid(); // Draw empty grid if server is dead
        }
    };
    
    loadRealtimeTasks();

    // Global listener for Timetable Grid matrix clicks (Specifically Deletions)
    timetableGrid.addEventListener('click', async (e) => {
        if(e.target.classList.contains('btn-delete-task')) {
            const subjectId = e.target.getAttribute('data-id');
            if(confirm("Confirm action: Completely wipe this subject from your schedule?")) {
                try {
                    const req = await fetch(`/api/tasks/${subjectId}`, { method: 'DELETE' });
                    if(req.ok) {
                        await loadRealtimeTasks(); // Immediately repaint the grid
                    } else {
                        alert("Failed to delete from Database.");
                    }
                } catch(error) {
                    alert("Database routing error preventing deletion.");
                }
            }
        }
    });

    // Async Hook for User Subject Adding
    if (btnAddTask) {
        btnAddTask.addEventListener('click', async () => {
            const title = document.getElementById('newTaskTitle').value;
            let deadline = document.getElementById('newTaskDeadline').value;
            const confidenceScore = parseInt(document.getElementById('newTaskConfidence').value || "3");

            if (!title || !deadline) return alert("Warning: Title and Deadline are absolutely required.");
            
            // Format datetime-local mapping ensuring Seconds exist for Java Parsing rules
            if(deadline.length === 16) deadline += ":00";

            const reqData = {
                userId: 1, // Simulated session user 1
                title: title,
                description: "Student generated focus session",
                deadline: deadline,
                confidenceScore: confidenceScore
            };

            const btnOriginal = btnAddTask.innerHTML;
            btnAddTask.innerHTML = "Syncing...";

            try {
                const response = await fetch('/api/tasks', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(reqData)
                });
                const jsonResponse = await response.json();
                
                if(response.ok && jsonResponse.status === 'success') {
                    // Update layout immediately!
                    await loadRealtimeTasks();
                    document.getElementById('newTaskTitle').value = '';
                } else {
                    alert("Database Foreign Key Error: " + (jsonResponse.message || "Reboot java to resolve tables."));
                }
            } catch (e) {
                alert("Java server offline. Required to execute persistence.");
            } finally {
                btnAddTask.innerHTML = btnOriginal;
            }
        });
    }
}

// 2. Immersive Pomodoro Timer
let timerInterval;
let timeRemaining = 25 * 60; 
let isRunning = false;
let isWorkMode = true;

const timerDisplay = document.getElementById('timerDisplay');
const btnStart = document.getElementById('btnStartTimer');
const btnPause = document.getElementById('btnPauseTimer');
const btnReset = document.getElementById('btnResetTimer');
const btnToggleMode = document.getElementById('btnToggleMode');

const updateTimerUI = () => {
    if(!timerDisplay) return;
    const m = Math.floor(timeRemaining / 60).toString().padStart(2, '0');
    const s = (timeRemaining % 60).toString().padStart(2, '0');
    timerDisplay.textContent = `${m}:${s}`;
};

if(timerDisplay) {
    btnStart.addEventListener('click', () => {
        if(!isRunning) {
            isRunning = true;
            timerInterval = setInterval(() => {
                if(timeRemaining > 0) {
                    timeRemaining--;
                    updateTimerUI();
                } else {
                    clearInterval(timerInterval);
                    isRunning = false;
                    alert(isWorkMode ? 'Block executed successfully! Initiating Break cycle.' : 'Rest cycle over. Resume workflow.');
                    new Audio('https://assets.mixkit.co/active_storage/sfx/2869/2869-preview.mp3').play().catch(()=>console.log("Audio unplayable"));
                }
            }, 1000);
            btnStart.textContent = "Time Engine Active";
            btnStart.style.filter = "brightness(1.5)";
        }
    });

    btnPause.addEventListener('click', () => {
        clearInterval(timerInterval);
        isRunning = false;
        btnStart.textContent = "Resume Execution";
        btnStart.style.filter = "none";
    });

    btnReset.addEventListener('click', () => {
        clearInterval(timerInterval);
        isRunning = false;
        timeRemaining = isWorkMode ? 25 * 60 : 5 * 60;
        updateTimerUI();
        btnStart.textContent = "Start Engine";
        btnStart.style.filter = "none";
    });

    btnToggleMode.addEventListener('click', () => {
        isWorkMode = !isWorkMode;
        btnToggleMode.textContent = isWorkMode ? 'Switch to Rest' : 'Switch to Focus';
        timeRemaining = isWorkMode ? 25 * 60 : 5 * 60;
        updateTimerUI();
    });
}

// 3. Mathematical Canvas Whiteboard Engine
if(whiteboard) {
    const ctx = whiteboard.getContext('2d');
    let isDrawing = false;

    // Fluid resize hook scaling to bounding boxes dynamically
    const adaptCanvas = () => {
        whiteboard.width = whiteboard.offsetWidth;
        whiteboard.height = whiteboard.offsetHeight;
        ctx.strokeStyle = '#22d3ee'; // Cyberpunk neon trace lines
        ctx.lineWidth = 2.5;
        ctx.lineCap = 'round';
    };
    
    // Inject bounding logic on structural paint
    setTimeout(adaptCanvas, 200); 
    window.addEventListener('resize', adaptCanvas);

    const calcCoordinates = (e) => {
        const rect = whiteboard.getBoundingClientRect();
        const clientX = e.touches ? e.touches[0].clientX : e.clientX;
        const clientY = e.touches ? e.touches[0].clientY : e.clientY;
        return {
            x: clientX - rect.left,
            y: clientY - rect.top
        };
    };

    const beginStroke = (e) => {
        isDrawing = true;
        const coords = calcCoordinates(e);
        ctx.beginPath();
        ctx.moveTo(coords.x, coords.y);
        e.preventDefault();
    };

    const renderStroke = (e) => {
        if(!isDrawing) return;
        const coords = calcCoordinates(e);
        ctx.lineTo(coords.x, coords.y);
        ctx.stroke();
        
        ctx.shadowBlur = 8;
        ctx.shadowColor = '#6366f1';
        e.preventDefault();
    };

    const finalizeStroke = () => {
        isDrawing = false;
        ctx.beginPath();
    };

    // Binding standard mouse protocols
    whiteboard.addEventListener('mousedown', beginStroke);
    whiteboard.addEventListener('mousemove', renderStroke);
    whiteboard.addEventListener('mouseup', finalizeStroke);
    whiteboard.addEventListener('mouseleave', finalizeStroke);
    
    // Binding touch capabilities for tablets
    whiteboard.addEventListener('touchstart', beginStroke, {passive: false});
    whiteboard.addEventListener('touchmove', renderStroke, {passive: false});
    whiteboard.addEventListener('touchend', finalizeStroke);

    document.getElementById('btnClearCanvas').addEventListener('click', () => {
        ctx.clearRect(0, 0, whiteboard.width, whiteboard.height);
    });
}

// 4. Async REST Hook for Java Backend bridging
if(focusScore) {
    focusScore.addEventListener('input', e => focusVal.textContent = e.target.value);
    confidenceScore.addEventListener('input', e => confidenceVal.textContent = e.target.value);

    sessionForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const formData = {
            userId: 1, // static for prototype
            taskId: 1, 
            scheduleId: 1,
            focusScore: parseInt(focusScore.value),
            confidenceScore: parseInt(confidenceScore.value),
            notes: document.getElementById('studyNotes').value
        };

        const submitBtn = sessionForm.querySelector('button');
        const defaultText = submitBtn.textContent;

        try {
            // Posting directly to our SessionController using pure Java API
            console.log("[Antigravity] Sending to /api/session/complete ->", formData);
            
            const req = await fetch('/api/session/complete', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(formData)
            });

            if (!req.ok) throw new Error("Backend offline or exception");

            // Visually alert user of success
            submitBtn.style.background = '#10b981';
            submitBtn.textContent = 'Session Registered! ✔️';
            
            setTimeout(() => {
                submitBtn.style.background = '';
                submitBtn.textContent = defaultText;
                document.getElementById('studyNotes').value = ''; 
            }, 3000);

        } catch (error) {
            console.warn("REST Error: Did you run the Spring Application?", error);
            
            // Fallback UI handling for UI dev / offline local runs
            submitBtn.style.background = '#f59e0b';
            submitBtn.textContent = 'Simulated Local Save ✔️ (Start Java Server to persist)';
            setTimeout(() => {
                submitBtn.style.background = '';
                submitBtn.textContent = defaultText;
            }, 3500);
        }
    });
}
