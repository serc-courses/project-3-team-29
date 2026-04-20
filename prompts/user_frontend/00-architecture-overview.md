# User Frontend — Architecture Overview

## What This Is
A **separate, user-facing mobile web app** for the Mutual Fund OMS. While the existing `frontend/` is an **admin/operations dashboard** (dark sidebar, data tables, bulk operations), this is a **customer-facing app** where individual investors interact with their portfolio.

## Key Differences from Admin Frontend

| Aspect | Admin (`frontend/`) | User (`user-frontend/`) |
|--------|---------------------|-------------------------|
| Audience | Back-office operators | Individual investors |
| Layout | Desktop-first, sidebar + data tables | Mobile-first, bottom tab nav |
| Design | Dense data, dark sidebar | Clean, card-based, light & airy |
| Features | Bulk ops, confirm, book, replay | View portfolio, place orders, track status |
| Tone | Operational, data-heavy | Friendly, clear, consumer fintech |

## Tech Stack
- **React 18 + Vite** (same toolchain, separate project)
- **React Router v6** with bottom tab navigation
- **No component library** — custom CSS (mobile-first)
- **No icon library** — inline SVG icons only
- **Same backend** at `http://localhost:8080` (same Vite proxy setup)

## Color Palette — Consumer Fintech (Emerald/Slate)
```
Primary:     #059669 (emerald-600)  — CTAs, active states, positive
Primary-50:  #ECFDF5               — light tint backgrounds
Primary-100: #D1FAE5               — badge backgrounds
Primary-700: #047857               — hover states

Surface:     #FFFFFF               — cards
Background:  #F8FAFC               — page bg (slate-50)
             #F1F5F9               — section bg (slate-100)

Text-900:    #0F172A               — headings (slate-900)
Text-700:    #334155               — body (slate-700)
Text-500:    #64748B               — secondary (slate-500)
Text-400:    #94A3B8               — placeholder (slate-400)

Success:     #10B981               — completed, booked
Warning:     #F59E0B               — pending, in-progress
Error:       #EF4444               — failed, errored
Info:        #3B82F6               — informational

Buy:         #059669               — BUY orders (green)
Sell:        #DC2626               — SELL orders (red)

Border:      #E2E8F0               — card borders (slate-200)
Divider:     #F1F5F9               — section dividers (slate-100)
```

## Typography
- Font: `Inter` (already used in admin, freely available)
- Headings: 600-700 weight, tight letter-spacing (-0.02em)
- Body: 400 weight, 1.5 line-height
- Monospace: `JetBrains Mono` for amounts, order IDs
- Use `font-feature-settings: 'tnum'` for all numeric displays

## Folder Structure
```
user-frontend/
├── index.html
├── package.json
├── vite.config.js
├── public/
│   └── manifest.json          # PWA manifest
└── src/
    ├── main.jsx
    ├── App.jsx
    ├── App.css
    ├── api/
    │   ├── client.js          # Fetch wrapper (same pattern as admin)
    │   ├── ordersApi.js       # Plan orders, get order status
    │   ├── portfolioApi.js    # Funds, accounts aggregates
    │   └── index.js
    ├── components/
    │   ├── BottomNav/         # Mobile bottom tab bar
    │   ├── OrderCard/         # Order summary card
    │   ├── FundCard/          # Fund info card
    │   ├── StatusPill/        # Order status indicator
    │   ├── AmountDisplay/     # Formatted currency display
    │   ├── EmptyState/        # Friendly empty state with illustration
    │   ├── PullToRefresh/     # Pull-to-refresh wrapper
    │   ├── Sheet/             # Bottom sheet / modal
    │   └── Toast/             # Notification toast
    ├── constants/
    │   ├── config.js
    │   ├── orderStatus.js     # Same enums as admin
    │   ├── statusColors.js    # Mapped to user palette
    │   └── routes.js
    ├── hooks/
    │   ├── useFetch.js
    │   ├── useSse.js
    │   └── useAccount.js      # Account context (selected account)
    ├── pages/
    │   ├── Home/              # Portfolio overview
    │   ├── Orders/            # Order history
    │   ├── PlaceOrder/        # New order form
    │   ├── OrderDetail/       # Single order status
    │   ├── Funds/             # Browse funds
    │   ├── FundDetail/        # Single fund info
    │   └── Account/           # Account selector + info
    ├── styles/
    │   ├── globals.css        # CSS variables, reset, mobile base
    │   └── components.css     # Shared component classes
    └── utils/
        ├── formatters.js      # Currency, quantity, date, relative time
        └── index.js
```

## Pages & Navigation

### Bottom Tab Bar (4 tabs)
| Tab | Icon | Route | Page |
|-----|------|-------|------|
| Home | house | `/` | Portfolio overview |
| Orders | list | `/orders` | Order history |
| Funds | trending-up | `/funds` | Browse funds |
| Account | user | `/account` | Account info |

### Additional Routes (no tab, navigate via actions)
| Route | Page |
|-------|------|
| `/orders/new` | Place new order |
| `/orders/:id` | Order detail/tracking |
| `/funds/:id` | Fund detail |

## Prompt Sequence
```
01 — Project setup, Vite config, router, folder structure
02 — API service layer (client.js, ordersApi.js, portfolioApi.js)
03 — Constants, config, formatters
04 — Design system (globals.css, components.css, CSS variables)
05 — Bottom navigation + app shell
06 — Reusable components (StatusPill, OrderCard, FundCard, AmountDisplay, EmptyState, Sheet, Toast)
07 — Home page (portfolio overview)
08 — Orders page (order history list)
09 — Order detail page (single order tracking)
10 — Funds browse page (fund cards grid)
11 — Fund detail page
12 — Place order page (order form — mobile optimized)
13 — Account page (account selector + info)
14 — SSE integration + pull-to-refresh
15 — PWA manifest + mobile meta tags + final polish
```

## Design Principles
1. **Mobile-first** — design for 375px, scale up
2. **Thumb-friendly** — all tap targets ≥ 44px, bottom-aligned CTAs
3. **Card-based** — no data tables, use stacked cards
4. **Progressive disclosure** — summary first, tap for details
5. **Instant feedback** — loading skeletons, optimistic UI, haptic-style animations
6. **No emojis** — inline SVG icons only, professional appearance
7. **Consumer fintech feel** — think Zerodha, Groww, Revolut mobile app
