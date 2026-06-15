---
name: KickOff World Cup
colors:
  surface: '#fff8ef'
  surface-dim: '#e1d9cb'
  surface-bright: '#fff8ef'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#fbf3e4'
  surface-container: '#f5edde'
  surface-container-high: '#efe7d9'
  surface-container-highest: '#e9e2d3'
  on-surface: '#1e1b13'
  on-surface-variant: '#534434'
  inverse-surface: '#343026'
  inverse-on-surface: '#f8f0e1'
  outline: '#867461'
  outline-variant: '#d8c3ad'
  surface-tint: '#865300'
  primary: '#865300'
  on-primary: '#ffffff'
  primary-container: '#f39c12'
  on-primary-container: '#603a00'
  inverse-primary: '#ffb961'
  secondary: '#006d38'
  on-secondary: '#ffffff'
  secondary-container: '#94f4ad'
  on-secondary-container: '#00723a'
  tertiary: '#4e6074'
  on-tertiary: '#ffffff'
  tertiary-container: '#9eb1c8'
  on-tertiary-container: '#324457'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#ffddb9'
  primary-fixed-dim: '#ffb961'
  on-primary-fixed: '#2b1700'
  on-primary-fixed-variant: '#663e00'
  secondary-fixed: '#96f7b0'
  secondary-fixed-dim: '#7bda96'
  on-secondary-fixed: '#00210d'
  on-secondary-fixed-variant: '#005228'
  tertiary-fixed: '#d1e4fc'
  tertiary-fixed-dim: '#b5c8e0'
  on-tertiary-fixed: '#091d2e'
  on-tertiary-fixed-variant: '#36485c'
  background: '#fff8ef'
  on-background: '#1e1b13'
  surface-variant: '#e9e2d3'
typography:
  display-lg:
    fontFamily: Bricolage Grotesque
    fontSize: 48px
    fontWeight: '800'
    lineHeight: 52px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Bricolage Grotesque
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
  headline-lg-mobile:
    fontFamily: Bricolage Grotesque
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 34px
  title-md:
    fontFamily: Hanken Grotesk
    fontSize: 20px
    fontWeight: '700'
    lineHeight: 28px
  body-lg:
    fontFamily: Hanken Grotesk
    fontSize: 18px
    fontWeight: '500'
    lineHeight: 26px
  body-md:
    fontFamily: Hanken Grotesk
    fontSize: 16px
    fontWeight: '500'
    lineHeight: 24px
  label-data:
    fontFamily: JetBrains Mono
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.05em
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  unit: 8px
  container-margin: 20px
  gutter: 16px
  section-gap: 40px
---

## Brand & Style

This design system captures the exuberant, festive spirit of the 2026 FIFA World Cup, drawing direct inspiration from traditional Latin American muralism and contemporary athletic energy. The brand personality is celebratory, communal, and textured. It moves away from the sterile "tech-bro" aesthetic in favor of a **Tactile / Illustrated** style that feels hand-crafted and authentic.

Key stylistic pillars include:
- **Grain & Texture:** Every surface features a subtle stippled or risograph texture to mimic printed posters and street art.
- **Botanical Energy:** Elements are framed by lush, hand-drawn floral and foliage motifs, breaking the rigid geometry of standard mobile grids.
- **Narrative Banners:** Navigation and section titles are housed within ribbon and banner shapes, creating a sense of "pageantry" throughout the user journey.
- **Human Connection:** A focus on high-vibrancy, warm tones to evoke the heat of the stadium and the warmth of the host cultures.

## Colors

The palette is a "Golden Hour" scheme, saturated and warm, designed to contrast beautifully with rich botanical greens and deep sky blues.

- **Primary (Sun Gold):** Used for primary actions, highlight states, and celebratory accents.
- **Secondary (Jungle Green):** Used for pitch-related data, success states, and botanical illustrations.
- **Tertiary (Deep Indigo):** Derived from the "2026" ribbon in the reference; used for high-contrast headers and navigation elements.
- **Background (Parchment):** A warm, cream-tinted off-white (#FDF5E6) used for all base surfaces to avoid the harshness of pure white and maintain the illustrated feel.
- **Accent (Terracotta):** A secondary warm tone (#D35400) for "Eliminated" states and secondary warnings.

## Typography

The typography system relies on a high-contrast pairing between a "funky" expressive display face and a precision-engineered sans-serif.

- **Headlines:** Uses **Bricolage Grotesque**. It captures the idiosyncratic, hand-lettered feel of the reference image's ribbon text. Use Bold or ExtraBold weights only.
- **Body:** Uses **Hanken Grotesk**. This provides a clean, modern contrast to the expressive headers, ensuring long-form content and news are highly legible.
- **Data & Stats:** Uses **JetBrains Mono**. For match scores, minutes played, and table rankings, a monospaced font provides a "technical" scoreboard feel that cuts through the organic illustrations.

## Layout & Spacing

The layout philosophy follows a **Fluid Grid** with intentional "organic breaks." While the core content aligns to a standard 12-column (desktop) or 4-column (mobile) grid, illustrative elements and ribbons are permitted to bleed outside of containers or overlap margins to create depth.

- **Margins:** 20px on mobile to ensure content doesn't feel cramped against the screen edge.
- **Rhythm:** An 8px base unit drives all padding and margin decisions.
- **Reflow:** On desktop, content is centered within a 1200px max-width container, with botanical illustrations filling the wide gutters as decorative "wings."

## Elevation & Depth

This system avoids traditional shadows in favor of **Tonal Layering** and **Graphic Offsets**.

- **Stacked Paper:** Depth is shown by placing Parchment cards on top of "textured" colored backgrounds.
- **Hard Offsets:** Instead of soft blurs, use a solid 4px offset "drop-block" in a darker shade (Indigo or Dark Green) to give buttons and cards a physical, sticker-like quality.
- **Overlays:** Ribbons should always appear at the highest z-index, slightly overlapping the content blocks they describe to create a 3D "scrapbook" effect.

## Shapes

The shape language is "Soft-Organic." We avoid the clinical perfection of high-radius circles.

- **Primary Corners:** Use a 4px (Soft) radius for most UI elements to maintain a hand-cut paper feel.
- **Irregularity:** Buttons and "Ticket" cards should utilize subtle irregular clipping paths or "notched" corners to reinforce the paper-ticket metaphor.
- **Banners:** Section headers use a "swallowtail" ribbon shape on either end, mimicking the central banner in the reference image.

## Components

### Buttons & Controls
- **Primary Action:** Large, Goldenrod-colored blocks with a heavy 2px Indigo border and a 4px solid "shadow" offset. Text is always Uppercase Bricolage Grotesque.
- **Checkboxes:** Styled as small hand-drawn "O" and "X" motifs rather than standard circles/squares.

### Cards
- **Match Ticket Card:** Used for upcoming fixtures. Features a vertical perforated line detail on the right side and a notched "bite" taken out of the top and bottom edges.
- **Standings Table:** 
    - **Qualifying Row:** Highlighted with a subtle Green botanical flourish in the background.
    - **Eliminated Row:** Desaturated to a warm grey-terracotta, with a thin "strikethrough" texture.

### Section Headers
- **Ribbon Banners:** Titles are placed inside "waving" Indigo banners. The text should follow a slight arc if possible, or at least be center-aligned within the ribbon shape.

### Inputs
- **Search/Forms:** Use the Parchment background with a thick bottom-border only (Indigo), mimicking a signature line on a physical document.