---
name: TriPro
colors:
  surface: '#f8f9ff'
  surface-dim: '#cbdbf5'
  surface-bright: '#f8f9ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#eff4ff'
  surface-container: '#e5eeff'
  surface-container-high: '#dce9ff'
  surface-container-highest: '#d3e4fe'
  on-surface: '#0b1c30'
  on-surface-variant: '#43474f'
  inverse-surface: '#213145'
  inverse-on-surface: '#eaf1ff'
  outline: '#747780'
  outline-variant: '#c4c6d0'
  surface-tint: '#405f91'
  primary: '#001736'
  on-primary: '#ffffff'
  primary-container: '#002b5b'
  on-primary-container: '#7594ca'
  inverse-primary: '#a9c7ff'
  secondary: '#7f5600'
  on-secondary: '#ffffff'
  secondary-container: '#f9ad00'
  on-secondary-container: '#664500'
  tertiary: '#141819'
  on-tertiary: '#ffffff'
  tertiary-container: '#292c2e'
  on-tertiary-container: '#909395'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#d6e3ff'
  primary-fixed-dim: '#a9c7ff'
  on-primary-fixed: '#001b3d'
  on-primary-fixed-variant: '#264778'
  secondary-fixed: '#ffdeae'
  secondary-fixed-dim: '#ffba3f'
  on-secondary-fixed: '#281800'
  on-secondary-fixed-variant: '#604100'
  tertiary-fixed: '#e0e3e5'
  tertiary-fixed-dim: '#c4c7c9'
  on-tertiary-fixed: '#191c1e'
  on-tertiary-fixed-variant: '#444749'
  background: '#f8f9ff'
  on-background: '#0b1c30'
  surface-variant: '#d3e4fe'
typography:
  display-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 56px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
  headline-lg-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-lg:
    fontFamily: Work Sans
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: Work Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-sm:
    fontFamily: Work Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-md:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.05em
  label-sm:
    fontFamily: JetBrains Mono
    fontSize: 10px
    fontWeight: '500'
    lineHeight: 14px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 8px
  container-max: 1280px
  gutter: 24px
  margin-desktop: 40px
  margin-mobile: 16px
  stack-sm: 4px
  stack-md: 12px
  stack-lg: 24px
---

## Brand & Style
The design system is built on a foundation of "Functional Inspiration." It targets modern travelers and groups who require professional-grade organization tools without sacrificing the wonder of discovery. 

The visual style is **Corporate / Modern** with a slight lean toward **Minimalism**. It prioritizes high-density information management through extreme clarity, while using subtle glassmorphism and soft shadows to provide a sense of depth and airiness. The UI should feel like a reliable travel concierge: structured, calm, and prepared. 

Key brand pillars include:
- **Clarity over Clutter:** Schedules are dense but never cramped.
- **Trust through Precision:** Professional alignment and consistent data visualization.
- **Collaborative Warmth:** Shared features are highlighted with soft, approachable roundedness.

## Colors
This design system utilizes a high-contrast palette to distinguish between structural elements and interactive prompts.

- **Voyage Blue (#002B5B):** The primary color used for global navigation, headers, and authoritative text. It establishes trust and stability.
- **Accent Amber (#FFB100):** Reserved exclusively for primary calls to action (CTAs) and active status indicators. Its high visibility ensures key "booking" or "adding" actions are never missed.
- **Cloud White (#F8FAFC):** The primary canvas color. It is a slightly cool off-white that reduces eye strain compared to pure white.
- **Slate Neutral (#64748B):** Used for secondary metadata, borders, and inactive icons.

**Functional Palettes:**
- **Success:** #10B981 (Document verified, access granted)
- **Warning:** #F59E0B (Upcoming departure, payment due)
- **Error:** #EF4444 (Flight cancelled, missing document)

## Typography
The typography strategy employs a dual-font approach to balance personality with utility.

- **Headlines (Plus Jakarta Sans):** Chosen for its friendly yet professional geometry. It is used for page titles and section headers to provide a welcoming, modern feel.
- **Body & Interface (Work Sans):** A highly legible, neutral sans-serif that excels in data-heavy environments like itinerary lists and booking details.
- **Technical Labels (JetBrains Mono):** Used sparingly for time-stamps, flight numbers, and confirmation codes. The monospaced nature helps users scan alphanumeric data quickly.

**Hierarchy Rules:**
- Use **Display LG** only for hero sections or destination titles.
- **Label SM** is always uppercase when used for metadata tags (e.g., "GATE", "SEAT").

## Layout & Spacing
This design system utilizes a **12-column fluid grid** for desktop and a **4-column grid** for mobile.

- **The 8px Rhythm:** All spacing (padding, margins, gap) must be multiples of 8px to ensure a consistent visual cadence.
- **Itinerary Stacking:** Within a trip schedule, cards are separated by a 12px `stack-md` vertical gap. Related sub-items (like a list of travelers under a booking) use a 4px `stack-sm` gap.
- **Safe Zones:** On mobile, page content maintains a 16px horizontal margin. On desktop, content is centered within a 1280px max-width container with 40px margins.

## Elevation & Depth
Depth is used to denote interactivity and "current focus" in the itinerary.

- **Level 0 (Flat):** The main background (Cloud White).
- **Level 1 (Tonal):** Cards use a subtle 1px border (#E2E8F0) with no shadow when resting.
- **Level 2 (Hover/Active):** When a user interacts with a flight or hotel card, it gains an ambient shadow: `0px 4px 20px rgba(0, 43, 91, 0.08)`. This soft, blue-tinted shadow makes the card appear to lift toward the user.
- **Level 3 (Overlays):** Modals and collaborative chat drawers use a backdrop blur (12px) to maintain context of the underlying itinerary while focusing on the task at hand.

## Shapes
The shape language is "Optimistically Rounded." 

- **Standard Elements:** Buttons, input fields, and small cards use a **0.5rem (8px)** radius.
- **Large Containers:** Trip overview sections and hero images use **1rem (16px)** to feel softer and more approachable.
- **Indicators:** Status badges (e.g., "Confirmed") and user avatars use **Full Round (Pill)** shapes to distinguish them from structural UI components.

## Components

### Cards
- **Flight Cards:** Feature a left-hand "Voyage Blue" accent bar. Emphasize departure/arrival times in `headline-md`.
- **Hotel Cards:** Use a secondary image-led layout with 1/3 width for the property photo and 2/3 for address/check-in details.
- **Attraction Cards:** Minimalist styling with a 1px dashed border to suggest a "ticket" or "coupon" feel.

### Buttons
- **Primary:** Background `Accent Amber`, text `Voyage Blue`, bold weight. Use for "Add to Trip" or "Book Now."
- **Secondary:** Transparent background, `Voyage Blue` border and text. Use for "Share" or "Details."
- **Ghost:** No border, `Slate Neutral` text. Used for "Cancel" or "Edit" actions.

### Collaborative Elements
- **Presence Indicators:** Small 32px circular avatars with a 2px Cloud White border, overlapped in a "Stack" to show who is currently viewing a day's itinerary.
- **Status Indicators:** Use small pill-shaped badges with a low-opacity background tint (e.g., Success Green at 10% opacity) and high-contrast text for document status.

### Input Fields
- Understated styling with a 1px border. On focus, the border transitions to `Voyage Blue` with a 2px outer glow in a light blue tint.
- Labels use `body-sm` in `Slate Neutral`, positioned strictly above the input.