import { Link } from 'react-router-dom'
import { formatCurrency, formatCompact } from '../../utils/formatters'
import './FundCard.css'

function ChartIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="22 7 13.5 15.5 8.5 10.5 2 17"/>
      <polyline points="16 7 22 7 22 13"/>
    </svg>
  )
}

export default function FundCard({ fund, onClick }) {
  const content = (
    <div className="fund-card card card-interactive card-elevated">
      <div className="fund-card-header">
        <div className="fund-card-icon">
          <ChartIcon />
        </div>
        <div className="fund-card-title">
          <span className="fund-card-name">{fund.fundName}</span>
          <span className="fund-card-id font-mono">{fund.fundID}</span>
        </div>
      </div>
      <div className="fund-card-nav">
        <span className="fund-card-nav-label">NAV</span>
        <span className="fund-card-nav-value font-mono">{formatCurrency(fund.nav)}</span>
      </div>
      <div className="fund-card-stats">
        <div className="fund-card-stat">
          <span className="fund-card-stat-value font-mono">{fund.orderCount ?? 0}</span>
          <span className="fund-card-stat-label">orders</span>
        </div>
        <div className="fund-card-stat">
          <span className="fund-card-stat-value font-mono">
            {fund.totalAmount != null ? `₹${formatCompact(fund.totalAmount)}` : '—'}
          </span>
          <span className="fund-card-stat-label">total</span>
        </div>
        <div className="fund-card-stat fund-card-sides">
          <span className="fund-card-buy">BUY: {fund.orderSides?.BUY ?? 0}</span>
          <span className="fund-card-sell">SELL: {fund.orderSides?.SELL ?? 0}</span>
        </div>
      </div>
    </div>
  )

  if (onClick) {
    return <div onClick={onClick}>{content}</div>
  }

  return (
    <Link to={`/funds/${fund.fundID}`} className="fund-card-link">
      {content}
    </Link>
  )
}
