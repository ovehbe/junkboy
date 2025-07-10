# Junkboy SMS Filter App - Project Summary

## 🎯 Overview

Junkboy is a privacy-first Android SMS filtering app that automatically categorizes and blocks unwanted messages using a combination of machine learning and rule-based filtering. All processing happens on-device, ensuring user privacy while providing effective spam protection.

## ✨ Key Features

### 🔒 Privacy-First Architecture
- **100% On-Device Processing**: No data leaves your device
- **No Internet Dependency**: Works completely offline
- **Local Storage**: All data stored locally using Room database
- **Minimal Permissions**: Only essential SMS and notification permissions

### 🤖 Advanced Filtering
- **AI Classification**: TensorFlow Lite model for intelligent categorization
- **Custom Filters**: User-defined keywords and regex patterns
- **Under Attack Mode**: Aggressive filtering for spam waves
- **Multi-Language Support**: English and Turkish spam detection

### 📱 Modern UI
- **Material Design 3**: Modern, intuitive interface
- **Jetpack Compose**: Reactive UI with smooth animations
- **Dark Mode Support**: Automatic system theme adaptation
- **Accessibility**: Full accessibility support

### 📊 Comprehensive Analytics
- **Real-Time Statistics**: Daily and total filtering metrics
- **Category Breakdown**: Detailed message categorization
- **Performance Metrics**: Filter effectiveness tracking
- **Historical Data**: Long-term usage analytics

## 🏗️ Architecture

### Core Components

1. **SMS Receiver (`SmsReceiver`)**
   - Intercepts incoming SMS messages
   - High-priority broadcast receiver
   - Launches background service for processing

2. **Filter Service (`SmsFilterService`)**
   - Foreground service for reliable processing
   - Combines ML and rule-based filtering
   - Manages notifications and database storage

3. **ML Classifier (`SmsClassifier`)**
   - TensorFlow Lite model integration
   - Text preprocessing and tokenization
   - Fallback to rule-based filtering

4. **Custom Filters (`CustomFilter`)**
   - Keyword and regex pattern matching
   - Built-in spam detection rules
   - User-customizable filter lists

5. **Database Layer (`Room`)**
   - Local SQLite storage
   - Message entities and DAOs
   - Automatic type conversions

6. **UI Layer (`Jetpack Compose`)**
   - Modern declarative UI
   - Navigation between screens
   - Real-time data binding

### Message Categories

- **General**: Regular personal messages
- **Promotion**: Marketing and promotional content
- **Notification**: System and app notifications  
- **Transaction**: Banking and payment messages
- **Junk**: Spam and unwanted messages (blocked)

### Filter Types

- **ML Classification**: AI-powered categorization
- **Keyword Filter**: Word/phrase-based blocking
- **Regex Filter**: Pattern-based filtering
- **User Rules**: Custom user-defined filters
- **Under Attack Mode**: Enhanced protection mode

## 🔧 Technical Implementation

### Technology Stack
- **Language**: Kotlin 100%
- **UI Framework**: Jetpack Compose
- **ML Framework**: TensorFlow Lite
- **Database**: Room SQLite
- **Architecture**: MVVM with Coroutines
- **Background**: Foreground Services + WorkManager

### Performance Optimizations
- **Lazy Loading**: UI components load on demand
- **Coroutines**: Non-blocking background processing
- **Compiled Regex**: Pre-compiled patterns for speed
- **Memory Efficient**: Minimal memory footprint
- **Battery Optimized**: Efficient background processing

### Security Features
- **No Network Access**: All processing local
- **Secure Storage**: Encrypted shared preferences option
- **Permission Minimal**: Only required permissions
- **Data Isolation**: App-specific storage only

## 📦 Project Structure

```
app/src/main/
├── java/com/junkboy/
│   ├── smsreceiver/         # SMS interception
│   │   └── SmsReceiver.kt
│   ├── service/             # Background processing
│   │   └── SmsFilterService.kt
│   ├── classifier/          # ML classification
│   │   └── SmsClassifier.kt
│   ├── filters/             # Rule-based filtering
│   │   └── CustomFilter.kt
│   ├── database/            # Data layer
│   │   ├── FilteredMessage.kt
│   │   ├── FilteredMessageDao.kt
│   │   ├── Converters.kt
│   │   └── AppDatabase.kt
│   ├── utils/               # Utilities
│   │   ├── PreferencesManager.kt
│   │   └── NotificationHelper.kt
│   └── ui/                  # User interface
│       ├── MainActivity.kt
│       ├── theme/
│       └── compose/
│           ├── JunkboyApp.kt
│           └── screens/
│               ├── DashboardScreen.kt
│               ├── MessagesScreen.kt
│               ├── SettingsScreen.kt
│               └── StatsScreen.kt
├── assets/                  # ML model files
│   ├── sms_model.tflite    # TensorFlow Lite model
│   ├── labels.txt          # Category labels
│   ├── vocabulary.txt      # Text vocabulary
│   └── model_info.txt      # Model documentation
└── res/                    # Android resources
    ├── drawable/           # Vector icons
    ├── values/             # Strings, themes
    └── xml/                # Configurations
```

## 🚀 Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 24+ (Android 7.0)
- Kotlin 1.9+

### Installation
1. Clone the repository
2. Open in Android Studio
3. Sync project with Gradle files
4. Run on device/emulator
5. Grant SMS permissions when prompted

### Custom ML Model
To use your own trained model:
1. Train a text classification model using TensorFlow
2. Convert to TensorFlow Lite format (.tflite)
3. Place in `app/src/main/assets/sms_model.tflite`
4. Update labels and vocabulary files accordingly

## 🎨 UI Screens

### 1. Dashboard
- Permission status and grant flow
- Today's filtering statistics
- Under Attack Mode toggle
- Quick actions and feature status

### 2. Messages
- View all filtered messages by category
- Category filter chips for navigation
- Message details with confidence scores
- Allow/block sender functionality

### 3. Settings
- Filter method toggles (AI, Keywords, Regex)
- Custom keyword and regex management
- Notification preferences
- Data management options

### 4. Statistics
- Comprehensive filtering analytics
- Daily and total statistics
- Category breakdown with percentages
- Filter effectiveness metrics

## 📱 User Experience

### Key User Flows

1. **First Launch**: Permission grant → Setup complete → Active filtering
2. **Message Filtering**: Incoming SMS → Background processing → Categorization → Notification (if blocked)
3. **Review Messages**: Dashboard → Messages → Category filter → Message details
4. **Customize Filters**: Settings → Add keywords/regex → Test filtering
5. **Monitor Performance**: Statistics → View analytics → Adjust settings

### Accessibility
- Screen reader support
- High contrast mode
- Large text support
- Keyboard navigation
- Voice commands ready

## 🔮 Future Enhancements

### Planned Features
- Export filtered messages
- Machine learning model updates
- Multi-language expansion
- Advanced statistics visualization
- Backup and restore functionality

### Potential Integrations
- Tasker automation support
- Wear OS companion app
- Widget for quick stats
- Share filter rules between devices

## 📄 License & Contributing

Open source project welcoming contributions. Key areas for improvement:
- ML model accuracy enhancement
- Additional language support
- UI/UX improvements
- Performance optimizations
- Testing coverage expansion

This app demonstrates modern Android development best practices while solving a real-world problem with privacy as the core principle. 