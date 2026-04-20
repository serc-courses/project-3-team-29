import { Link } from 'react-router-dom'
import StatusPill from '../StatusPill/StatusPill'
import { formatCurrency } from '../../utils/formatters'
import { SIDE_COLORS } from '../../constants/statusColors'
import './OrderCard.css'

export default function OrderCard({ order, onClick }) {
  const sideColors = SIDE_COLORS[order.orderSide] || { text: '#64748B', bg: '#F1F5F9' }

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
        <StatusPill status={order.orderStatus} />
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
