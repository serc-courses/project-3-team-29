import { useNavigate } from 'react-router-dom'
import { getTransactions } from '../../api/ordersApi'
import { useFetch } from '../../hooks/useFetch'
import { useAuth } from '../../context/AuthContext'
import EmptyState from '../../components/EmptyState/EmptyState'
import { formatCurrency } from '../../utils/formatters'
import '../Orders/Orders.css'

function NoTransactionsIcon() {
  return (
    <svg width="64" height="64" viewBox="0 0 64 64" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
      <rect x="8" y="16" width="48" height="32" rx="4" />
      <line x1="8" y1="24" x2="56" y2="24" />
      <line x1="32" y1="36" x2="48" y2="36" />
    </svg>
  )
}

export default function Transactions() {
  const navigate = useNavigate()
  const { user } = useAuth()
  const accountID = user?.accountID

  const { data: transactions, loading } = useFetch(
    () => getTransactions(accountID),
    [accountID]
  )

  if (loading) {
    return (
      <div className="page orders-page">
        <div className="skeleton skeleton-heading" style={{ width: '40%', marginBottom: 20 }} />
      </div>
    )
  }

  return (
    <div className="page orders-page">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
        <h1 className="page-title" style={{ margin: 0 }}>Ledger</h1>
        <button className="btn btn-ghost" onClick={() => navigate(-1)} style={{ padding: '4px 12px' }}>Back</button>
      </div>
      <p className="page-subtitle">{transactions?.length || 0} realized transactions</p>

      {(!transactions || transactions.length === 0) ? (
        <EmptyState
          icon={<NoTransactionsIcon />}
          title="No history yet"
          message="Your executed capital flows will appear here once booked."
        />
      ) : (
        <div className="orders-list">
          {transactions.map(t => {
            const isBuy = t.orderSide === 'BUY' || !t.orderSide
            const sign = isBuy ? '-' : '+'
            const color = isBuy ? 'var(--color-error)' : 'var(--color-success)'
            return (
              <div key={t.orderID} className="card card-elevated" style={{ padding: 'var(--sp-3)', display: 'flex', flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                  <div style={{ fontWeight: 600 }}>{t.fundName || t.fundID}</div>
                  <div className="font-mono text-muted" style={{ fontSize: '11px', marginTop: 4 }}>{t.tradeDate || 'N/A'}</div>
                  <div className="font-mono text-muted" style={{ fontSize: '10px' }}>Ref: {t.contractRef || t.orderID}</div>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <div className="font-mono" style={{ color: color, fontWeight: 700, fontSize: 'var(--text-md)' }}>
                    {sign}{formatCurrency(t.amount)}
                  </div>
                  {t.allocatedShares ? (
                    <div className="font-mono text-muted" style={{ fontSize: '11px', marginTop: 4 }}>
                      {parseFloat(t.allocatedShares).toFixed(4)} Units
                    </div>
                  ) : null}
                  {t.nav ? (
                    <div className="font-mono text-muted" style={{ fontSize: '10px' }}>
                      @ {formatCurrency(t.nav)}
                    </div>
                  ) : null}
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
