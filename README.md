# 🛡️ Junkboy SMS Filter

**An intelligent, AI-powered SMS filtering application for Android that automatically categorizes and manages your text messages using machine learning and customizable rules.**

*Inspired by [Junkman](https://apps.apple.com/tr/app/junkman-a-i-sms-spam-blocker/id1591815272) for iOS, bringing similar AI-powered SMS filtering capabilities to Android with enhanced features and open-source transparency.*

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
[![Material Design 3](https://img.shields.io/badge/UI-Material%20Design%203-purple.svg)](https://m3.material.io)
[![TensorFlow Lite](https://img.shields.io/badge/AI-TensorFlow%20Lite-orange.svg)](https://tensorflow.org/lite)

---

## 🌟 Features

### 🤖 **Intelligent SMS Classification**
- **AI-Powered Filtering**: Uses TensorFlow Lite machine learning models to automatically categorize SMS messages
- **Smart Hierarchy System**: ML classification takes precedence with rule-based enhancement for optimal accuracy
- **Multi-Method Filtering**: Combines ML, keyword filtering, and regex patterns for comprehensive coverage
- **Real-time Processing**: Instant classification of incoming messages with foreground service reliability

### 📱 **Modern User Experience**
- **Material Design 3 UI**: Beautiful, intuitive interface built with Jetpack Compose
- **Dashboard Overview**: Quick stats and system status at a glance
- **Message Categories**: Automatic sorting into General, Promotion, Notification, Transaction, and Junk
- **Filter Testing**: Built-in testing screen to verify filter effectiveness before deployment

### 🔔 **Granular Notification Control**
- **Category-Specific Notifications**: Choose which message types to receive notifications for
- **Smart Notification Channels**: Separate Android notification channels for each category with appropriate priority levels
- **Blocked Message Alerts**: Optional notifications for filtered junk messages
- **Notification Actions**: Quick actions to allow senders or mark messages as read

### 🛠️ **Advanced Customization**
- **Custom Keywords**: Add your own spam indicators and important terms
- **Regex Patterns**: Advanced pattern matching for power users
- **Allowed Senders**: Whitelist trusted contacts to bypass all filtering
- **Under Attack Mode**: Enhanced protection during spam waves
- **Auto-Delete Junk**: Automatically remove junk messages from system SMS database (requires default SMS app)

### 📊 **Comprehensive Analytics**
- **Real-time Statistics**: Daily and total message filtering metrics
- **Category Breakdown**: Detailed analysis of message distribution
- **Filter Effectiveness**: Performance metrics showing how well your filters work
- **Export Functionality**: CSV export of all filtered messages for analysis

### 🗃️ **Data Management**
- **Local Database**: Secure, offline storage using Room database
- **Message Archive**: Complete history of all filtered messages
- **Bulk Processing**: Process existing SMS messages with current filter settings
- **Data Export/Import**: Full data portability and backup capabilities

---

## 🏗️ Technical Architecture

### **Core Technologies**
- **Language**: Kotlin with Coroutines for async processing
- **UI Framework**: Jetpack Compose with Material Design 3
- **Database**: Room persistence library with SQLite backend
- **ML Engine**: TensorFlow Lite for on-device AI inference
- **Architecture**: MVVM pattern with Repository pattern for data access

### **AI Classification System**
- **TensorFlow Lite Model**: Custom-trained SMS classification model
- **Vocabulary Processing**: Tokenization and text preprocessing pipeline
- **Fallback Classification**: Rule-based backup for model-unavailable scenarios
- **Confidence Scoring**: Probabilistic classification with confidence metrics

### **SMS Integration**
- **Broadcast Receivers**: Real-time SMS interception and processing
- **Foreground Service**: Reliable background processing with system notifications
- **Default SMS App Support**: Full SMS app capabilities for enhanced features
- **Permission Management**: Proper handling of SMS, notification, and storage permissions

---

## 🚀 Installation & Setup

### **Prerequisites**
- Android 8.0 (API level 26) or higher
- ~50MB storage space for app and ML model
- SMS and notification permissions

### **Installation Steps**

1. **Download and Install**
   ```bash
   # Clone the repository
   git clone https://github.com/ovehbe/junkboy.git
   cd junkboy
   
   # Build the APK
   ./gradlew assembleDebug
   
   # Install on connected device
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

2. **Grant Permissions**
   - **SMS Permissions**: Allow reading and receiving SMS messages
   - **Notification Permission**: Enable categorized notifications
   - **Storage Permission**: For data export functionality

3. **Initial Configuration**
   - Open the app and complete the setup wizard
   - Configure your notification preferences
   - Add custom keywords or allowed senders if desired
   - Test the filtering system with the built-in testing tool

### **Optional: Default SMS App Setup**
For enhanced features like auto-delete junk messages:
1. Go to Android Settings → Apps → Default Apps → SMS app
2. Select "Junkboy SMS Filter"
3. Enable auto-delete in Junkboy settings

---

## 📖 Usage Guide

### **Getting Started**
1. **Dashboard**: Monitor daily filtering statistics and system status
2. **Messages**: Browse filtered messages by category with search and filtering
3. **Settings**: Customize filtering methods, notifications, and preferences
4. **Statistics**: Analyze filtering performance and message patterns
5. **Test Filter**: Verify filter behavior before applying to real messages

### **Customizing Filters**

#### **AI Classification**
- Enabled by default for optimal accuracy
- Automatically categorizes messages into 5 categories
- Self-improving through confidence-based decisions

#### **Keyword Filtering**
```
Settings → Custom Keywords → Add
Examples:
- "URGENT OFFER" (promotional spam)
- "Bank Alert" (transaction indicator)
- "Verify your" (notification pattern)
```

#### **Regex Patterns**
```
Settings → Custom Regex Patterns → Add
Examples:
- \b\d{4}\s?\d{4}\s?\d{4}\s?\d{4}\b (credit card numbers)
- (?i)click\s+here (case-insensitive "click here")
- \$\d+\.?\d*\s?(off|discount) (discount offers)
```

#### **Notification Preferences**
1. **Global Settings**: Enable/disable all filtered or blocked notifications
2. **Category-Specific**: Choose which categories to receive notifications for
   - ✅ General Messages (personal/important)
   - ✅ Notifications (codes, alerts)
   - ✅ Transactions (banking, payments)
   - ❌ Promotions (marketing, sales)

### **Managing Allowed Senders**
- Add trusted contacts to bypass all filtering
- Supports phone numbers and sender names
- Useful for important business contacts or family members

---

## 🔍 Key Features Deep Dive

### **Smart Classification Hierarchy**
Junkboy uses an intelligent multi-layered approach:

1. **ML First**: TensorFlow Lite model provides primary classification
2. **Rule Enhancement**: Keyword/regex filters enhance or override ML when confidence is high
3. **Confidence Weighting**: Higher confidence results take precedence
4. **FilterType Preservation**: UI always shows "ML Classification" when ML is enabled

### **Notification System Architecture**
- **Separate Channels**: Each category has its own Android notification channel
- **Priority Mapping**: 
  - Transactions: HIGH (banking alerts)
  - General/Notifications: DEFAULT (personal messages)
  - Promotions: LOW (marketing)
  - Blocked: MIN (spam notifications)
- **Smart Actions**: Context-appropriate quick actions per category

### **Under Attack Mode**
Enhanced protection during spam waves:
- Stricter filtering thresholds
- Expanded junk keyword detection
- Automatic temporary rule activation
- Ideal for dealing with spam campaigns

---

## 📊 Message Categories

| Category | Description | Examples | Default Notifications |
|----------|-------------|----------|---------------------|
| **General** | Personal and uncategorized messages | Friends, family, personal | ✅ Enabled |
| **Promotion** | Marketing and sales messages | Offers, discounts, ads | ❌ Disabled |
| **Notification** | System alerts and codes | OTP, delivery updates, alerts | ✅ Enabled |
| **Transaction** | Banking and payment messages | Bank alerts, payment confirmations | ✅ Enabled |
| **Junk** | Spam and unwanted messages | Scams, unwanted marketing | ❌ Disabled |

---

## 🛠️ Development

### **Project Structure**
```
app/src/main/java/com/ovehbe/junkboy/
├── classifier/          # AI SMS classification engine
│   └── SmsClassifier.kt
├── database/           # Room database entities and DAOs
│   ├── AppDatabase.kt
│   ├── FilteredMessage.kt
│   └── ...
├── filters/            # Custom filtering logic
│   └── CustomFilter.kt
├── service/            # Background SMS processing
│   ├── SmsFilterService.kt
│   └── SmsSendService.kt
├── smsreceiver/        # SMS broadcast receivers
│   └── SmsReceiver.kt
├── ui/                 # Jetpack Compose UI components
│   ├── compose/
│   │   ├── JunkboyApp.kt
│   │   └── screens/
│   └── theme/
└── utils/              # Helper utilities and managers
    ├── NotificationHelper.kt
    ├── PreferencesManager.kt
    └── ...
```

### **Building from Source**
```bash
# Prerequisites
# - Android Studio Arctic Fox or later
# - Android SDK 26+
# - Kotlin 1.8+

# Clone and build
git clone https://github.com/ovehbe/junkboy.git
cd junkboy
./gradlew assembleDebug

# Run tests
./gradlew test

# Generate release build
./gradlew assembleRelease
```

### **ML Model Information**
- **Model File**: `app/src/main/assets/sms_model.tflite`
- **Labels**: `app/src/main/assets/labels.txt`
- **Vocabulary**: `app/src/main/assets/vocabulary.txt`
- **Model Info**: `app/src/main/assets/model_info.txt`

---

## 🔒 Privacy & Security

### **Data Handling**
- **Local Processing**: All SMS analysis happens on-device
- **No Cloud Sync**: Messages never leave your device
- **Encrypted Storage**: Local database uses Android's built-in encryption
- **Minimal Permissions**: Only requests necessary permissions for functionality

### **Open Source**
- **Transparent Code**: Full source code available for security auditing
- **No Tracking**: No analytics, telemetry, or user tracking
- **Community Driven**: Open to contributions and security reviews

---

## 🤝 Contributing

We welcome contributions! Here's how you can help:

### **Types of Contributions**
- 🐛 **Bug Reports**: Report issues via GitHub Issues
- 💡 **Feature Requests**: Suggest new functionality
- 🔧 **Code Contributions**: Submit pull requests
- 📖 **Documentation**: Improve README, code comments
- 🌐 **Translations**: Add support for new languages

### **Development Guidelines**
1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes (`git commit -m 'Add amazing feature'`)
4. **Push** to the branch (`git push origin feature/amazing-feature`)
5. **Open** a Pull Request

### **Code Style**
- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add comments for complex logic
- Include unit tests for new features

---

## 📝 Changelog

### **v1.0.0** (Current)
- ✨ **Initial Release**: Complete SMS filtering application
- 🤖 **AI Classification**: TensorFlow Lite integration
- 🎨 **Material Design 3**: Modern Jetpack Compose UI
- 🔔 **Smart Notifications**: Category-specific notification controls
- 📊 **Analytics Dashboard**: Comprehensive filtering statistics
- 🛠️ **Custom Filters**: Keywords, regex, and allowed senders
- 🗃️ **Data Management**: Export, import, and bulk processing
- 🚀 **Performance**: Optimized background processing

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

Special thanks to:
- **[Junkman: A.I. SMS Spam Blocker](https://apps.apple.com/tr/app/junkman-a-i-sms-spam-blocker/id1591815272)** by **Kerem Erkan** - This project was heavily inspired by Junkman's excellent approach to AI-powered SMS filtering on iOS. Kerem's pioneering work in on-device ML classification and privacy-first design served as the foundation for bringing similar functionality to Android.
- **Cursor AI** - This project was built with the assistance of Cursor AI, which provided invaluable support in architecting the codebase, implementing complex features, and maintaining best practices throughout development.
- **TensorFlow Team** for the excellent Lite framework
- **Android Team** for Jetpack Compose and Material Design 3
- **Kotlin Team** for the amazing programming language
- **Open Source Community** for inspiration and best practices

---

## 📞 Support

- **GitHub Issues**: [Report bugs or request features](https://github.com/ovehbe/junkboy/issues)
- **Discussions**: [Community discussions and questions](https://github.com/ovehbe/junkboy/discussions)

---

**⭐ If you find Junkboy useful, please consider starring the repository to help others discover it!** 