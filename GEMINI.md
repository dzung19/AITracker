# SmartSpend Architecture & Context

## 1. Primary Tech Stack
- **Framework**: Jetpack Compose (Single-Activity Architecture)
- **Language**: Kotlin
- **Dependency Injection**: Dagger Hilt (`@HiltViewModel`, `AppModule`)
- **Local Storage**: Room Database locally caching `Expense` entities. SharedPreferences for configuration, streaks, and cache metadata.
- **Navigation**: `androidx.navigation.compose.NavHost`, fully Jetpack Compose managed inside `Navigation.kt`.
- **Media & Camera**: MLKit Text Recognition (legacy logic), CameraX API for real-time capture.

## 2. Artificial Intelligence (Gemini Service)
The app heavily utilizes Google's Generative AI Client (`generativeai`) to categorize receipts, extract cost amounts, detect currency codes, provide weekly digest summaries, and offer chatbot interactions.
- **Model Tiers**: We expose 4 tiers (BASIC, STANDARD, ADVANCED, ELITE), mapped to Gemini limits (`Gemini 2.5 Flash-Lite`, `Gemini 2.5 Pro`, `Gemini 3.0 Flash Preview`, etc.).
- **Managers**: 
  - `GeminiService`: Wraps raw `GenerativeModel` calls.
  - `GeminiServiceManager`: Keeps track of the globally selected Tier & unlocked models across components.
- **State Management**: A caching layer exists for analysis to avoid hitting Gemini rate limits (7-day cache TTL for generic requests).

## 3. Google Play Monetization (In-App Purchases & Ads)
The project utilizes a dynamic Ads & Premium multi-tier lock system.
- **Multiple Modules**: The specific logic for monetization relies on a distinct `:ads` Gradle module.
- **BillingManager.kt**: Handles all In-App Billing subscriptions via `BillingClient`. Connects internally to both Ads states and AI Model states.
- **Purchased Products StateFlow**: The system monitors `BillingManager.purchasedProducts`. We map SKUs (like `ai_tier_standard`, `ai_tier_advanced`, `ai_tier_elite`) to their respective `AiTier` models to auto-unlock premium tiers.
- **Ads Removal**: There is a legacy product token `remove_ads_sku` configured with a backward-compatible boolean listener.

## 4. Multi-Currency Flow
- Uses `ExchangeRateService` via Frankfurter API (`api.frankfurter.app`) to fetch latest exchange rates using simple HTTP.
- Responses are locally cached for 24-hours to circumvent API limits and allow offline reading.
- The user declares a "Home Currency" globally, and the `CurrencyFormatter` dynamically re-calculates all receipts on `ExpenseCard` views. AI dynamically parses original receipt currency codes.

## 5. UI Structure & State
- **MainViewModel**: The monolithic bridge spanning across practically all core flows (`ExpenseRepository`, `GeminiServiceManager`, `BillingManager`, `ExchangeRateService`, `ChatService`). Emits `StateFlow` streams.
- **Screens**:
  - `HomeScreen`: Displays list and charts in the calculated `HomeCurrency`.
  - `AddExpenseScreen`: Captures via Camera X or form entry; integrates Currency Selector Chips.
  - `TierManagementScreen`: Displays available LLMs and provides 1-tap "buy/unlock" via the Google Play bottom sheet natively.
  - `AnalyticsScreen` & `ChatScreen`: Secondary visualization spaces.
