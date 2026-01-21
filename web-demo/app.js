const API_URL = 'http://localhost:8000';

const elements = {
    uploadSection: document.getElementById('uploadSection'),
    previewSection: document.getElementById('previewSection'),
    loadingSection: document.getElementById('loadingSection'),
    resultsSection: document.getElementById('resultsSection'),
    errorSection: document.getElementById('errorSection'),

    imageInput: document.getElementById('imageInput'),
    uploadBtn: document.getElementById('uploadBtn'),
    changeImageBtn: document.getElementById('changeImageBtn'),
    identifyBtn: document.getElementById('identifyBtn'),
    tryAnotherBtn: document.getElementById('tryAnotherBtn'),
    retryBtn: document.getElementById('retryBtn'),

    previewImage: document.getElementById('previewImage'),
    primaryResult: document.getElementById('primaryResult'),
    secondaryResults: document.getElementById('secondaryResults'),
    confidenceBadge: document.getElementById('confidenceBadge'),
    errorMessage: document.getElementById('errorMessage')
};

let currentImage = null;

function showSection(sectionToShow) {
    const sections = [
        elements.uploadSection,
        elements.previewSection,
        elements.loadingSection,
        elements.resultsSection,
        elements.errorSection
    ];

    sections.forEach(section => {
        if (section === sectionToShow) {
            section.classList.remove('hidden');
        } else {
            section.classList.add('hidden');
        }
    });
}

function handleImageSelect(event) {
    const file = event.target.files[0];
    if (!file) return;

    if (!file.type.startsWith('image/')) {
        showError('Please select a valid image file');
        return;
    }

    if (file.size > 10 * 1024 * 1024) {
        showError('Image too large. Please select an image under 10MB');
        return;
    }

    currentImage = file;

    const reader = new FileReader();
    reader.onload = (e) => {
        elements.previewImage.src = e.target.result;
        showSection(elements.previewSection);
    };
    reader.readAsDataURL(file);
}

async function identifyObject() {
    if (!currentImage) return;

    showSection(elements.loadingSection);

    const formData = new FormData();
    formData.append('file', currentImage);
    formData.append('top_k', '3');
    formData.append('threshold', '0.3');

    try {
        const response = await fetch(`${API_URL}/identify`, {
            method: 'POST',
            body: formData
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.detail || `Server error: ${response.status}`);
        }

        const data = await response.json();
        displayResults(data.predictions);

    } catch (error) {
        console.error('Identification error:', error);
        showError(error.message || 'Failed to identify object. Please try again.');
    }
}

function displayResults(predictions) {
    if (!predictions || predictions.length === 0) {
        showError('No objects detected in the image');
        return;
    }

    const primary = predictions[0];
    const confidence = Math.round(primary.confidence * 100);

    elements.primaryResult.innerHTML = `
        <div class="primary-label">${formatLabel(primary.label)}</div>
        <div class="primary-confidence">${confidence}% confidence</div>
    `;

    elements.confidenceBadge.textContent = `${confidence}% Match`;
    elements.confidenceBadge.className = 'confidence-badge ' + getConfidenceClass(primary.confidence);

    if (predictions.length > 1) {
        const secondaryHTML = predictions.slice(1).map(pred => {
            const conf = Math.round(pred.confidence * 100);
            return `
                <div class="result-item">
                    <span class="result-label">${formatLabel(pred.label)}</span>
                    <span class="result-confidence">${conf}%</span>
                </div>
            `;
        }).join('');

        elements.secondaryResults.innerHTML = secondaryHTML;
    } else {
        elements.secondaryResults.innerHTML = '';
    }

    showSection(elements.resultsSection);
}

function formatLabel(label) {
    return label
        .split('_')
        .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
        .join(' ');
}

function getConfidenceClass(confidence) {
    if (confidence >= 0.7) return 'confidence-high';
    if (confidence >= 0.4) return 'confidence-medium';
    return 'confidence-low';
}

function showError(message) {
    elements.errorMessage.textContent = message;
    showSection(elements.errorSection);
}

function resetToUpload() {
    currentImage = null;
    elements.imageInput.value = '';
    elements.previewImage.src = '';
    showSection(elements.uploadSection);
}

elements.uploadBtn.addEventListener('click', () => {
    elements.imageInput.click();
});

elements.imageInput.addEventListener('change', handleImageSelect);

elements.changeImageBtn.addEventListener('click', () => {
    elements.imageInput.click();
});

elements.identifyBtn.addEventListener('click', identifyObject);

elements.tryAnotherBtn.addEventListener('click', resetToUpload);

elements.retryBtn.addEventListener('click', () => {
    if (currentImage) {
        identifyObject();
    } else {
        resetToUpload();
    }
});

document.addEventListener('DOMContentLoaded', () => {
    showSection(elements.uploadSection);
});
