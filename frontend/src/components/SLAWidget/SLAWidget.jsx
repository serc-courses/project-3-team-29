import { useState, useEffect, useRef } from 'react'
import { getSla } from '../../api/slaApi'
import './SLAWidget.css'

function LatencyCard({ p99, avg, total }) {
  return (
    <div className="sla-card">
      <div className="sla-card-label">API Latency</div>
      <div className="sla-card-countdown font-mono" style={{ color: p99 > 500 ? '#EF4444' : '#10B981' }}>
        p99 {p99}ms
      </div>
      <div className="sla-card-subtitle">avg {avg}ms &nbsp;·&nbsp; {total.toLocaleString()} req</div>
    </div>
  )
}

export default function SLAWidget() {
  const [sla, setSla] = useState(null)
  const fetchRef = useRef(null)

  const fetchSla = async () => {
    try {
      const data = await getSla()
      setSla(data)
    } catch {
      // silently ignore — server may not be authenticated yet
    }
  }

  useEffect(() => {
    fetchSla()

    // Re-sync with server every 60 s to correct drift
    fetchRef.current = setInterval(fetchSla, 60_000)

    return () => {
      clearInterval(fetchRef.current)
    }
  }, [])

  if (!sla) return null

  return (
    <div className="sla-widget">
      <span className="sla-widget-title">SLA Tracker</span>
      <div className="sla-widget-cards">
        <LatencyCard
          p99={sla.latency?.p99Ms ?? 0}
          avg={sla.latency?.avgMs ?? 0}
          total={sla.latency?.totalRequests ?? 0}
        />
      </div>
    </div>
  )
}
