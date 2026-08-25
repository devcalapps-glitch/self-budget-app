# Privacy Policy for Self Budget

**Last Updated:** August 25, 2026

**Self Budget** ("we", "our", or "the app") is a privacy-first, local-first Android financial application developed by **DevCalApps**. We believe your financial data belongs exclusively to you. This Privacy Policy details how data is handled within the Self Budget application.

---

## 1. Zero External Data Collection & Tracking

* **No Server Infrastructure**: Self Budget does not operate external databases, tracking servers, or telemetry analytics.
* **No Third-Party Analytics or Ads**: We do not use third-party advertising frameworks, tracking cookies, or diagnostic data collection tools.
* **No Selling of User Data**: We never collect, transmit, sell, or rent your personal or financial data to any third party.

---

## 2. On-Device Storage (Local-First Architecture)

All financial entries created within Self Budget—including transactions, income, expense categories, monthly budgets, savings goals, custom accounts, and exchange rates—are stored locally on your Android device using an encrypted local SQLite database (Android Room Persistence Library).

Your data remains entirely on your physical device unless you explicitly initiate a backup or export.

---

## 3. Google Drive Cloud Sync & Storage

Self Budget offers an optional **Automated Google Drive Cloud Sync** feature:

* **Private Sandbox Storage (`appDataFolder`)**: Cloud backups are saved to a hidden, private folder in your personal Google Drive (`appDataFolder`) using the `https://www.googleapis.com/auth/drive.appdata` OAuth scope.
* **Isolated File Access**: Backups (`self_budget_cloud_backup.json`) are stored within your own Google Account storage quota. Other Google Drive applications and standard file views cannot view or access this private data.
* **No Developer Access**: DevCalApps has zero access to your Google Account, Google Drive files, or backup payloads.

---

## 4. Manual Export & Backup Options

You may manually export your financial records at any time:

* **JSON Backup Export**: Creates a structured, cent-safe JSON file handed to the Android Share Sheet for local storage or manual backup.
* **CSV Transaction Export**: Exports your transaction history as a standard CSV spreadsheet to any app or destination of your choice.

---

## 5. Security & Biometric Protection

* **Biometric Authentication**: If enabled in Settings, device-level biometric authentication (Fingerprint, Face Unlock) uses Android’s native `BiometricPrompt` system framework. Biometric templates remain securely stored inside your device’s hardware Trusted Execution Environment (TEE) and are never accessed by or transmitted to the app.

---

## 6. User Control & Data Erasure

You retain 100% control over your data:

* **Data Reset**: You can erase all local accounts, transactions, and settings at any time by selecting **Clear All Data** in the Settings tab.
* **Cloud Backup Erasure**: You can delete cloud backups directly within your Google Account settings (**Google Drive Web $\rightarrow$ Settings $\rightarrow$ Manage Apps $\rightarrow$ Self Budget $\rightarrow$ Delete hidden app data**).

---

## 7. Contact Information

If you have questions regarding this Privacy Policy or Self Budget, please contact us:

* **GitHub Repository**: [https://github.com/devcalapps-glitch/self-budget-app](https://github.com/devcalapps-glitch/self-budget-app)
* **Privacy Policy URL**: [https://github.com/devcalapps-glitch/self-budget-app/blob/main/PRIVACY.md](https://github.com/devcalapps-glitch/self-budget-app/blob/main/PRIVACY.md)
* **Developer Email**: `devcalapps@gmail.com`
