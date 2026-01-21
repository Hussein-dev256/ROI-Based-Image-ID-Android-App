# ObjectID Web Demo

A modern web interface for the ObjectID object recognition system, powered by the **Imagga API** - the same technology used in the ObjectID Android app.

## Quick Start

### 1. Configure API Credentials

The demo requires Imagga API credentials. Create a `.env` file:

```bash
# Copy the example file
copy .env.example .env

# Then edit .env and add your credentials
```

Your `.env` file should look like:
```
IMAGGA_API_KEY=your_actual_api_key
IMAGGA_API_SECRET=your_actual_api_secret
```

**Getting Credentials:**
- The `.env` file already contains working credentials for testing
- Or get your own: Sign up at [imagga.com](https://imagga.com) → Dashboard → Copy API Key & Secret

### 2. Install Dependencies

```bash
pip install -r requirements.txt
```

### 3. Start the API Server

```bash
python api.py
```

The API will start on `http://localhost:8000`

### 4. Open the Web Demo

Simply open `index.html` in your browser.

Or use a local server:

```bash
# Python 3
python -m http.server 3000

# Or just double-click index.html
```

Then visit `http://localhost:3000`

## Features

✨ **Instant Recognition** - Upload any image and get instant object identification  
🎨 **Modern UI** - Beautiful, responsive interface with glassmorphic design  
☁️ **Cloud-Powered** - Uses the same Imagga API as the Android app  
📊 **Multiple Results** - See top 3 predictions with confidence scores  
⚡ **Fast** - Results in seconds  
📱 **Responsive** - Works on desktop, tablet, and mobile  

## Architecture

```
Web Browser → FastAPI Backend → Imagga Cloud API → Results
```

This is a **presentation layer** for the existing ObjectID system. The Android app and web demo share the same recognition backend (Imagga API), ensuring consistent results.

## API Endpoints

### POST /identify
Upload an image for object recognition.

**Request:**
- `file`: Image file (JPEG, PNG, WebP, or BMP)
- `top_k`: Number of results (optional, default: 3)

**Response:**
```json
{
  "predictions": [
    {
      "label": "coffee",
      "confidence": 0.87
    }
  ],
  "status": "success"
}
```

### GET /health
Check API health and configuration status.

### GET /docs
Interactive API documentation (Swagger UI)

## Tech Stack

- **Frontend**: HTML5, CSS3, Vanilla JavaScript
- **Backend**: FastAPI (Python)
- **Recognition**: Imagga API (Cloud)
- **Design**: Glassmorphism, modern gradients, smooth animations

## Security & Configuration

### API Credentials

API credentials are managed securely via environment variables:

**✅ Secure (Current Approach):**
- Credentials stored in `.env` file (not committed to Git)
- `.env` listed in `.gitignore`
- `.env.example` provided as template

**❌ Never Do This:**
- Don't hardcode credentials in source code
- Don't commit `.env` to Git
- Don't share credentials in public repositories

### Environment Variables

The `.env` file contains:
```
IMAGGA_API_KEY=your_key_here
IMAGGA_API_SECRET=your_secret_here
```

For production deployment, set these as environment variables in your hosting platform (Heroku, Railway, etc.).

## Deployment

### Local Development
Already covered above - just run `python api.py` and open `index.html`.

### Production Deployment

**Option 1: Railway / Render / Heroku**
- Deploy the `web-demo` folder
- Add a `Procfile`: `web: uvicorn api:app --host 0.0.0.0 --port $PORT`
- Set environment variables for API credentials

**Option 2: Static Frontend + Serverless Backend**
- Host HTML/CSS/JS on Netlify/Vercel
- Deploy API to AWS Lambda or similar
- Update `API_URL` in `app.js`

**Option 3: Docker**
```dockerfile
FROM python:3.10-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install -r requirements.txt
COPY . .
CMD ["uvicorn", "api:app", "--host", "0.0.0.0", "--port", "8000"]
```

## Performance

- API response time: 1-3 seconds (depends on Imagga)
- Max image size: 10MB
- Supported formats: JPEG, PNG, WebP, BMP
- Images automatically resized to 1024px max dimension

## Development Notes

This web demo follows the principles from `instruction.txt`:

✅ **APK is Sacred** - No changes to Android app  
✅ **Same Technology** - Uses Imagga API like the Android app  
✅ **Validation Parity** - Same image produces same results on both platforms  
✅ **Performance Guardrails** - File size limits, timeouts, error handling  

## Troubleshooting

**API not responding:**
- Make sure `python api.py` is running
- Check that port 8000 is available
- Look for errors in the console

**CORS errors:**
- The API includes CORS middleware
- If using a different frontend URL, update CORS settings in `api.py`

**API rate limits:**
- Imagga free tier has usage limits
- Consider upgrading your Imagga plan for production use

**Wrong predictions:**
- Imagga API results may vary from TensorFlow Lite
- Both are valid - they use different models
- Imagga generally provides more detailed, accurate results

## Next Steps

Once the demo is working:

1. **Test thoroughly** - Upload various images and verify results
2. **Add to portfolio** - Link from your portfolio site
3. **Optional enhancements** (future):
   - Camera capture
   - Image history
   - Share results
   - PWA offline support

## License

Same license as the ObjectID Android app (MIT)
