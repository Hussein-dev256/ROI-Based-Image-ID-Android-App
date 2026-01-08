# ObjectID - Object Recognition Application

ObjectID is an Android application that combines cloud-based and on-device machine learning to identify objects in images. The app allows users to capture photos, annotate objects, and get instant recognition results using either the Imagga API or TensorFlow Lite.

## Features

- Capture photos using device camera
- Select images from gallery
- Draw annotations around objects of interest
- Real-time object recognition using:
  - Imagga Cloud API (primary)
  - TensorFlow Lite (fallback)
- Confidence scores for recognition results
- Error handling and retry mechanisms
- Efficient image processing and memory management

## Prerequisites

- Android 5.0 (API level 21) or higher
- Camera permission
- Internet connection (for Imagga API)
- Android Studio Arctic Fox or newer

## Getting Started

1. Clone the repository
2. Open the project in Android Studio
3. Build and run the app on an Android device or emulator

## Project Structure

```
ObjectID/
├── app/                               # Main application module
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/objectid/
│   │   │   │   ├── MainActivity.kt           # Main UI controller
│   │   │   │   ├── SplashActivity.kt         # App startup activity
│   │   │   │   ├── ObjectIDApplication.kt    # Application class for initialization
│   │   │   │   ├── RecognitionViewModel.kt   # ViewModel for recognition logic
│   │   │   │   ├── AnnotationView.kt         # Custom view for object annotation
│   │   │   │   ├── ImageUtils.kt             # Image processing utilities
│   │   │   │   │
│   │   │   │   ├── Object Recognition/
│   │   │   │   │   ├── TFLiteObjectClassifier.kt    # TensorFlow Lite classifier
│   │   │   │   │   ├── SimpleTFLiteClassifier.kt    # Fallback classifier
│   │   │   │   │   ├── ImaggaObjectClassifier.kt    # Imagga API classifier
│   │   │   │   │   └── ImaggaApiService.kt          # Imagga REST API client
│   │   │   │
│   │   │   ├── res/                   # Android resources
│   │   │   │   ├── layout/            # UI layout files
│   │   │   │   ├── values/            # Strings, colors, styles
│   │   │   │   ├── drawable/          # Images and icons
│   │   │   │   └── mipmap/            # App icons
│   │   │   │
│   │   │   ├── assets/                # Asset files (ML models)
│   │   │   │   └── MobileNet-v3-Large-Quantized.tflite  # TF Lite model
│   │   │   │
│   │   │   └── AndroidManifest.xml    # App configuration
│   │
│   └── build.gradle.kts               # App-level build configuration
│
├── gradle/                            # Gradle configuration
└── build.gradle.kts                   # Project-level build configuration
```

## Using Imagga API

To use the Imagga API for better recognition results:

1. Sign up at https://imagga.com/
2. Get your API key and secret from the dashboard
3. Replace the placeholder credentials in `ImaggaApiService.kt`:
   ```kotlin
   private const val API_KEY = "YOUR_API_KEY_HERE"
   private const val API_SECRET = "YOUR_API_SECRET_HERE"
   ```

## Building the Project

1. Open Android Studio
2. Select "Open an Existing Android Studio Project"
3. Navigate to the project directory and select it
4. Wait for Gradle to sync
5. Build and run the app

## Key Technologies

- TensorFlow Lite
- Imagga API
- Android Architecture Components (ViewModel, LiveData)
- Kotlin Coroutines
- OkHttp
- Gson

## Error Handling

The app implements several error handling mechanisms:

1. Multiple classifier initialization attempts
2. Fallback from Imagga API to TensorFlow Lite
3. Retry mechanisms for failed recognition
4. Memory leak prevention
5. Network timeout handling

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details

## Acknowledgments

- Imagga API for cloud-based image recognition
- TensorFlow Lite for on-device machine learning
- AndroidX libraries for modern Android development
