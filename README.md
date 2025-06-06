# Urban Safety App

A comprehensive personal safety application for urban environments. This app is designed to help users navigate safely, trigger emergency responses, and monitor health data for automatic SOS.

## Features

- **Emergency SOS**: Trigger emergency alerts with manual button, voice activation or health triggers
- **Safe Routes**: Find safe navigation paths avoiding high-risk areas
- **Wearable Integration**: Connect with wearable devices to monitor health metrics and detect emergencies
- **Emergency Contacts**: Set up trusted contacts to receive alerts and location data during emergencies
- **Travel Check-In**: Mark destinations and notify contacts when safely arrived
- **Safety Score**: View safety scores for different areas based on incident reports
- **Community Helpers**: Find nearby verified users who can assist in emergencies
- **Google Sign-In**: Seamless authentication using Google accounts

## Tech Stack

### Frontend (Android Native)
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Maps and Location**: OpenStreetMap with OSMDroid library
- **Routing/Navigation**: OSRM (Open Source Routing Machine)
- **Authentication**: Firebase Authentication with Google Sign-In
- **Notifications**: Firebase Cloud Messaging
- **Voice Activation**: Android Speech Recognizer
- **Local Storage**: Room Database
- **Wearable Support**: Wear OS SDK + Google Fit API

### Backend
- **Server**: Firebase Realtime Database
- **Authentication**: Firebase Authentication
- **Hosting**: Firebase Hosting
- **Realtime Communication**: Firebase Realtime Database
- **Database**: Firebase Realtime Database/Firestore

## Setup Instructions

### Prerequisites
- Android Studio latest version
- Android SDK 24+
- Firebase account

### Firebase Setup
1. Create a Firebase project at [Firebase Console](https://console.firebase.google.com/)
2. Add an Android app to your Firebase project
   - Use package name: `com.example.urban_safety`
   - Download `google-services.json` file
   - Place it in the app directory
3. Configure Google Sign-In
   - Generate SHA-1 certificate fingerprint (in Android Studio: Gradle > Tasks > android > signingReport)
   - Add the SHA-1 fingerprint to your Firebase project settings
   - Download the updated google-services.json file and replace the existing one
   - Enable Google Sign-In in Firebase Authentication section
   
### Build and Run
1. Clone the repository
2. Open project in Android Studio
3. Sync Gradle files
4. Build and run on a device or emulator

## Firebase Configuration

The app requires the following Firebase products:
- Authentication (Email/Phone and Google Sign-In)
- Realtime Database
- Firestore
- Cloud Messaging
- App Distribution (optional for testing)

Enable these services in your Firebase console.

## Required Permissions

The app requires the following permissions:
- `ACCESS_FINE_LOCATION` - For precise location tracking
- `ACCESS_COARSE_LOCATION` - For approximate location
- `ACCESS_BACKGROUND_LOCATION` - For background location tracking
- `INTERNET` - For network connectivity
- `ACTIVITY_RECOGNITION` - For wearable integration
- `SEND_SMS` - For emergency SMS alerts
- `RECORD_AUDIO` - For voice command activation
- `BODY_SENSORS` - For health monitoring
- `FOREGROUND_SERVICE` - For ongoing location services

## Project Structure

- `app/src/main/java/com/example/urban_safety/`
  - `data/` - Data models, repositories, and local database
  - `services/` - Background services for location, health monitoring, etc.
  - `ui/` - UI components, screens and view models
    - `screens/` - Compose screen components
    - `viewmodels/` - ViewModels for UI state management
    - `theme/` - Application theme and styling
  - `util/` - Utility classes and extensions

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- OpenStreetMap for mapping data
- OSMDroid for Android map integration
- Firebase for backend services
- Google Sign-In for authentication #   u r b a n _ s a f t e y  
 