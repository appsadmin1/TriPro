# Design mapping: TriPro → Compose/Material 3

`DESIGN.md`'s token names (`surface-container-high`, `on-primary-fixed-variant`, ...)
are literally Material 3 `ColorScheme` role names — the mockups were exported from an
M3 design. That makes the Compose port a **1:1 transcription**, not an approximation.
Every hex value in `ui/theme/Color.kt` is copied verbatim from `DESIGN.md`, and
`ui/theme/Theme.kt` passes every one of them into `lightColorScheme(...)`, including the
"fixed tone" roles (`primaryFixed`, `onSecondaryFixedVariant`, etc.) that most starter
templates skip.

## Typography

`DESIGN.md` defines two type scales for headlines — `headline-lg` (32/40, desktop) and
`headline-lg-mobile` (24/32, mobile). Since TriPro is phone-only, `Type.kt` uses the
mobile scale as `MaterialTheme.typography.headlineLarge` throughout; the desktop scale
has no equivalent here on purpose.

| DESIGN.md token | Compose `Typography` role | Used for |
|---|---|---|
| `display-lg` | `displayLarge` | Page hero titles ("My Trips") |
| `headline-lg-mobile` | `headlineLarge` | Nav / brand titles |
| `headline-md` | `headlineMedium` | Card titles, section headers |
| `body-lg` / `body-md` / `body-sm` | `bodyLarge` / `bodyMedium` / `bodySmall` | Prose |
| `label-md` | `labelLarge` + `labelMedium` | Button text, mono technical labels |
| `label-sm` | `labelSmall` | Metadata tags ("4 DAYS AWAY", timestamps) |

Font families (Plus Jakarta Sans / Work Sans / JetBrains Mono) default to
`FontFamily.Default` so the project builds with zero extra setup. To use the real
webfonts: download the static `.ttf` files from Google Fonts, drop them in
`app/src/main/res/font/`, and swap the three `FontFamily.Default` vals in `Type.kt` —
see the comment at the top of that file for the exact `Font(...)` call.

## Shape & spacing

| DESIGN.md token | Compose value |
|---|---|
| `rounded.DEFAULT` (8px) | `MaterialTheme.shapes.small` — buttons, inputs, small cards |
| `rounded.lg` (16px) | `MaterialTheme.shapes.large` — trip overview sections, hero images |
| `rounded.full` | `PillShape` in `Shape.kt` — avatars, status badges |
| `spacing.margin-mobile` (16px) | `TriProSpacing.marginMobile` — horizontal safe zone |
| `spacing.stack-md` (12px) | `TriProSpacing.stackMd` — gap between cards in a schedule |
| `spacing.stack-lg` (24px) | `TriProSpacing.stackLg` — gap between major sections |

`spacing.container-max` and `spacing.margin-desktop` from `DESIGN.md` are web
breakpoint concerns and have no Android equivalent — TriPro is a single (phone) form
factor.

## Components that don't map 1:1

- **Presence indicators** (`AvatarStack.kt`) — DESIGN.md specifies 32px circular
  avatars, 2px Cloud White border, overlapped. Implemented exactly; the "+N" overflow
  bubble is an addition not shown in the static mockups but implied by the
  overflow-truncation pattern the mockups already show (e.g. the "+2" avatar bubble in
  the trip cards).
- **Alert pills** (the amber "closes early at 18:00" badge in the Day 3 mockup) — ported
  to `ItineraryItemRow.kt` using `errorContainer`/`onErrorContainer` rather than a
  literal amber, since DESIGN.md's own "Functional Palettes" section defines a
  dedicated Warning color (`#F59E0B`) distinct from Accent Amber (`#FFB100`) for exactly
  this kind of status use — Error/Warning containers were the closer semantic match for
  a "closes early" alert than reusing the CTA color.
- **Map component** — not specified in DESIGN.md at all (no map appears in the
  mockups). `DayMapPreview.kt` borrows the same rounded-16dp/bordered-card language
  used everywhere else so it reads as part of the same system.
