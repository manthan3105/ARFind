# ARFind
Here’s an updated version of the `README.md` file for your Java-based Android application:

---

# ARFind

ARFind is an augmented reality (AR) navigation tool built on Android that enhances your shopping experience by allowing you to search for products or locations using either image recognition or text-based input. The app uses a machine learning model hosted on Google Cloud and Google Maps API to identify products and guide users to nearby stores or specific destinations, displayed in real-time through an AR interface.

## Features

- **Image-based Product Search:**  
  Capture a picture of a product, and ARFind will classify the product using a model trained on Google Cloud. The app then displays nearby stores where you can find the product, leveraging Google Maps API.

- **Text-based Search:**  
  You can also type in the name of a product or destination to get relevant results. Google Maps API will retrieve nearby store locations or directions to the destination.

- **AR Navigation:**  
  The app will display the distance and direction to your destination through augmented reality, overlaying information onto the live camera feed.

## Getting Started

### Prerequisites

Before running the project, ensure you have the following:

- **Android Studio**  
  [Download Android Studio](https://developer.android.com/studio)

- **Google Cloud SDK**  
  Required for the image recognition model.  
  [Google Cloud Setup Guide](https://cloud.google.com/sdk/docs/install)

- **Google Maps API Key**  
  Required to integrate Google Maps services.  
  [Generate API Key](https://developers.google.com/maps/gmp-get-started)

- **ARCore SDK**  
  The app uses ARCore for augmented reality features.  
  [ARCore SDK for Android](https://developers.google.com/ar/develop)

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/manthan3105/ARFind.git
   cd ARFind
   ```

2. Open the project in Android Studio:
   - Launch Android Studio and select "Open an existing Android Studio project".
   - Navigate to the `ARFind` directory and open the project.

3. Add API keys:
   - **Google Cloud API key** for image classification.
   - **Google Maps API key** for location services.
   
   Add your keys to the `local.properties` file:
   ```properties
   GOOGLE_CLOUD_API_KEY=your_google_cloud_api_key
   GOOGLE_MAPS_API_KEY=your_google_maps_api_key
   ```

4. Build and run the project:
   - Select your preferred device/emulator in Android Studio and click "Run".

### Usage

- **Image Search:**
  1. Open the app and click on the camera icon to capture an image of the product.
  2. The app will use Google Cloud’s machine learning model to classify the product.
  3. Nearby stores will be displayed, and you can use AR to navigate to the store.

- **Text Search:**
  1. Enter the product name or destination into the search bar.
  2. The app will retrieve relevant stores or locations using Google Maps API.
  3. Navigate to your destination with AR-enhanced guidance.

### Project Structure

```bash
├── app/src/main/java/com/arfind        # Main Java code for the Android application
├── app/src/main/res                    # Resources (layouts, images, etc.)
├── app/src/main/AndroidManifest.xml    # App manifest file
├── app/build.gradle                    # Gradle build configuration
└── README.md                           # Project documentation
```

### Technologies Used

- **Java**  
  The core language for Android app development.
  
- **Google Cloud**  
  For product recognition and categorization.

- **Google Maps API**  
  For displaying stores and navigation.

- **ARCore**  
  For rendering AR navigation features.

### Future Enhancements

- Expand the product recognition database.
- Add multi-language support.
- Improve AR rendering to include more immersive features.

## Contributing

Contributions are welcome! Follow these steps to contribute:

1. Fork the repository.
2. Create a feature branch: `git checkout -b feature/your-feature-name`.
3. Commit your changes: `git commit -m 'Add some feature'`.
4. Push to the branch: `git push origin feature/your-feature-name`.
5. Open a pull request.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for more details.

---

This version reflects that the project is developed in Java using Android Studio and includes the GitHub link for the project. 
