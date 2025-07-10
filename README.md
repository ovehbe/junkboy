# Junkboy - Privacy-First SMS Filter App

A privacy-respecting Android app that filters SMS messages entirely on-device using TensorFlow Lite and customizable filters.

## Features

🔒 **Privacy-First**: All processing happens on-device, no internet dependency
🤖 **ML Classification**: TensorFlow Lite model for intelligent SMS categorization
🎯 **Custom Filters**: Regex and keyword-based filtering
📱 **Modern UI**: Jetpack Compose interface
💾 **Local Storage**: Room database for offline data management

## SMS Categories

- **General**: Regular personal messages
- **Promotion**: Marketing and promotional content
- **Notification**: System and app notifications
- **Transaction**: Banking and payment messages
- **Junk**: Spam and unwanted messages

## Tech Stack

- **SMS Interception**: BroadcastReceiver
- **ML Classification**: TensorFlow Lite (.tflite)
- **Filtering**: Kotlin Regex + Keywords
- **UI**: Jetpack Compose
- **Database**: Room SQLite
- **Background**: WorkManager/Service
- **Notifications**: NotificationManager

## Permissions

- `RECEIVE_SMS`: Intercept incoming SMS
- `READ_SMS`: Access SMS content
- `FOREGROUND_SERVICE`: Background processing

## Project Structure

```
app/src/main/
├── java/com/junkboy/
│   ├── smsreceiver/
│   │   └── SmsReceiver.kt
│   ├── classifier/
│   │   └── SmsClassifier.kt
│   ├── filters/
│   │   └── CustomFilter.kt
│   ├── database/
│   │   ├── FilteredMessage.kt
│   │   ├── FilteredMessageDao.kt
│   │   └── AppDatabase.kt
│   ├── ui/
│   │   ├── MainActivity.kt
│   │   └── compose/
│   └── utils/
├── assets/
│   ├── sms_model.tflite
│   └── labels.txt
└── AndroidManifest.xml
```

## Installation

1. Clone the repository
2. Open in Android Studio
3. Grant SMS permissions when prompted
4. The app will automatically start filtering incoming SMS

## Configuration

- Toggle "Under Attack Mode" for aggressive filtering
- View filtered messages by category
- Manual override for individual messages
- Customize regex and keyword filters

## Model Training

If training your own model:
1. Prepare dataset with format: `"message", "category"`
2. Train using TensorFlow + TextVectorization + LSTM
3. Export to .tflite format
4. Place in `assets/` directory

## License

Open source - feel free to contribute! 