import { useParams, useNavigate } from 'react-router-dom'
import { useMemo } from 'react'
import { getAdvisorClients, getAdvisorClientOrders } from '../../api/advisorApi'
import { useFetch } from '../../hooks/useFetch'
import { ADVISOR_CONFIG } from '../../constants/advisorConfig'
import OrderCard from '../../components/OrderCard/OrderCard'
import AmountDisplay from '../../components/AmountDisplay/AmountDisplay'
import EmptyState from '../../components/EmptyState/EmptyState'
import StatusDonut from '../../components/Charts/StatusDonut'
import { formatQuantity } from '../../utils/formatters'
import { STATUS_GROUP } from '../../constants/orderStatus'
import './AdvisorClientDetail.css'

const STATUS_DONUT_GROUPS = [
  { name: 'Completed', statuses: STATUS_GROUP.COMPLETED,  color: '#059669' },
  { name: 'Pending',   statuses: STATUS_GROUP.PENDING,    color: '#D97706' },
  { name: 'Processing',statuses: STATUS_GROUP.PROCESSING, color: '#2563EB' },
  { name: 'Failed',    statuses: STATUS_GROUP.FAILED,     color: '#DC2626' },
]

function BackIcon() {
  return (<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="15 18 9 12 15 6"/></svg>)
}

export default function AdvisorClientDetail({ advisorId }) {
  const { accountId } = useParams()
  const navigate = useNavigate()
  const id = advisorId || ADVISOR_CONFIG.DEFAULT_ADVISOR_ID

  const { data: clients } = useFetch(() => getAdvisorClients(id), [id])
  const { data: orders, loading } = useFetch(() => getAdvisorClientOrders(accountId), [accountId])

  const clientData = useMemo(() => clients?.find(c => c.accountID === accountId), [clients, accountId])
  const orderList = orders || []

  const donutData = useMemo(() =>
    STATUS_DONUT_GROUPS.map(g => ({
      name:  g.name,
      color: g.color,
      value: orderList.filter(o => g.statuses.includes(o.orderStatus)).length,
    })),
  [orderList])

  const recentOrders = useMemo(() =>
    [...orderList].sort((a, b) => b.orderID?.localeCompare(a.orderID)).slice(0, 5), [orderList])

  const totalAmount = useMemo(() => orderList.reduce((s, o) => s + (o.amount || 0), 0), [orderList])
  const totalQuantity = useMemo(() => orderList.reduce((s, o) => s + (o.quantity || 0), 0), [orderList])

  const activeCount = useMemo(() => orderList.filter(o => STATUS_GROUP.PROCESSING.includes(o.orderStatus)).length, [orderList])
  const failedCount = useMemo(() => orderList.filter(o => o.orderStatus === 'ERRORED').length, [orderList])

  const handlePlaceOrder = () => {
    localStorage.setItem(ADVISOR_CONFIG.STORAGE_KEYS.selectedClientId, accountId)
    navigate('/advisor/orders/new')
  }

  if (loading) {
    return (
      <div className="advisor-client-detail-page">
        <div className="order-detail-topbar">
          <button className="order-detail-back" onClick={() => navigate(-1)}><BackIcon /></button>
          <span className="order-detail-topbar-title">Client Detail</span>
          <span style={{ width: 36 }} />
        </div>
        <div style={{ padding: 16 }}>
          <div className="skeleton skeleton-card" style={{ height: 130, marginBottom: 16 }} />
          <div className="skeleton skeleton-card" style={{ height: 100, marginBottom: 16 }} />
          {[1,2].map(i => <div key={i} className="skeleton skeleton-card" style={{ marginBottom: 8 }} />)}
        </div>
      </div>
    )
  }

  const knownClients = ADVISOR_CONFIG.ADVISOR_CLIENT_MAP[id] || []
  if (!knownClients.includes(accountId) && clients && !clientData) {
    return (
      <div className="advisor-client-detail-page">
        <div className="order-detail-topbar">
          <button className="order-detail-back" onClick={() => navigate(-1)}><BackIcon /></button>
          <span className="order-detail-topbar-title">Client Detail</span>
          <span style={{ width: 36 }} />
        </div>
        <EmptyState title="Client not found" message={`${accountId} is not in your client roster`} action="Back" onAction={() => navigate(-1)} />
      </div>
    )
  }

  return (
    <div className="advisor-client-detail-page">
      <div className="order-detail-topbar">
        <button className="order-detail-back" onClick={() => navigate(-1)}><BackIcon /></button>
        <span className="order-detail-topbar-title">Client Detail</span>
        <span style={{ width: 36 }} />
      </div>

      <div className="advisor-client-detail-content">
        {/* Hero */}
        <div className="card card-elevated advisor-client-hero">
          <p className="font-mono" style={{ fontSize: 'var(--text-2xl)', fontWeight: 700, color: 'var(--color-text-900)' }}>{accountId}</p>
          <AmountDisplay amount={totalAmount} size="lg" />
          <p style={{ fontSize: 'var(--text-sm)', color: 'var(--color-text-500)' }}>
            {orderList.length} orders &middot; {formatQuantity(totalQuantity)} units
          </p>
        </div>

        {/* Metrics */}
        <div className="advisor-stats-row">
          <div className="advisor-stat-card"><span className="advisor-stat-value font-mono">{orderList.length}</span><span className="advisor-stat-label">orders</span></div>
          <div className="advisor-stat-card"><span className="advisor-stat-value font-mono" style={{ color: '#2563EB' }}>{activeCount}</span><span className="advisor-stat-label">active</span></div>
          <div className="advisor-stat-card"><span className="advisor-stat-value font-mono" style={{ color: '#DC2626' }}>{failedCount}</span><span className="advisor-stat-label">failed</span></div>
        </div>

        {/* Status donut */}
        <div className="section">
          <div className="section-header"><span className="section-title">Status Breakdown</span></div>
          <div className="card card-elevated">
            <StatusDonut data={donutData} innerRadius={40} outerRadius={65} height={160} />
          </div>
        </div>

        {/* Recent orders */}
        <div className="section">
          <div className="section-header">
            <span className="section-title">Recent Orders</span>
            <button className="section-action" onClick={() => navigate(`/advisor/activity?accountID=${accountId}`)}>View All →</button>
          </div>
          {recentOrders.length === 0
            ? <EmptyState title="No orders for this client" />
            : <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>{recentOrders.map(o => <OrderCard key={o.orderID} order={o} />)}</div>}
        </div>
      </div>

      {/* CTAs */}
      <div className="sticky-cta-no-nav" style={{ display: 'flex', gap: 12 }}>
        <button className="btn btn-outline" style={{ flex: 1 }} onClick={() => navigate(`/advisor/activity?accountID=${accountId}`)}>All Activity</button>
        <button className="btn btn-primary" style={{ flex: 1 }} onClick={handlePlaceOrder}>Place Order</button>
      </div>
    </div>
  )
}
