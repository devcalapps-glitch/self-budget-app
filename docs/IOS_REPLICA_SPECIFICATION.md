# Self Budget — Native iOS Complete Replica Specification

## Executive Summary & Purpose

This document is the absolute, definitive implementation blueprint for creating an exact 1:1 replica of the **Self Budget** Android application as a **pure native iOS application** written in **Swift 6** and **SwiftUI**. 

No cross-platform frameworks, bridges, or Kotlin Multiplatform (KMP) code will be used. Every single feature, interaction pattern, design token, color ramp, typography rule, calculation formula, database schema, and native system integration is specified herein with production-ready Swift code snippets, data structures, and architectural standards.

---

## Table of Contents
1. [Native iOS Architecture & Tech Stack](#1-native-ios-architecture--tech-stack)
2. [Complete Design System & Visual Hierarchy](#2-complete-design-system--visual-hierarchy)
   - 2.1 Color Ramps & Exact Hex Codes
   - 2.2 Semantic Ramp Extension Functions (Swift)
   - 2.3 Section Identity Mapping
   - 2.4 Neutral Surfaces, Borders & Dividers
   - 2.5 Typography Scale (`SelfBudgetType`)
   - 2.6 Shape Scale & Corner Radii
   - 2.7 Reusable UI Component Library
   - 2.8 The 18 Strict Structural Rules
3. [Data Persistence & Schema (SwiftData / Core Data v22)](#3-data-persistence--schema-swiftdata--core-data-v22)
   - 3.1 Entity Model Specifications
   - 3.2 Database Indexes & Performance Optimization
   - 3.3 Activity Log Automatic Pruning Engine
4. [Financial Calculation Engines & Business Logic](#4-financial-calculation-engines--business-logic)
   - 4.1 Cent-Safe Precision Math (`CentSafeMoney`)
   - 4.2 Account Balance & Signed Liability Debt Engine
   - 4.3 Recurring Frequency Normalization Engine
   - 4.4 Persistent Forward-Inheriting Monthly Budgeting (Monarch/Mint Model)
   - 4.5 Multi-Month Persistent Deficit Rollover Engine
   - 4.6 Monthly Cash Flow Equation Engine
   - 4.7 Daily Pace Safeguard Engine
   - 4.8 Amortization Debt Payoff Calculator
   - 4.9 Linear-Time Net Worth Historical Snapshots ($O(N \log N + M \times A)$)
   - 4.10 Multi-Currency Conversion Engine
5. [Screen-by-Screen Layout & Feature Specifications](#5-screen-by-screen-layout--feature-specifications)
   - 5.1 Main Tab Navigation & Top App Bar
   - 5.2 Screen 1: Dashboard (`HomeScreen`)
   - 5.3 Screen 2: Spending Plan & Goals (`BudgetScreen`)
   - 5.4 Screen 3: Recurring Bills & Paychecks (`RecurringScreen`)
   - 5.5 Screen 4: Analytics & Trends (`AnalyticsScreen`)
   - 5.6 Screen 5: Activity Feed & Filtered Search (`SearchScreen`)
   - 5.7 Screen 6: Accounts & Wallets Manager (`AccountsScreen` & `AccountsViewAllModal`)
   - 5.8 Global "+" Action Chooser (`AddEntryPointScreen`)
   - 5.9 Transaction Entry Forms (`AddIncomeSheet`, `AddExpenseSheet`, `EditTransactionSheet`)
   - 5.10 Budget & Recurring Forms (`SetBudgetSheet`, `SetRecurringSheet`)
   - 5.11 Transfer Modal (`TransferSheet`)
   - 5.12 Selection Modals (`CategorySelectionModal`, `AccountSelectionModal`)
   - 5.13 Analytics Sub-Modals (`NetWorthHistoryModal`, `CategoryAnalyticsDetailModal`, etc.)
   - 5.14 Settings, Profile & Data Management (`SettingsScreen`, `DataManagementScreen`)
6. [Native iOS Platform Integrations](#6-native-ios-platform-integrations)
   - 6.1 Biometrics & Privacy Shield (`LocalAuthentication`)
   - 6.2 ML Kit / Vision OCR Receipt Scanning (`Vision` Framework)
   - 6.3 Speech Recognition Audio Logging (`Speech` & `AVFoundation`)
   - 6.4 Push Notifications (`UserNotifications` & Background Tasks)
   - 6.5 Cloud Backup & Google Drive Sync Interoperability
   - 6.6 Multi-Format Data Import & Export (Excel `.xlsx`, CSV, JSON)
7. [Automated Verification Suite (XCTest)](#7-automated-verification-suite-xctest)

---

## 1. Native iOS Architecture & Tech Stack

| Layer | Native iOS Technology | Justification / Requirement |
| :--- | :--- | :--- |
| **Platform Target** | iOS 17.0+ / iPadOS 17.0+ | Modern Observation framework (`@Observable`), SwiftData, and SF Symbols 5+. |
| **Language** | Swift 6.0 | Strict concurrency (`Sendable`, structured actors) and modern pattern matching. |
| **UI Framework** | SwiftUI | Fully declarative reactive UI mirroring Jetpack Compose 1:1. |
| **State Management** | Observation (`@Observable`) + MVVM | Unidirectional Data Flow (UDF) matching Kotlin `StateFlow<UiState>`. |
| **Local Persistence** | SwiftData (or SQLite via GRDB) | Schema versioning matching Room DB Schema v22 with compound indices. |
| **Authentication** | `AuthenticationServices` + GoogleSignIn SDK | Sign in with Apple and Google Authentication (`GIDSignIn`) without raw credential disk storage. |
| **Biometrics** | `LocalAuthentication` (`LAContext`) | Face ID / Touch ID with full Privacy Shield masking. |
| **OCR Text Recognition**| Apple `Vision` Framework | Native on-device text recognition matching Google ML Kit. |
| **Voice Processing** | `Speech` + `AVFoundation` | On-device speech recognition matching Android SpeechRecognizer. |
| **Notifications** | `UserNotifications` | Daily 8:00 AM local notification scheduling matching Android WorkManager. |
| **Cloud Storage** | Google Drive REST API (`appDataFolder`) | Direct zero-cost cloud backup interoperability with the Android app, plus optional CloudKit. |
| **Data Export/Import** | `UniformTypeIdentifiers`, CoreXLSX, Codable | JSON snapshot, multi-sheet Excel (.xlsx), and RFC 4180 CSV export/import. |

---

## 2. Complete Design System & Visual Hierarchy

The iOS app must follow the exact design tokens documented in `docs/DESIGN_SYSTEM.md`. There are **no ad-hoc hex colors, no arbitrary corner radii, and no random font weights** anywhere in the app.

### 2.1 Color Ramps & Exact Hex Codes

All colored components derive their styling from eight 7-stop color ramps:

| Ramp | c50 | c100 | c200 | c400 | c600 | c800 | c900 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Teal** | `#E1F5EE` | `#9FE1CB` | `#5DCAA5` | `#1D9E75` | `#0F6E56` | `#085041` | `#04342C` |
| **Blue** | `#E6F1FB` | `#B5D4F4` | `#85B7EB` | `#378ADD` | `#185FA5` | `#0C447C` | `#042C53` |
| **Purple** | `#EEEDFE` | `#CECBF6` | `#AFA9EC` | `#7F77DD` | `#534AB7` | `#3C3489` | `#26215C` |
| **Coral** | `#FAECE7` | `#F5C4B3` | `#F0997B` | `#D85A30` | `#993C1D` | `#712B13` | `#4A1B0C` |
| **Amber** | `#FAEEDA` | `#FAC775` | `#EF9F27` | `#BA7517` | `#854F0B` | `#633806` | `#412402` |
| **Red** | `#FCEBEB` | `#F7C1C1` | `#F09595` | `#E24B4A` | `#A32D2D` | `#791F1F` | `#501313` |
| **Pink** | `#FBEAF0` | `#F4C0D1` | `#ED93B1` | `#D4537E` | `#993556` | `#72243E` | `#4B1528` |
| **Gray** | `#F1EFE8` | `#D3D1C7` | `#B4B2A9` | `#888780` | `#5F5E5A` | `#444441` | `#2C2C2A` |

#### Dark Mode Green & Contrast Rules (Critical Rule)
In Dark Mode, the brand color **Teal** dynamically maps to authentic **Dark Green** tones rather than glowing cyan/blue-green:
- `solidFill` (Dark Mode): `#196338` (Authentic Dark Green CTA background)
- `onSolidFill` (Dark Mode): `#FFFFFF` (Pure White text for high contrast)
- `tintFill` (Dark Mode): `#0E2E18` (Deep Dark Green tinted container background)
- `titleText` (Dark Mode): `#FFFFFF` (Pure White headline text on tint)
- `secondaryText` (Dark Mode): `#81C784` (Soft pastel green text/icon)
- `containerBorder` (Dark Mode): `#1B542C` (Subtle dark green container border)
- `pillFill` (Dark Mode): `#1B542C`
- `pillText` (Dark Mode): `#C8E6C9`

### 2.2 Semantic Ramp Extension Functions (Swift)

Create a Swift implementation of the Ramp enum in `Theme/ColorRamp.swift`:

```swift
import SwiftUI

public enum ColorRamp: String, CaseIterable, Sendable {
    case teal, blue, purple, coral, amber, red, pink, gray

    public func hex(_ stop: Int) -> Color {
        switch (self, stop) {
        // Teal
        case (.teal, 50):  return Color(hex: 0xE1F5EE)
        case (.teal, 100): return Color(hex: 0x9FE1CB)
        case (.teal, 200): return Color(hex: 0x5DCAA5)
        case (.teal, 400): return Color(hex: 0x1D9E75)
        case (.teal, 600): return Color(hex: 0x0F6E56)
        case (.teal, 800): return Color(hex: 0x085041)
        case (.teal, 900): return Color(hex: 0x04342C)
        // Blue
        case (.blue, 50):  return Color(hex: 0xE6F1FB)
        case (.blue, 100): return Color(hex: 0xB5D4F4)
        case (.blue, 200): return Color(hex: 0x85B7EB)
        case (.blue, 400): return Color(hex: 0x378ADD)
        case (.blue, 600): return Color(hex: 0x185FA5)
        case (.blue, 800): return Color(hex: 0x0C447C)
        case (.blue, 900): return Color(hex: 0x042C53)
        // Purple
        case (.purple, 50):  return Color(hex: 0xEEEDFE)
        case (.purple, 100): return Color(hex: 0xCECBF6)
        case (.purple, 200): return Color(hex: 0xAFA9EC)
        case (.purple, 400): return Color(hex: 0x7F77DD)
        case (.purple, 600): return Color(hex: 0x534AB7)
        case (.purple, 800): return Color(hex: 0x3C3489)
        case (.purple, 900): return Color(hex: 0x26215C)
        // Coral
        case (.coral, 50):  return Color(hex: 0xFAECE7)
        case (.coral, 100): return Color(hex: 0xF5C4B3)
        case (.coral, 200): return Color(hex: 0xF0997B)
        case (.coral, 400): return Color(hex: 0xD85A30)
        case (.coral, 600): return Color(hex: 0x993C1D)
        case (.coral, 800): return Color(hex: 0x712B13)
        case (.coral, 900): return Color(hex: 0x4A1B0C)
        // Amber
        case (.amber, 50):  return Color(hex: 0xFAEEDA)
        case (.amber, 100): return Color(hex: 0xFAC775)
        case (.amber, 200): return Color(hex: 0xEF9F27)
        case (.amber, 400): return Color(hex: 0xBA7517)
        case (.amber, 600): return Color(hex: 0x854F0B)
        case (.amber, 800): return Color(hex: 0x633806)
        case (.amber, 900): return Color(hex: 0x412402)
        // Red
        case (.red, 50):  return Color(hex: 0xFCEBEB)
        case (.red, 100): return Color(hex: 0xF7C1C1)
        case (.red, 200): return Color(hex: 0xF09595)
        case (.red, 400): return Color(hex: 0xE24B4A)
        case (.red, 600): return Color(hex: 0xA32D2D)
        case (.red, 800): return Color(hex: 0x791F1F)
        case (.red, 900): return Color(hex: 0x501313)
        // Pink
        case (.pink, 50):  return Color(hex: 0xFBEAF0)
        case (.pink, 100): return Color(hex: 0xF4C0D1)
        case (.pink, 200): return Color(hex: 0xED93B1)
        case (.pink, 400): return Color(hex: 0xD4537E)
        case (.pink, 600): return Color(hex: 0x993556)
        case (.pink, 800): return Color(hex: 0x72243E)
        case (.pink, 900): return Color(hex: 0x4B1528)
        // Gray
        case (.gray, 50):  return Color(hex: 0xF1EFE8)
        case (.gray, 100): return Color(hex: 0xD3D1C7)
        case (.gray, 200): return Color(hex: 0xB4B2A9)
        case (.gray, 400): return Color(hex: 0x888780)
        case (.gray, 600): return Color(hex: 0x5F5E5A)
        case (.gray, 800): return Color(hex: 0x444441)
        case (.gray, 900): return Color(hex: 0x2C2C2A)
        default: return .clear
        }
    }

    public func tintFill(isDark: Bool, large: Bool = false) -> Color {
        if self == .teal && isDark { return Color(hex: 0x0E2E18) }
        return isDark ? hex(900) : (large ? hex(100) : hex(50))
    }

    public func titleText(isDark: Bool) -> Color {
        if self == .teal && isDark { return Color.white }
        return isDark ? hex(100) : hex(900)
    }

    public func secondaryText(isDark: Bool) -> Color {
        if self == .teal && isDark { return Color(hex: 0x81C784) }
        return isDark ? hex(400) : hex(600)
    }

    public func icon(isDark: Bool) -> Color {
        secondaryText(isDark: isDark)
    }

    public func containerBorder(isDark: Bool) -> Color {
        if self == .teal && isDark { return Color(hex: 0x1B542C) }
        return isDark ? hex(800) : hex(100)
    }

    public func pillFill(isDark: Bool) -> Color {
        if self == .teal && isDark { return Color(hex: 0x1B542C) }
        return isDark ? hex(800) : hex(100)
    }

    public func pillText(isDark: Bool) -> Color {
        if self == .teal && isDark { return Color(hex: 0xC8E6C9) }
        return isDark ? hex(200) : hex(800)
    }

    public func solidFill(isDark: Bool) -> Color {
        if self == .teal && isDark { return Color(hex: 0x196338) }
        return isDark ? hex(200) : hex(800)
    }

    public func onSolidFill(isDark: Bool) -> Color {
        if self == .teal && isDark { return Color.white }
        return isDark ? hex(900) : hex(50)
    }
}
```

### 2.3 Section Identity Mapping

Every functional section across the entire app uses one fixed ramp identity:

| Section Name | Ramp | Description |
| :--- | :--- | :--- |
| **Housing & Essentials** | `.blue` | Rent, mortgage, utilities, home repairs |
| **Food & Daily Living** | `.teal` | Groceries, dining out, coffee |
| **Lifestyle & Entertainment** | `.pink` | Subscriptions, movies, shopping, vacations |
| **Debt & Financial** | `.coral` | Credit card payoff, student loans, auto loans |
| **Custom Categories** | `.purple` | User-created custom spending categories |
| **Accounts & Wallets** | `.teal` | Banking, cash wallets, cards |
| **Recent Activity / History**| `.purple` | Transaction logs, search records, audit events |
| **Earned Income** | `.teal` | Salaries, wages, client payments |
| **Investments & Passive** | `.purple` | Dividends, capital gains, brokerage accounts |
| **Gifts & Other** | `.pink` | Reimbursements, windfalls, cash gifts |
| **Unmapped / General** | `.gray` | Fallback neutral identity |

### 2.4 Neutral Surfaces, Borders & Dividers

```swift
extension Color {
    static func appBackground(isDark: Bool) -> Color {
        isDark ? Color(hex: 0x1C1D1C) : Color(hex: 0xFFFFFF)
    }

    static func appCardSurface(isDark: Bool) -> Color {
        isDark ? Color(hex: 0x2C2C2A) : Color(hex: 0xF1EFE8)
    }

    static func appDivider(isDark: Bool) -> Color {
        isDark ? Color(hex: 0x444441) : Color.black.opacity(0.08)
    }

    static func appTextPrimary(isDark: Bool) -> Color {
        isDark ? Color(hex: 0xF1EFE8) : Color(hex: 0x2C2C2A)
    }

    static func appTextSecondary(isDark: Bool) -> Color {
        isDark ? Color(hex: 0xB4B2A9) : Color(hex: 0x5F5E5A)
    }

    static func appTextMuted() -> Color {
        Color(hex: 0x888780)
    }
}
```

### 2.5 Typography Scale (`SelfBudgetType`)

Strict Rule: Only two weights exist in the entire app: **Regular (Weight 400)** for body & metadata, and **Medium (Weight 500)** for headlines, titles, and buttons. Bold/Semibold/Black are strictly prohibited. Tabular figures (`.monospacedDigit()`) must be applied to all monetary figures.

| Token | Size | Weight | Line Spacing | Tracking | Usage |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `display` | 32pt | Medium (500) | 38pt | -0.5pt | Hero dollar balances ($26,638.36) |
| `title` | 20pt | Medium (500) | 26pt | 0pt | Top App Bar title, page headlines |
| `heading` | 16pt | Medium (500) | 22pt | 0pt | Section titles within pages |
| `section` | 15pt | Medium (500) | 20pt | 0pt | Header band titles |
| `rowTitle` | 14pt | Medium (500) | 18pt | 0pt | Category names, transaction titles, buttons |
| `body` | 13pt | Regular (400) | 18pt | 0pt | Row amounts, list descriptions |
| `meta` | 12pt | Regular (400) | 16pt | 0pt | "safe to spend", dates, subtitles |
| `eyebrow` | 11pt | Medium (500) | 14pt | +0.5pt | Small all-caps labels (e.g. "DAILY CHECK-IN") |
| `badge` | 11pt | Medium (500) | 14pt | 0pt | Count pills ("9 active", "580% spent") |

```swift
public struct SelfBudgetType {
    public static let display = Font.system(size: 32, weight: .medium).monospacedDigit()
    public static let title = Font.system(size: 20, weight: .medium)
    public static let heading = Font.system(size: 16, weight: .medium)
    public static let section = Font.system(size: 15, weight: .medium)
    public static let rowTitle = Font.system(size: 14, weight: .medium)
    public static let body = Font.system(size: 13, weight: .regular).monospacedDigit()
    public static let meta = Font.system(size: 12, weight: .regular).monospacedDigit()
    public static let eyebrow = Font.system(size: 11, weight: .medium)
    public static let badge = Font.system(size: 11, weight: .medium).monospacedDigit()
}
```

### 2.6 Shape Scale & Corner Radii

No arbitrary corner radii are allowed. All shapes must use one of these six tokens:

```swift
public struct SelfBudgetShape {
    public static let pill: CGFloat = 999.0   // Buttons, active chips, badges
    public static let tile: CGFloat = 12.0    // Icon tiles (36x36pt container)
    public static let chip: CGFloat = 14.0    // Filter chips, segmented controls
    public static let card: CGFloat = 16.0    // Default section containers & lists
    public static let hero: CGFloat = 18.0    // Hero cards (Cash flow, balance hero)
    public static let page: CGFloat = 20.0    // Full-page frames
}
```

### 2.7 Reusable UI Component Library

#### 1. `SectionHeaderBand`
A unified, bordered container featuring a tinted header row and hairline-divided rows. Never separate the header from its rows into floating cards.

```swift
struct SectionHeaderBand<Content: View>: View {
    let title: String
    let ramp: ColorRamp
    var icon: String? = nil
    var countPill: String? = nil
    var trailingText: String? = nil
    var onTrailingClick: (() -> Void)? = nil
    @ViewBuilder let content: () -> Content

    @Environment(\.colorScheme) var colorScheme

    var body: some View {
        let isDark = colorScheme == .dark
        VStack(spacing: 0) {
            // Header Row
            HStack(spacing: 8) {
                if let icon = icon {
                    Image(systemName: icon)
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(isDark ? ColorRamp.gray.hex(400) : ramp.secondaryText(isDark: false))
                }
                Text(title)
                    .font(SelfBudgetType.section)
                    .foregroundColor(isDark ? Color.appTextPrimary(isDark: true) : ramp.titleText(isDark: false))
                Spacer()
                if let countPill = countPill {
                    Text(countPill)
                        .font(SelfBudgetType.badge)
                        .foregroundColor(ramp.pillText(isDark: isDark))
                        .padding(.horizontal, 8)
                        .padding(.vertical, 2)
                        .background(ramp.pillFill(isDark: isDark))
                        .clipShape(Capsule())
                }
                if let trailingText = trailingText {
                    Text(trailingText)
                        .font(SelfBudgetType.meta)
                        .foregroundColor(isDark ? Color.appTextSecondary(isDark: true) : ramp.secondaryText(isDark: false))
                        .onTapGesture { onTrailingClick?() }
                }
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 11)
            .background(isDark ? Color.appCardSurface(isDark: true) : ramp.tintFill(isDark: false))

            // Body Content
            VStack(spacing: 0) {
                content()
            }
        }
        .background(Color.appBackground(isDark: isDark))
        .clipShape(RoundedRectangle(cornerRadius: SelfBudgetShape.card))
        .overlay(
            RoundedRectangle(cornerRadius: SelfBudgetShape.card)
                .stroke(isDark ? Color.appDivider(isDark: true) : ramp.containerBorder(isDark: false), lineWidth: 0.5)
        )
    }
}
```

#### 2. `RampIconTile` and `GrayIconTile`
Standard 36x36pt squircle container with an 18pt centered icon:
```swift
struct RampIconTile: View {
    let systemName: String
    let ramp: ColorRamp
    @Environment(\.colorScheme) var colorScheme

    var body: some View {
        let isDark = colorScheme == .dark
        ZStack {
            RoundedRectangle(cornerRadius: SelfBudgetShape.tile)
                .fill(isDark ? ramp.hex(900) : ramp.hex(50))
            Image(systemName: systemName)
                .font(.system(size: 16, weight: .medium))
                .foregroundColor(isDark ? ramp.hex(400) : ramp.secondaryText(isDark: false))
        }
        .frame(width: 36, height: 36)
    }
}
```

#### 3. Buttons: `PrimaryPillButton`, `SecondaryPillButton`, `DestructivePillButton`
- `PrimaryPillButton`: Solid CTA button. In light mode: `ramp.hex(800)` fill with `c50` text. In dark mode: `#196338` Dark Green fill with `#FFFFFF` pure white text. Height 48pt, shape `Capsule()`.
- `SecondaryPillButton`: Outlined button. `ramp.hex(100)` border with `ramp.hex(800)` text in light mode; `ramp.hex(800)` border with `ramp.hex(200)` text in dark mode. Height 48pt.
- `DestructivePillButton`: Outlined Red button (`Ramp.red`). Never solid. Always isolated from primary/secondary button rows.

#### 4. `CircularBackButton`
Unified back button used across every sub-screen, form, and settings page:
```swift
struct CircularBackButton: View {
    let action: () -> Void
    @Environment(\.colorScheme) var colorScheme

    var body: some View {
        Button(action: action) {
            ZStack {
                Circle()
                    .fill(ColorRamp.gray.tintFill(isDark: colorScheme == .dark))
                Image(systemName: "chevron.left")
                    .font(.system(size: 16, weight: .medium))
                    .foregroundColor(Color.appTextPrimary(isDark: colorScheme == .dark))
            }
            .frame(width: 38, height: 38)
        }
    }
}
```

### 2.8 The 18 Strict Structural Rules

1. **Header holds only close/back + title**: Save/Submit lives in the footer or sticky bottom bar, never duplicated in both header and footer.
2. **Never repeat a card's own headline**: If the navigation bar reads "Net Worth History", the top card must not repeat "Net Worth History".
3. **Never show the same headline number in two cards on one screen**: One card is the single canonical source of truth.
4. **Destructive actions are strictly isolated**: `DestructivePillButton` is always outlined Red, never solid, and never placed adjacent to a primary/secondary button row.
5. **Sign-out / Log-out is not destructive**: Styled as a neutral `SecondaryPillButton(ramp: .gray)`, never Red.
6. **Icon tile color follows semantics, not decoration**: Category rows use `RampIconTile` (colored). Generic fields (date, amount, settings) use `GrayIconTile`.
7. **No emoji in UI text**: All statuses and visual cues use color ramps + SF Symbols, not emoji glyphs.
8. **One canonical calculation, many call sites**: Single shared calculation engine files in `Core/Util/`.
9. **Ephemeral status messages are color-coded by outcome**: Failure reads `Ramp.red`, success reads `Ramp.teal`.
10. **150pt bottom scroll clearance**: Every primary scroll view ends with `Spacer().frame(height: 150)` so content is never obscured by the tab bar or floating buttons.
11. **Text Field Capitalization (Word Title Case)**: All user text inputs enforce Word Title Case (`toWordTitleCase()`).
12. **Dark Mode Muted Icon Style**: Muted `c900` background with `c400` icon glyph in dark mode.
13. **Uniform Line-Item Row Amount Typography**: Line-item amounts use `SelfBudgetType.body` (13pt Regular with `monospacedDigit()`).
14. **Uniform Page & Modal Header Typography**: Page and modal titles use `SelfBudgetType.title` (20pt Medium). Section headings use `SelfBudgetType.heading` (16pt Medium).
15. **Uniform Read-Only Modal Edit Action Color (`Ramp.teal`)**: Detail modal edit action uses `PrimaryPillButton(ramp: .teal)`.
16. **Subpage Navigation with Circular Back Button**: All subpages and forms use `CircularBackButton`. Only the root global "+" entry chooser uses an "✕" (Close) button.
17. **Header Container: Flat, background-colored, no divider**: Top header surface uses `Color.appBackground`, flat without elevation or borders, preventing the 38pt back button from blending into `.surface`.
18. **Uniform Back-Button Inset**: Exactly 16pt inset from the screen's leading edge, and 8pt trailing gap before the title text.

---

## 3. Data Persistence & Schema (SwiftData / Core Data v22)

The database schema matches Room Database Schema v22 with complete non-destructive data parity.

### 3.1 Entity Model Specifications

```swift
import SwiftData
import Foundation

@Model
final class AccountEntity {
    @Attribute(.unique) var id: String
    var userId: String
    var name: String
    var typeRaw: String // Checking, Savings, Cash, CreditCard, Loan, Mortgage, AutoLoan, StudentLoan, Investment, Retirement, Custom
    var initialBalance: Double
    var currencyCode: String
    var colorHex: String
    var creditLimit: Double?
    var apr: Double?
    var minimumPayment: Double?
    var createdAt: Int64

    init(id: String = UUID().uuidString, userId: String, name: String, typeRaw: String, initialBalance: Double = 0.0, currencyCode: String = "USD", colorHex: String = "#1D9E75", creditLimit: Double? = nil, apr: Double? = nil, minimumPayment: Double? = nil, createdAt: Int64 = Int64(Date().timeIntervalSince1970 * 1000)) {
        self.id = id
        self.userId = userId
        self.name = name
        self.typeRaw = typeRaw
        self.initialBalance = initialBalance
        self.currencyCode = currencyCode
        self.colorHex = colorHex
        self.creditLimit = creditLimit
        self.apr = apr
        self.minimumPayment = minimumPayment
        self.createdAt = createdAt
    }
}

@Model
final class TransactionEntity {
    @Attribute(.unique) var id: String
    var userId: String
    var title: String
    var amount: Double
    var typeRaw: String // EXPENSE, INCOME, TRANSFER
    var categoryId: String
    var accountId: String
    var transferAccountId: String?
    var timestamp: Int64
    var note: String?
    var receiptUri: String?

    init(id: String = UUID().uuidString, userId: String, title: String, amount: Double, typeRaw: String, categoryId: String, accountId: String, transferAccountId: String? = nil, timestamp: Int64 = Int64(Date().timeIntervalSince1970 * 1000), note: String? = nil, receiptUri: String? = nil) {
        self.id = id
        self.userId = userId
        self.title = title
        self.amount = amount
        self.typeRaw = typeRaw
        self.categoryId = categoryId
        self.accountId = accountId
        self.transferAccountId = transferAccountId
        self.timestamp = timestamp
        self.note = note
        self.receiptUri = receiptUri
    }
}

@Model
final class BudgetEntity {
    @Attribute(.unique) var id: String
    var userId: String
    var categoryId: String
    var amountLimit: Double
    var monthYear: String // "yyyy-MM"
    var isRolloverEnabled: Bool
    var isAutoSynced: Bool

    init(id: String = UUID().uuidString, userId: String, categoryId: String, amountLimit: Double, monthYear: String, isRolloverEnabled: Bool = false, isAutoSynced: Bool = true) {
        self.id = id
        self.userId = userId
        self.categoryId = categoryId
        self.amountLimit = amountLimit
        self.monthYear = monthYear
        self.isRolloverEnabled = isRolloverEnabled
        self.isAutoSynced = isAutoSynced
    }
}

@Model
final class RecurringTransactionEntity {
    @Attribute(.unique) var id: String
    var userId: String
    var title: String
    var amount: Double
    var typeRaw: String // EXPENSE, INCOME
    var categoryId: String
    var frequencyRaw: String // WEEKLY, BI_WEEKLY, MONTHLY, YEARLY
    var nextDueDate: Int64
    var lastPostedDate: String? // "yyyy-MM-dd"
    var isAutoPost: Bool
    var isArchived: Bool
    var createdAt: Int64

    init(id: String = UUID().uuidString, userId: String, title: String, amount: Double, typeRaw: String, categoryId: String, frequencyRaw: String, nextDueDate: Int64, lastPostedDate: String? = nil, isAutoPost: Bool = false, isArchived: Bool = false, createdAt: Int64 = Int64(Date().timeIntervalSince1970 * 1000)) {
        self.id = id
        self.userId = userId
        self.title = title
        self.amount = amount
        self.typeRaw = typeRaw
        self.categoryId = categoryId
        self.frequencyRaw = frequencyRaw
        self.nextDueDate = nextDueDate
        self.lastPostedDate = lastPostedDate
        self.isAutoPost = isAutoPost
        self.isArchived = isArchived
        self.createdAt = createdAt
    }
}

@Model
final class GoalEntity {
    @Attribute(.unique) var id: String
    var userId: String
    var name: String
    var targetAmount: Double
    var savedAmount: Double
    var targetDate: Int64?
    var linkedAccountId: String?
    var createdAt: Int64

    init(id: String = UUID().uuidString, userId: String, name: String, targetAmount: Double, savedAmount: Double = 0.0, targetDate: Int64? = nil, linkedAccountId: String? = nil, createdAt: Int64 = Int64(Date().timeIntervalSince1970 * 1000)) {
        self.id = id
        self.userId = userId
        self.name = name
        self.targetAmount = targetAmount
        self.savedAmount = savedAmount
        self.targetDate = targetDate
        self.linkedAccountId = linkedAccountId
        self.createdAt = createdAt
    }
}

@Model
final class ActivityLogEntity {
    @Attribute(.unique) var id: String
    var userId: String
    var entityTypeRaw: String
    var entityId: String
    var actionRaw: String
    var title: String
    var amount: Double?
    var timestamp: Int64

    init(id: String = UUID().uuidString, userId: String, entityTypeRaw: String, entityId: String, actionRaw: String, title: String, amount: Double? = nil, timestamp: Int64 = Int64(Date().timeIntervalSince1970 * 1000)) {
        self.id = id
        self.userId = userId
        self.entityTypeRaw = entityTypeRaw
        self.entityId = entityId
        self.actionRaw = actionRaw
        self.title = title
        self.amount = amount
        self.timestamp = timestamp
    }
}
```

### 3.2 Database Indexes & Performance Optimization

To match Schema v22, SQLite compound indices must be configured:
1. `TransactionEntity`:
   - Composite Index: `(userId, timestamp)`
   - Single Index: `accountId`
   - Single Index: `categoryId`
2. `ActivityLogEntity`:
   - Composite Index: `(userId, timestamp)`
3. `BudgetEntity`:
   - Unique Compound Index: `(userId, categoryId, monthYear)`

### 3.3 Activity Log Automatic Pruning Engine

Whenever a new `ActivityLogEntity` is inserted, automatically prune records exceeding 1,000 items:

```swift
func logActivity(entry: ActivityLogEntity, context: ModelContext) {
    context.insert(entry)
    try? context.save()

    // Prune entries older than the top 1000 for this user
    let userId = entry.userId
    var fetchDescriptor = FetchDescriptor<ActivityLogEntity>(
        predicate: #Predicate { $0.userId == userId },
        sortBy: [SortDescriptor(\.timestamp, order: .reverse)]
    )
    fetchDescriptor.fetchOffset = 1000

    if let oldEntries = try? context.fetch(fetchDescriptor) {
        for item in oldEntries {
            context.delete(item)
        }
        try? context.save()
    }
}
```

---

## 4. Financial Calculation Engines & Business Logic

All financial calculations must execute with **exact cent precision**, eliminating binary floating-point rounding errors.

### 4.1 Cent-Safe Precision Math (`CentSafeMoney`)

```swift
import Foundation

public struct CentSafeMoney {
    public static func round(_ value: Double) -> Double {
        let decimal = Decimal(value)
        var rounded = Decimal()
        var mutableDecimal = decimal
        NSDecimalRound(&rounded, &mutableDecimal, 2, .plain)
        return (rounded as NSDecimalNumber).doubleValue
    }

    public static func toCents(_ value: Double) -> Int64 {
        let decimal = Decimal(value)
        var rounded = Decimal()
        var mutableDecimal = decimal
        NSDecimalRound(&rounded, &mutableDecimal, 2, .plain)
        return (rounded * 100 as NSDecimalNumber).int64Value
    }

    public static func fromCents(_ cents: Int64) -> Double {
        return Double(cents) / 100.0
    }

    public static func add(_ a: Double, _ b: Double) -> Double {
        fromCents(toCents(a) + toCents(b))
    }

    public static func subtract(_ a: Double, _ b: Double) -> Double {
        fromCents(toCents(a) - toCents(b))
    }

    public static func sum(_ values: [Double]) -> Double {
        let totalCents = values.reduce(Int64(0)) { $0 + toCents($1) }
        return fromCents(totalCents)
    }
}
```

### 4.2 Account Balance & Signed Liability Debt Engine

```swift
public struct AccountBalanceCalculator {
    public static func isLiability(typeRaw: String) -> Bool {
        let t = typeRaw.uppercased()
        return t == "CREDIT_CARD" || t == "LOAN" || t == "MORTGAGE" || t == "AUTO_LOAN" || t == "STUDENT_LOAN"
    }

    public static func computeBalance(account: AccountEntity, transactions: [TransactionEntity]) -> Double {
        var deltaCents: Int64 = 0
        for tx in transactions {
            let amountCents = CentSafeMoney.toCents(tx.amount)
            if tx.typeRaw == "INCOME" && tx.accountId == account.id {
                deltaCents += amountCents
            } else if tx.typeRaw == "EXPENSE" && tx.accountId == account.id {
                deltaCents -= amountCents
            } else if tx.typeRaw == "TRANSFER" && tx.accountId == account.id {
                deltaCents -= amountCents
            } else if tx.typeRaw == "TRANSFER" && tx.transferAccountId == account.id {
                deltaCents += amountCents
            } else if tx.typeRaw == "EXPENSE" && tx.transferAccountId == account.id {
                // Debt payment from another account credits this liability
                deltaCents += amountCents
            }
        }
        return CentSafeMoney.fromCents(CentSafeMoney.toCents(account.initialBalance) + deltaCents)
    }

    public static func getMonthTimestampRange(monthYear: String) -> (Int64, Int64) {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM"
        formatter.timeZone = .current
        guard let startDate = formatter.date(from: monthYear) else { return (0, Int64.max) }

        var calendar = Calendar.current
        calendar.timeZone = .current
        let startComponents = calendar.dateComponents([.year, .month], from: startDate)
        let startOfMonth = calendar.date(from: startComponents) ?? startDate
        let startMs = Int64(startOfMonth.timeIntervalSince1970 * 1000)

        guard let range = calendar.range(of: .day, in: .month, for: startOfMonth),
              let endDay = range.last else { return (startMs, Int64.max) }

        var endComponents = startComponents
        endComponents.day = endDay
        endComponents.hour = 23
        endComponents.minute = 59
        endComponents.second = 59
        endComponents.nanosecond = 999_000_000

        let endOfMonth = calendar.date(from: endComponents) ?? startDate
        let endMs = Int64(endOfMonth.timeIntervalSince1970 * 1000)
        return (startMs, endMs)
    }
}
```

### 4.3 Recurring Frequency Normalization Engine

All recurring transactions are converted to their monthly equivalent:

$$\text{Monthly Amount} = \begin{cases} 
\text{amount} \times \frac{52}{12} & \text{if WEEKLY} \\
\text{amount} \times \frac{26}{12} & \text{if BI\_WEEKLY} \\
\text{amount} & \text{if MONTHLY} \\
\text{amount} \div 12 & \text{if YEARLY}
\end{cases}$$

### 4.4 Persistent Forward-Inheriting Monthly Budgeting

- **Baseline Inheritance**: A budget set at month $M$ applies to all subsequent months $T \ge M$ automatically unless overwritten.
- **Historical Immutability**: Editing or deleting a budget at month $M$ sets a checkpoint from month $M$ forward; months prior to $M$ never mutate.
- **Auto-Sync Ceiling**: When `isAutoSynced == true`, recurring bills update the ceiling limit:

$$\text{Suggested Ceiling} = \lceil \text{RecurringFrequencyNormalizer.toMonthly}(\text{amount}, \text{frequency}) \rceil$$

### 4.5 Monthly Cash Flow Equation Engine

The 4-column Cash Flow equation displayed on the Dashboard Hero card:
1. **Income Column (🟢)**: $\max(\text{Logged Income}, \text{Expected Paychecks}) + \text{Ad-Hoc Income}$
2. **Budgets Column (🔵)**: $\sum \max(\text{EffectiveLimit}_c, \, \text{RecurringBill}_c)$ across all budgeted categories.
3. **Unbudgeted Column (Neutral Slate)**: $\sum \max(\text{RecurringBill}, \text{ActualSpent})$ for categories with no active budget.
4. **Paid Column (Conditional Red)**: Total posted expense transactions for the month.
- **Headline Result**:
  $$\text{Free Cash} = \text{Income} - \text{Budgets} - \text{Unbudgeted} - \text{Committed Goals}$$
  - If $\text{Free Cash} \ge 0$: Display `"$X Free"`.
  - If $\text{Free Cash} < 0$: Display `"$X Over Budget"`.

### 4.6 Linear-Time Net Worth Historical Snapshots

Runs in $O(N \log N + M \times A)$ by sorting transactions once and advancing a single cursor through the months:

```swift
public static func computeHistoricalSnapshots(
    userId: String,
    accounts: [AccountEntity],
    allTransactions: [TransactionEntity],
    baseCurrency: String
) -> [NetWorthSnapshotEntity] {
    let sortedTxs = allTransactions.sorted { $0.timestamp < $1.timestamp }
    var runningDeltas = [String: Int64]()
    var txIndex = 0

    // Build list of months from earliest transaction to current month
    // Walk through months:
    // 1. Advance txIndex and update runningDeltas for all tx.timestamp <= monthEndMs
    // 2. Sum assets, liabilities, and total net worth
    // 3. Append NetWorthSnapshotEntity
}
```

---

## 5. Screen-by-Screen Layout & Feature Specifications

### 5.1 Main Tab Navigation & Top App Bar
- **Tab Bar**: 5 items with custom icons:
  1. `Home` (`house.fill`)
  2. `Plan` (`target`)
  3. `Recurring` (`repeat`)
  4. `Analytics` (`chart.bar.xaxis`)
  5. `Activity` (`clock.arrow.circlepath`)
- **Global "+" Floating Action Button**: Positioned above the tab bar, opening `AddEntryPointScreen`.
- **Top App Bar**:
  - Top Left: App Brand Logo badge (28x28pt) + "Self Budget" text.
  - Top Center: Unified Month Navigation Pill (`< August 2026 >`).
  - Top Right: User circular initial avatar (e.g. "JD"), opening `SettingsScreen`.

### 5.2 Screen 1: Dashboard (`HomeScreen`)
- **Cash Flow Hero Card**: 4-column layout (`Income` | `Budgets` | `Unbudgeted` | `Paid`) with responsive typography scaling (`13pt` $\rightarrow$ `11.5pt` $\rightarrow$ `10pt`).
- **Safe-to-Spend Summary Banner**: Pill component showing remaining safe spend for the active month.
- **Accounts Carousel**: Horizontally scrolling carousel of account balance cards (135pt width) with a 60pt `+` button. Includes `[ ✏️ Edit ]` toggle to switch between activity feed view and account edit/delete modal.
- **Recent Activity Preview**: Displays top 5 recent transactions with a prominent `View All (X) ›` button opening the full history sheet.

### 5.3 Screen 2: Plan Tab (`BudgetScreen`)
- **Segmented Control**: `[ Budgets | Savings Goals ]` (Ramp.teal active fill).
- **Total Budget Hero Card**: Progress bar, total limit, total spent, and remaining budget.
- **Daily Pace Safeguard Banner**: Max daily spend limit ($\max(0, \text{Remaining} / \text{Days Left})$).
- **Category Budget Cards**: Neobank-styled cards showing category icon tile, progress bar (Teal < 80%, Amber 80-100%, Red > 100%), Safe-to-Spend pill, and commitment badges.

### 5.4 Screen 3: Recurring Tab (`RecurringScreen`)
- **Monthly Commitments Hero**: Paychecks vs Bills tiles.
- **Filter Chips**: `[ All | Bills | Paychecks ]`.
- **Recurring Item Cards**: Next due date, cycle-aware posted badge (`Posted for Aug ✅`), 1-tap `Post Now` button (with duplicate confirmation modal), and muted archived card styling.

### 5.5 Screen 4: Analytics Tab (`AnalyticsScreen`)
- **Timeframe Selector**: `[ Monthly (Month Year) | Annual (Year YTD) ]`.
- **Spending Breakdown**: Visual progress bars and category breakdown rings.
- **Annual Spending Pace**: Extrapolated pace projection.
- **Debt Payoff Comparison Card**: Month-over-month debt reduction difference (% and $).

### 5.6 Screen 5: Activity Tab (`SearchScreen`)
- **AppSearchBar**: Instant text filtering by merchant, note, or account.
- **Filter Chips Row**: Date range, transaction type, category, account, and sort order.
- **Virtualized LazyVStack**: Wrapped inside a single `SectionHeaderBand` card style, rendering rows using `ForEach(sortedEntries, id: \.id)` so iOS recycles rows with constant 60/120 FPS performance.

### 5.7 Global "+" Chooser (`AddEntryPointScreen`)
Opens a full-page modal with four 1-tap entry cards:
1. **Add Income** (Deposit money, link paycheck)
2. **Add Expense** (Merchant, card selection, receipt OCR scan)
3. **Create a Budget** (Set category limit)
4. **Add a Recurring Bill** (Subscriptions, rent, utility schedules)
5. **Add an Account or Wallet** (Checking, credit card, cash wallet)

---

## 6. Native iOS Platform Integrations

### 6.1 Biometrics & Privacy Shield (`LocalAuthentication`)

```swift
import LocalAuthentication
import SwiftUI

@Observable
final class SecurityManager {
    var isAppLocked: Bool = true

    func authenticate() {
        let context = LAContext()
        var error: NSError?

        if context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) {
            context.evaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, localizedReason: "Unlock Self Budget to access your finances") { success, _ in
                DispatchQueue.main.async {
                    if success { self.isAppLocked = false }
                }
            }
        }
    }
}
```
When `isAppLocked == true`, the root view renders `AppLockScreen`, masking all financial numbers with a solid background shield.

### 6.2 Vision OCR Receipt Scanning

```swift
import Vision
import UIKit

func scanReceipt(image: UIImage, completion: @escaping (String?, Double?, Date?) -> Void) {
    guard let cgImage = image.cgImage else { return }
    let request = VNRecognizeTextRequest { request, _ in
        guard let observations = request.results as? [VNRecognizedTextObservation] else { return }
        let recognizedStrings = observations.compactMap { $0.topCandidates(1).first?.string }
        // Regex parse merchant title, total currency amount, and transaction date
        parseReceiptLines(recognizedStrings, completion: completion)
    }
    request.recognitionLevel = .accurate
    try? VNImageRequestHandler(cgImage: cgImage, options: [:]).perform([request])
}
```

### 6.3 Push Notifications (`UserNotifications`)

Schedules daily 8:00 AM bill reminder notifications matching Android's `BillReminderWorker`:
- Reminds user 1 day before a recurring bill is due or on the due date.
- Uses exact Option C Neobank emojis: `📆` `🎯` `🚀` `💳`.

### 6.4 Google Drive Cloud Sync Interoperability

To allow an iOS user to seamlessly restore backups created by the Android app:
- Connect to Google Drive REST API v3 using the `https://www.googleapis.com/auth/drive.appdata` scope.
- Target the hidden file `self_budget_cloud_backup.json` inside `appDataFolder`.
- Serialize and deserialize `SyncDataPayload` with identical JSON keys (`accounts`, `transactions`, `budgets`, `recurringTransactions`, `goals`, `categories`, `exchangeRates`, `netWorthSnapshots`).

---

## 7. Automated Verification Suite (XCTest)

The iOS project must include a test suite with 100% parity to the 159 Android unit tests:
1. `CentSafeMoneyTests`: Cent rounding and floating-point drift elimination.
2. `AccountBalanceTests`: Signed liability debt reduction and net worth invariance.
3. `CashFlowCalculationTests`: 4-column cash flow equation and income exclusion safeguards.
4. `RecurringFrequencyNormalizerTests`: Weekly, bi-weekly, monthly, yearly conversions.
5. `BudgetRolloverTests`: Multi-month persistent deficit carryover across Dec $\rightarrow$ Jan.
6. `BudgetCalculatorTests`: Forward-inheriting baseline budgets and historical immutability.
7. `DebtPayoffCalculatorTests`: Amortization estimates and 0% APR simple division.
8. `CloudSyncSerializationTests`: Cent-safe JSON round-trip compatibility with Android payloads.
9. `FullLifecycleIntegrationTests`: Complete multi-month end-to-end financial workflows.

---
*End of Absolute iOS Replica Specification.*
