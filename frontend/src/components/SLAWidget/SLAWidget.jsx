import { useState, useEffect, useRef } from 'react'
import { getSla } from '../../api/slaApi'
import './SLAWidget.css'

function formatCountdown(seconds) {
  if (seconds <= 0) return '00:00:00'
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  return [h, m, s].map((v) => String(v).padStart(2, '0')).join(':')
}

function urgencyColor(seconds) {
  if (seconds < 1800) return '#EF4444'  // red  < 30 min
  if (seconds < 7200) return '#F59E0B'  // amber < 2 h
  return '#10B981'                       // green
}

function CutoffCard({ label, seconds }) {
  const color = urgencyColor(seconds)
  return (
    <div className="sla-card">
      <div className="sla-card-label">{label}</div>
      <div className="sla-card-countdown font-mono" style={{ color }}>
        {formatCountdown(seconds)}
      </div>
      <div className="sla-card-status" style={{ background: color }} />
    </div>
  )
}

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
  const [onshoreSecsLeft, setOnshoreSecsLeft] = useState(0)
  const [offshoreSecsLeft, setOffshoreSecsLeft] = useState(0)
  const tickRef = useRef(null)
  const fetchRef = useRef(null)

  const fetchSla = async () => {
    try {
      const data = await getSla()
      setSla(data)
      setOnshoreSecsLeft(data.secondsToOnshore)
      setOffshoreSecsLeft(data.secondsToOffshore)
    } catch {
      // silently ignore — server may not be authenticated yet
    }
  }

  useEffect(() => {
    fetchSla()

    // Tick every second locally
    tickRef.current = setInterval(() => {
      setOnshoreSecsLeft((s) => Math.max(0, s - 1))
      setOffshoreSecsLeft((s) => Math.max(0, s - 1))
    }, 1000)

    // Re-sync with server every 60 s to correct drift
    fetchRef.current = setInterval(fetchSla, 60_000)

    return () => {
      clearInterval(tickRef.current)
      clearInterval(fetchRef.current)
    }
  }, [])

  if (!sla) return null

  return (
    <div className="sla-widget">
      <span className="sla-widget-title">SLA Tracker</span>
      <div className="sla-widget-cards">
        <CutoffCard label="Onshore Cutoff  16:00 UTC" seconds={onshoreSecsLeft} />
        <CutoffCard label="Offshore Cutoff  01:00 UTC" seconds={offshoreSecsLeft} />
        <LatencyCard
          p99={sla.latency?.p99Ms ?? 0}
          avg={sla.latency?.avgMs ?? 0}
          total={sla.latency?.totalRequests ?? 0}
        />
      </div>
    </div>
  )
}
