import { useState } from 'react'
import { Link } from 'react-router-dom'
import { cancelOrder } from '../../api/ordersApi'
import StatusPill from '../StatusPill/StatusPill'
import { formatCurrency } from '../../utils/formatters'
import { SIDE_COLORS } from '../../constants/statusColors'
import './OrderCard.css'

export default function OrderCard({ order, onClick }) {
  const sideColors = SIDE_COLORS[order.orderSide] || { text: '#64748B', bg: '#F1F5F9' }
  const [cancelling, setCancelling] = useState(false)
  const canCancel = ['PLANNED', 'VALIDATED', 'ENRICHED', 'PLACED'].includes(order.orderStatus)

  const handleCancel = async (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (!window.confirm("Are you sure you want to cancel this order?")) return;
    setCancelling(true);
    try {
        await cancelOrder(order.orderID);
    } catch(err) {
        alert("Failed to cancel order: " + (err.response?.data?.message || err.message));
    } finally {
        setCancelling(false);
    }
  }

  const content = (
    <div className="order-card card card-interactive card-elevated">
      <div className="order-card-top">
        <span className="order-card-fund">{order.fundName || order.fundID}</span>
        <span
          className="order-card-side"
          style={{ color: sideColors.text, background: sideColors.bg }}
        >
          {order.orderSide}
        </span>
      </div>
      <div className="order-card-mid">
        <span className="order-card-id font-mono">{order.orderID}</span>
        <span className="order-card-amount font-mono">{formatCurrency(order.amount)}</span>
      </div>
      <div className="order-card-bottom">
        <span className="order-card-account font-mono text-muted">{order.accountID}</span>
        <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
          {canCancel && (
            <button 
              className="btn btn-outline" 
              style={{ padding: '0 8px', fontSize: '11px', height: '24px', minHeight: '24px', borderColor: 'var(--color-error)', color: 'var(--color-error)' }}
              onClick={handleCancel}
              disabled={cancelling}
            >
              {cancelling ? '...' : 'Cancel'}
            </button>
          )}
          <StatusPill status={order.orderStatus} />
        </div>
      </div>
    </div>
  )

  if (onClick) {
    return <div onClick={onClick}>{content}</div>
  }

  return (
    <Link to={`/orders/${order.orderID}`} className="order-card-link">
      {content}
    </Link>
  )
}
