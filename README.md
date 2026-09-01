# Self Budget 💸

**Self Budget** is a privacy-first, offline-first personal financial management app for Android built with modern Android standards: **Kotlin 2.1**, **Jetpack Compose (Material 3)**, **MVVM Architecture**, **Room Database**, **Hilt Dependency Injection**, and **Kotlin Coroutines / StateFlow**.

---

## 🌟 Key Features

### 1. 📊 Monthly Cash Flow Engine
* **Per-Source Effective Income**: Accurately computes monthly income across recurring salaries, paychecks, and freelance/ad-hoc payments ($\max(\text{Logged}, \text{Expected}) + \text{Ad-Hoc}$).
* **Unified Budget & Fixed Bill Commitments**: Calculates unassigned "Free Cash" by balancing category budgets, fixed recurring obligations, unbudgeted expenses, and savings goal commitments.

### 2. 🎯 Smart Category Budgets & Persistent Baselines
* **Persistent Monthly Baselines (Monarch/Mint Model)**: Budgets set in any month automatically carry forward to all future months without requiring manual monthly recreation.
* **Historical Immutability**: Modifying or deleting a budget in the current month creates a forward baseline without retroactively altering past historical reports or calculations.
* **Persistent Deficit Carryover**: Rollover carryover chains off the previous month's unclamped net position, so overspending deficits persist across consecutive months until fully repaid.
* **Safe-to-Spend Safeguard**: Real-time spending headroom calculator accounting for upcoming recurring bills in each category.
* **Daily Pace Safeguard**: Zero-floored daily spending pace tracker ($\max(0.0, \, \text{Remaining Budget} / \text{Remaining Days})$).

### 3. ☁️ Automated Google Drive Cloud Sync & Offline Backups
* **Private Sandbox Cloud Sync**: Automated background sync directly to your personal Google Drive private `appDataFolder` (`self_budget_cloud_backup.json`).
* **Zero Cost & High Privacy**: Uses the user's personal Google storage quota ($0.00 cloud server cost to developer). Data is hidden from third-party apps and standard Drive views.
* **Offline Share Sheet Export**: Cent-safe JSON export/import via Android Share Sheet and Storage Access Framework document picker.

### 4. 💳 Net Worth, Accounts & Wallet Management
* **Signed Asset & Debt Balancing**: Accurately tracks checking, savings, cash, investments, retirement accounts (e.g. 401(k), IRA), credit card debts, and loan balances. Purchases increase liability debt, while payments reduce debt and adjust net worth.
* **Unified Global Add Chooser**: Fast 1-tap entry for Income, Expense, Budgets, Recurring Bills, and Accounts/Wallets directly from the global `+` FAB.
* **Interactive Edit Mode Toggle**: Seamlessly switch between viewing an account's filtered transaction activity feed and editing/deleting accounts with pencil badge overlays.
* **Net Worth Snapshot History**: Interactive historical net worth tracking with time-range filtering.

### 5. 🧾 ML Kit OCR Receipt Scanning & Voice Logging
* **On-Device Receipt OCR**: Uses Google ML Kit Text Recognition to extract merchant name, date, and total transaction amounts from receipt photos.
* **Natural Language Voice Input**: Voice parser for spoken transaction logging.

### 6. 📅 Universal Month Navigation & Recurring Manager
* **Unified Month Header (`MonthYearHeader`)**: Consistent top month selector header (`< Month Year >`) across all main tabs (Dashboard, Plan, Recurring, Analytics).
* **Start / Next Due Date Picker**: Select custom start and due dates for recurring bills & paychecks without forcing immediate activity transactions.
* **Archived Bill Safeguards & Styled Cards**: Disable posting on archived bills with muted semi-transparent containers, soft borders, and Archived 📦 status badges.
* **Calibrated Preset Chips**: Quick amount preset chips (`+$50`, `+$100`, `+$250`, `+$500`, `+$1000`) matching the Budget page.

### 7. 🔒 Biometric Security, Data Management & High-Contrast Themes
* **Native Android Biometrics**: Fingerprint & Face Unlock protection using `BiometricPrompt`.
* **Isolated Data Management**: Dedicated Data & Account Management section housing Reset All Data / Clean Sweep tools with dual-step confirmation dialogs.
* **Adaptive Dark & Light Themes**: Accessible Material 3 design system with WCAG AA compliant text contrast (`#1E5631` light mode income green) and calibrated ring charts.

---

## 🏗️ Architecture & Technology Stack

| Layer | Technology |
| :--- | :--- |
| **Language** | Kotlin 2.1 |
| **UI Framework** | Jetpack Compose (Material 3), Compose Navigation |
| **Architecture** | Model-View-ViewModel (MVVM) + Clean Architecture |
| **Dependency Injection** | Hilt (Dagger 2) |
| **Database & Persistence** | Room Database (SQLite), Gson Serialization |
| **Asynchronous State** | Kotlin Coroutines, `StateFlow`, `SharedFlow` |
| **Auth & Cloud Sync** | Google Credential Manager, Google Drive REST API (`appDataFolder`) |
| **Machine Learning** | Google ML Kit Vision (Text Recognition) |
| **Build Tooling** | Gradle 9.3 (AGP 8.8), KSP (Kotlin Symbol Processing) |

---

## 📁 Repository Structure

```text
self-budget-app/
├── app/
│   ├── src/main/java/com/selfbudget/app/
│   │   ├── core/
│   │   │   ├── ui/               # Reusable Compose UI components & dialogs
│   │   │   └── util/             # Calculation engines (Money, Rollover, Balances, Income, Drive Sync)
│   │   ├── data/
│   │   │   ├── local/            # Room Database, Entities & DAOs
│   │   │   ├── model/            # Data models & JSON payloads
│   │   │   └── repository/       # Repository implementations
│   │   ├── di/                   # Hilt Dependency Injection modules
│   │   ├── feature/
│   │   │   ├── accounts/         # Accounts & Transfer management tab
│   │   │   ├── analytics/        # Timeframe analytics & extrapolated annual pace
│   │   │   ├── budget/           # Category budget limits & daily pace screen
│   │   │   ├── dashboard/        # Home screen, Cash Flow Hero card & Goals
│   │   │   ├── profile/          # Profile & Settings screen
│   │   │   ├── recurring/        # Paychecks & Bills recurring tab
│   │   │   ├── search/           # Filtered transaction search screen
│   │   │   └── transaction/      # Income/Expense modal dialogs
│   │   └── ui/theme/             # Material 3 Color palette & SelfBudgetTheme
│   └── src/test/java/            # Automated financial calculation unit test suite
├── adr.md                        # Architecture Decision Record & System Blueprint
├── PRIVACY.md                    # Official Privacy Policy
└── README.md                     # Application overview & developer guide
```

---

## 🧪 Testing & Verification

The codebase includes an automated unit test suite covering core financial calculations, safe-to-spend logic, cash flow equations, recurring frequency normalizations, and multi-month rollover chaining.

### Running Unit Tests
```bash
./gradlew test
```

Documentation breakdown available in [`adr.md`](adr.md).

---

## 🚀 Getting Started & Building

### Prerequisites
* **Android Studio** Ladybug (2024.2.1+) or newer
* **JDK**: Java 17 or Java 21
* **Android SDK**: API 35 (Android 15) compile SDK, Minimum SDK API 26 (Android 8.0)

### Build Commands

* **Build Debug APK**:
  ```bash
  ./gradlew assembleDebug
  ```

* **Install Debug APK on Device/Emulator**:
  ```bash
  ./gradlew installDebug
  ```

---

## 🔒 Privacy & Compliance

Self Budget operates with **Zero Telemetry**:
* No analytics tracking or user monitoring.
* All database data is stored strictly on your local device.
* Cloud backups are stored in your personal Google Drive (`appDataFolder`).
* See full details in [`PRIVACY.md`](PRIVACY.md).

---

## 📄 License

Developed by **DevCalApps**. All rights reserved.