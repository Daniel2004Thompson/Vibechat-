# VibeChat - Android APK Release

Welcome to **VibeChat**, a modern, real-time messaging and social communication Android app built with Kotlin and Jetpack Compose.

---

## 📱 APK File Details

- **File Name:** `app-debug.apk`
- **Application ID:** `com.aistudio.vibechat.vbxkqp`
- **Min Android SDK:** Android 7.0 (API Level 24)
- **Target Android SDK:** Android 15 (API Level 36)

---

## 🚀 How to Install on Android

1. **Transfer the APK:** Copy `app-debug.apk` to your Android device via USB, Google Drive, or direct download.
2. **Enable Unknown Sources:**
   - Go to **Settings** > **Security** (or **Apps & notifications** > **Special app access**).
   - Enable **Install unknown apps** for your file manager or browser.
3. **Install:** Tap on `app-debug.apk` and confirm the installation.
4. **Launch:** Open **VibeChat** from your app drawer.

---

## 🔐 Architecture & Security

- **Edge Function Access-Point:**
  - All database, authentication, and cloud endpoints route securely through your dedicated Supabase Edge Function:
    `https://dktdmacqwloszwkvprcy.supabase.co/functions/v1/Access-point`
- **Zero-Key APK Security:**
  - Zero project API keys or sensitive secrets are stored or compiled into this APK.
  - Authentication tokens are attached as personal bearer tokens only when an authorized user is logged in.
- **Offline-First Room Persistence:**
  - Backed by an on-device Room SQLite database so all chats, contacts, and status stories remain accessible offline and sync seamlessly when connected.

---

## ✨ Core Features

1. **Flexible Sign In:**
   - Sign in with either **Email Address** or **@username** in a single field.
   - 6-digit session verification code (OTP).
   - Fast and secure Sign in with Google via Android Credential Manager.
2. **Direct & Group Chats:**
   - Real-time text messaging with read receipts and emoji reactions.
   - Reply quotes and message bookmarking (starred messages).
   - Group squad creation with member administration.
3. **Voice Notes:**
   - Record and preview voice messages with live waveform visualization and custom playback controls.
4. **24-Hour Status Stories:**
   - Post text and media status updates with vibrant background colors and typography styles.
   - Interactive segmented story ring viewer with reaction feedback.
5. **Customization & Themes:**
   - Light and Dark modes.
   - Dynamic accent themes (Emerald Vibe, Cyber Teal, Electric Indigo, Sunset Coral, Royal Amethyst).
   - Editable profiles with custom avatars and bios.
