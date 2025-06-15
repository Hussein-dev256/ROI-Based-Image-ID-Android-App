# ObjectID - Object Recognition Application

ObjectID is an Android application that combines cloud-based and on-device machine learning to identify objects in images. The app allows users to capture photos, annotate objects, and get instant recognition results using either the Imagga API or TensorFlow Lite.

## Features

- Capture images using device camera
- Select images from gallery
- Multiple recognition modes:
  - **Fast**: On-device TensorFlow Lite
  - **Accurate**: Imagga API (cloud-based)
  - **Auto**: Falls back to on-device if API fails
- Object detection with bounding boxes
- Confidence scoring for detected objects
- Save and share recognition results
- Dark/Light theme support
- Smooth animations and transitions
- Optimized for performance and battery life
- Clean and modern Material Design UI

## Prerequisites

- Android Studio Giraffe or later
- Android SDK 33
- Kotlin 1.8.0 or later
- An API key from [Imagga](https://imagga.com/) for cloud-based recognition
- Android device or emulator with camera support

## Getting Started

1. Clone the repository:
   ```bash
   git clone https://github.com/Hussein-dev256/ROI-Based-Image-ID-Android-App.git
   ```

2. Open the project in Android Studio

3. Create a `secrets.properties` file in the project root with your Imagga API credentials:
   ```properties
   IMAGGA_API_KEY=your_api_key_here
   IMAGGA_API_SECRET=your_api_secret_here
   ```

4. Sync the project with Gradle files

5. Build and run the app on an emulator or physical device

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

## API Key Security

API keys are stored in a `secrets.properties` file that is not committed to version control. The app uses the `local.properties` file to access these values during build time.

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [Imagga](https://imagga.com/) for their image recognition API
- TensorFlow for the on-device machine learning model
- Android Developer documentation
- Material Design guidelines

## Acknowledgments

- Imagga API for cloud-based image recognition
- TensorFlow Lite for on-device machine learning
- AndroidX libraries for modern Android development
