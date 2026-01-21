"""
FastAPI backend for ObjectID Web Demo.
Uses the Imagga API - the same service used in the Android app.
"""

from fastapi import FastAPI, File, UploadFile, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Optional
from PIL import Image
import io
import logging
import requests
from requests.auth import HTTPBasicAuth
import traceback
import os
from dotenv import load_dotenv

load_dotenv()

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="ObjectID Web Demo API",
    description="Object recognition using Imagga API",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

IMAGGA_API_KEY = os.getenv("IMAGGA_API_KEY")
IMAGGA_API_SECRET = os.getenv("IMAGGA_API_SECRET")
IMAGGA_API_URL = "https://api.imagga.com/v2/tags"

MAX_FILE_SIZE_MB = 10
MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024 * 1024
MAX_IMAGE_DIMENSION = 1024
SUPPORTED_FORMATS = {'image/jpeg', 'image/png', 'image/webp', 'image/bmp'}


class PredictionResponse(BaseModel):
    label: str
    confidence: float


class IdentifyResponse(BaseModel):
    predictions: List[PredictionResponse]
    status: str = "success"


def resize_image_if_needed(image: Image.Image) -> Image.Image:
    """Resize image if it exceeds maximum dimensions."""
    if image.width > MAX_IMAGE_DIMENSION or image.height > MAX_IMAGE_DIMENSION:
        scale = MAX_IMAGE_DIMENSION / max(image.width, image.height)
        new_width = int(image.width * scale)
        new_height = int(image.height * scale)
        logger.info(f"Resizing image from {image.width}x{image.height} to {new_width}x{new_height}")
        return image.resize((new_width, new_height), Image.Resampling.LANCZOS)
    return image


def image_to_bytes(image: Image.Image) -> bytes:
    """Convert PIL Image to JPEG bytes."""
    if image.mode != 'RGB':
        image = image.convert('RGB')
    
    buffer = io.BytesIO()
    image.save(buffer, format='JPEG', quality=85)
    return buffer.getvalue()


async def call_imagga_api(image_bytes: bytes, top_k: int = 3) -> List[dict]:
    """Call Imagga API for image tagging."""
    try:
        logger.info(f"Calling Imagga API with image size: {len(image_bytes)} bytes")
        
        files = {
            'image': ('image.jpg', image_bytes, 'image/jpeg')
        }
        
        response = requests.post(
            IMAGGA_API_URL,
            auth=HTTPBasicAuth(IMAGGA_API_KEY, IMAGGA_API_SECRET),
            files=files,
            timeout=30
        )
        
        logger.info(f"Imagga API response status: {response.status_code}")
        
        if response.status_code == 200:
            data = response.json()
            
            if 'result' in data and 'tags' in data['result']:
                tags = data['result']['tags']
                logger.info(f"Received {len(tags)} tags from Imagga")
                
                results = []
                for tag in tags[:top_k]:
                    results.append({
                        'label': tag['tag']['en'],
                        'confidence': tag['confidence'] / 100.0
                    })
                
                return results
            else:
                logger.error(f"Unexpected response format: {data}")
                raise HTTPException(
                    status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                    detail="Unexpected response format from Imagga API"
                )
        
        elif response.status_code == 401:
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail="API authentication failed. Check credentials."
            )
        
        elif response.status_code == 429:
            raise HTTPException(
                status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                detail="Rate limit exceeded. Please try again later."
            )
        
        else:
            error_msg = f"Imagga API error: {response.status_code}"
            logger.error(f"{error_msg} - {response.text}")
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail=error_msg
            )
            
    except requests.exceptions.Timeout:
        raise HTTPException(
            status_code=status.HTTP_504_GATEWAY_TIMEOUT,
            detail="Request to Imagga API timed out"
        )
    except requests.exceptions.RequestException as e:
        logger.error(f"Request error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=f"Network error: {str(e)}"
        )


@app.on_event("startup")
async def startup_event():
    """Validate API credentials on startup."""
    if not IMAGGA_API_KEY or not IMAGGA_API_SECRET:
        logger.error("❌ IMAGGA_API_KEY and IMAGGA_API_SECRET must be set in .env file")
        logger.error("   Copy .env.example to .env and add your credentials")
        raise RuntimeError("Imagga API credentials not configured")
    
    logger.info("✅ Imagga API credentials loaded successfully")
    logger.info(f"   API Key: {IMAGGA_API_KEY[:10]}...")


@app.get("/")
async def root():
    """API information."""
    return {
        "name": "ObjectID Web Demo API",
        "version": "1.0.0",
        "status": "running",
        "powered_by": "Imagga API",
        "endpoints": {
            "health": "/health",
            "identify": "POST /identify",
            "docs": "/docs"
        }
    }


@app.get("/health")
async def health_check():
    """Check API health."""
    return {
        "status": "healthy",
        "api_configured": bool(IMAGGA_API_KEY and IMAGGA_API_SECRET),
        "message": "ObjectID Web Demo API is ready"
    }


@app.post("/identify", response_model=IdentifyResponse)
async def identify_object(
    file: UploadFile = File(...),
    top_k: Optional[int] = 3
):
    """
    Identify objects in an uploaded image using Imagga API.
    
    Args:
        file: Image file (JPEG, PNG, WebP, or BMP)
        top_k: Number of results to return (default: 3)
        
    Returns:
        IdentifyResponse with predictions from Imagga
    """
    
    if file.content_type not in SUPPORTED_FORMATS:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Unsupported format. Supported: {', '.join(SUPPORTED_FORMATS)}"
        )
    
    try:
        file_content = await file.read()
        file_size = len(file_content)
        
        if file_size > MAX_FILE_SIZE_BYTES:
            raise HTTPException(
                status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE,
                detail=f"File too large. Maximum size: {MAX_FILE_SIZE_MB}MB"
            )
        
        if file_size == 0:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Empty file uploaded"
            )
        
        image = Image.open(io.BytesIO(file_content))
        image = resize_image_if_needed(image)
        image_bytes = image_to_bytes(image)
        
        logger.info(f"Processing image: {file.filename} ({file_size} bytes)")
        
        results = await call_imagga_api(image_bytes, top_k=min(max(1, top_k), 10))
        
        if not results:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="No objects detected in the image"
            )
        
        predictions = [
            PredictionResponse(label=r['label'], confidence=r['confidence'])
            for r in results
        ]
        
        logger.info(f"Successfully identified {len(predictions)} objects")
        
        return IdentifyResponse(predictions=predictions)
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Unexpected error: {e}")
        logger.error(traceback.format_exc())
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Processing failed: {str(e)}"
        )


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "api:app",
        host="0.0.0.0",
        port=8000,
        reload=True,
        log_level="info"
    )
