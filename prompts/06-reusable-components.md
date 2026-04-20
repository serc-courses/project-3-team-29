# Prompt 06: Reusable UI Components

## Context
You are building reusable components for a Mutual Fund OMS dashboard. All components should use CSS variables from `globals.css`, import colors from `constants/statusColors.js`, and be designed for reuse across multiple pages.

## Task
Create reusable components in `frontend/src/components/`.

## Files to Create

### 1. `frontend/src/components/StatusBadge/StatusBadge.jsx`

A color-coded pill/badge that displays an order or bulk-order status.

**Props:**
- `status` (string) — e.g., 'PLANNED', 'BOOKED', 'ERRORED'
- `variant` (string, optional) — 'order' | 'bulk', defaults to 'order'

**Behavior:**
- Looks up colors from `ORDER_STATUS_COLORS` or `BULK_STATUS_COLORS` based on variant
- Renders a small rounded pill with background, text color, and border from the color map
- Font: small (`var(--text-xs)`), uppercase, font-weight 600
- Falls back to gray if status is not recognized
- Include `StatusBadge.css` for styles

### 2. `frontend/src/components/SummaryCard/SummaryCard.jsx`

A metric summary card for the dashboard.

**Props:**
- `title` (string) — e.g., "Total Orders"
- `value` (string | number) — the metric value
- `subtitle` (string, optional) — extra context line
- `icon` (string, optional) — emoji or icon character
- `trend` (string, optional) — 'up' | 'down' | 'neutral'
- `accentColor` (string, optional) — CSS color for the top accent border

**Behavior:**
- White card with subtle shadow, top border accent color (4px)
- Large metric value (font-size: var(--text-3xl), font-weight 700, font-mono class)
- Title in muted text above the value
- Subtle hover lift animation
- Include `SummaryCard.css`

### 3. `frontend/src/components/DataTable/DataTable.jsx`

A reusable data table component with sorting, filtering header, and empty state.

**Props:**
- `columns` (array of objects): `[{ key, label, render?, sortable?, align? }]`
  - `key`: field name in data
  - `label`: column header text
  - `render`: optional custom render function `(value, row) => JSX`
  - `sortable`: boolean, default true
  - `align`: 'left' | 'right' | 'center', default 'left'
- `data` (array of objects): the rows
- `emptyMessage` (string, optional): shown when data is empty
- `loading` (boolean, optional): show loading state
- `onRowClick` (function, optional): callback when a row is clicked

**Behavior:**
- Renders an HTML `<table>` with proper `<thead>` and `<tbody>`
- Clickable column headers for sorting (toggle asc/desc), show sort indicator arrow
- Alternating row backgrounds for readability
- Hover row highlight
- Loading state: show spinner
- Empty state: show the emptyMessage centered
- Numeric columns (amount, quantity, nav) right-aligned
- ID columns use `font-mono` class
- Include `DataTable.css`

### 4. `frontend/src/components/Charts/StatusChart.jsx`

A chart showing order distribution by status using Recharts.

**Props:**
- `data` (object): `{ PLANNED: 5, VALIDATED: 3, ... }` — status → count mapping  
- `title` (string, optional): chart title
- `type` (string, optional): 'bar' | 'donut', default 'bar'

**Behavior:**
- Transform data object into Recharts-compatible array: `[{ name: 'PLANNED', value: 5, fill: '#...' }, ...]`
- Use colors from `CHART_COLORS` and `ORDER_STATUS_COLORS` 
- For 'bar': horizontal BarChart with status labels on Y axis
- For 'donut': PieChart with inner radius for donut effect
- Responsive container
- Include `StatusChart.css`

### 5. `frontend/src/components/FilterBar/FilterBar.jsx`

A filter bar with dropdowns and search that sits above data tables.

**Props:**
- `filters` (array of objects): `[{ key, label, options: [{ value, label }], value, onChange }]`
- `searchPlaceholder` (string, optional)
- `searchValue` (string)
- `onSearchChange` (function)

**Behavior:**
- Horizontal bar with filter dropdowns and a search input
- Each filter renders as a `<select>` with an "All" option
- Search input with a magnifying glass icon
- Compact design, single row
- Include `FilterBar.css`

### 6. `frontend/src/components/Toast/Toast.jsx`

A toast notification component.

**Props:**
- `message` (string)
- `type` ('success' | 'error' | 'warning')
- `visible` (boolean)
- `onClose` (function)

**Behavior:**
- Fixed position bottom-right
- Slides up on appear, fades out after 4 seconds
- Close button (×)
- Auto dismiss with setTimeout
- Use the `.toast` styles from `components.css`

## Important Notes
- Every component should have its own `.css` file alongside it
- Use CSS variables from globals.css — do NOT hardcode any colors
- All components should handle edge cases: null/undefined values, empty arrays
- Use `formatCurrency` and `formatQuantity` from `utils/formatters.js` when rendering monetary values
