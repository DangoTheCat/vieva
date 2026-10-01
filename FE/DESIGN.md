# DESIGN.md — AIVES Design System Specification

> **AIVES (Artificial Intelligence Voice Evaluation System)**  
> *Course: SWD392 — Software Architecture and Design (FPT University)*  
> *Version: 2.4.0 — High-Contrast Academic & EdTech Standards*

---

## 1. Overview & Aesthetic Direction

AIVES is an enterprise-grade online oral examination platform with an AI Virtual Examiner. The design language must evoke **trust, academic rigor, calmness, and reliability**—never looking like a flashy consumer crypto app or generic template ("AI Slop").

- **Primary Metaphor:** High-contrast University Assessment Operations Center.
- **Visual Weight:** Deep Navy Top Bar & Sidebar, Slate-100 Dot-grid Canvas, Clean White High-contrast Surface Cards.
- **Target Accessibility:** WCAG 2.1 AA Compliance (minimum 4.5:1 text contrast).

---

## 2. Design Tokens

### Color Palette

```json
{
  "theme": {
    "colors": {
      "sidebarBg": "#0B132B",
      "sidebarHover": "#1C2541",
      "canvasBg": "#F1F5F9",
      "cardBg": "#FFFFFF",
      "accentSky": "#0284C7",
      "accentNavy": "#0F172A",
      "emeraldCustom": "#10B981",
      "amberCustom": "#F59E0B",
      "roseCustom": "#EF4444",
      "brand": {
        "50": "#F0F7FF",
        "100": "#E0EFFF",
        "500": "#0066FF",
        "600": "#0052CC",
        "700": "#003D99"
      }
    }
  }
}
```

### Typography

| Hierarchy Level | Font Family | Weight | Size / Leading | Usage |
|---|---|---|---|---|
| **Display H1** | `Plus Jakarta Sans` | 800 (Extrabold) | `24px` / `32px` (`tracking-tight`) | Page title, Main metric |
| **Heading H2** | `Plus Jakarta Sans` | 700 (Bold) | `18px` / `24px` | Card title, Modal header |
| **Heading H3** | `Plus Jakarta Sans` | 600 (Semibold) | `14px` / `20px` | Section subhead, Table header |
| **Body Regular** | `Be Vietnam Pro`, `Inter` | 400 (Regular) | `12px` / `18px` | Data rows, Descriptions |
| **Body Semibold** | `Be Vietnam Pro`, `Inter` | 600 (Semibold) | `12px` / `18px` | Labels, Button text |
| **Mono / Code** | `JetBrains Mono` | 500 (Medium) | `11px` / `16px` | UUIDs, Email, MSSV, Status |

---

## 3. Component Standards

### A. Button Hierarchy
1. **Primary Button:**
   - Background: `bg-gradient-to-r from-sky-600 to-sky-500 hover:from-sky-500 hover:to-sky-400 text-white`
   - Radius: `rounded-xl`
   - Shadow: `shadow-md shadow-sky-600/20`
   - States: `active:scale-95 disabled:opacity-50`
2. **Secondary Outline Button:**
   - Border: `border border-slate-300 bg-white hover:bg-slate-50 text-slate-700`
   - Radius: `rounded-xl`
3. **Pill Action Button:**
   - Background: `bg-[#0066FF] hover:bg-[#0052CC] text-white rounded-full py-2.5 px-5`
   - Used for quick login pills and primary gateway actions.

### B. Status Badges & Pills
- **ACTIVE:** `bg-emerald-50 text-emerald-700 border border-emerald-200 rounded-full text-[10px] font-bold`
- **INACTIVE:** `bg-amber-50 text-amber-700 border border-amber-200 rounded-full text-[10px] font-bold`
- **BANNED:** `bg-purple-50 text-purple-700 border border-purple-200 rounded-full text-[10px] font-bold`
- **DELETED:** `bg-rose-50 text-rose-700 border border-rose-200 rounded-full text-[10px] font-bold`

### C. Form Controls
- **Input Fields:**
  - Height: `py-2.5 px-3`
  - Border: `border border-slate-200 rounded-xl`
  - Background: `bg-slate-50/50 focus:bg-white`
  - Focus Ring: `focus:outline-none focus:ring-2 focus:ring-sky-500`

---

## 4. Required States (The 6 Mandatory UI States)

Every screen and component must account for:
1. **Default State:** Clean layout with standard data.
2. **Hover State:** Subtle color change (`bg-slate-50/80`), icon color transition.
3. **Focus State:** 2px high-visibility ring for keyboard navigation (`focus:ring-2 focus:ring-sky-500`).
4. **Active/Press State:** `active:scale-[0.98]` physical tactile feedback.
5. **Loading Skeleton State:** Pulsing placeholder rows (`animate-pulse bg-slate-200/80`).
6. **Empty / Error State:** Elegant empty container with illustration/icon and guidance text.
