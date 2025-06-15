# Object Identification App

An Android application that uses the Imagga API to identify objects in images. The app captures or selects an image, sends it to the Imagga API, and displays the identified objects with their confidence scores.

## Features

- Capture images using the device camera
- Select images from the gallery
- Analyze images using the Imagga API
- Display identified objects with confidence scores
- Clean and modern Material Design UI

## Prerequisites

- Android Studio Giraffe or later
- Android SDK 33
- Kotlin 1.8.0 or later
- An API key from [Imagga](https://imagga.com/)

## Setup

1. Clone the repository:
   ```bash
   git clone <repository-url>
   ```

2. Open the project in Android Studio

3. Create a `secrets.properties` file in the root directory with your Imagga API credentials:
   ```properties
   IMAGGA_API_KEY=your_api_key_here
   IMAGGA_API_SECRET=your_api_secret_here
   ```

4. Sync the project with Gradle files

5. Build and run the app on an emulator or physical device

## Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/example/objectid/
│   │   │   ├── ImaggaApiService.kt  # Handles API communication
│   │   │   └── MainActivity.kt      # Main UI and logic
│   │   └── res/                     # Layouts, strings, and other resources
│   └── test/                        # Unit tests
├── build.gradle.kts                  # App-level build configuration
└── ...
```

## Dependencies

- AndroidX Core KTX
- AndroidX AppCompat
- Material Design Components
- ViewModel and LiveData
- CameraX
- OkHttp for network requests
- Gson for JSON parsing
- Coil for image loading

## API Key Security

API keys are stored in a `secrets.properties` file that is not committed to version control. The app uses the `local.properties` file to access these values during build time.

## Contributing

1. Fork the repository
2. Create a new branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [Imagga](https://imagga.com/) for their image recognition API
- Android Developer documentation
- Material Design guidelines
