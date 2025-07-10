# 🎉 Junkboy SMS Filter v1.0.0 - Initial Release

**The ultimate AI-powered SMS filtering solution for Android!**

*Inspired by [Junkman: A.I. SMS Spam Blocker](https://apps.apple.com/tr/app/junkman-a-i-sms-spam-blocker/id1591815272) for iOS, Junkboy brings similar intelligent SMS filtering to Android with enhanced features, open-source transparency, and modern Material Design 3 interface.*

---

## 🌟 What's New

### ✨ **Initial Release Features**

#### 🤖 **Intelligent SMS Classification**
- **AI-Powered Filtering**: Advanced TensorFlow Lite machine learning model for automatic SMS categorization
- **Smart Hierarchy System**: ML classification takes precedence with rule-based enhancement for optimal accuracy
- **Real-time Processing**: Instant classification of incoming messages with reliable background service
- **5 Message Categories**: General, Promotion, Notification, Transaction, and Junk

#### 🔔 **Granular Notification Control**
- **Category-Specific Notifications**: Choose exactly which message types to receive notifications for
- **Smart Notification Channels**: Separate Android notification channels with appropriate priority levels
- **Default Settings**: Sensible defaults (General ✅, Notifications ✅, Transactions ✅, Promotions ❌)
- **Quick Actions**: Allow senders or mark messages as read directly from notifications

#### 🛠️ **Advanced Customization**
- **Custom Keywords**: Add your own spam indicators and important terms
- **Regex Patterns**: Advanced pattern matching for power users
- **Allowed Senders**: Whitelist trusted contacts to bypass all filtering
- **Under Attack Mode**: Enhanced protection during spam waves
- **Auto-Delete Junk**: Remove spam from system SMS database (requires default SMS app)

#### 📱 **Modern User Experience**
- **Material Design 3**: Beautiful, intuitive interface built with Jetpack Compose
- **Dashboard Overview**: Quick stats and system status at a glance
- **Filter Testing**: Built-in testing screen to verify filter effectiveness
- **Dark/Light Theme**: Automatic theme switching based on system preference

#### 📊 **Comprehensive Analytics**
- **Real-time Statistics**: Daily and total message filtering metrics
- **Category Breakdown**: Detailed analysis of message distribution
- **Filter Effectiveness**: Performance metrics showing protection level
- **Export Functionality**: CSV export of all filtered messages for analysis

#### 🗃️ **Data Management**
- **Local Database**: Secure, offline storage using Room database
- **Message Archive**: Complete history of all filtered messages with search
- **Bulk Processing**: Process existing SMS messages with current filter settings
- **Data Export**: Full data portability and backup capabilities

---

## 🔒 **Privacy & Security**

- **🔐 Local Processing**: All SMS analysis happens on-device - no cloud dependency
- **🚫 No Tracking**: Zero analytics, telemetry, or user tracking
- **💾 Encrypted Storage**: Local database uses Android's built-in encryption
- **🔓 Open Source**: Full source code available for security auditing

---

## 📱 **System Requirements**

- **Android Version**: 8.0 (API level 26) or higher
- **Storage Space**: ~50MB for app and ML model
- **Permissions**: SMS (reading/receiving), Notifications, Storage (for export)
- **RAM**: Minimum 2GB recommended for optimal performance

---

## 🚀 **Installation Instructions**

### **Option 1: Direct APK Installation**
1. Download `junkboy-v1.0-release.apk` from this release
2. Enable "Install from Unknown Sources" in Android Settings
3. Install the APK file
4. Grant required permissions when prompted
5. Configure your filtering preferences

### **Option 2: Build from Source**
```bash
git clone https://github.com/ovehbe/junkboy.git
cd junkboy
./gradlew assembleRelease
```

### **Initial Setup**
1. **Grant Permissions**: Allow SMS reading, notifications, and storage access
2. **Configure Notifications**: Choose which categories you want notifications for
3. **Test Filters**: Use the built-in testing screen to verify effectiveness
4. **Optional**: Set as default SMS app for auto-delete functionality

---

## 🔧 **Technical Highlights**

- **🧠 TensorFlow Lite**: On-device AI model (2.5MB) for fast, private classification
- **⚡ Kotlin Coroutines**: Efficient async processing without blocking UI
- **🏗️ Jetpack Compose**: Modern, declarative UI built with Material Design 3
- **💾 Room Database**: Robust local storage with full-text search capabilities
- **🔄 Background Service**: Reliable SMS processing with foreground service

---

## 📊 **Performance Metrics**

- **⚡ Classification Speed**: < 100ms per message on average device
- **🧠 ML Model Size**: 2.5MB (lightweight for mobile)
- **💾 Memory Usage**: < 50MB RAM during active filtering
- **🔋 Battery Impact**: Minimal - optimized background processing
- **📱 APK Size**: 29MB (includes ML model and all dependencies)

---

## 🐛 **Known Issues**

- **Default SMS App**: Cannot be set as default SMS app on sideloaded installations due to Android security policies (not a bug - this is intentional system behavior)
- **TensorFlow Warnings**: Harmless namespace warnings during build (doesn't affect functionality)
- **Android 14**: Some UI elements may appear differently due to Android 14 theming changes

---

## 🔮 **Coming Soon (v1.1)**

- **Enhanced ML Model**: Improved accuracy with larger training dataset
- **Cloud Backup**: Optional encrypted cloud sync for settings (privacy-first)
- **Scheduling**: Time-based filtering rules and quiet hours
- **Widgets**: Home screen widgets for quick stats
- **Multiple Languages**: Localization for non-English SMS content

---

## 📞 **Support & Feedback**

- **🐛 Bug Reports**: [GitHub Issues](https://github.com/ovehbe/junkboy/issues)
- **💡 Feature Requests**: [GitHub Discussions](https://github.com/ovehbe/junkboy/discussions)
- **⭐ Leave a Star**: Help others discover Junkboy on GitHub!

---

## 🙏 **Acknowledgments**

Special thanks to:
- **[Junkman: A.I. SMS Spam Blocker](https://apps.apple.com/tr/app/junkman-a-i-sms-spam-blocker/id1591815272)** by **Kerem Erkan** - This project was heavily inspired by Junkman's excellent approach to AI-powered SMS filtering on iOS. Kerem's pioneering work in on-device ML classification and privacy-first design served as the foundation for bringing similar functionality to Android with enhanced features and open-source transparency.
- **Cursor AI** - This project was built with the assistance of Cursor AI, which provided invaluable support in architecting the codebase, implementing complex features, and maintaining best practices throughout development.
- **TensorFlow Team** for the excellent Lite framework
- **Android Team** for Jetpack Compose and Material Design 3
- **Open Source Community** for inspiration and best practices

---

## 🔐 **Security Information**

**SHA256 Checksum**: `31c738e3aa4688175fb92f416fefad3318a9bc6ccf6cacd803c28f454225c892`

**APK Details**:
- Package: `com.ovehbe.junkboy`
- Version Code: 1
- Version Name: 1.0
- Min SDK: 24 (Android 7.0)
- Target SDK: 34 (Android 14)
- Signed: ✅ APK Signature Scheme v2

---

**🎯 Ready to take control of your SMS inbox? Download Junkboy v1.0 now!** 