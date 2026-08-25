# Project Context: Personal Expense App (Self Budget)

## 1. Executive Summary
**Self Budget** is a modern personal finance and expense tracking application built for Android. The application enables users to track daily income and expenses, manage multiple bank accounts and wallets with live computed balances, transfer money between their own accounts, hold accounts in different currencies with manually-entered exchange rates, toggle financial balance privacy (`👁️ / 🙈`), monitor category budgets with daily pace safeguards (`💡 $140.00 / day max pace`) and optional month-to-month rollover, track credit card / loan debt with payoff estimates, set savings goals tied to live account balances, view monthly net worth history via a dedicated full-screen overlay modal, analyze spending trends (including month-over-month comparative analytics), manage recurring expenses & bi-weekly paychecks with automatic due-date posting, capture receipt photos via camera, scan receipts using ML Kit OCR, log transactions/budgets/recurring bills via voice speech-to-text with Big Round Mic buttons (`🎙️`), customize theme appearance (Light / Dark / System), lock app with biometrics (Fingerprint / Face ID / PIN) with a solid Privacy Shield during prompts, receive local bill and budget push notifications with Android 13+ runtime permissions, enjoy a streamlined 2-tier Recent Activity dashboard preview with full history sheet, add income and expenses through two dedicated entry forms reached from a single unified full-page "+" entry point shared by every tab, view a 4-column Monthly Cash Flow breakdown (`Income` | `Budgets` | `Unbudgeted` | `Paid`), backup and restore complete database payloads as a JSON file via the Android Share Sheet and System Document Picker (the user can save it to Google Drive, email, or local storage - there is no dedicated Drive API integration), and sign in with Google Authentication (without raw ID tokens ever being persisted to disk).

> **Note on scope**: Self Budget remains fully offline-first and single-device. "Multi-currency" means each account is tracked in its own currency with rates you enter yourself — there is no live FX feed. All financial amounts inherit the single system default currency configured in Settings. "Google Authentication" establishes local identity only; backup/restore is a manual JSON file export/import via the Android Share Sheet and System Document Picker, not an automatic Drive sync — there is no third-party server involved either way. See §5 for what was deliberately left as-is.

---

## 2. Core Goals & Objectives
- **Seamless Authentication**: Fast and secure Google Sign-In using Android Credential Manager API / Firebase Auth. The ID token is used only for the duration of sign-in and is never written to the local database.
- **Zero-Cost File Backup**: Schema-versioned, cent-safe JSON serialization engine (`SyncDataPayload.kt`) backed by `CloudSyncManager.kt`. Users can export/import full Room DB backups as a JSON file via the Android Share Sheet (to Google Drive, email, or any app) or the System Document Picker, at $0.00 infrastructure cost - there is no dedicated Google Drive API integration.
- **Offline-First & Fast UX**: Local storage (Room DB v12 with Kotlin Flow) for immediate response times.
- **Multi-Account & Wallet Support**: Live balance tracking for Checking, Credit Cards, Cash Wallets, Savings, Loans, and Custom Accounts, computed from each account's starting balance plus its actual income/expense/transfer history — not a static number.
- **Account Transfers**: Move money between the user's own accounts without it being miscounted as income or expense. Smart credit card/loan payoff payments deduct from source account and simultaneously reduce debt balance on target liability account.
- **Single System Default Currency Source of Truth**: All screens, account dialogs, budget cards, and transaction forms inherit the user's system default currency configured in Settings without label clutter.
- **Debt & Liability Tracking**: Credit limit, APR, and minimum payment on Credit Card / Loan accounts, with an amortization-based payoff time & total interest estimate.
- **Savings Goals & Net Worth History**: Goals track progress against an asset account's live balance (disallowing debt accounts); a dedicated full-screen overlay `NetWorthHistoryModal` renders live net worth, assets vs liabilities split cards, and month-by-month trend logs.
- **Budget Rollover**: Optional per-category carryover of unspent (or overspent) budget into the next month.
- **Automatic Recurring Posting**: Due recurring bills/paychecks can post themselves to transactions instead of requiring a manual tap, with cycle-aware status badges (`Posted for Aug ✅`) and persistent greyed-out button states.
- **Privacy First**: Interactive Eye Toggle (`👁️ / 🙈`) to hide sensitive dollar balances on dashboard and clean account dropdown selections.
- **Biometric Privacy Shield**: The dashboard is 100% masked behind a solid `AppLockScreen` privacy shield whenever biometric authentication or the setup dialog is active.
- **Streamlined Home Dashboard**: The main dashboard displays a clean **5-item Recent Activity preview**. Search and Category Filter chips (`All`, `Expenses 🔴`, `Income 🟢`, Category chips) are housed inside the **`View All (X) ›`** full transaction history dialog for zero clutter.
- **Unified Full-Screen Modal Architecture with Big Round Mic Buttons**: Consistent full-screen modal pattern across Add Income, Add Expense, Edit Transaction, Set Budget, and Add Recurring Entry forms with **Big Round Mic Buttons (`🎙️`)**, TopAppBar quick save, M3 `ExposedDropdownMenuBox` with checkmarks, focus jump prevention, and sticky bottom action bar (`Red Cancel` + `Green Save`).
- **Single Global Entry Point**: One `+` button (shared across all 5 tabs, not one per tab) opens a full-page "What do you want to add?" chooser (`AddEntryPointScreen`) with four dedicated cards — **Add Income**, **Add Expense**, **Create a Budget**, **Add a Recurring Bill** — each routing straight to its own form so there is exactly one way to create each kind of entry, reducing the chance of the ledger and budget getting out of sync.
- **Contextual Transaction & Budget Entry**: Speech-to-text voice recognition with Big Round Mic buttons across Transactions, Budgets, and Recurring entry forms.
- **4-Column Monthly Cash Flow Card**: Renders 4 distinct metrics: `Income` (🟢), `Budgets` (🔵), `Unbudgeted` (Neutral Slate/Dark), and `Paid` (Conditional Red when spent > commitments) with dynamic responsive typography scaling (`13.sp` $\rightarrow$ `11.5.sp` $\rightarrow$ `10.sp`) ensuring amounts up to millions never wrap or truncate.
- **Category Budgets & Daily Pace Safeguard**: Re-imagined modern Neobank category limit cards with category avatar boxes, pill badges, extra bold numbers, floating percentage pills, & bottom metrics insets.
- **Top-Right Profile & Settings Avatar**: Settings and Profile controls are housed behind a top-right circular user initial badge avatar in the TopAppBar, opening a full-screen `Profile & Settings` modal sheet and keeping the bottom bar 100% focused on core financial management.
- **Consistent 5-Panel Navigation Bar**: Streamlined 5 tabs for active financial tracking: `Home` 🏠 (Dashboard summary), `Plan` 🎯 (Category budgets & rollover `BudgetScreen.kt`), `Recurring` 🔄 (Bills calendar & paychecks `RecurringScreen.kt`), `Analytics` 📊 (Spending charts & net worth trends `AnalyticsScreen.kt`), and `Activity` 📜 (Search & line-item feed `SearchScreen.kt`).
- **Theme & Appearance Customization**: Full support for System Default ⚙️, Light ☀️, and high-contrast Dark 🌙 mode (WCAG AAA compliant).
- **Modern Android Stack**: Built with Kotlin, Jetpack Compose, Material 3, Clean Architecture, Hilt DI, and Room DB (Schema v12).

---

## 3. Key Feature Scope

### 3.1 Authentication & Security
- Google Sign-In integration via Credential Manager API.
- User profile display (Avatar, Name, Email) in Settings.
- **Biometric App Lock Screen & Solid Privacy Shield**: Android BiometricPrompt (Fingerprint / Face ID / PIN) on launch/resume with solid `AppLockScreen` privacy shield masking financial dashboard during prompts.

### 3.2 System Permissions & Push Notifications
- Android 13+ runtime permission request (`Manifest.permission.POST_NOTIFICATIONS`) right after sign-in for system alert notifications (*"Allow Self Budget to send you notifications?"*).
- Notifications for bill reminders, budget alerts, and test alerts.

### 3.3 Multi-Account & Wallet Support
- Manage Checking Account 🏦, Credit Card 💳, Cash Wallet 💵, Savings Account 💰, Loan accounts, and Custom Accounts.
- Live account balance calculation (`initialBalance + sum(income) − sum(expense) ± transfers`), computed by `AccountBalanceCalculator` and used everywhere a balance is shown (dashboard cards, account selection modal, transfer picker).
- **Account Transfers**: A dedicated `TransferDialog` (via the "Transfer" action in Accounts & Wallets) moves money between two of the user's own accounts as a `TRANSFER`-type transaction — debiting the source, crediting the destination, and excluded from income/expense totals.
- **Per-Account Currency**: Each account carries its own `currencyCode` (USD, EUR, GBP, INR, JPY, AUD). Net worth and cross-account totals convert through exchange rates the user enters manually in Settings.
- **Debt & Liability Tracking**: Credit Card and Loan accounts can record a credit limit, APR, and minimum monthly payment; `DebtPayoffCalculator` estimates months-to-payoff and total interest using standard amortization math.
- **Privacy Eye Toggle**: `👁️ / 🙈` toggle on dashboard hides total net balance and individual account balances (`$ ••••••`).

### 3.4 Dashboard & Transaction Management
- **Streamlined Home Dashboard**: Renders Month Header, Total Balance Card, Accounts Carousel, 4-Column Monthly Cash Flow Card, and the top 5 most recent transactions (`Recent Activity: Top 5 of 24`).
- **Full History Dialog**: Tapping `View All (24) ›` or the bottom `See All 24 Transactions ›` card opens a full-screen sheet featuring full search, category filter chips (`All`, `Expenses 🔴`, `Income 🟢`, Category chips), date formatting, tap-to-edit, and delete options.
- **Two Dedicated Entry Forms**: `AddIncomeDialog` (`Title / Payer`, deposit account, category, recurring paycheck toggle, mic) and `AddExpenseDialog` (`Title / Merchant`, payment account, category, debt-account paydown field, budget-ceiling shortcut, mic + receipt OCR scan) replace the old single toggle-switch form — each only shows the fields relevant to that type. `EditTransactionDialog` still edits either type from one screen.
- **Merchant Category Memory**: Auto-detects past merchant titles (e.g. *"Starbucks"*) and auto-selects category with interactive suggestion badge.
- **Integrated Recurring Setup**: Toggle `Is Recurring?` with Weekly 📅 (+1 week), Bi-Weekly 🗓️ (+14 days), Monthly 🗓️ (+1 month), or Annual 🎆 (+1 year) auto-calculated next due dates.

### 3.5 Recurring Expenses & Subscriptions Manager
- Unified full-screen modal for adding recurring bills and paychecks featuring a **Big Round Mic Button (`🎙️`)** for hands-free voice logging.
- Subscriptions, rent, utility bills, credit card payments, and bi-weekly salary tracker.
- Monthly Commitments Hero Card (Paychecks vs Bills summary tiles).
- Persistent cycle-aware posted status badges (`Posted for Aug ✅`) and locked greyed-out button states.
- Filter Chips (`All`, `Bills 🔴`, `Paychecks 🟢`).
- Next due date tracking and 1-tap **"Post Now to Transactions"** action.

### 3.6 Category Budgeting & Daily Pace Safeguard
- Unified full-screen modal for setting category budget limits featuring a **Big Round Mic Button (`🎙️`)** for hands-free voice logging.
- Modern Neobank category budget limit cards with avatar boxes, pill badges, and metrics insets.
- Daily Pace Safeguard (`💡 $140.00 / day max pace across 10 days left`).
- Set category monthly budget limits with color-coded progress bars.
- **Budget Rollover**: An optional `Roll Over Unused Budget` toggle per category carries last month's unspent amount forward into this month's effective limit.
- **Month-over-Month Comparative Analytics**: Comparative card in Analytics tab showing percentage difference and dollar difference vs preceding month.
- **Auto-Synced vs. Manually-Set Budget Ceilings**: `BudgetEntity.isAutoSynced` tracks whether a category's limit is still the auto-suggested figure derived from its recurring bills, or a number the user set by hand on the budget screen. Recurring-bill changes recompute the auto-suggested ceiling from scratch (so lowering a bill lowers the ceiling too) but never overwrite a manually-set one; deleting a manual budget lets auto-sync suggest a fresh number again.

### 3.7 Settings, Appearance & Data Backup / Restore
- **Zero-Cost Data Backups**: Export full database snapshot to a `.json` backup file via the Android Share Sheet (user picks Google Drive, email, or any app); 1-tap restore using the System Document Picker.
- **Push Notifications**: System notification channel for bill reminders and budget threshold alerts (with Test Notification button in Settings).
- **Appearance**: Toggle between System Default ⚙️, Light Mode ☀️, and high-contrast Dark Mode 🌙.
- **Preferred Currency**: Live selector for $, €, £, ₹, ¥, A$ updating all app balance formatters.
- **In-App Reset All Data**: 1-tap `[ 🧹 Reset All App Data ]` feature with red trash badge M3 confirmation dialog.
- **Data Export**: Export transaction history into standard `.csv` format via Android Share Sheet.

### 3.8 Savings Goals & Net Worth History
- **Savings Goals**: Create a named goal with a target amount, strictly linked to asset accounts (Checking, Savings, Cash, Investment); progress is that account's live balance vs target.
- **Net Worth History Modal**: Full-screen overlay `NetWorthHistoryModal` rendering live net worth hero card, assets vs liabilities split cards, and month-by-month trend log table.

---

## 4. Architecture & Tech Stack

| Component | Technology |
| :--- | :--- |
| **Language** | Kotlin |
| **UI Framework** | Jetpack Compose with Material 3 (MD3) |
| **Architecture** | Clean Architecture + MVVM / Unidirectional Data Flow (UDF) |
| **Dependency Injection** | Hilt |
| **Local Database** | Room DB (Schema v12) + Kotlin Flow |
| **Backup & Serialization** | `CloudSyncManager` + Gson cent-safe JSON serialization engine (`SyncDataPayload`) |
| **Money Math** | `Money` utility — `BigDecimal.valueOf`-backed cent rounding for every sum/multiply, avoiding raw `Double` floating-point drift |
| **Theme & Dark Mode** | Custom MD3 High-Contrast Dark & Light Color Schemes |
| **Authentication & Security** | Google Credential Manager API & Android BiometricPrompt (ID token not persisted) |
| **OCR Text Recognition** | Google ML Kit Vision Text Recognition (`com.google.android.gms:play-services-mlkit-text-recognition`) |
| **Voice Processing** | Android Speech Recognizer (`RecognizerIntent.ACTION_RECOGNIZE_SPEECH`) + Regex Parser |
| **Notifications** | Android NotificationManager & NotificationChannel (`POST_NOTIFICATIONS`) |
| **Data Export** | CsvExporter + FileProvider / Android Share Sheet |

---

## 5. Known Limitations (Deliberately Out of Scope)
- **Automatic Destructive Fallback Migration**: Room uses fallback migration to version 12 to handle entity updates smoothly without manual SQL scripts.
- **No Dedicated Cloud Sync**: Backup/restore is a manual JSON file export/import via the Android Share Sheet and Storage Access Framework - the user can choose Google Drive as a destination, but there is no Drive API integration or automatic sync.
- **No live FX rates**: Multi-currency conversion relies on exchange rates entered by hand in Settings — there is no background job or API call fetching current rates.
