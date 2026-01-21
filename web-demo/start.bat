@echo off
echo ========================================
echo   ObjectID Web Demo Launcher
echo ========================================
echo.

echo [1/3] Checking Python installation...
python --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Python is not installed or not in PATH
    echo Please install Python 3.8 or later from python.org
    pause
    exit /b 1
)
echo Python found!
echo.

echo [2/3] Installing dependencies...
pip install -r requirements.txt
if errorlevel 1 (
    echo ERROR: Failed to install dependencies
    pause
    exit /b 1
)
echo Dependencies installed!
echo.

echo [3/3] Starting API server...
echo.
echo ========================================
echo   API Server Running
echo   URL: http://localhost:8000
echo   Docs: http://localhost:8000/docs
echo ========================================
echo.
echo Open index.html in your browser to use the demo
echo Press Ctrl+C to stop the server
echo.

python api.py
