# Prompt 25: User Frontend — Advisor SSE Live Alerts

## Context
Advisor mode should react to real-time order updates across the advisor's client book. Reuse the existing SSE stream but add advisor-facing refresh and alert patterns.

## Task
Update the SSE integration so advisor pages can auto-refresh and show lightweight live alerts.

## Files to Update
- `user-frontend/src/hooks/useSse.js`
- `user-frontend/src/App.jsx`
- advisor pages that need live updates

## Requirements
1. Reuse the existing SSE stream at `GET /view/stream`
2. Continue incrementing a global `eventCount`
3. Add a small in-memory alert buffer for advisor mode:
   - latest 5 event summaries
   - event type + timestamp
4. Advisor pages that should auto-refresh on SSE updates:
   - Advisor Home
   - Clients Directory
   - Advisor Activity
5. Client detail page may also refresh if visible
6. Add a subtle live activity chip on advisor home:
   - `Live updates on` when connected
   - `Reconnecting...` when disconnected
7. Do not create disruptive toast spam for every SSE event
8. If the most recent event corresponds to an errored order, surface one highlighted alert row on Advisor Home

## Suggested Alert Data Shape
```js
{
  type: 'order-updated',
  timestamp: Date.now(),
  label: 'Order status updated'
}
```

## Integration Pattern
```js
const { connected, eventCount, alerts } = useSse()

const { data } = useFetch(
  () => getAdvisorBookOverview(advisorId),
  [advisorId, eventCount]
)
```

## Important Notes
- The existing SSE stream is shared by investor and advisor flows
- Keep the connection indicator subtle and mobile-appropriate
- Advisor alerts are informational, not a substitute for a notification center