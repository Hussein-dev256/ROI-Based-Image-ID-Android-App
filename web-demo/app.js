const API_URL = 'http://localhost:8000';

const app = {
    state: {
        currentImage: null,
        currentImageData: null,
        history: [],
        imageStage: {
            zoom: 1,
            panX: 0,
            panY: 0,
            minZoom: 0.5,
            maxZoom: 3
        },
        roi: {
            x: 0,
            y: 0,
            width: 200,
            height: 200,
            isDragging: false,
            isResizing: false,
            dragStartX: 0,
            dragStartY: 0,
            resizeHandle: null
        }
    },

    views: {
        scan: document.getElementById('scanView'),
        roi: document.getElementById('roiView'),
        loading: document.getElementById('loadingView'),
        results: document.getElementById('resultsView'),
        history: document.getElementById('historyView'),
        permissions: document.getElementById('permissionsView')
    },

    init() {
        this.loadHistory();
        this.setupNavigation();
        this.setupUpload();
        this.setupResults();
        this.setupHistory();
        this.showView('scan');
    },

    setupNavigation() {
        document.querySelectorAll('.nav-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                const view = btn.dataset.view;
                document.querySelectorAll('.nav-btn').forEach(b => b.classList.remove('active'));
                btn.classList.add('active');

                if (view === 'history') this.renderHistory();
                this.showView(view);
            });
        });
    },

    setupUpload() {
        const uploadBtn = document.getElementById('uploadBtn');
        const fileInput = document.getElementById('fileInput');

        uploadBtn.addEventListener('click', () => fileInput.click());

        fileInput.addEventListener('change', (e) => {
            const file = e.target.files[0];
            if (!file) return;

            if (!file.type.startsWith('image/')) {
                this.showError('Please select a valid image file');
                return;
            }

            if (file.size > 10 * 1024 * 1024) {
                this.showError('Image too large (max 10MB)');
                return;
            }

            this.state.currentImage = file;
            this.loadImageToROI(file);
        });
    },

    loadImageToROI(file) {
        const reader = new FileReader();
        reader.onload = (e) => {
            const img = new Image();
            img.onload = () => {
                const canvas = document.getElementById('roiCanvas');
                const ctx = canvas.getContext('2d');

                // Calculate scaled dimensions to fit viewport
                const maxWidth = 1200;
                const maxHeight = 600;

                let width = img.width;
                let height = img.height;

                // Scale to fit
                const scale = Math.min(maxWidth / width, maxHeight / height, 1);
                width = Math.floor(width * scale);
                height = Math.floor(height * scale);

                // Set canvas size
                canvas.width = width;
                canvas.height = height;

                // Draw with high quality
                ctx.imageSmoothingEnabled = true;
                ctx.imageSmoothingQuality = 'high';
                ctx.drawImage(img, 0, 0, width, height);

                // Store image data
                this.state.currentImageData = {
                    originalWidth: img.width,
                    originalHeight: img.height,
                    displayWidth: width,
                    displayHeight: height,
                    scale: scale,
                    imageDataURL: e.target.result
                };

                // Initialize ROI and show
                this.initializeROI(width, height);
                this.showView('roi');
            };
            img.src = e.target.result;
        };
        reader.readAsDataURL(file);
    },

    initializeROI(canvasWidth, canvasHeight) {
        // Set initial ROI in center of canvas
        const size = Math.min(canvasWidth, canvasHeight) * 0.5;
        this.state.roi = {
            x: (canvasWidth - size) / 2,
            y: (canvasHeight - size) / 2,
            width: size,
            height: size,
            isDragging: false,
            isResizing: false,
            dragStartX: 0,
            dragStartY: 0,
            resizeHandle: null
        };

        this.updateROIBox();
        // Setup will happen after view is shown
    },

    updateROIBox() {
        const box = document.getElementById('roiSelector');
        const roi = this.state.roi;

        box.style.left = roi.x + 'px';
        box.style.top = roi.y + 'px';
        box.style.width = roi.width + 'px';
        box.style.height = roi.height + 'px';
    },

    setupImageStage() {
        const zoomInBtn = document.getElementById('zoomInBtn');
        const zoomOutBtn = document.getElementById('zoomOutBtn');
        const resetViewBtn = document.getElementById('resetViewBtn');
        const canvas = document.getElementById('roiCanvas');

        if (!zoomInBtn || !zoomOutBtn || !resetViewBtn || !canvas) return;

        // Zoom in
        zoomInBtn.addEventListener('click', () => {
            const stage = this.state.imageStage;
            if (stage.zoom < stage.maxZoom) {
                stage.zoom = Math.min(stage.zoom + 0.25, stage.maxZoom);
                this.applyImageTransform();
            }
        });

        // Zoom out
        zoomOutBtn.addEventListener('click', () => {
            const stage = this.state.imageStage;
            if (stage.zoom > stage.minZoom) {
                stage.zoom = Math.max(stage.zoom - 0.25, stage.minZoom);
                this.applyImageTransform();
            }
        });

        // Reset view
        resetViewBtn.addEventListener('click', () => {
            this.resetImageStage();
        });

        // Mouse wheel zoom
        canvas.addEventListener('wheel', (e) => {
            e.preventDefault();
            const stage = this.state.imageStage;
            const delta = e.deltaY > 0 ? -0.1 : 0.1;
            stage.zoom = Math.max(stage.minZoom, Math.min(stage.maxZoom, stage.zoom + delta));
            this.applyImageTransform();
        });
    },

    applyImageTransform() {
        const canvas = document.getElementById('roiCanvas');
        const stage = this.state.imageStage;
        const zoomLevel = document.getElementById('zoomLevel');

        if (canvas) {
            canvas.style.transform = `scale(${stage.zoom}) translate(${stage.panX}px, ${stage.panY}px)`;
        }

        if (zoomLevel) {
            zoomLevel.textContent = `${Math.round(stage.zoom * 100)}%`;
        }
    },

    resetImageStage() {
        this.state.imageStage = {
            zoom: 1,
            panX: 0,
            panY: 0,
            minZoom: 0.5,
            maxZoom: 3
        };
        this.applyImageTransform();
    },

    setupROIInteraction() {
        const canvas = document.getElementById('roiCanvas');
        const box = document.getElementById('roiSelector');
        const wrapper = document.querySelector('.roi-workspace'); // Changed from getElementById

        // Null checks - return early if elements don't exist
        if (!canvas || !box || !wrapper) {
            console.error('ROI elements not found', { canvas: !!canvas, box: !!box, wrapper: !!wrapper });
            return;
        }

        // Get mouse position relative to canvas
        const getMousePos = (e) => {
            const rect = canvas.getBoundingClientRect();
            return {
                x: e.clientX - rect.left,
                y: e.clientY - rect.top
            };
        };

        // Check if mouse is over resize handle
        const getResizeHandle = (mouseX, mouseY) => {
            const roi = this.state.roi;
            const handleSize = 12;

            const handles = {
                'top-left': { x: roi.x, y: roi.y },
                'top-right': { x: roi.x + roi.width, y: roi.y },
                'bottom-left': { x: roi.x, y: roi.y + roi.height },
                'bottom-right': { x: roi.x + roi.width, y: roi.y + roi.height }
            };

            for (let [name, pos] of Object.entries(handles)) {
                if (Math.abs(mouseX - pos.x) < handleSize &&
                    Math.abs(mouseY - pos.y) < handleSize) {
                    return name;
                }
            }
            return null;
        };

        // Check if mouse is inside ROI box
        const isInsideROI = (mouseX, mouseY) => {
            const roi = this.state.roi;
            return mouseX >= roi.x && mouseX <= roi.x + roi.width &&
                mouseY >= roi.y && mouseY <= roi.y + roi.height;
        };

        // Mouse down
        wrapper.addEventListener('mousedown', (e) => {
            const pos = getMousePos(e);
            const handle = getResizeHandle(pos.x, pos.y);

            console.log('Mouse down at', pos, 'Handle:', handle, 'Inside:', isInsideROI(pos.x, pos.y));

            if (handle) {
                // Start resizing
                this.state.roi.isResizing = true;
                this.state.roi.resizeHandle = handle;
                this.state.roi.dragStartX = pos.x;
                this.state.roi.dragStartY = pos.y;
                this.state.roi.startX = this.state.roi.x;
                this.state.roi.startY = this.state.roi.y;
                this.state.roi.startWidth = this.state.roi.width;
                this.state.roi.startHeight = this.state.roi.height;
                console.log('✅ Started resizing from', handle);
            } else if (isInsideROI(pos.x, pos.y)) {
                // Start dragging
                this.state.roi.isDragging = true;
                this.state.roi.dragStartX = pos.x - this.state.roi.x;
                this.state.roi.dragStartY = pos.y - this.state.roi.y;
                console.log('✅ Started dragging');
            }

            e.preventDefault();
        });

        // Mouse move - ATTACH TO DOCUMENT FOR GLOBAL TRACKING
        document.addEventListener('mousemove', (e) => {
            if (!this.state.currentImageData) return;

            const pos = getMousePos(e);
            const roi = this.state.roi;
            const data = this.state.currentImageData;

            // Handle dragging
            if (roi.isDragging) {
                let newX = pos.x - roi.dragStartX;
                let newY = pos.y - roi.dragStartY;

                // Constrain to canvas
                newX = Math.max(0, Math.min(newX, data.displayWidth - roi.width));
                newY = Math.max(0, Math.min(newY, data.displayHeight - roi.height));

                roi.x = newX;
                roi.y = newY;
                this.updateROIBox();
            }

            // Handle resizing
            if (roi.isResizing) {
                const dx = pos.x - roi.dragStartX;
                const dy = pos.y - roi.dragStartY;
                const minSize = 40;

                let newX = roi.startX;
                let newY = roi.startY;
                let newWidth = roi.startWidth;
                let newHeight = roi.startHeight;

                if (roi.resizeHandle === 'bottom-right') {
                    newWidth = Math.max(minSize, roi.startWidth + dx);
                    newHeight = Math.max(minSize, roi.startHeight + dy);
                } else if (roi.resizeHandle === 'bottom-left') {
                    newWidth = Math.max(minSize, roi.startWidth - dx);
                    newHeight = Math.max(minSize, roi.startHeight + dy);
                    newX = roi.startX + roi.startWidth - newWidth;
                } else if (roi.resizeHandle === 'top-right') {
                    newWidth = Math.max(minSize, roi.startWidth + dx);
                    newHeight = Math.max(minSize, roi.startHeight - dy);
                    newY = roi.startY + roi.startHeight - newHeight;
                } else if (roi.resizeHandle === 'top-left') {
                    newWidth = Math.max(minSize, roi.startWidth - dx);
                    newHeight = Math.max(minSize, roi.startHeight - dy);
                    newX = roi.startX + roi.startWidth - newWidth;
                    newY = roi.startY + roi.startHeight - newHeight;
                }

                // Constrain to canvas
                if (newX >= 0 && newY >= 0 &&
                    newX + newWidth <= data.displayWidth &&
                    newY + newHeight <= data.displayHeight) {
                    roi.x = newX;
                    roi.y = newY;
                    roi.width = newWidth;
                    roi.height = newHeight;
                    this.updateROIBox();
                }
            }
        });

        // Hover effects - attach to wrapper for cursor changes
        wrapper.addEventListener('mousemove', (e) => {
            if (this.state.roi.isDragging || this.state.roi.isResizing) return;

            const pos = getMousePos(e);
            const handle = getResizeHandle(pos.x, pos.y);

            if (handle) {
                if (handle.includes('top-left') || handle.includes('bottom-right')) {
                    wrapper.style.cursor = 'nwse-resize';
                } else {
                    wrapper.style.cursor = 'nesw-resize';
                }
            } else if (isInsideROI(pos.x, pos.y)) {
                wrapper.style.cursor = 'move';
            } else {
                wrapper.style.cursor = 'default';
            }
        });

        // Mouse up
        document.addEventListener('mouseup', () => {
            if (this.state.roi.isDragging || this.state.roi.isResizing) {
                console.log('✅ Finished interaction');
            }
            this.state.roi.isDragging = false;
            this.state.roi.isResizing = false;
            this.state.roi.resizeHandle = null;
            wrapper.style.cursor = 'default';
        });

        // Buttons
        document.getElementById('roiCancelBtn').addEventListener('click', () => {
            this.showView('scan');
        });

        document.getElementById('roiRecognizeBtn').addEventListener('click', () => {
            this.performRecognition();
        });
    },

    async performRecognition() {
        this.showView('loading');

        try {
            const blob = await this.extractROIBlob();

            const formData = new FormData();
            formData.append('file', blob, 'roi.jpg');
            formData.append('top_k', '3');

            const response = await fetch(`${API_URL}/identify`, {
                method: 'POST',
                body: formData
            });

            if (!response.ok) {
                const error = await response.json().catch(() => ({}));
                throw new Error(error.detail || 'Recognition failed');
            }

            const data = await response.json();
            this.displayResults(data.predictions, blob);

        } catch (error) {
            console.error('Recognition error:', error);
            this.showError(error.message);
            this.showView('roi');
        }
    },

    extractROIBlob() {
        return new Promise((resolve, reject) => {
            const roi = this.state.roi;
            const data = this.state.currentImageData;

            // Scale coordinates to original image
            const scaleX = data.originalWidth / data.displayWidth;
            const scaleY = data.originalHeight / data.displayHeight;

            const cropX = Math.round(roi.x * scaleX);
            const cropY = Math.round(roi.y * scaleY);
            const cropWidth = Math.round(roi.width * scaleX);
            const cropHeight = Math.round(roi.height * scaleY);

            // Load original image
            const img = new Image();
            img.onload = () => {
                const canvas = document.createElement('canvas');
                const ctx = canvas.getContext('2d');

                canvas.width = cropWidth;
                canvas.height = cropHeight;

                ctx.imageSmoothingEnabled = true;
                ctx.imageSmoothingQuality = 'high';
                ctx.drawImage(img, cropX, cropY, cropWidth, cropHeight, 0, 0, cropWidth, cropHeight);

                canvas.toBlob((blob) => {
                    if (blob) {
                        resolve(blob);
                    } else {
                        reject(new Error('Failed to create blob'));
                    }
                }, 'image/jpeg', 0.92);
            };

            img.onerror = () => reject(new Error('Failed to load image'));
            img.src = data.imageDataURL;
        });
    },

    setupResults() {
        document.getElementById('resultAdjustBtn').addEventListener('click', () => {
            this.showView('roi');
        });

        document.getElementById('resultNewBtn').addEventListener('click', () => {
            this.state.currentImage = null;
            document.getElementById('fileInput').value = '';
            this.showView('scan');
        });
    },

    displayResults(predictions, blob) {
        if (!predictions || predictions.length === 0) {
            this.showError('No objects detected');
            this.showView('roi');
            return;
        }

        const url = URL.createObjectURL(blob);
        document.getElementById('resultImage').src = url;

        const primary = predictions[0];
        const confidence = Math.round(primary.confidence * 100);

        document.getElementById('resultLabel').textContent = this.formatLabel(primary.label);
        document.getElementById('confidenceFill').style.width = confidence + '%';
        document.getElementById('confidenceValue').textContent = `${confidence}% Match`;

        const alts = document.getElementById('resultAlternatives');
        if (predictions.length > 1) {
            alts.innerHTML = predictions.slice(1).map(p => {
                const conf = Math.round(p.confidence * 100);
                return `
                    <div class="alt-item">
                        <span class="alt-label">${this.formatLabel(p.label)}</span>
                        <span class="alt-confidence">${conf}%</span>
                    </div>
                `;
            }).join('');
        } else {
            alts.innerHTML = '';
        }

        this.saveToHistory({
            image: url,
            label: primary.label,
            confidence: primary.confidence,
            timestamp: Date.now()
        });

        this.showView('results');
    },

    setupHistory() {
        document.getElementById('clearHistoryBtn').addEventListener('click', () => {
            if (confirm('Clear all scan history?')) {
                this.state.history = [];
                localStorage.removeItem('objectid_history');
                this.renderHistory();
            }
        });
    },

    saveToHistory(item) {
        this.state.history.unshift(item);
        if (this.state.history.length > 30) this.state.history.pop();
        localStorage.setItem('objectid_history', JSON.stringify(this.state.history));
    },

    loadHistory() {
        const saved = localStorage.getItem('objectid_history');
        if (saved) {
            this.state.history = JSON.parse(saved);
        }
    },

    renderHistory() {
        const grid = document.getElementById('historyGrid');

        if (this.state.history.length === 0) {
            grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; padding: 3rem; color: var(--text-muted);"><p>No scan history yet</p></div>';
            return;
        }

        grid.innerHTML = this.state.history.map(item => `
            <div class="history-item">
                <img src="${item.image}" alt="${item.label}" class="history-img">
                <div class="history-label">${this.formatLabel(item.label)}</div>
                <div class="history-time">${this.timeAgo(item.timestamp)}</div>
            </div>
        `).join('');
    },

    formatLabel(label) {
        return label.split('_').map(w => w.charAt(0).toUpperCase() + w.slice(1).toLowerCase()).join(' ');
    },

    timeAgo(timestamp) {
        const seconds = Math.floor((Date.now() - timestamp) / 1000);
        if (seconds < 60) return 'Just now';
        if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
        if (seconds < 86400) return `${Math.floor(seconds / 3600)}h ago`;
        return `${Math.floor(seconds / 86400)}d ago`;
    },

    showView(name) {
        Object.values(this.views).forEach(view => view.classList.remove('active'));
        this.views[name]?.classList.add('active');

        // Setup ROI interaction when ROI view becomes active
        if (name === 'roi' && this.state.currentImageData) {
            // Give DOM time to render
            setTimeout(() => {
                this.resetImageStage(); // Reset zoom/pan
                this.setupImageStage(); // Setup zoom/pan controls
                this.updateROIBox();
                this.setupROIInteraction();
            }, 50);
        }
    },

    showError(message) {
        // Create or update error notification
        let errorEl = document.getElementById('errorNotification');
        if (!errorEl) {
            errorEl = document.createElement('div');
            errorEl.id = 'errorNotification';
            errorEl.className = 'error-notification';
            document.body.appendChild(errorEl);
        }

        errorEl.textContent = message;
        errorEl.classList.add('show');

        setTimeout(() => {
            errorEl.classList.remove('show');
        }, 4000);
    }
};

document.addEventListener('DOMContentLoaded', () => app.init());
