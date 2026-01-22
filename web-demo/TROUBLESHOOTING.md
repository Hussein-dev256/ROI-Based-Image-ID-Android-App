# CRITICAL FIXES - ROI & CORS Issues

## Issue 1: CORS Error (BLOCKING API CALLS)

### Problem
You're accessing the app via `file://` protocol, which causes CORS errors when calling `http://localhost:8000`

### Solution - Use Local Server

**Open 2 terminals:**

**Terminal 1 - Backend API:**
```powershell
cd "C:\Users\HUSSEIN\Desktop\Projects\RESEARCH PROJECT\SYSTEM\web-demo"
python api.py
```

**Terminal 2 - Frontend Server:**
```powershell
cd "C:\Users\HUSSEIN\Desktop\Projects\RESEARCH PROJECT\SYSTEM\web-demo"
python -m http.server 8080
```

**Then open:** `http://localhost:8080` (NOT file://)

---

## Issue 2: ROI Box Not Moving

### Debug Check

Add this to browser console to verify:
```javascript
// Check if elements exist
console.log('Canvas:', document.getElementById('roiCanvas'));
console.log('ROI Box:', document.getElementById('roiSelector'));
console.log('Wrapper:', document.querySelector('.roi-workspace'));

// Check if app is initialized
console.log('App:', window.app || app);
```

### If Elements Missing
Refresh page (Ctrl + F5) to clear cache

### If setupROIInteraction Not Called
Check browser console for:
- "ROI elements not found" error
- Other JavaScript errors

---

## Quick Test Checklist

1. ✅ Backend running on port 8000?
   ```
   python api.py
   ```
   Should show: `Uvicorn running on http://0.0.0.0:8000`

2. ✅ Frontend running on port 8080?
   ```
   python -m http.server 8080
   ```
   Should show: `Serving HTTP on :: port 8080`

3. ✅ Open correct URL?
   - ❌ WRONG: `file:///C:/Users/...`
   - ✅ CORRECT: `http://localhost:8080`

4. ✅ .env file configured?
   ```
   IMAGGA_API_KEY=your_key
   IMAGGA_API_SECRET=your_secret
   ```

---

## Still Not Working?

### Check Browser Console
Press F12, look for errors in Console tab

### Common Errors:
- "CORS policy" → Use http://localhost:8080 NOT file://
- "Failed to fetch" → Backend not running
- "undefined" errors → Clear cache (Ctrl+F5)

### Nuclear Option - Full Reset:
```powershell
# Close all browsers
# Kill all Python servers
# Clear browser cache
# Start fresh:
python api.py
# New terminal:
python -m http.server 8080
# Open: http://localhost:8080
```
