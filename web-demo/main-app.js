const API_URL = 'http://localhost:8000';

const state = {
    currentImage: null,
    currentImageData: null,
    roiCoords: null,
    history: []
};

const elements = {
    pages: {
        landing: document.getElementById('landingPage'),
        home: document.getElementById('homePage'),
        roi: document.getElementById('roiPage'),
        loading: document.getElementById('loadingPage'),
        results: document.getElementById('resultsPage'),
        history: document.getElementById('historyPage'),
        permissions: document.getElementById('permissionsPage')
    },
    nav: {
        links: document.querySelectorAll('.nav-link'),
        navbar: document.getElementById('navbar')
    },
    landing: {
        getStarted: document.getElementById('getStartedBtn')
    },
    home: {
        uploadArea: document.getElementById('uploadArea'),
        uploadBtn: document.getElementById('uploadBtn'),
        imageInput: document.getElementById('imageInput'),
        canvas: document.getElementById('imageCanvas'),
        placeholder: document.getElementById('uploadPlaceholder')
    },
    roi: {
        canvas: document.getElementById('roiCanvas'),
        wrapper: document.getElementById('roiCanvasWrapper'),
        box: document.getElementById('roiBox'),
        recognizeBtn: document.getElementById('recognizeBtn'),
        cancelBtn: document.getElementById('cancelROI')
    },
    results: {
        preview: document.getElementById('resultImagePreview'),
        label: document.getElementById('resultLabel'),
        confidence: document.getElementById('resultConfidence'),
        alternatives: document.getElementById('resultAlternatives'),
        adjustBtn: document.getElementById('adjustRegionBtn'),
        scanAgainBtn: document.getElementById('scanAgainBtn')
    },
    history: {
        grid: document.getElementById('historyGrid'),
        clearBtn: document.getElementById('clearHistoryBtn')
    }
};

function showPage(pageName) {
    Object.values(elements.pages).forEach(page => page.classList.remove('active'));
    elements.pages[pageName]?.classList.add('active');

    elements.nav.links.forEach(link => {
        link.classList.toggle('active', link.dataset.page === pageName);
    });

    elements.nav.navbar.style.display = pageName === 'landing' ? 'none' : 'block';
}

elements.landing.getStarted.addEventListener('click', () => {
    showPage('home');
});

elements.nav.links.forEach(link => {
    link.addEventListener('click', (e) => {
        e.preventDefault();
        const page = link.dataset.page;
        if (page === 'history') {
            renderHistory();
        }
        showPage(page);
    });
});

elements.home.uploadBtn.addEventListener('click', () => {
    elements.home.imageInput.click();
});

elements.home.imageInput.addEventListener('change', (e) => {
    const file = e.target.files[0];
    if (!file) return;

    if (!file.type.startsWith('image/')) {
        alert('Please select a valid image file');
        return;
    }

    if (file.size > 10 * 1024 * 1024) {
        alert('Image too large. Please select an image under 10MB');
        return;
    }

    state.currentImage = file;
    loadImageToROI(file);
});

function loadImageToROI(file) {
    const reader = new FileReader();
    reader.onload = (e) => {
        const img = new Image();
        img.onload = () => {
            const canvas = elements.roi.canvas;
            const ctx = canvas.getContext('2d');

            const maxWidth = 700;
            const scale = Math.min(1, maxWidth / img.width);
            canvas.width = img.width * scale;
            canvas.height = img.height * scale;

            ctx.drawImage(img, 0, 0, canvas.width, canvas.height);

            state.currentImageData = {
                width: img.width,
                height: img.height,
                displayWidth: canvas.width,
                displayHeight: canvas.height,
                scale: scale
            };

            initializeROI();
            showPage('roi');
        };
        img.src = e.target.result;
    };
    reader.readAsDataURL(file);
}

function initializeROI() {
    const canvas = elements.roi.canvas;
    const box = elements.roi.box;

    const defaultSize = Math.min(canvas.width, canvas.height) * 0.6;
    const defaultX = (canvas.width - defaultSize) / 2;
    const defaultY = (canvas.height - defaultSize) / 2;

    box.style.left = defaultX + 'px';
    box.style.top = defaultY + 'px';
    box.style.width = defaultSize + 'px';
    box.style.height = defaultSize + 'px';

    setupROIDragging();
}

function setupROIDragging() {
    const box = elements.roi.box;
    const canvas = elements.roi.canvas;
    let isDragging = false;
    let isResizing = false;
    let resizeHandle = null;
    let startX, startY, startLeft, startTop, startWidth, startHeight;

    box.addEventListener('mousedown', (e) => {
        if (e.target.classList.contains('roi-handle')) {
            isResizing = true;
            resizeHandle = e.target;
        } else {
            isDragging = true;
        }

        startX = e.clientX;
        startY = e.clientY;
        startLeft = box.offsetLeft;
        startTop = box.offsetTop;
        startWidth = box.offsetWidth;
        startHeight = box.offsetHeight;

        e.preventDefault();
    });

    document.addEventListener('mousemove', (e) => {
        if (!isDragging && !isResizing) return;

        const deltaX = e.clientX - startX;
        const deltaY = e.clientY - startY;

        if (isDragging) {
            let newLeft = startLeft + deltaX;
            let newTop = startTop + deltaY;

            newLeft = Math.max(0, Math.min(newLeft, canvas.width - box.offsetWidth));
            newTop = Math.max(0, Math.min(newTop, canvas.height - box.offsetHeight));

            box.style.left = newLeft + 'px';
            box.style.top = newTop + 'px';
        }

        if (isResizing && resizeHandle) {
            const minSize = 50;
            let newWidth = startWidth;
            let newHeight = startHeight;
            let newLeft = startLeft;
            let newTop = startTop;

            if (resizeHandle.classList.contains('roi-handle-se')) {
                newWidth = Math.max(minSize, startWidth + deltaX);
                newHeight = Math.max(minSize, startHeight + deltaY);
            } else if (resizeHandle.classList.contains('roi-handle-sw')) {
                newWidth = Math.max(minSize, startWidth - deltaX);
                newHeight = Math.max(minSize, startHeight + deltaY);
                newLeft = startLeft + (startWidth - newWidth);
            } else if (resizeHandle.classList.contains('roi-handle-ne')) {
                newWidth = Math.max(minSize, startWidth + deltaX);
                newHeight = Math.max(minSize, startHeight - deltaY);
                newTop = startTop + (startHeight - newHeight);
            } else if (resizeHandle.classList.contains('roi-handle-nw')) {
                newWidth = Math.max(minSize, startWidth - deltaX);
                newHeight = Math.max(minSize, startHeight - deltaY);
                newLeft = startLeft + (startWidth - newWidth);
                newTop = startTop + (startHeight - newHeight);
            }

            if (newLeft >= 0 && newTop >= 0 &&
                newLeft + newWidth <= canvas.width &&
                newTop + newHeight <= canvas.height) {
                box.style.width = newWidth + 'px';
                box.style.height = newHeight + 'px';
                box.style.left = newLeft + 'px';
                box.style.top = newTop + 'px';
            }
        }
    });

    document.addEventListener('mouseup', () => {
        isDragging = false;
        isResizing = false;
        resizeHandle = null;
    });
}

function getROICoordinates() {
    const box = elements.roi.box;
    const data = state.currentImageData;

    const x = box.offsetLeft / data.scale;
    const y = box.offsetTop / data.scale;
    const width = box.offsetWidth / data.scale;
    const height = box.offsetHeight / data.scale;

    return { x, y, width, height };
}

function cropImageToROI() {
    return new Promise((resolve) => {
        const coords = getROICoordinates();
        const canvas = document.createElement('canvas');
        const ctx = canvas.getContext('2d');

        canvas.width = coords.width;
        canvas.height = coords.height;

        const img = new Image();
        img.onload = () => {
            ctx.drawImage(img, coords.x, coords.y, coords.width, coords.height, 0, 0, coords.width, coords.height);
            canvas.toBlob((blob) => {
                resolve(blob);
            }, 'image/jpeg', 0.9);
        };
        img.src = elements.roi.canvas.toDataURL();
    });
}

elements.roi.recognizeBtn.addEventListener('click', async () => {
    showPage('loading');

    try {
        const croppedBlob = await cropImageToROI();
        const formData = new FormData();
        formData.append('file', croppedBlob, 'roi.jpg');
        formData.append('top_k', '3');

        const response = await fetch(`${API_URL}/identify`, {
            method: 'POST',
            body: formData
        });

        if (!response.ok) {
            const error = await response.json().catch(() => ({}));
            throw new Error(error.detail || `Server error: ${response.status}`);
        }

        const data = await response.json();
        displayResults(data.predictions, croppedBlob);

    } catch (error) {
        console.error('Recognition error:', error);
        alert(`Failed to recognize object: ${error.message}`);
        showPage('roi');
    }
});

function displayResults(predictions, imageBlob) {
    if (!predictions || predictions.length === 0) {
        alert('No objects detected');
        showPage('roi');
        return;
    }

    const url = URL.createObjectURL(imageBlob);
    elements.results.preview.src = url;

    const primary = predictions[0];
    const confidence = Math.round(primary.confidence * 100);

    elements.results.label.textContent = formatLabel(primary.label);
    elements.results.confidence.textContent = `${confidence}% confidence`;

    if (predictions.length > 1) {
        const alt = predictions.slice(1).map(p => {
            const conf = Math.round(p.confidence * 100);
            return `
                <div class="result-alternative">
                    <span>${formatLabel(p.label)}</span>
                    <span>${conf}%</span>
                </div>
            `;
        }).join('');
        elements.results.alternatives.innerHTML = alt;
    } else {
        elements.results.alternatives.innerHTML = '';
    }

    saveToHistory({
        image: url,
        label: primary.label,
        confidence: primary.confidence,
        timestamp: Date.now()
    });

    showPage('results');
}

function formatLabel(label) {
    return label.split('_').map(word =>
        word.charAt(0).toUpperCase() + word.slice(1).toLowerCase()
    ).join(' ');
}

function saveToHistory(item) {
    state.history.unshift(item);
    if (state.history.length > 20) state.history.pop();
    localStorage.setItem('objectid_history', JSON.stringify(state.history));
}

function loadHistory() {
    const saved = localStorage.getItem('objectid_history');
    if (saved) {
        state.history = JSON.parse(saved);
    }
}

function renderHistory() {
    const grid = elements.history.grid;

    if (state.history.length === 0) {
        grid.innerHTML = `
            <div class="history-empty">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"></path>
                    <path d="M3 3v5h5"></path>
                </svg>
                <p>No recognition history yet</p>
                <span>Start scanning to build your history</span>
            </div>
        `;
        return;
    }

    grid.innerHTML = state.history.map(item => `
        <div class="history-item glass">
            <img src="${item.image}" alt="${item.label}" class="history-item-image">
            <div class="history-item-label">${formatLabel(item.label)}</div>
            <div class="history-item-time">${timeAgo(item.timestamp)}</div>
        </div>
    `).join('');
}

function timeAgo(timestamp) {
    const seconds = Math.floor((Date.now() - timestamp) / 1000);
    if (seconds < 60) return 'Just now';
    if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
    if (seconds < 86400) return `${Math.floor(seconds / 3600)}h ago`;
    return `${Math.floor(seconds / 86400)}d ago`;
}

elements.history.clearBtn.addEventListener('click', () => {
    if (confirm('Clear all history?')) {
        state.history = [];
        localStorage.removeItem('objectid_history');
        renderHistory();
    }
});

elements.roi.cancelBtn.addEventListener('click', () => {
    showPage('home');
});

elements.results.adjustBtn.addEventListener('click', () => {
    showPage('roi');
});

elements.results.scanAgainBtn.addEventListener('click', () => {
    state.currentImage = null;
    state.currentImageData = null;
    elements.home.imageInput.value = '';
    showPage('home');
});

loadHistory();
showPage('landing');
