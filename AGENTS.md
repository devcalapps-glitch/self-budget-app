# AGENTS.md

## UI Design System

All Compose screens follow one shared design system (color ramps, typography
scale, shape tokens, and reusable components like pill buttons, icon tiles,
and section header bands). Before adding or restyling any screen, read
[docs/DESIGN_SYSTEM.md](docs/DESIGN_SYSTEM.md) — it documents the tokens to
use, the shared components to reuse instead of writing one-off UI, and the
structural rules (header/footer action placement, destructive-action
isolation, no duplicate headline numbers, and uniform word title-case text
field capitalization via `toWordTitleCase()`) that every screen must follow.

## Testing Guidelines

The codebase includes an automated unit test suite covering core financial calculations, safe-to-spend logic, cash flow equations, and recurring bill normalizations.

- **Test Suite Documentation**: See [docs/testing.md](docs/testing.md) for a full breakdown.
- **Running Tests**: Run `./gradlew test` after making changes to core financial calculations or view models.
