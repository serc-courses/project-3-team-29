import { useState, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { planAdvisorBasket, getBasketDraft, clearBasketDraft, groupBasketByClient, basketTotals } from '../../api/advisorApi'
import Toast from '../../components/Toast/Toast'
import Sheet from '../../components/Sheet/Sheet'
import { formatCurrency } from '../../utils/formatters'
import { SIDE_COLORS } from '../../constants/statusColors'
import './AdvisorBasketReview.css'

function BackIcon() {
  return (<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="15 18 9 12 15 6"/></svg>)
}

export default function AdvisorBasketReview() {
  const navigate = useNavigate()
  const rows = getBasketDraft().filter(r => r.accountID && r.fundID && r.amount > 0)
  const grouped = groupBasketByClient(rows)
  const totals = basketTotals(rows)

  const [loading, setLoading] = useState(false)
  const [toast, setToast] = useState(null)
  const [successSheet, setSuccessSheet] = useState(null)

  async function handleSubmit() {
    if (rows.length === 0) return
    setLoading(true)
    try {
      const payload = rows.map(r => ({ productID: r.fundID, amount: r.amount, accountID: r.accountID, orderSide: r.orderSide }))
      const result = await planAdvisorBasket(payload)
      clearBasketDraft()
      setSuccessSheet({ orderIDs: result.orderIDs || [] })
    } catch (err) {
      setToast({ message: err.message || 'Submission failed', type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  if (rows.length === 0) {
    return (
      <div className="advisor-review-page">
        <div className="order-detail-topbar">
          <button className="order-detail-back" onClick={() => navigate(-1)}><BackIcon /></button>
          <span className="order-detail-topbar-title">Review Basket</span>
          <span style={{ width: 36 }} />
        </div>
        <div className="empty-state">
          <p className="empty-state-title">No basket rows</p>
          <button className="btn btn-primary" onClick={() => navigate('/advisor/orders/new')}>Build Basket</button>
        </div>
      </div>
    )
  }

  return (
    <div className="advisor-review-page">
      <div className="order-detail-topbar">
        <button className="order-detail-back" onClick={() => navigate(-1)}><BackIcon /></button>
        <span className="order-detail-topbar-title">Review Basket</span>
        <button className="btn btn-ghost" style={{ fontSize: 'var(--text-sm)' }} onClick={() => navigate('/advisor/orders/new')}>Edit</button>
      </div>

      {toast && <Toast message={toast.message} type={toast.type} onClose={() => setToast(null)} />}

      <div className="advisor-review-content">
        {/* Summary */}
        <div className="card card-elevated advisor-review-summary">
          <div className="advisor-review-summary-row"><span>{totals.count} orders</span><span>&middot;</span><span>{totals.clientCount} clients</span></div>
          <p className="advisor-review-total font-mono">{formatCurrency(totals.total)}</p>
          <p style={{ fontSize: 'var(--text-xs)', color: 'var(--color-text-500)' }}>BUY: {totals.buyCount} &middot; SELL: {totals.sellCount}</p>
        </div>

        {/* Grouped rows */}
        {Object.entries(grouped).map(([accountID, clientRows]) => (
          <div key={accountID} className="section">
            <div className="section-header">
              <span className="font-mono" style={{ fontWeight: 700, color: 'var(--color-text-900)', fontSize: 'var(--text-base)' }}>{accountID}</span>
              <span style={{ fontSize: 'var(--text-xs)', color: 'var(--color-text-500)' }}>{clientRows.length} order{clientRows.length > 1 ? 's' : ''}</span>
            </div>
            <div className="card card-elevated" style={{ padding: 0, overflow: 'hidden' }}>
              {clientRows.map((r, i) => {
                const sc = SIDE_COLORS[r.orderSide] || {}
                return (
                  <div key={i} className="advisor-review-order-row" style={{ borderTop: i > 0 ? '1px solid var(--color-border-light)' : 'none' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      <span className="font-mono" style={{ fontSize: 'var(--text-sm)', color: 'var(--color-text-700)' }}>{r.fundID}</span>
                      <span style={{ fontSize: 'var(--text-xs)', fontWeight: 700, color: sc.text, background: sc.bg, padding: '2px 6px', borderRadius: 9999 }}>{r.orderSide}</span>
                    </div>
                    <span className="font-mono" style={{ fontSize: 'var(--text-sm)', fontWeight: 600 }}>{formatCurrency(r.amount)}</span>
                  </div>
                )
              })}
            </div>
          </div>
        ))}
      </div>

      <div className="place-order-cta">
        <button className="btn btn-primary btn-full btn-lg" onClick={handleSubmit} disabled={loading}>
          {loading ? <><span className="spinner" /> Submitting...</> : 'Submit Basket'}
        </button>
      </div>

      {/* Success sheet */}
      <Sheet isOpen={!!successSheet} onClose={() => { setSuccessSheet(null); navigate('/advisor/activity') }} title="Basket Submitted">
        <div className="success-content">
          <div className="success-check">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
          </div>
          <h2 className="success-title">Basket Submitted!</h2>
          <div style={{ width: '100%', maxHeight: 160, overflowY: 'auto' }}>
            {successSheet?.orderIDs?.map(id => (
              <p key={id} className="font-mono" style={{ fontSize: 'var(--text-sm)', color: 'var(--color-text-500)', padding: '4px 0', borderBottom: '1px solid var(--color-border-light)' }}>{id}</p>
            ))}
          </div>
          <div style={{ display: 'flex', gap: 12, width: '100%', marginTop: 8 }}>
            <button className="btn btn-outline" style={{ flex: 1 }} onClick={() => { setSuccessSheet(null); navigate('/advisor/orders/new') }}>New Basket</button>
            <button className="btn btn-primary" style={{ flex: 1 }} onClick={() => { setSuccessSheet(null); navigate('/advisor/activity') }}>View Activity</button>
          </div>
        </div>
      </Sheet>
    </div>
  )
}
