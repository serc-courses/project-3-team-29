import { useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell } from 'recharts'
import { getAdvisorBookOverview, getAdvisorClients } from '../../api/advisorApi'
import { useFetch } from '../../hooks/useFetch'
import { ADVISOR_CONFIG } from '../../constants/advisorConfig'
import { STATUS_GROUP } from '../../constants/orderStatus'
import { formatCurrency, formatCompact } from '../../utils/formatters'
import StatusDonut from '../../components/Charts/StatusDonut'
import './AdvisorHome.css'

const TOOLTIP_STYLE = {
  background: '#fff',
  border: '1px solid #E2E8F0',
  borderRadius: 8,
  boxShadow: '0 4px 12px rgba(0,0,0,0.08)',
  fontSize: 13,
  fontFamily: 'Inter, sans-serif',
  padding: '6px 10px',
}

const STATUS_DONUT_GROUPS = [
  { name: 'Completed', statuses: STATUS_GROUP.COMPLETED,  color: '#059669' },
  { name: 'Pending',   statuses: STATUS_GROUP.PENDING,    color: '#D97706' },
  { name: 'Processing',statuses: STATUS_GROUP.PROCESSING, color: '#2563EB' },
  { name: 'Failed',    statuses: STATUS_GROUP.FAILED,     color: '#DC2626' },
]

function SkeletonAdvisorHome() {
  return (
    <div className="page advisor-home-page">
      <div className="skeleton skeleton-heading" style={{ width: '50%', marginBottom: 4 }} />
      <div className="skeleton skeleton-text-sm" style={{ width: '40%', marginBottom: 20 }} />
      <div className="skeleton skeleton-card" style={{ height: 110, marginBottom: 16 }} />
      <div style={{ display: 'flex', gap: 12, marginBottom: 16 }}>
        {[1,2,3,4].map(i => <div key={i} className="skeleton" style={{ height: 72, flex: 1, borderRadius: 12 }} />)}
      </div>
      {[1,2].map(i => <div key={i} className="skeleton skeleton-card" style={{ marginBottom: 8 }} />)}
    </div>
  )
}

export default function AdvisorHome({ sseEventCount = 0, advisorId }) {
  const id = advisorId || ADVISOR_CONFIG.DEFAULT_ADVISOR_ID
  const navigate = useNavigate()

  const { data: overview, loading: ovLoading } = useFetch(() => getAdvisorBookOverview(id), [id, sseEventCount])
  const { data: clients, loading: clLoading } = useFetch(() => getAdvisorClients(id), [id, sseEventCount])

  const loading = ovLoading || clLoading

  const attentionClients = useMemo(() => {
    if (!clients) return []
    return clients.filter(c => (c.statuses?.ERRORED || 0) > 0 || (c.statuses?.BULKED || 0) + (c.statuses?.CONFIRMED || 0) > 2)
  }, [clients])

  const topClients = useMemo(() => {
    if (!clients) return []
    return [...clients].sort((a, b) => (b.totalAmount || 0) - (a.totalAmount || 0)).slice(0, 5)
  }, [clients])

  /* Book status donut data */
  const bookStatusData = useMemo(() => {
    if (!clients) return STATUS_DONUT_GROUPS.map(g => ({ ...g, value: 0 }))
    return STATUS_DONUT_GROUPS.map(g => ({
      name: g.name,
      color: g.color,
      value: clients.reduce(
        (sum, c) => sum + g.statuses.reduce((s, k) => s + (c.statuses?.[k] || 0), 0),
        0
      ),
    }))
  }, [clients])

  const completedCount = useMemo(() =>
    bookStatusData.find(g => g.name === 'Completed')?.value ?? 0,
  [bookStatusData])

  /* Client allocation bar data */
  const allocationData = useMemo(() => {
    if (!clients) return []
    return [...clients]
      .sort((a, b) => (b.totalAmount || 0) - (a.totalAmount || 0))
      .map(c => ({ name: c.accountID, amount: c.totalAmount || 0 }))
  }, [clients])

  if (loading) return <SkeletonAdvisorHome />

  return (
    <div className="page advisor-home-page">
      <div className="advisor-home-header">
        <h1 className="page-title">Advisor Book</h1>
        <p className="page-subtitle">{id} &middot; {overview?.clientCount ?? 0} clients</p>
      </div>

      {/* Hero */}
      <div className="card card-elevated advisor-hero">
        <p className="advisor-hero-label">Total Book Value</p>
        <p className="advisor-hero-amount font-mono">
          {overview?.totalAmount != null ? formatCurrency(overview.totalAmount) : '—'}
        </p>
        <p className="advisor-hero-meta">
          {overview?.activeOrders ?? 0} active &middot; {overview?.failedOrders ?? 0} failed
        </p>
      </div>

      {/* Stats */}
      <div className="advisor-stats-row">
        <div className="advisor-stat-card">
          <span className="advisor-stat-value font-mono">{overview?.clientCount ?? 0}</span>
          <span className="advisor-stat-label">clients</span>
        </div>
        <div className="advisor-stat-card">
          <span className="advisor-stat-value font-mono" style={{ color: '#2563EB' }}>{overview?.activeOrders ?? 0}</span>
          <span className="advisor-stat-label">active</span>
        </div>
        <div className="advisor-stat-card">
          <span className="advisor-stat-value font-mono" style={{ color: '#DC2626' }}>{overview?.failedOrders ?? 0}</span>
          <span className="advisor-stat-label">failed</span>
        </div>
        <div className="advisor-stat-card">
          <span className="advisor-stat-value font-mono" style={{ color: '#059669' }}>{completedCount}</span>
          <span className="advisor-stat-label">completed</span>
        </div>
      </div>

      {/* Book status donut */}
      <div className="card card-elevated advisor-book-status-chart">
        <p className="advisor-chart-title">Book Status</p>
        <StatusDonut data={bookStatusData} innerRadius={50} outerRadius={78} height={200} />
      </div>

      {/* Client allocation bar */}
      {allocationData.length > 0 && (
        <div className="card card-elevated advisor-allocation-chart">
          <p className="advisor-chart-title">Client Allocation</p>
          <ResponsiveContainer width="100%" height={allocationData.length * 40}>
            <BarChart data={allocationData} layout="vertical" margin={{ left: 80, right: 16, top: 4, bottom: 4 }}>
              <XAxis type="number" hide />
              <YAxis type="category" dataKey="name" tick={{ fontSize: 12, fill: '#64748B' }} width={80} />
              <Tooltip
                contentStyle={TOOLTIP_STYLE}
                formatter={v => [formatCurrency(v), 'Amount']}
              />
              <Bar dataKey="amount" fill="#059669" radius={[0, 4, 4, 0]}
                isAnimationActive animationDuration={600} animationEasing="ease-out" />
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}

      {/* Quick actions */}
      <div className="advisor-actions">
        <button className="btn btn-primary advisor-action-btn" onClick={() => navigate('/advisor/orders/new')}>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
            <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
          </svg>
          New Basket
        </button>
        <button className="btn btn-outline advisor-action-btn" onClick={() => navigate('/advisor/clients')}>
          Clients
        </button>
      </div>

      {/* Attention needed */}
      {attentionClients.length > 0 && (
        <div className="section">
          <div className="section-header">
            <span className="section-title">Attention Needed</span>
          </div>
          <div className="card card-elevated" style={{ padding: 0, overflow: 'hidden' }}>
            {attentionClients.map((c, i) => (
              <button
                key={c.accountID}
                className="advisor-attention-row"
                onClick={() => navigate(`/advisor/clients/${c.accountID}`)}
                style={{ borderTop: i > 0 ? '1px solid var(--color-border-light)' : 'none' }}
              >
                <span className="font-mono" style={{ fontSize: 'var(--text-sm)', fontWeight: 600 }}>{c.accountID}</span>
                <span className="advisor-attention-badge">
                  {c.statuses?.ERRORED > 0 ? `${c.statuses.ERRORED} errored` : 'needs review'}
                </span>
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Top clients */}
      {topClients.length > 0 && (
        <div className="section">
          <div className="section-header">
            <span className="section-title">Top Clients</span>
            <button className="section-action" onClick={() => navigate('/advisor/clients')}>View All →</button>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {topClients.map(c => (
              <button
                key={c.accountID}
                className="card card-interactive card-elevated advisor-client-row"
                onClick={() => navigate(`/advisor/clients/${c.accountID}`)}
              >
                <div className="advisor-client-row-top">
                  <span className="font-mono" style={{ fontWeight: 700, color: 'var(--color-text-900)', fontSize: 'var(--text-base)' }}>{c.accountID}</span>
                  <span className="font-mono" style={{ color: 'var(--color-primary)', fontWeight: 600 }}>
                    {c.totalAmount != null ? `₹${formatCompact(c.totalAmount)}` : '—'}
                  </span>
                </div>
                <div className="advisor-client-row-meta">
                  <span>{c.orderCount ?? 0} orders</span>
                  {c.statuses?.ERRORED > 0 && <span style={{ color: 'var(--color-error)' }}>{c.statuses.ERRORED} failed</span>}
                </div>
              </button>
            ))}
          </div>
        </div>
      )}

      {!clients?.length && (
        <div className="empty-state">
          <p className="empty-state-title">No clients assigned</p>
          <p className="empty-state-text">No clients are assigned to advisor {id}</p>
        </div>
      )}
    </div>
  )
}
