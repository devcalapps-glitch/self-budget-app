# Self Budget — Design System Guide

This is the canonical reference for Self Budget's Jetpack Compose design system:
every color, type style, shape, and shared component a screen should be built
from, plus the structural rules that keep every screen consistent. It reflects
what's actually implemented in `ui/theme/` and `core/ui/components/` — if this
doc and the code ever disagree, the code wins; fix the doc.

Use this doc when building a new screen, or when bringing an older screen up
to date: replace ad-hoc `MaterialTheme.typography.*`, raw hex colors,
`CircleShape` icon badges, and plain `Button`/`OutlinedButton` calls with the
tokens and components below.

## Table of contents
1. [Core principle: ramps](#1-core-principle-ramps)
2. [Color ramps](#2-color-ramps)
3. [Neutral surfaces & text](#3-neutral-surfaces--text)
4. [Money & status colors](#4-money--status-colors)
5. [Typography scale](#5-typography-scale)
6. [Shape scale](#6-shape-scale)
7. [Shared components](#7-shared-components)
8. [Structural rules](#8-structural-rules)
9. [File index](#9-file-index)
10. [Migrating an old screen](#10-migrating-an-old-screen)

---

## 1. Core principle: ramps

Every color in the app comes from one of eight **ramps** (`Ramp` enum in
[`Color.kt`](../app/src/main/java/com/selfbudget/app/ui/theme/Color.kt)). A
ramp is a family of 7 stops (`c50`…`c900`) of one hue. There are no ad-hoc hex
values anywhere else in the app — if a screen needs a new color, it picks a
ramp, not a hex code.

**One-ramp rule**: a single colored element (a tinted card, a status pill, a
category icon tile) draws its fill, title text, secondary text/icon, and
border all from the **same ramp**. Light and dark mode are handled by
flipping which *stop* of that ramp is used, never by switching hue. This is
what all the `Ramp.*` extension functions below encode — always reach for one
of them instead of hand-picking a stop.

```kotlin
val isDark = isAppInDarkTheme()
Surface(color = Ramp.Teal.tintFill(isDark), border = BorderStroke(1.dp, Ramp.Teal.containerBorder(isDark))) {
    Text("Balance", color = Ramp.Teal.titleText(isDark))
    Icon(icon, tint = Ramp.Teal.secondaryText(isDark))
}
```

## 2. Color ramps

| Ramp | 50 | 100 | 200 | 400 | 600 | 800 | 900 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Teal** | `#E1F5EE` | `#9FE1CB` | `#5DCAA5` | `#1D9E75` | `#0F6E56` | `#085041` | `#04342C` |
| **Blue** | `#E6F1FB` | `#B5D4F4` | `#85B7EB` | `#378ADD` | `#185FA5` | `#0C447C` | `#042C53` |
| **Purple** | `#EEEDFE` | `#CECBF6` | `#AFA9EC` | `#7F77DD` | `#534AB7` | `#3C3489` | `#26215C` |
| **Coral** | `#FAECE7` | `#F5C4B3` | `#F0997B` | `#D85A30` | `#993C1D` | `#712B13` | `#4A1B0C` |
| **Amber** | `#FAEEDA` | `#FAC775` | `#EF9F27` | `#BA7517` | `#854F0B` | `#633806` | `#412402` |
| **Red** | `#FCEBEB` | `#F7C1C1` | `#F09595` | `#E24B4A` | `#A32D2D` | `#791F1F` | `#501313` |
| **Pink** | `#FBEAF0` | `#F4C0D1` | `#ED93B1` | `#D4537E` | `#993556` | `#72243E` | `#4B1528` |
| **Gray** | `#F1EFE8` | `#D3D1C7` | `#B4B2A9` | `#888780` | `#5F5E5A` | `#444441` | `#2C2C2A` |

**Teal / Dark Green is the app's only brand color** — active tab, selected filter,
links, and primary actions all read from it (`getBrandColor()`/`getAccentColor()`).
In dark mode, Teal stops dynamically resolve to authentic **Dark Green** tones
(`solidFill` `#196338`, `onSolidFill` `Color.White`, `tintFill` `#0E2E18`,
`titleText` `Color.White`, `secondaryText` `#81C784`, `containerBorder` `#1B542C`).

### Ramp extension functions (pick one, never a raw stop)

| Function | Light | Dark | Use for |
| :--- | :--- | :--- | :--- |
| `tintFill(isDark, large=false)` | c50 (c100 if `large`) | c900 (Dark Green `#0E2E18` for Teal) | Tinted card/chip/hero background |
| `titleText(isDark)` | c900 | c100 (White `#FFFFFF` for Teal) | Title text/icon on a tint |
| `secondaryText(isDark)` | c600 | c400 (Green `#81C784` for Teal) | Secondary text, band icon/meta text |
| `icon(isDark)` | = `secondaryText` | = `secondaryText` | Icon on a plain (non-filled) surface |
| `containerBorder(isDark)` | c100 | c800 (Green `#1B542C` for Teal) | 0.5px border on a sectioned container |
| `pillFill(isDark)` | c100 | c800 (Green `#1B542C` for Teal) | Count-pill background on a header band |
| `pillText(isDark)` | c800 | c200 (`#C8E6C9` for Teal) | Count-pill text on a header band |
| `solidFill(isDark)` | c800 | c200 (Dark Green `#196338` for Teal) | Solid selected/active control (active tab, primary pill bg) |
| `onSolidFill(isDark)` | c50 | c900 (White `#FFFFFF` for Teal) | Text/icon on top of `solidFill` |

### Section identity colors

Each content section keeps one hue across every page (`sectionRamp()`):

| Section | Ramp |
| :--- | :--- |
| Housing & Essentials | Blue |
| Food & Daily Living | Teal |
| Lifestyle & Entertainment | Pink |
| Debt & Financial | Coral |
| Custom Categories | Purple |
| Accounts and wallets | Teal |
| Recent activity | Purple |
| Earned Income | Teal |
| Investments & Passive | Purple |
| Gifts & Other | Pink |
| *(anything unmapped, "Other")* | Gray |

### Status thresholds

`budgetStatus(spent, limit)` → `BudgetStatus`, used by `StatusProgressBar`/`StatusBadge`:

| Spent-of-limit | Status | Ramp |
| :--- | :--- | :--- |
| < 80% | Safe | Teal |
| 80–100% | Watch | Amber |
| > 100% | Over | Red |

## 3. Neutral surfaces & text

| Token | Light | Dark |
| :--- | :--- | :--- |
| Page background | `#FFFFFF` | `#1C1D1C` |
| Card surface | Gray c50 | Gray c900 |
| Divider | `rgba(0,0,0,0.08)` | Gray c800 |
| Text primary | Gray c900 | Gray c50 |
| Text secondary | Gray c600 | Gray c200 |
| Text muted (`TextMuted`) | Gray c400 | Gray c400 |
| Progress track | Gray c50 | Gray c800 |

These back Material3's `colorScheme.background`/`surface`/`onSurface`/
`onSurfaceVariant` directly (see [`Theme.kt`](../app/src/main/java/com/selfbudget/app/ui/theme/Theme.kt)),
so plain `MaterialTheme.colorScheme.*` calls are fine for neutral text/surfaces
— it's only *colored* elements that must go through a `Ramp`.

## 4. Money & status colors

| Function | Meaning | Light | Dark |
| :--- | :--- | :--- | :--- |
| `getIncomeColor()` | Income / positive amounts | Teal 600 | Green 400 (`#81C784`) |
| `getExpenseColor()` | "This is an expense" semantics — type badges, outflow dots, over-limit/negative-balance text. **Not** for ordinary expense row amounts, which stay neutral `onSurface`. | Red 600 | Red 200 |
| `getWarningColor()` | Watch-status / 80–100% of budget | Amber 800 | Amber 100 |
| `getBrandColor()` / `getAccentColor()` | Brand/interactive — active tab, links, primary actions | Teal 600 | Green 400 (`#81C784`) |
| `getProgressBarColor(lightColor, isOverLimit)` | Progress bar fill: uses `lightColor` in light mode; in dark mode uses dark green (`#196338`) normally or Red 200 (`#F09595`) when over limit | `lightColor` | Dark Green (`#196338`) / Red 200 (`#F09595`) |

**Dark Mode Green & Contrast Rules**:
1. In dark mode, do not use Teal cyan/blue-green tones; use authentic **Dark Green** tokens (`#196338` for solid CTA/active controls, `#0E2E18` for tint background, `#81C784` for soft green text/icons).
2. If a dark green background exists on CTA buttons (e.g. `PrimaryPillButton`) or Hero cards, the text on top must strictly be **pure white** (`#FFFFFF` / `Color.White`) for high legibility and contrast.

Money-direction color rule (from `TransactionAmountHero`): a **type badge** is
the only element allowed to carry money-direction color. The amount digits
themselves stay neutral in every variant, and +/− steppers are both neutral
gray — never tint one of them red/green.

## 5. Typography scale

`SelfBudgetType` (in [`Type.kt`](../app/src/main/java/com/selfbudget/app/ui/theme/Type.kt)).
Only two weights exist app-wide — **Normal (400)** for body/metadata, **Medium
(500)** for everything else. Never Bold/SemiBold/ExtraBold. Every style has
tabular figures (`tnum`) so money lines up in columns.

| Token | Size / weight | Use for |
| :--- | :--- | :--- |
| `display` | 32sp / Medium | Hero numbers ($26,638.36) |
| `title` | 20sp / Medium | Hero headline ("You're on track") |
| `heading` | 16sp / Medium | Page section titles ("Spending plan") |
| `section` | 15sp / Medium | Header band titles |
| `rowTitle` | 14sp / Medium | Category/transaction names, button labels |
| `body` | 13sp / Normal | Row amounts, descriptions |
| `meta` | 12sp / Normal | "safe to spend", dates, "$X of $Y" |
| `eyebrow` | 11sp / Medium, tracked | "DAILY CHECK-IN" — apply `.uppercase()` at call site |
| `badge` | 11sp / Medium | Pills: "580% spent", "9 active" |

`MaterialTheme.typography.*` slots are wired to this same scale (see the
`SelfBudgetTypography` mapping in `Type.kt`), so legacy `titleMedium`/
`bodySmall` etc. calls aren't *wrong*, but prefer `SelfBudgetType.*` directly —
it has the slots (`eyebrow`, `badge`, `section`) Material3 doesn't, and makes
the intent explicit at the call site.

## 6. Shape scale

`ShapePill`/`ShapeTile`/… in [`Shapes.kt`](../app/src/main/java/com/selfbudget/app/ui/theme/Shapes.kt).
No other `RoundedCornerShape(N.dp)` should ship — replace ad-hoc radii with one of these.

| Token | Radius | Use for |
| :--- | :--- | :--- |
| `ShapePill` | 999dp | Buttons, chips, badges |
| `ShapeTile` | 12dp | Icon tiles |
| `ShapeChip` | 14dp | Filter chips, segmented controls |
| `ShapeCard` | 16dp | Sectioned cards (the default container shape) |
| `ShapeHero` | 18dp | Hero cards (amount entry, dashboard hero) |
| `ShapePage` | 20dp | Full-page-frame containers only |

## 7. Shared components

All in `com.selfbudget.app.core.ui.components` unless noted. Reach for these
before writing a one-off `Row`/`Surface`/`Button`.

| Component | File | What it's for |
| :--- | :--- | :--- |
| `RampIconTile(icon, ramp)` | `IconTile.kt` | Colored icon tile for category/money-direction/danger-flagged rows |
| `GrayIconTile(icon)` | `IconTile.kt` | Neutral icon tile for field-type/generic detail rows (forms, settings) |
| `PrimaryPillButton(text, onClick, ramp=Coral, icon=null)` | `AppButtons.kt` | The one solid action per card |
| `SecondaryPillButton(text, onClick, ramp=Teal, icon=null)` | `AppButtons.kt` | Outlined action, and neutral (Gray) actions like sign-out |
| `DestructivePillButton(text, onClick, icon=null)` | `AppButtons.kt` | Isolated destructive trigger — always outlined Red, never solid, never paired beside the primary/secondary row |
| `DoneChip(text, ramp=Teal)` | `AppButtons.kt` | Non-interactive "completed" chip (e.g. "Posted") instead of a re-clickable button |
| `ToggleRow(icon, title, checked, onCheckedChange, description=null)` | `ToggleRow.kt` | Settings toggle: gray tile + statement title + switch (Teal when on) |
| `FieldRow(icon, label, value, isPlaceholder=false, showChevron=false, onClick=null)` | `FieldRow.kt` | Form/detail-sheet field: gray tile + floating label + value (muted if placeholder) |
| `SectionHeaderBand(title, ramp, icon=null, countPill=null, ...) { content }` | `SectionHeaderBand.kt` | The sectioned-container pattern: tinted header band + hairline-divided rows, one bordered `Surface` — never a floating color bar above separate cards |
| `SectionRowDivider()` | `SectionHeaderBand.kt` | Hairline divider between rows inside a band |
| `DeltaBadge(percentChange, metric)` | `DeltaBadge.kt` | "vs last month" pill — color encodes whether the change is *good* for that metric, not its arithmetic sign; exactly 0% is always neutral gray |
| `StatusProgressBar(progress, status)` / `StatusBadge(text, status)` | `StatusProgressChip.kt` | Budget-status bar/pill, both read the same `BudgetStatus` |
| `NeutralBadge(text)` | `StatusProgressChip.kt` | Plain count/label pill (e.g. "13 active") |
| `FrequencySegmentedControl(selected, onSelect, ramp=Teal)` | `FrequencySegmentedControl.kt` | Shared recurring-frequency picker (Weekly/Bi-Wk/2x-Mo/Monthly/Yearly) |
| `TransactionAmountHero(type, amountText, onAmountChange, currencySymbol, ramp=type.ramp, stepAmount=1.0)` | `TransactionAmountHero.kt` | Shared amount-entry hero for every add/edit transaction form |
| `QuickAmountChips(presets, currencySymbol, onPick)` | `TransactionAmountHero.kt` | Inactive quick-add amount chips, 44dp min tap height |
| `FilterChipGroup` / `TypeFilterChip` | `SearchScreen.kt` | Shared filter-chip row (promoted from a private composable — reuse rather than re-copy) |

## 8. Structural rules

Learned/enforced while bringing the app's screens up to this system — treat
these as load-bearing, not optional style preferences:

1. **Header holds only close + title.** Edit/Save (or any page-level action)
   lives in the footer, never duplicated in both the header and the footer.
2. **Never repeat a card's own headline.** If a sub-page's top bar already
   reads "About Developer & Support" / "Privacy Policy" / etc., the first
   card on that page must not re-print the same title — start straight with
   the content (found and fixed on About, Delete Account & Data, Privacy
   Policy, Terms of Service, Net Worth).
3. **Never show the same headline number in two cards on one screen.** If two
   hero cards would show the identical figure (e.g. net worth), remove the
   duplicate — one canonical source of truth, one card.
4. **Destructive actions are isolated.** `DestructivePillButton` is always
   outlined Red, never solid, and never placed directly beside a
   `PrimaryPillButton`/`SecondaryPillButton` pair — it sits alone, typically
   with a one-line caption under it explaining exactly what gets wiped vs.
   kept. Only the actual confirm-step button in a confirmation dialog may be
   a solid `PrimaryPillButton(ramp = Ramp.Red)`.
5. **Sign-out / log-out is not destructive.** It's a plain, reversible
   action — style it as a neutral `SecondaryPillButton(ramp = Ramp.Gray)`,
   never Red.
6. **Icon tile color follows semantics, not decoration.** Category rows,
   money-direction, and danger-flagged rows use `RampIconTile` (colored).
   Generic/neutral field types (amount, date, account picker in a form,
   settings rows) use `GrayIconTile`.
7. **No emoji in UI text.** Status/labels communicate through the ramp
   system (color + icon), not emoji glyphs.
8. **One canonical calculation, many call sites.** When the same number
   (net worth, cycle-due amount, asset/liability classification) is computed
   in more than one place, those implementations drift — consolidate into a
   single function in `core/util/` (e.g. `AccountBalanceCalculator.isLiability()`,
   `RecurringCycleCalculator.getCyclePaymentSummary()`) and have every screen
   call it.
9. **Ephemeral status messages are color-coded by outcome**, not a single
   fixed color — a sync/export status line reads `Ramp.Red` on failure and
   `Ramp.Teal` on success, never `MaterialTheme.colorScheme.primary`
   regardless of outcome.
10. **150dp bottom scroll clearance** on every primary scrollable screen
    (`Spacer(Modifier.height(150.dp))` at the end of the content `Column`),
    120–150dp inside modal sheets, so content never sits under a FAB/nav bar.
11. **Text Field Capitalization (Word Title Case)**: In all user-entered text
    fields (account names, asset names, transaction titles, merchant names,
    category names, notes, and search bars), the first character of every word
    must always be capitalized. Always pair `KeyboardOptions(capitalization = KeyboardCapitalization.Words)`
    with programmatic formatting via `toWordTitleCase()` (`com.selfbudget.app.core.util.toWordTitleCase`)
    so that typing a space automatically capitalizes the first character of the
    next word consistently across all keyboards and paste inputs.
12. **Dark Mode Muted Icon Style**: In dark mode, all icon tiles (category rows,
    header bands, list items, and detail modals) must strictly follow the muted
    icon style established on the Budgeted Categories page: a subtle `c900`
    dark-tinted background container paired with a softened `c400` (`ramp.icon(isDark)`)
    glyph tint via `RampIconTile` (or `IconTile`). Never use high-contrast solid
    white glyphs on stark saturated backgrounds in dark mode.
13. **Uniform Subpage & Modal Row Amount Typography**: In both light and dark mode,
    all subpages and modal sheets (Spent & Committed Bills, Unbudgeted Expenses,
    Committed Recurring Bills, Committed Recurring Income, and Category Analytics)
    must strictly use uniform typography for row amounts matching the Budgeted
    Categories page: `SelfBudgetType.body` (13sp / Normal with `tnum`), while
    top hero summary amounts use `SelfBudgetType.display` (32sp / Medium with `tnum`).
    Never use oversized `heading` or ad-hoc weights for line-item row amounts.
14. **Uniform Page & Modal Header Typography**: All subpages, subcategory pages,
    modal sheets, and full-screen dialogs must strictly use `SelfBudgetType.title`
    (20sp / Medium, `color = MaterialTheme.colorScheme.onSurface`) for their top
    app bar / header title matching `BudgetedCategoriesModal`. Section titles
    within page content use `SelfBudgetType.heading` (16sp / Medium).
15. **Uniform Read-Only Modal Edit Action Color (`Ramp.Teal`)**: On all read-only
    detail summary sheets and modals (Transaction Details, Recurring Details,
    Savings Goal Details, Category Budget Details, and Account Details), the primary
    action button that unlocks the form to edit (`Edit transaction`, `Edit recurring`,
    `Edit goal`, `Edit budget`, `Edit account`) must strictly use `PrimaryPillButton(..., ramp = Ramp.Teal)`.
    This provides uniform brand consistency across both light mode (Brand Teal 400 `#1D9E75`)
    and dark mode (Dark Green `#196338` with pure white text), paired alongside
    a neutral `SecondaryPillButton(text = "Close")`.
16. **Subpage Navigation with Back Arrow (`Icons.AutoMirrored.Filled.ArrowBack`)**:
    - **All Subpages, Detail Views, Entry-Point Flows, Action Pages & Settings**: On all subpages, sub-screens, Settings, Settings sub-pages, read-only detail sheets, edit modals, history views, confirmation/action pages, and all creation subpages opened from "What do you want to add?" (including **Add Expense**, **Add Income**, **Add Transfer**, **Set / Edit Budget**, **Add / Edit Recurring**, **Add / Edit Account or Asset**, **Add / Edit Goal**, **Add Custom Category**, **Transaction Details**, **Confirm & Post**, **Category Budget Details**, **Account & Asset Details**, **Savings Goal Details**, **Adjust Budget Limit**, Settings, Settings sub-pages, Spent & Bills breakdown, Committed Bills, Committed Paychecks, Unbudgeted Spending, Budgeted Categories, Category Analytics, Savings Goal Analytics, Debt Payoff Analytics, Net Worth History, All Transactions, Data Export, and All Accounts), the top navigation bar must strictly use the Back arrow (`Icons.AutoMirrored.Filled.ArrowBack`, `contentDescription = "Back"` with `tint = MaterialTheme.colorScheme.onSurface`). Never use an `✕` (Close) icon on subpages, detail screens, creation flow subpages, action pages, or Settings.
    - **Root Menu Hubs**: Only the root entry picker modal ("What do you want to add?") uses `✕` / Close. Once a subpage/flow is selected, that subpage uses the Back arrow.

## 9. File index

```text
app/src/main/java/com/selfbudget/app/
├── core/util/
│   └── AppConstants.kt # App-wide defaults (account IDs, category IDs, payment methods, colors)
├── ui/theme/
│   ├── Color.kt      # Ramp enum, ramp extension fns, section/status/money colors
│   ├── Type.kt        # SelfBudgetType scale + Material3 Typography mapping
│   ├── Shapes.kt       # ShapePill/Tile/Chip/Card/Hero/Page
│   └── Theme.kt        # SelfBudgetTheme(), light/dark ColorScheme wiring
└── core/ui/components/
    ├── AppButtons.kt              # Primary/Secondary/DestructivePillButton, DoneChip
    ├── IconTile.kt                # IconTile, RampIconTile, GrayIconTile
    ├── FieldRow.kt                # FieldRow
    ├── ToggleRow.kt               # ToggleRow
    ├── SectionHeaderBand.kt       # SectionHeaderBand, SectionRowDivider
    ├── DeltaBadge.kt              # DeltaBadge
    ├── StatusProgressChip.kt      # StatusProgressBar, StatusBadge, NeutralBadge
    ├── FrequencySegmentedControl.kt
    └── TransactionAmountHero.kt   # TransactionAmountHero, QuickAmountChips
```

## 10. Migrating an old screen

When a screen still has old-style code, replace in this order:

1. `MaterialTheme.typography.*` → the matching `SelfBudgetType.*` token (§5).
2. Raw `RoundedCornerShape(N.dp)` / `CircleShape` icon badges → the matching
   `Shape*` token (§6) and `RampIconTile`/`GrayIconTile` (§7).
3. Plain `Button`/`OutlinedButton` → `PrimaryPillButton`/`SecondaryPillButton`/
   `DestructivePillButton` (§7), respecting the destructive-isolation and
   sign-out-is-neutral rules (§8.4–5).
4. Hard-coded hex colors or raw `MaterialTheme.colorScheme.primary` used for
   a *semantic* (not neutral) purpose → a `Ramp.*` call (§1–2).
5. Toast.makeText / one-off `AlertDialog` → the app's existing modal/snackbar
   pattern used elsewhere (check a sibling screen first — don't introduce a
   third alert style).
6. Re-check for the structural rules in §8 (duplicate headers, duplicate
   headline numbers, destructive buttons not isolated) — these are the bugs
   most likely to have shipped in the old version of the screen.
7. Compile (`./gradlew :app:compileDebugKotlin -q`) and run unit tests
   (`./gradlew :app:testDebugUnitTest -q`) before considering the migration done.
