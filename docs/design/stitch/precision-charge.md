---
name: Precision Charge
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
  on-surface-variant: '#3d4947'
  inverse-surface: '#213145'
  inverse-on-surface: '#eaf1ff'
  outline: '#6d7a77'
  outline-variant: '#bcc9c6'
  surface-tint: '#006a61'
  primary: '#00685f'
  on-primary: '#ffffff'
  primary-container: '#008378'
  on-primary-container: '#f4fffc'
  inverse-primary: '#6bd8cb'
  secondary: '#565e74'
  on-secondary: '#ffffff'
  secondary-container: '#dae2fd'
  on-secondary-container: '#5c647a'
  tertiary: '#006947'
  on-tertiary: '#ffffff'
  tertiary-container: '#00855b'
  on-tertiary-container: '#f5fff6'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#89f5e7'
  primary-fixed-dim: '#6bd8cb'
  on-primary-fixed: '#00201d'
  on-primary-fixed-variant: '#005049'
  secondary-fixed: '#dae2fd'
  secondary-fixed-dim: '#bec6e0'
  on-secondary-fixed: '#131b2e'
  on-secondary-fixed-variant: '#3f465c'
  tertiary-fixed: '#6ffbbe'
  tertiary-fixed-dim: '#4edea3'
  on-tertiary-fixed: '#002113'
  on-tertiary-fixed-variant: '#005236'
  background: '#f8f9ff'
  on-background: '#0b1c30'
  surface-variant: '#d3e4fe'
typography:
  headline-xl:
    fontFamily: Inter
    fontSize: 36px
    fontWeight: '700'
    lineHeight: 44px
    letterSpacing: -0.025em
  headline-xl-mobile:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 36px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.015em
  headline-sm:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 24px
    letterSpacing: -0.01em
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Inter
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 18px
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.01em
  label-sm:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 14px
    letterSpacing: 0.02em
  mono-data:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: -0.01em
  mono-data-sm:
    fontFamily: JetBrains Mono
    fontSize: 11px
    fontWeight: '400'
    lineHeight: 14px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-mobile: 0.75rem
  margin: 1.5rem
  margin-mobile: 1rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

The design system is engineered for utility-first clarity, situational awareness, and total diagnostic transparency under high-stress driving conditions. Designed for EV drivers seeking dependable infrastructure information, the product avoids visual decoration in favor of high-legibility telemetry, explicit freshness indicators, and unambiguous station states. The emotional tone is reassuring, analytical, and authoritative—eradicating "phantom availability" anxiety by treating data freshness and status reasoning as core navigational tools rather than hidden metadata.

The design movement combines **Modern High-Contrast Technical Minimalism** with **Instrumental Data Density**. Surfaces are structured using balanced neutral slates, crisp mechanical borders, and purposeful chromatic status indicators. Every visual element serves to deliver glanceable verification: whether a charger is physically operating, when the status was last observed at the source, and which immediate fallback station to divert to if capacity is compromised.

## Colors

The palette leverages a functional, semantic-first hierarchy. Electric teal (`#0D9488`) anchors primary interactive controls and high-priority directional cues. Deep slate navy (`#0F172A`) commands core text, structural framing, and primary containers, conveying stability and industrial precision.

### Functional Status System
To eliminate ambiguous states on the road, station operational signals use strict semantic pairings:
- **Available / Verified Operational**: Emerald Green (`#10B981`) text/icons on Emerald Light (`#ECFDF5`) background with `#A7F3D0` borders.
- **Occupied / In Use / Faulted**: Rose Red (`#EF4444`) on Rose Light (`#FEF2F2`) with `#FECACA` borders.
- **Stale / Lagging Telemetry / Unverified**: Amber Warning (`#F59E0B`) on Amber Light (`#FFFBEB`) with `#FDE68A` borders. Used explicitly when data age crosses the confidence threshold.
- **Telemetry Freshness Grayscale**: Muted Slate (`#64748B`) for structural metadata, neutral canvas backgrounds (`#F8FAFC`), and subtle border dividers (`#E2E8F0`).

## Typography

Typography prioritizes fast scanning, numerical clarity, and structural consistency across Korean and Latin scripts. **Inter** serves as the primary system typeface for all headings and navigational content, paired with system fallbacks (`Pretendard`, `-apple-system`, `BlinkMacSystemFont`) for native Hangul rendering.

For telemetry timestamps, network latency counters, kilowatt ratings (e.g., `100kW`, `350kW`), and split freshness indicators (`sourceObservedAt` vs. `collectedAt`), **JetBrains Mono** is specified. Monospaced tabular figures prevent layout shifts during live polling cycles and reinforce the computational credibility of the platform.

## Layout & Spacing

The design system implements an adaptive fluid-to-fixed layout architecture tuned for in-car handheld use and widescreen desktop mapping:
- **Mobile (< 768px):** Single-column layout with fixed bottom drawer sheets for selected station dossiers. Canvas margins stay at `margin-mobile` (16px) with an 8px base rhythm to maximize tap surfaces and horizontal view space.
- **Tablet & Desktop (≥ 768px):** Split-view architecture featuring an edge-to-edge fluid viewport for geospatial navigation (Map view) with an anchored, 420px fixed-width data panel for station hierarchies, filter lists, and alternative routing candidates.
- **Density Rhythm:** Component internal padding uses compact steps (`space-sm` for dense telemetry chips, `space-md` for standard station cards) ensuring maximum comparative information without requiring excessive scrolling.

## Elevation & Depth

Visual hierarchy relies on structural tonal separation and crisp low-contrast outlines rather than heavy atmospheric shadows. This ensures sunlight legibility on mobile screens mounted on car dashboards.

- **Level 0 (Base Canvas):** Light gray background (`#F8FAFC`). Flat with no shadow.
- **Level 1 (Card Panels & Static Containers):** Pure white background (`#FFFFFF`) with a 1px border (`#E2E8F0`). Flat surface elevated strictly through tonal difference.
- **Level 2 (Interactive Floating Elements & Filter Pills):** White background with a 1px border (`#CBD5E1`) and a subtle ambient drop shadow: `box-shadow: 0 1px 3px 0 rgba(15, 23, 42, 0.06), 0 1px 2px -1px rgba(15, 23, 42, 0.04)`.
- **Level 3 (Modal Sheets, Detail Flyouts, & Critical Warnings):** White background enclosed with a 1px border (`#94A3B8`), elevated via: `box-shadow: 0 10px 15px -3px rgba(15, 23, 42, 0.08), 0 4px 6px -4px rgba(15, 23, 42, 0.03)`.

## Shapes

The shape system employs Level 2 (Rounded) geometry, balancing software precision with ergonomic touch handling:
- **Small Indicators & Technical Badges:** 4px (`rounded-sm`) to 6px radii to maintain a crisp, tag-like mechanical structure.
- **Connector Type & Power Pills:** 9999px (full pill) for rapid visual isolation of physical plugs (DC Combo, CHAdeMO, AC Complete).
- **Cards, Filter Trays, & Interactive Modules:** 8px (`rounded-md`) to 12px (`rounded-lg`) radius, framing telemetry without softening corners to the point of lost usable content area.
- **Bottom Navigation Sheets:** 16px (`rounded-t-xl`) top edge radiused boundaries on mobile viewports.

## Components

### Station Cards & Alternative Candidates
- **Container:** Pure white card with 1px `#E2E8F0` border, `p-4`, 12px roundedness.
- **Header:** Station name in `headline-sm` with rapid-action bookmarking and navigation launch button.
- **Status Cluster:** Left-aligned prominent semantic status badge (`AVAILABLE`, `OCCUPIED`, `DEGRADED`) flanked by connector availability ratios (e.g., `3/4 Available`).
- **Telemetry Block:** Monospaced freshness timestamp pairing displaying real-time delay (e.g., `Observed: 2m ago` vs `Synced: 5s ago`) paired with confidence dot indicator.
- **Alternative Candidate Module:** A distinct sub-card within or adjacent to an unavailable station, encased in a `#F1F5F9` background, showing immediate alternatives within a 3km radius along with status confidence score.

### Badges & Status Pills
- **Available Badge:** `#ECFDF5` background, `#065F46` text, `#A7F3D0` outline. Includes green pulsating dot (when freshness < 5 min).
- **Stale / Lagging Badge:** `#FFFBEB` background, `#92400E` text, `#FDE68A` outline. Features alert icon indicating data has not refreshed within 15 minutes.
- **Occupied / In Use Badge:** `#FEF2F2` background, `#991B1B` text, `#FECACA` outline. Displays reason code pill if supplied by API (e.g., `CHARGING_ACTIVE`, `OFFLINE`).

### Connector & Power Specs
- Enclosed pill format (`rounded-full`, 4px vertical / 10px horizontal padding).
- Neutral active state: `#0F172A` fill with white text for selected connector filters. Inactive state: `#F1F5F9` fill with `#475569` text.
- Power rating tags (e.g., `350kW Superfast`) highlighted with electric teal subtle tint (`#CCFBF1` background, `#0F766E` text).

### Buttons
- **Primary:** Deep Teal (`#0D9488`) fill, white text, 8px radius, medium bold weight. Hover: `#0F766E`. Active: `#115E59`.
- **Secondary (Navigation / Route):** Slate Navy (`#0F172A`) fill, white text. High contrast for critical actions.
- **Outline / Ghost (Filters):** Transparent fill, 1px border `#CBD5E1`, `#334155` text.

### Form Inputs & Search
- Surface: `#FFFFFF` with `#CBD5E1` border; transitions to `#0D9488` border with `0 0 0 1px #0D9488` ring on focus.
- Inner elements incorporate clear filter chips (e.g., `DC Combo only`, `Min 100kW`, `Available Only`) that toggle state with single-tap response.
