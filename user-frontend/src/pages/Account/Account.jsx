import { useState, useEffect, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { getOrders } from '../../api/ordersApi'
import { getAdvisorBookOverview, getAdvisorClients } from '../../api/advisorApi'
import { useAuth } from '../../context/AuthContext'
import OrderCard from '../../components/OrderCard/OrderCard'
import AmountDisplay from '../../components/AmountDisplay/AmountDisplay'
import EmptyState from '../../components/EmptyState/EmptyState'
import StatusDonut from '../../components/Charts/StatusDonut'
import { formatCurrency, formatQuantity, formatCompact } from '../../utils/formatters'
import { STATUS_GROUP } from '../../constants/orderStatus'
import { CONFIG } from '../../constants/config'
import './Account.css'

const STATUS_BARS = [
  { key: 'completed', label: 'Completed', statuses: STATUS_GROUP.COMPLETED, color: '#059669' },
  { key: 'pending',   label: 'Pending',   statuses: STATUS_GROUP.PENDING,   color: '#D97706' },
  { key: 'processing',label: 'Processing',statuses: STATUS_GROUP.PROCESSING,color: '#2563EB' },
  { key: 'failed',    label: 'Failed',    statuses: STATUS_GROUP.FAILED,    color: '#DC2626' },
]

const DONUT_GROUPS = [
  { name: 'Completed', statuses: STATUS_GROUP.COMPLETED,  color: '#059669' },
  { name: 'Pending',   statuses: STATUS_GROUP.PENDING,    color: '#D97706' },
  { name: 'Processing',statuses: STATUS_GROUP.PROCESSING, color: '#2563EB' },
  { name: 'Failed',    statuses: STATUS_GROUP.FAILED,     color: '#DC2626' },
]

function NoOrdersIcon() {
  return (
    <svg width="64" height="64" viewBox="0 0 64 64" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
      <rect x="12" y="8" width="40" height="48" rx="4"/>
      <line x1="22" y1="24" x2="42" y2="24"/>
      <line x1="22" y1="32" x2="36" y2="32"/>
    </svg>
  )
}

export default function Account() {
  const navigate = useNavigate()
  const { user, logout } = useAuth()
  const accountID = user?.accountID
  const isAdvisor = user?.role === 'ADVISOR'
  const roleLabel = isAdvisor ? 'Advisor' : 'Investor'

  /* ── Investor state ── */
  const [orders, setOrders] = useState([])
  const [dataLoading, setDataLoading] = useState(true)
  const [fetchError, setFetchError] = useState(null)

  /* ── Advisor state ── */
  const [advisorOverview, setAdvisorOverview] = useState(null)
  const [advisorClients, setAdvisorClients] = useState(null)
  const [advisorLoading, setAdvisorLoading] = useState(false)

  /* Investor fetch */
  useEffect(() => {
    if (isAdvisor) { setDataLoading(false); return }
    if (!accountID) { setDataLoading(false); return }
    setDataLoading(true)
    setFetchError(null)
    getOrders({ accountID })
      .then(result => setOrders(Array.isArray(result) ? result : []))
      .catch(err => setFetchError(err?.message || 'Failed to load orders'))
      .finally(() => setDataLoading(false))
  }, [accountID, isAdvisor])

  /* Advisor fetch */
  useEffect(() => {
    if (!isAdvisor) return
    const advisorId = user?.advisorID
    if (!advisorId) return
    setAdvisorLoading(true)
    Promise.all([
      getAdvisorBookOverview(advisorId).catch(() => null),
      getAdvisorClients(advisorId).catch(() => []),
    ]).then(([ov, cl]) => {
      setAdvisorOverview(ov)
      setAdvisorClients(cl)
    }).finally(() => setAdvisorLoading(false))
  }, [isAdvisor, user?.advisorID])

  /* Investor derived data */
  const statusCounts = useMemo(() => {
    const counts = {}
    STATUS_BARS.forEach(b => { counts[b.key] = orders.filter(o => b.statuses.includes(o.orderStatus)).length })
    return counts
  }, [orders])

  const maxCount = Math.max(...Object.values(statusCounts), 1)

  const totalAmount   = useMemo(() => orders.reduce((s, o) => s + (o.amount   || 0), 0), [orders])
  const totalQuantity = useMemo(() => orders.reduce((s, o) => s + (o.quantity || 0), 0), [orders])
  const recentOrders  = useMemo(() => [...orders].sort((a, b) => b.orderID?.localeCompare(a.orderID)).slice(0, 5), [orders])

  /* Advisor derived data */
  const advisorDonutData = useMemo(() => {
    if (!advisorClients) return DONUT_GROUPS.map(g => ({ ...g, value: 0 }))
    return DONUT_GROUPS.map(g => ({
      name:  g.name,
      color: g.color,
      value: advisorClients.reduce(
        (sum, c) => sum + g.statuses.reduce((s, k) => s + (c.statuses?.[k] || 0), 0),
        0
      ),
    }))
  }, [advisorClients])

  const top3Clients = useMemo(() => {
    if (!advisorClients) return []
    return [...advisorClients].sort((a, b) => (b.totalAmount || 0) - (a.totalAmount || 0)).slice(0, 3)
  }, [advisorClients])

  return (
    <div className="page account-page">
      <div className="account-header">
        <h1 className="page-title">My Account</h1>
        <button className="btn btn-outline account-logout" onClick={logout}>Sign Out</button>
      </div>

      {/* Identity card */}
      <div className="card card-elevated account-identity-card">
        <p className="account-identity-name">{user?.displayName || user?.username}</p>
        <p className="account-identity-id font-mono">
          {isAdvisor ? user?.advisorID : accountID}
        </p>
        <p className={`account-identity-role${isAdvisor ? ' advisor' : ''}`}>{roleLabel}</p>
      </div>

      {/* ── ADVISOR SECTION ── */}
      {isAdvisor && (
        <>
          {advisorLoading ? (
            <>
              <div className="skeleton skeleton-card" style={{ height: 100, marginBottom: 'var(--sp-3)' }} />
              <div className="skeleton skeleton-card" style={{ height: 200 }} />
            </>
          ) : (
            <>
              {/* Book hero */}
              <div className="card card-elevated account-hero" style={{ marginBottom: 'var(--sp-3)' }}>
                <p style={{ fontSize: 'var(--text-xs)', fontWeight: 600, color: '#2563EB', textTransform: 'uppercase', letterSpacing: '.05em' }}>Total Book Value</p>
                <p style={{ fontSize: 'var(--text-3xl)', fontWeight: 700, letterSpacing: '-0.02em', color: 'var(--color-text-900)', fontFamily: 'var(--font-mono)' }}>
                  {advisorOverview?.totalAmount != null ? formatCurrency(advisorOverview.totalAmount) : '—'}
                </p>
                <p className="account-hero-meta">
                  {advisorOverview?.clientCount ?? 0} clients &middot; {advisorOverview?.activeOrders ?? 0} active &middot; {advisorOverview?.failedOrders ?? 0} failed
                </p>
              </div>

              {/* Book status donut */}
              <div className="card card-elevated" style={{ marginBottom: 'var(--sp-3)' }}>
                <p style={{ fontSize: 'var(--text-xs)', fontWeight: 600, color: 'var(--color-text-400)', textTransform: 'uppercase', letterSpacing: '.05em', marginBottom: 4 }}>Book Status</p>
                <StatusDonut data={advisorDonutData} innerRadius={44} outerRadius={68} height={168} />
              </div>

              {/* Top 3 clients */}
              {top3Clients.length > 0 && (
                <div className="section">
                  <div className="section-header">
                    <span className="section-title">Top Clients</span>
                  </div>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                    {top3Clients.map(c => (
                      <button
                        key={c.accountID}
                        className="card card-interactive card-elevated"
                        style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', textAlign: 'left', width: '100%' }}
                        onClick={() => navigate(`/advisor/clients/${c.accountID}`)}
                      >
                        <span className="font-mono" style={{ fontWeight: 700, fontSize: 'var(--text-sm)', color: 'var(--color-text-900)' }}>{c.accountID}</span>
                        <span className="font-mono" style={{ fontWeight: 600, color: 'var(--color-primary)' }}>
                          {c.totalAmount != null ? `₹${formatCompact(c.totalAmount)}` : '—'}
                        </span>
                      </button>
                    ))}
                  </div>
                </div>
              )}

              <div style={{ marginTop: 'var(--sp-4)' }}>
                <button className="btn btn-outline btn-full" onClick={() => navigate('/advisor/clients')}>
                  Go to Clients →
                </button>
              </div>
            </>
          )}
        </>
      )}

      {/* Error banner */}
      {fetchError && !dataLoading && (
        <div className="account-error-banner">
          <span>{fetchError}</span>
          <button className="btn btn-ghost" style={{ fontSize: 'var(--text-sm)' }} onClick={() => {
            setFetchError(null)
            setDataLoading(true)
            getOrders({ accountID })
              .then(r => setOrders(Array.isArray(r) ? r : []))
              .catch(e => setFetchError(e?.message || 'Failed to load orders'))
              .finally(() => setDataLoading(false))
          }}>Retry</button>
        </div>
      )}

      {/* ── INVESTOR SECTIONS ── */}
      {!isAdvisor && (
        <>
          {/* Investment summary */}
          {dataLoading ? (
            <div className="skeleton skeleton-card" style={{ height: 120, marginBottom: 'var(--sp-4)' }} />
          ) : (
            <div className="card card-elevated account-hero">
              <AmountDisplay amount={totalAmount} size="lg" />
              <p className="account-hero-meta">
                {orders.length} orders &middot; {formatQuantity(totalQuantity)} units
              </p>
            </div>
          )}

          {/* Status breakdown */}
          <div className="section">
            <div className="section-header">
              <span className="section-title">Status Breakdown</span>
            </div>
            {dataLoading ? (
              <div className="skeleton skeleton-card" style={{ height: 120 }} />
            ) : (
              <div className="card card-elevated">
                {STATUS_BARS.filter(b => statusCounts[b.key] > 0).map(b => (
                  <div key={b.key} className="status-bar-row">
                    <span className="status-bar-label">{b.label}</span>
                    <div className="status-bar-track">
                      <div className="status-bar-fill" style={{ width: `${(statusCounts[b.key] / maxCount) * 100}%`, background: b.color }} />
                    </div>
                    <span className="status-bar-count font-mono">{statusCounts[b.key]}</span>
                  </div>
                ))}
                {Object.values(statusCounts).every(v => v === 0) && (
                  <p style={{ color: 'var(--color-text-400)', fontSize: 'var(--text-sm)', textAlign: 'center', padding: '8px 0' }}>No orders yet</p>
                )}
              </div>
            )}
          </div>

          {/* Recent orders */}
          <div className="section">
            <div className="section-header">
              <span className="section-title">Recent Orders</span>
              <button className="section-action" onClick={() => navigate('/orders')}>View All →</button>
            </div>
            {dataLoading ? (
              [1,2].map(i => <div key={i} className="skeleton skeleton-card" style={{ marginBottom: 8 }} />)
            ) : recentOrders.length === 0 ? (
              <EmptyState icon={<NoOrdersIcon />} title="No orders for this account" />
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                {recentOrders.map(order => (
                  <OrderCard key={order.orderID} order={order} />
                ))}
              </div>
            )}
          </div>
        </>
      )}

      {/* App info */}
      <div className="card account-info-card">
        <p className="account-info-title">{CONFIG.APP_NAME}</p>
        <p className="account-info-row">Version: v1.0</p>
        <p className="account-info-row">Backend: localhost:8080</p>
      </div>
    </div>
  )
}
