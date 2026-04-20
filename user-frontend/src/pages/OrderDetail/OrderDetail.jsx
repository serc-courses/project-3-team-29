import { useParams, useNavigate } from 'react-router-dom'
import { useMemo } from 'react'
import { getOrders } from '../../api/ordersApi'
import { useFetch } from '../../hooks/useFetch'
import StatusPill from '../../components/StatusPill/StatusPill'
import AmountDisplay from '../../components/AmountDisplay/AmountDisplay'
import EmptyState from '../../components/EmptyState/EmptyState'
import { formatCurrency, formatQuantity } from '../../utils/formatters'
import { SIDE_COLORS } from '../../constants/statusColors'
import './OrderDetail.css'

const ORDER_STEPS = ['PLANNED', 'VALIDATED', 'ENRICHED', 'PLACED', 'BULKED', 'CONFIRMED', 'CONTRACTED', 'BOOKED']

const STEP_LABELS = {
  PLANNED: 'Order Planned',
  VALIDATED: 'Validated',
  ENRICHED: 'Enriched',
  PLACED: 'Order Placed',
  BULKED: 'Grouped',
  CONFIRMED: 'Confirmed',
  CONTRACTED: 'Contracted',
  BOOKED: 'Completed',
}

function CheckIcon() {
  return (
    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="20 6 9 17 4 12"/>
    </svg>
  )
}

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

function ProgressTracker({ status }) {
  const currentIndex = ORDER_STEPS.indexOf(status)
  const isErrored = status === 'ERRORED'

  const steps = isErrored ? ORDER_STEPS : ORDER_STEPS

  return (
    <div className="progress-tracker">
      {steps.map((step, idx) => {
        const isCompleted = !isErrored && idx < currentIndex
        const isCurrent = !isErrored && idx === currentIndex
        const isUpcoming = !isErrored && idx > currentIndex
        const isLast = idx === steps.length - 1

        return (
          <div key={step}>
            <div className="progress-step">
              <div className="progress-step-indicator">
                <div className={`progress-dot${isCompleted ? ' completed' : isCurrent ? ' current' : isErrored ? ' errored' : ''}`}>
                  {isCompleted && <CheckIcon />}
                </div>
                {!isLast && (
                  <div className={`progress-line${isCompleted ? ' completed' : ''}`} />
                )}
              </div>
              <div className="progress-step-content">
                <span className={`progress-step-label${isCurrent ? ' current' : isCompleted ? ' done' : ''}`}>
                  {STEP_LABELS[step]}
                </span>
                {isCurrent && <span className="progress-step-badge">In progress</span>}
              </div>
            </div>
          </div>
        )
      })}
      {isErrored && (
        <div className="progress-step">
          <div className="progress-step-indicator">
            <div className="progress-dot errored">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round">
                <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </div>
          </div>
          <div className="progress-step-content">
            <span className="progress-step-label" style={{ color: 'var(--color-error)' }}>Failed</span>
          </div>
        </div>
      )}
    </div>
  )
}

function DetailRow({ label, value, mono }) {
  return (
    <div className="detail-row">
      <span className="detail-label">{label}</span>
      <span className={`detail-value${mono ? ' font-mono' : ''}`}>{value ?? '—'}</span>
    </div>
  )
}

export default function OrderDetail() {
  const { orderId } = useParams()
  const navigate = useNavigate()
  const { data, loading } = useFetch(() => getOrders({ orderID: orderId }), [orderId])

  const order = data?.[0]
  const sideColors = SIDE_COLORS[order?.orderSide] || { text: '#64748B', bg: '#F1F5F9' }

  if (loading) {
    return (
      <div className="order-detail-page">
        <div className="order-detail-topbar">
          <button className="order-detail-back" onClick={() => navigate(-1)}><BackIcon /></button>
          <span className="order-detail-topbar-title">Order Detail</span>
          <span style={{ width: 36 }} />
        </div>
        <div style={{ padding: 16 }}>
          <div className="skeleton skeleton-card" style={{ height: 160, marginBottom: 16 }} />
          <div className="skeleton skeleton-card" style={{ height: 300 }} />
        </div>
      </div>
    )
  }

  if (!order) {
    return (
      <div className="order-detail-page">
        <div className="order-detail-topbar">
          <button className="order-detail-back" onClick={() => navigate(-1)}><BackIcon /></button>
          <span className="order-detail-topbar-title">Order Detail</span>
          <span style={{ width: 36 }} />
        </div>
        <EmptyState
          icon={<NotFoundIcon />}
          title="Order not found"
          message="This order could not be found"
          action="Go Back"
          onAction={() => navigate(-1)}
        />
      </div>
    )
  }

  return (
    <div className="order-detail-page">
      <div className="order-detail-topbar">
        <button className="order-detail-back" onClick={() => navigate(-1)}><BackIcon /></button>
        <span className="order-detail-topbar-title">Order Detail</span>
        <span style={{ width: 36 }} />
      </div>

      <div className="order-detail-content">
        {/* Hero */}
        <div className="card card-elevated order-detail-hero">
          <p className="order-detail-fund-name">{order.fundName || order.fundID}</p>
          <p className="order-detail-fund-id font-mono text-muted">{order.fundID}</p>
          <div className="order-detail-amount">
            <AmountDisplay amount={order.amount} size="lg" />
          </div>
          {order.quantity && order.nav && (
            <p className="order-detail-nav-line text-muted">
              {formatQuantity(order.quantity)} units @ {formatCurrency(order.nav)}
            </p>
          )}
          <div className="order-detail-meta">
            <span
              className="order-detail-side"
              style={{ color: sideColors.text, background: sideColors.bg }}
            >
              {order.orderSide}
            </span>
            <span className="font-mono text-muted" style={{ fontSize: 'var(--text-sm)' }}>
              {order.accountID}
            </span>
          </div>
        </div>

        {/* Progress */}
        <div className="section">
          <div className="section-header">
            <span className="section-title">Order Progress</span>
          </div>
          <div className="card card-elevated">
            <ProgressTracker status={order.orderStatus} />
            {order.errorDescription && (
              <p className="order-detail-error">{order.errorDescription}</p>
            )}
          </div>
        </div>

        {/* Details */}
        <div className="section">
          <div className="section-header">
            <span className="section-title">Order Details</span>
          </div>
          <div className="card card-elevated">
            <DetailRow label="Order ID" value={order.orderID} mono />
            <DetailRow label="Fund ID" value={order.fundID} mono />
            <DetailRow label="Account" value={order.accountID} mono />
            <DetailRow label="Side" value={
              <span style={{ color: sideColors.text, fontWeight: 600 }}>{order.orderSide}</span>
            } />
            <DetailRow label="Amount" value={formatCurrency(order.amount)} mono />
            <DetailRow label="Quantity" value={order.quantity ? formatQuantity(order.quantity) : '—'} mono />
            <DetailRow label="NAV" value={order.nav ? formatCurrency(order.nav) : '—'} mono />
            <DetailRow label="Bulk ID" value={order.bulkOrderID || '—'} mono />
            <DetailRow label="Status" value={<StatusPill status={order.orderStatus} />} />
          </div>
        </div>
      </div>
    </div>
  )
}
