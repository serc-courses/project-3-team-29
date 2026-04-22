import { useParams, useNavigate } from 'react-router-dom'
import { useMemo } from 'react'
import { getFunds } from '../../api/portfolioApi'
import { getOrders } from '../../api/ordersApi'
import { useFetch } from '../../hooks/useFetch'
import OrderCard from '../../components/OrderCard/OrderCard'
import AmountDisplay from '../../components/AmountDisplay/AmountDisplay'
import EmptyState from '../../components/EmptyState/EmptyState'
import { formatCurrency, formatCompact, formatQuantity } from '../../utils/formatters'
import './FundDetail.css'

function BackIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="15 18 9 12 15 6"/>
    </svg>
  )
}

function NotFoundIcon() {
  return (
    <svg width="64" height="64" viewBox="0 0 64 64" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="32" cy="32" r="24"/>
      <line x1="32" y1="20" x2="32" y2="36"/>
      <circle cx="32" cy="44" r="1" fill="currentColor"/>
    </svg>
  )
}

export default function FundDetail() {
  const { fundId } = useParams()
  const navigate = useNavigate()

  const { data: fundsData, loading: fundsLoading } = useFetch(getFunds, [])
  const { data: ordersData, loading: ordersLoading } = useFetch(
    () => getOrders({ fundID: fundId }), [fundId]
  )

  const fund = useMemo(() => {
    if (!fundsData) return null
    return fundsData.find(f => f.fundID === fundId) || null
  }, [fundsData, fundId])

  const recentOrders = useMemo(() => {
    if (!ordersData) return []
    return [...ordersData].sort((a, b) => b.orderID?.localeCompare(a.orderID)).slice(0, 5)
  }, [ordersData])

  const loading = fundsLoading || ordersLoading

  if (loading) {
    return (
      <div className="fund-detail-page">
        <div className="fund-detail-topbar">
          <button className="fund-detail-back" onClick={() => navigate(-1)}><BackIcon /></button>
          <span className="fund-detail-topbar-title">Fund Detail</span>
          <span style={{ width: 36 }} />
        </div>
        <div style={{ padding: 16 }}>
          <div className="skeleton skeleton-card" style={{ height: 140, marginBottom: 16 }} />
          <div className="skeleton" style={{ height: 72, borderRadius: 12, marginBottom: 16 }} />
          {[1,2,3].map(i => <div key={i} className="skeleton skeleton-card" style={{ marginBottom: 8 }} />)}
        </div>
      </div>
    )
  }

  if (!fund && fundsData) {
    return (
      <div className="fund-detail-page">
        <div className="fund-detail-topbar">
          <button className="fund-detail-back" onClick={() => navigate(-1)}><BackIcon /></button>
          <span className="fund-detail-topbar-title">Fund Detail</span>
          <span style={{ width: 36 }} />
        </div>
        <EmptyState
          icon={<NotFoundIcon />}
          title="Fund not found"
          message={`No data available for ${fundId}`}
          action="Go Back"
          onAction={() => navigate(-1)}
        />
      </div>
    )
  }

  const displayFund = fund || { fundID: fundId, fundName: fundId, nav: null, orderCount: 0, totalAmount: 0, totalQuantity: 0, orderSides: {} }

  return (
    <div className="fund-detail-page">
      <div className="fund-detail-topbar">
        <button className="fund-detail-back" onClick={() => navigate(-1)}><BackIcon /></button>
        <span className="fund-detail-topbar-title">Fund Detail</span>
        <span style={{ width: 36 }} />
      </div>

      <div className="fund-detail-content">
        {/* Hero */}
        <div className="card card-elevated fund-detail-hero">
          <p className="fund-detail-name">{displayFund.fundName}</p>
          <p className="fund-detail-id font-mono text-muted">{displayFund.fundID}</p>
          <div className="fund-detail-nav-block">
            <span className="fund-detail-nav-label">NAV</span>
            <span className="fund-detail-nav-value font-mono">
              {displayFund.nav ? formatCurrency(displayFund.nav) : '—'}
            </span>
          </div>
        </div>

        {/* Stats */}
        <div className="fund-detail-stats">
          <div className="fund-detail-stat card">
            <span className="fund-detail-stat-value font-mono">{ordersData?.length ?? displayFund.orderCount ?? 0}</span>
            <span className="fund-detail-stat-label">orders</span>
          </div>
          <div className="fund-detail-stat card">
            <span className="fund-detail-stat-value font-mono">
              {displayFund.totalAmount != null ? `₹${formatCompact(displayFund.totalAmount)}` : '—'}
            </span>
            <span className="fund-detail-stat-label">invested</span>
          </div>
          <div className="fund-detail-stat card">
            <span className="fund-detail-stat-value font-mono">
              {displayFund.totalQuantity != null ? formatCompact(displayFund.totalQuantity) : '—'}
            </span>
            <span className="fund-detail-stat-label">units</span>
          </div>
        </div>

        {/* Side split */}
        <div className="fund-detail-sides">
          <div className="fund-detail-side-card fund-detail-side-buy">
            <span className="fund-detail-side-count font-mono">{displayFund.orderSides?.BUY ?? 0}</span>
            <span className="fund-detail-side-label">BUY orders</span>
          </div>
          <div className="fund-detail-side-card fund-detail-side-sell">
            <span className="fund-detail-side-count font-mono">{displayFund.orderSides?.SELL ?? 0}</span>
            <span className="fund-detail-side-label">SELL orders</span>
          </div>
        </div>

        {/* Orders */}
        <div className="section">
          <div className="section-header">
            <span className="section-title">Orders in this Fund</span>
            <button className="section-action" onClick={() => navigate('/orders')}>View All →</button>
          </div>
          {recentOrders.length === 0 ? (
            <EmptyState
              icon={<NotFoundIcon />}
              title="No orders for this fund yet"
            />
          ) : (
            <div className="fund-detail-orders">
              {recentOrders.map(order => (
                <OrderCard key={order.orderID} order={order} />
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Sticky CTA */}
      <div className="sticky-cta-no-nav">
        <button
          className="btn btn-primary btn-full btn-lg"
          onClick={() => navigate('/orders/new')}
        >
          Invest in this Fund
        </button>
      </div>
    </div>
  )
}
