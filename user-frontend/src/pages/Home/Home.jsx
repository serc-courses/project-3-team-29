import { useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { getOrders } from '../../api/ordersApi'
import { useFetch } from '../../hooks/useFetch'
import { useAuth } from '../../context/AuthContext'
import OrderCard from '../../components/OrderCard/OrderCard'
import AmountDisplay from '../../components/AmountDisplay/AmountDisplay'
import EmptyState from '../../components/EmptyState/EmptyState'
import StatusDonut from '../../components/Charts/StatusDonut'
import { STATUS_GROUP } from '../../constants/orderStatus'
import './Home.css'

const PORTFOLIO_DONUT_GROUPS = [
  { name: 'Completed', statuses: STATUS_GROUP.COMPLETED,  color: '#059669' },
  { name: 'Pending',   statuses: STATUS_GROUP.PENDING,    color: '#D97706' },
  { name: 'Processing',statuses: STATUS_GROUP.PROCESSING, color: '#2563EB' },
  { name: 'Failed',    statuses: STATUS_GROUP.FAILED,     color: '#DC2626' },
]

function getGreeting() {
  const h = new Date().getHours()
  if (h < 12) return 'Good Morning'
  if (h < 17) return 'Good Afternoon'
  return 'Good Evening'
}

function SkeletonHome() {
  return (
    <div className="page home-page">
      <div className="skeleton skeleton-heading" style={{ width: '60%', marginBottom: 4 }} />
      <div className="skeleton skeleton-text-sm" style={{ width: '40%', marginBottom: 24 }} />
      <div className="skeleton skeleton-card" style={{ height: 120, marginBottom: 16 }} />
      <div className="home-stats-row">
        {[1,2,3].map(i => <div key={i} className="skeleton" style={{ height: 72, flex: 1, borderRadius: 12 }} />)}
      </div>
      <div style={{ height: 16 }} />
      {[1,2,3].map(i => <div key={i} className="skeleton skeleton-card" style={{ marginBottom: 8 }} />)}
    </div>
  )
}

function NoOrdersIcon() {
  return (
    <svg width="64" height="64" viewBox="0 0 64 64" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
      <rect x="12" y="8" width="40" height="48" rx="4"/>
      <line x1="22" y1="24" x2="42" y2="24"/>
      <line x1="22" y1="32" x2="36" y2="32"/>
      <line x1="22" y1="40" x2="30" y2="40"/>
    </svg>
  )
}

export default function Home({ sseEventCount = 0 }) {
  const navigate = useNavigate()
  const { user } = useAuth()
  const accountID = user?.accountID

  const { data: rawOrders, loading, error, refetch } = useFetch(
    () => getOrders(accountID ? { accountID } : {}),
    [sseEventCount, accountID]
  )
  const orders = rawOrders || []

  const stats = useMemo(() => {
    const totalInvested = orders.reduce((sum, o) => sum + (o.amount || 0), 0)
    const uniqueFunds = new Set(orders.map(o => o.fundID).filter(Boolean)).size
    const completed = orders.filter(o => STATUS_GROUP.COMPLETED.includes(o.orderStatus)).length
    const pending = orders.filter(o => STATUS_GROUP.PENDING.includes(o.orderStatus)).length
    const failed = orders.filter(o => STATUS_GROUP.FAILED.includes(o.orderStatus)).length
    const processing = orders.filter(o => STATUS_GROUP.PROCESSING.includes(o.orderStatus)).length
    return { totalInvested, uniqueFunds, completed, pending, failed, processing }
  }, [orders])

  const recentOrders = useMemo(() =>
    [...orders].sort((a, b) => b.orderID?.localeCompare(a.orderID)).slice(0, 5),
  [orders])

  const portfolioDonutData = useMemo(() =>
    PORTFOLIO_DONUT_GROUPS.map(g => ({
      name:  g.name,
      color: g.color,
      value: orders.filter(o => g.statuses.includes(o.orderStatus)).length,
    })),
  [orders])

  if (loading) return <SkeletonHome />

  if (error) return (
    <div className="page home-page">
      <div className="home-error-banner">
        <span>Failed to load portfolio</span>
        <button className="btn btn-ghost" onClick={refetch}>Retry</button>
      </div>
    </div>
  )

  return (
    <div className="page home-page">
      <div className="home-greeting">
        <h1 className="home-greeting-text">{getGreeting()}, {user?.displayName?.split(' ')[0]}</h1>
        <p className="home-greeting-sub">Your Portfolio</p>
      </div>

      {/* Hero investment card */}
      <div className="home-hero-card card card-elevated">
        <p className="home-hero-label">Total Invested</p>
        <AmountDisplay amount={stats.totalInvested} size="lg" />
        <p className="home-hero-meta">
          {orders.length} orders &middot; {stats.uniqueFunds} funds
        </p>
      </div>

      {/* Status summary row */}
      <div className="home-stats-row">
        <div className="home-stat-card">
          <span className="home-stat-value font-mono" style={{ color: '#059669' }}>{stats.completed}</span>
          <span className="home-stat-label">Completed</span>
        </div>
        <div className="home-stat-card">
          <span className="home-stat-value font-mono" style={{ color: '#D97706' }}>{stats.pending}</span>
          <span className="home-stat-label">Pending</span>
        </div>
        <div className="home-stat-card">
          {stats.failed > 0 ? (
            <>
              <span className="home-stat-value font-mono" style={{ color: '#DC2626' }}>{stats.failed}</span>
              <span className="home-stat-label">Failed</span>
            </>
          ) : (
            <>
              <span className="home-stat-value font-mono" style={{ color: '#2563EB' }}>{stats.processing}</span>
              <span className="home-stat-label">Processing</span>
            </>
          )}
        </div>
      </div>

      {/* Quick actions */}
      <div className="home-actions">
        <button className="btn btn-primary home-action-btn" onClick={() => navigate('/orders/new')}>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
            <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
          </svg>
          Invest
        </button>
        <button className="btn btn-outline home-action-btn" onClick={() => navigate('/orders')}>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
            <polyline points="14 2 14 8 20 8"/>
            <line x1="8" y1="13" x2="16" y2="13"/>
            <line x1="8" y1="17" x2="12" y2="17"/>
          </svg>
          My Orders
        </button>
      </div>

      {/* Portfolio status donut */}
      {orders.length > 0 && (
        <div className="card card-elevated home-portfolio-status">
          <p style={{ fontSize: 'var(--text-xs)', fontWeight: 600, color: 'var(--color-text-400)', textTransform: 'uppercase', letterSpacing: '.05em', marginBottom: 4 }}>
            Portfolio Status
          </p>
          <StatusDonut data={portfolioDonutData} innerRadius={42} outerRadius={65} height={165} />
        </div>
      )}

      {/* Recent orders */}
      <div className="section">
        <div className="section-header">
          <span className="section-title">Recent Orders</span>
          <button className="section-action" onClick={() => navigate('/orders')}>View All →</button>
        </div>
        {recentOrders.length === 0 ? (
          <EmptyState
            icon={<NoOrdersIcon />}
            title="No investments yet"
            message="Start your investment journey today"
            action="Invest Now"
            onAction={() => navigate('/orders/new')}
          />
        ) : (
          <div className="home-orders-list">
            {recentOrders.map(order => (
              <OrderCard key={order.orderID} order={order} />
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
