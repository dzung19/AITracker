# SmartSpend Architecture & Context

## 1. Primary Tech Stack
- **Framework**: Jetpack Compose (Single-Activity Architecture)
- **Language**: Kotlin
- **Dependency Injection**: Dagger Hilt (`@HiltViewModel`, `AppModule`)
- **Local Storage**: Room Database locally caching `Expense` entities. SharedPreferences for configuration, streaks, and cache metadata.
- **Navigation**: `androidx.navigation.compose.NavHost`, fully Jetpack Compose managed inside `Navigation.kt`.
- **Media & Camera**: MLKit Text Recognition (legacy logic), CameraX API for real-time capture.
- **ML**: MediaPipe Tasks Text for intent classification. TensorFlow Lite GPU was removed (no source code references it).

## 2. Artificial Intelligence (Gemini Service)
The app heavily utilizes Google's Generative AI Client (`generativeai`) to categorize receipts, extract cost amounts, detect currency codes, provide weekly digest summaries, and offer chatbot interactions.
- **Model Tiers**: We expose 4 tiers (BASIC, STANDARD, ADVANCED, ELITE), mapped to Gemini limits (`Gemini 2.5 Flash-Lite`, `Gemini 2.5 Flash`, `Gemini 2.5 Pro`, `Gemini 3 Flash Preview`).
- **Managers**: 
  - `GeminiService`: Wraps raw `GenerativeModel` calls. Defined in `data/ai/GeminiService.kt`.
  - `GeminiServiceManager`: Keeps track of the globally selected Tier & unlocked models across components.
- **State Management**: A caching layer exists for analysis to avoid hitting Gemini rate limits (7-day cache TTL for generic requests).
- **API Key**: Stored in `local.properties` (not in git), injected via `BuildConfig.GEMINI_API_KEY`. The key MUST be restricted in Google Cloud Console to only the `Generative Language API` and locked to the Android package + SHA-1 fingerprint.

## 3. Google Play Monetization (In-App Purchases & Ads)
The project utilizes a dynamic Ads & Premium multi-tier lock system.
- **Multiple Modules**: The specific logic for monetization relies on a distinct `:ads` Gradle module.
- **BillingManager.kt** (in `:ads` module): Handles BOTH one-time purchases AND subscriptions via `BillingClient`.
  - **IMPORTANT DECISION (Apr 2026)**: AI tier products are configured as **SUBSCRIPTIONS** (`ProductType.SUBS`) in Google Play Console, NOT one-time purchases. The `remove_ads_sku` remains a one-time purchase (`ProductType.INAPP`).
  - BillingManager queries INAPP and SUBS separately and merges results.
  - Subscription purchases REQUIRE an `offerToken` from `subscriptionOfferDetails` to launch the purchase flow.
  - `PendingPurchasesParams` uses both `enableOneTimeProducts()` and `enablePrepaidPlans()`.
- **Purchased Products StateFlow**: The system monitors `BillingManager.purchasedProducts`. We map SKUs to their respective `AiTier` models to auto-unlock premium tiers.

### Product IDs (must match Play Console EXACTLY):
| Product ID | Type | Play Console Section | Purpose |
|---|---|---|---|
| `remove_ads_sku` | `INAPP` (one-time) | In-app products | Remove ads / Premium |
| `ai_tier_standard` | `SUBS` (subscription) | Subscriptions | Unlock Gemini 2.5 Flash |
| `ai_tier_advanced` | `SUBS` (subscription) | Subscriptions | Unlock Gemini 2.5 Pro |
| `ai_tier_elite` | `SUBS` (subscription) | Subscriptions | Unlock Gemini 3 Flash |

### Purchase Flow:
```
User taps "Buy" on TierManagementScreen
  → Navigation.kt → viewModel.launchPurchaseFlow(activity, tier)
    → MainViewModel → tier.skuId → BillingManager.launchPurchaseFlow(activity, sku)
      → BillingManager detects SUBS type → extracts offerToken → launches Google Play sheet
        → Purchase callback → acknowledgePurchase → purchasedProducts StateFlow updates
          → MainViewModel.init collector → GeminiServiceManager.syncPurchasedTiers()
```

### Ads System:
- `DynamicAdsManager`: Singleton that reads ad config from Firebase Remote Config (release) or uses test IDs (debug).
- Ads are currently **disabled** via `ADS_DISABLED = true` in both debug and release `buildTypes`.
- Ad IDs are only defined in the `debug` build type. Release build gets them from Firebase Remote Config.

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

## 6. Build & Release Notes
- **R8/ProGuard**: `isMinifyEnabled = true` and `isShrinkResources = true` for release builds.
- ProGuard rules keep fields for `di`, `data`, `ui`, `util` packages and MLKit classes.
- **Removed dependencies**: `tensorflow-lite-gpu` was fully removed (dependency, version catalog entry, and ProGuard rules) — no source code uses it.
- **Release signing**: Release build type does NOT define ad unit IDs in BuildConfig (they come from Firebase Remote Config at runtime).
