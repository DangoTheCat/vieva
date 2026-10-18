# Design System — Blush Pink & Sage Green

A two-color design system. **Blush pink** is the primary color and **sage green** is the secondary color. Both are soft and slightly muted, supported by warm neutrals.

---

## Color

### Core colors

| Token | Hex | Role |
|---|---|---|
| `pink-500` | `#CE9893` | Primary: main buttons, key accents |
| `green-500` | `#819266` | Secondary: borders, icons, supporting actions |

### Pink scale

| Step | Hex | Used for |
|---|---|---|
| `pink-50` | `#FAF5F5` | Subtle accent background |
| `pink-100` | `#F3E8E7` | Card background, light hover |
| `pink-200` | `#EAD4D2` | Light borders, badge background |
| `pink-300` | `#DEB9B5` | Decorative fills |
| `pink-400` | `#D5A6A2` | Button hover |
| `pink-500` | `#CE9893` | **Primary** |
| `pink-600` | `#B96C65` | Button pressed, large text, icons |
| `pink-700` | `#984F48` | Accent text on light backgrounds, errors |
| `pink-800` | `#713D38` | Headings on light backgrounds |
| `pink-900` | `#492A27` | Secondary text |

### Green scale

| Step | Hex | Used for |
|---|---|---|
| `green-50` | `#F5F6F3` | Subtle green background |
| `green-100` | `#EBEEE8` | Secondary card background |
| `green-200` | `#DADFD3` | Dividers, light borders |
| `green-300` | `#C4CCB7` | Default input border |
| `green-400` | `#A4B190` | Secondary icons, disabled state |
| `green-500` | `#819266` | **Secondary**, borders |
| `green-600` | `#687750` | Secondary button, focus ring |
| `green-700` | `#505B3E` | Links on light backgrounds |
| `green-800` | `#3A422E` | Dark sections |
| `green-900` | `#25291E` | Footer, dark mode background |

### Neutrals

| Token | Hex | Role |
|---|---|---|
| `paper` | `#FBF5EE` | Page background |
| `ink` | `#1F1F1D` | Primary text |
| `sand` | `#CFB997` | Warning background |

### State colors

| State | Color | Notes |
|---|---|---|
| Success | `green-600` `#687750` | Pair with a check icon |
| Error | `pink-700` `#984F48` | Pair with an icon and a message |
| Warning | `sand` `#CFB997` | Use `ink` text on it |
| Info | `#57717B` | Muted teal, use sparingly |

### Usage ratio

- **60%** `paper`, `pink-50` (backgrounds)
- **25%** `green-*` (borders, icons, navigation)
- **10%** `pink-*` (primary buttons, accents)
- **5%** `ink`, `sand` (text, small details)

---

## Contrast (WCAG)

| Text / background | Ratio | Result | Use for |
|---|---|---|---|
| `ink` on `pink-500` | 6.71 | Passes AA | Primary button text |
| `ink` on `green-500` | 4.90 | Passes AA | Secondary button text |
| `ink` on `pink-50` | 15.29 | Passes AAA | Body text |
| `white` on `green-600` | 4.84 | Passes AA | Dark green buttons |
| `pink-700` on `paper` | 5.47 | Passes AA | Accent text, error messages |
| `green-700` on `paper` | 6.67 | Passes AA | Links |
| `pink-800` on `paper` | 7.99 | Passes AAA | Headings |
| `green-600` on `paper` | 4.47 | Large text and UI only | Borders, icons, text 24px and up |
| `pink-600` on `paper` | 3.59 | Large text and UI only | Icons, text 24px and up |
| `pink-500` on `paper` | 2.27 | Not for text | Decoration only |
| `green-500` on `paper` | 3.11 | UI and borders only | Not for regular text |

---

## Typography

| Role | Font |
|---|---|
| Headings | **Fraunces** (soft serif) |
| Body, UI | **Be Vietnam Pro** (supports English and Vietnamese) |

```css
@import url("https://fonts.googleapis.com/css2?family=Fraunces:opsz,wght,SOFT,WONK@9..144,400..700,0..100,0..1&family=Be+Vietnam+Pro:wght@400;500;600&display=swap");
```

| Token | Size | Line height | Font | Weight |
|---|---|---|---|---|
| `display` | 56px | 1.05 | Fraunces | 600 |
| `h1` | 40px | 1.1 | Fraunces | 600 |
| `h2` | 32px | 1.15 | Fraunces | 600 |
| `h3` | 24px | 1.25 | Fraunces | 500 |
| `body-lg` | 18px | 1.6 | Be Vietnam Pro | 400 |
| `body` | 16px | 1.6 | Be Vietnam Pro | 400 |
| `small` | 14px | 1.5 | Be Vietnam Pro | 500 |
| `caption` | 12px | 1.4 | Be Vietnam Pro | 500 |

Keep line length under 68 characters and use sentence case for labels and buttons.

---

## Spacing and layout

| Token | Value |
|---|---|
| Base unit | 4px |
| Spacing scale | 4, 8, 12, 16, 24, 32, 48, 64, 96 |
| Content max width | 1120px |
| Page margin (mobile) | 20px |
| Grid | 12 columns, 24px gutter (desktop); 4 columns, 16px gutter (mobile) |

---

## Shape and elevation

| Token | Value | Used for |
|---|---|---|
| `radius-sm` | 8px | Inputs, badges |
| `radius-md` | 16px | Buttons, small cards |
| `radius-lg` | 24px | Large cards, modals |
| `radius-pill` | 999px | Tags, chips |
| `border-width` | 1px (subtle), 2px (default) | Borders |
| `shadow-sm` | `0 1px 2px rgba(31, 31, 29, 0.08)` | Inputs, small elements |
| `shadow-md` | `0 4px 12px rgba(31, 31, 29, 0.10)` | Cards |
| `shadow-lg` | `0 12px 32px rgba(31, 31, 29, 0.14)` | Modals, popovers |

---

## Components

### Buttons

| Type | Background | Text | Border |
|---|---|---|---|
| Primary | `pink-500` | `ink` | none |
| Secondary | `green-500` | `ink` | none |
| Outline | transparent | `green-700` | 2px `green-500` |
| Text | transparent | `green-700` | none |

- Hover: `pink-400` (primary) or `green-400` (secondary).
- Pressed: `pink-600` (primary) or `green-600` (secondary, with `white` text).
- Disabled: `green-200` background, `green-400` text.
- Minimum height 44px, horizontal padding 20px, `radius-md`, 16px text at weight 600.

### Cards

- `paper` or `pink-100` background, 1px `green-200` border, `radius-lg`, `shadow-md`, 24px padding.

### Inputs

- `white` background, 2px `green-300` border, `radius-sm`, 44px height.
- Focus: border `green-600` with a 3px `green-300` ring.
- Error: border `pink-700`, with a message below in `pink-700` text.

### Badges and tags

- `pink-200` or `green-200` background, `pink-800` or `green-800` text, `radius-pill`, `caption` size.

### Links

- `green-700`, underlined. Hover: `pink-700`.

---

## Accessibility

- Regular text needs at least 4.5:1 contrast, large text (24px and up, or 19px bold) at least 3:1.
- Don't rely on color alone. States always include an icon and text.
- Focus ring: 3px `green-600`, offset 2px.
- Minimum touch target 44×44px.

---

## Design tokens

### CSS variables

```css
:root {
  /* Pink */
  --pink-50:  #FAF5F5;
  --pink-100: #F3E8E7;
  --pink-200: #EAD4D2;
  --pink-300: #DEB9B5;
  --pink-400: #D5A6A2;
  --pink-500: #CE9893;
  --pink-600: #B96C65;
  --pink-700: #984F48;
  --pink-800: #713D38;
  --pink-900: #492A27;

  /* Green */
  --green-50:  #F5F6F3;
  --green-100: #EBEEE8;
  --green-200: #DADFD3;
  --green-300: #C4CCB7;
  --green-400: #A4B190;
  --green-500: #819266;
  --green-600: #687750;
  --green-700: #505B3E;
  --green-800: #3A422E;
  --green-900: #25291E;

  /* Neutrals */
  --paper: #FBF5EE;
  --ink:   #1F1F1D;
  --sand:  #CFB997;

  /* Semantic */
  --color-bg:            var(--paper);
  --color-surface:       var(--pink-100);
  --color-text:          var(--ink);
  --color-text-muted:    var(--pink-900);
  --color-primary:       var(--pink-500);
  --color-primary-hover: var(--pink-400);
  --color-secondary:     var(--green-500);
  --color-border:        var(--green-200);
  --color-link:          var(--green-700);
  --color-focus:         var(--green-600);
  --color-success:       var(--green-600);
  --color-error:         var(--pink-700);
  --color-warning:       var(--sand);

  /* Shape */
  --radius-sm: 8px;
  --radius-md: 16px;
  --radius-lg: 24px;
  --radius-pill: 999px;
  --shadow-sm: 0 1px 2px rgba(31, 31, 29, 0.08);
  --shadow-md: 0 4px 12px rgba(31, 31, 29, 0.10);
  --shadow-lg: 0 12px 32px rgba(31, 31, 29, 0.14);

  /* Fonts */
  --font-display: "Fraunces", Georgia, serif;
  --font-body: "Be Vietnam Pro", system-ui, sans-serif;
}
```

### Tailwind (`tailwind.config.js`)

```js
module.exports = {
  theme: {
    extend: {
      colors: {
        pink: {
          50: "#FAF5F5", 100: "#F3E8E7", 200: "#EAD4D2", 300: "#DEB9B5",
          400: "#D5A6A2", 500: "#CE9893", 600: "#B96C65", 700: "#984F48",
          800: "#713D38", 900: "#492A27",
        },
        green: {
          50: "#F5F6F3", 100: "#EBEEE8", 200: "#DADFD3", 300: "#C4CCB7",
          400: "#A4B190", 500: "#819266", 600: "#687750", 700: "#505B3E",
          800: "#3A422E", 900: "#25291E",
        },
        paper: "#FBF5EE",
        ink: "#1F1F1D",
        sand: "#CFB997",
      },
      fontFamily: {
        display: ["Fraunces", "Georgia", "serif"],
        body: ["Be Vietnam Pro", "system-ui", "sans-serif"],
      },
      borderRadius: { sm: "8px", md: "16px", lg: "24px" },
      boxShadow: {
        sm: "0 1px 2px rgba(31, 31, 29, 0.08)",
        md: "0 4px 12px rgba(31, 31, 29, 0.10)",
        lg: "0 12px 32px rgba(31, 31, 29, 0.14)",
      },
    },
  },
};
```
