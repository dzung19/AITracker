# SmartSpend - AI Expense Tracker

A modern Android expense tracker that uses **Hybrid AI** to scan receipts with near-zero API costs.

## ✨ Features

- 📷 **Smart Receipt Scanning** - Take a photo and let AI extract the details
- 💰 **Expense Tracking** - Track spending with beautiful Material 3 UI
- 🏷️ **Auto-Categorization** - AI automatically categorizes your expenses
- 🔒 **Privacy First** - OCR runs on-device, only text goes to cloud

## 🧠 How the "Hybrid AI" Works

| Step | Technology | Cost |
|------|------------|------|
| 1. Capture | CameraX | Free |
| 2. OCR | ML Kit (on-device) | **FREE** |
| 3. Parse | Gemini Flash (text only) | **~$0** |

Instead of sending images to the cloud (expensive), we:
1. Extract text on-device using Google ML Kit (FREE)
2. Send only the text to Gemini Flash for parsing (CHEAP)

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug or newer
- Android device with camera (emulator won't work well for camera)
- Gemini API key (free tier available)

### Setup

1. Clone this repository
2. Open in Android Studio
3. Add your Gemini API key to `local.properties`:
   ```properties
   GEMINI_API_KEY=your_api_key_here
   ```
4. Get your free API key at: https://aistudio.google.com/app/apikey
5. Build and run!

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Database**: Room
- **Camera**: CameraX
- **OCR**: Google ML Kit (on-device)
- **AI**: Gemini Flash API
- **Architecture**: MVVM + Clean Architecture

## 📱 Screenshots

*Coming soon*

## 📄 License

MIT License
