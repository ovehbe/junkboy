# Junkboy Clone - Android SMS Filter App

## 项目概述
Android 短信过滤应用，支持 ML 分类、默认短信应用功能、通知监听。

## Gradle 配置

**Gradle 用户主页（缓存目录）位于**：
```
D:\code\cache\gradle
```

构建时必须设置 `GRADLE_USER_HOME` 环境变量指向该路径：
```bash
export GRADLE_USER_HOME="D:/code/cache/gradle"
```

Gradle 版本：**8.13**（已升级自 8.2，因本地 SSL 限制无法下载 8.2，`gradle/wrapper/gradle-wrapper.properties` 已更新）

## 构建 APK

> ⚠️ **重要**：由于环境 SSL 证书问题，Gradle wrapper 无法从远程下载。请使用本地已缓存的 Gradle 二进制文件直接运行。

```bash
# 方式一（推荐）：使用本地 Gradle 8.13 直接编译
GRADLE_USER_HOME="D:/code/cache/gradle" \
  "D:/code/cache/gradle/wrapper/dists/gradle-8.13-bin/ap7pdhvhnjtc6mxtzz89gkh0c/gradle-8.13/bin/gradle" \
  assembleDebug

# 方式二：如果 wrapper 能正常联网下载，也可使用
GRADLE_USER_HOME="D:/code/cache/gradle" ./gradlew assembleDebug

# Debug APK 输出路径：
# D:\code\Android\junkboy_clone\app\build\outputs\apk\debug\app-debug.apk
```

> 注意：当前分支是 `localization-zh`，已应用无限跳转 Bug 修复。如需最新稳定版请切换到 `main` 分支。

## 关键技术栈
- Kotlin + Jetpack Compose
- Room 数据库
- Navigation Compose
- TensorFlow Lite (ML 分类)
- Android SMS/MMS Content Provider

## 包结构
- `com.ovehbe.junkboy.ui` - Activity + Compose UI
- `com.ovehbe.junkboy.database` - Room DAO/Entity
- `com.ovehbe.junkboy.service` - 后台服务（SmsFilterService, NotificationListenerService）
- `com.ovehbe.junkboy.smsreceiver` - SMS/MMS 广播接收器与默认短信 App 收件箱持久化辅助逻辑
- `com.ovehbe.junkboy.utils` - 工具类
- `SmsConversationFallbacks.kt` - 短信 Tab 的 Room 兜底合并逻辑；不要让短信 Tab 只依赖 `Telephony.Sms` Provider
- `SmsDefaultAppStatus.kt` - 默认短信应用状态的纯判断逻辑；UI 层优先消费这个快速状态，不要在 Compose 组合阶段做 Provider 写探测
- 分类缓存必须基于归一化 sender（号码格式兼容），不要再用原始 `sender` 字符串直接做 `Map` key
- `app/src/test/java` - JVM 单元测试，覆盖短信接收动作选择、系统收件箱持久化策略、短信 Tab 的 Room 兜底会话/详情
