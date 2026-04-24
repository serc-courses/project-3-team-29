import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { getAdvisorClients } from '../../api/advisorApi'
import { saveBasketDraft, basketTotals } from '../../api/advisorApi'
import { getFunds } from '../../api/portfolioApi'
import { ADVISOR_CONFIG } from '../../constants/advisorConfig'
import { formatCurrency } from '../../utils/formatters'
import './AdvisorBasketOrder.css'

function BackIcon() {
  return (<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="15 18 9 12 15 6"/></svg>)
}

function newRow(accountID = '') {
  return { id: Date.now() + Math.random(), accountID, fundID: '', amount: '', orderSide: 'BUY' }
}

export default function AdvisorBasketOrder({ advisorId }) {
  const id = advisorId || ADVISOR_CONFIG.DEFAULT_ADVISOR_ID
  const navigate = useNavigate()

  const [clients, setClients] = useState([])
  const [funds, setFunds] = useState([])
  const [rows, setRows] = useState(() => {
    const preselect = localStorage.getItem(ADVISOR_CONFIG.STORAGE_KEYS.selectedClientId) || ''
    return [newRow(preselect)]
  })
  const [errors, setErrors] = useState({})

  useEffect(() => {
    getAdvisorClients(id).then(setClients).catch(() => {
      const ids = ADVISOR_CONFIG.ADVISOR_CLIENT_MAP[id] || []
      setClients(ids.map(accountID => ({ accountID })))
    })
    getFunds().then(setFunds).catch(() => {
      setFunds(Array.from({ length: 50 }, (_, i) => ({ fundID: `FND${String(i+1).padStart(3,'0')}`, fundName: `Fund ${i+1}` })))
    })
  }, [id])

  // Save draft on every change
  useEffect(() => {
    saveBasketDraft(rows.map(r => ({ accountID: r.accountID, fundID: r.fundID, amount: Number(r.amount), orderSide: r.orderSide })))
  }, [rows])

  function updateRow(rowId, field, value) {
    setRows(prev => prev.map(r => r.id === rowId ? { ...r, [field]: value } : r))
    setErrors(prev => { const n = { ...prev }; delete n[`${rowId}_${field}`]; return n })
  }

  function addRow() {
    setRows(prev => [...prev, newRow()])
  }

  function removeRow(rowId) {
    if (rows.length === 1) return
    setRows(prev => prev.filter(r => r.id !== rowId))
  }

  function validate() {
    const errs = {}
    rows.forEach(r => {
      if (!r.accountID) errs[`${r.id}_accountID`] = 'Required'
      if (!r.fundID) errs[`${r.id}_fundID`] = 'Required'
      if (!r.amount || Number(r.amount) <= 0) errs[`${r.id}_amount`] = 'Must be > 0'
    })
    return errs
  }

  function handleReview() {
    const errs = validate()
    if (Object.keys(errs).length > 0) { setErrors(errs); return }
    navigate('/advisor/orders/review')
  }

  const totals = basketTotals(rows.map(r => ({ ...r, amount: Number(r.amount) || 0 })))

  return (
    <div className="advisor-basket-page">
      <div className="order-detail-topbar">
        <button className="order-detail-back" onClick={() => navigate(-1)}><BackIcon /></button>
        <span className="order-detail-topbar-title">New Basket</span>
        <span style={{ width: 36 }} />
      </div>

      <div className="advisor-basket-form">
        <p className="advisor-basket-section-label">Basket Rows</p>

        {rows.map((row, idx) => (
          <div key={row.id} className="advisor-basket-row card card-elevated">
            <div className="advisor-basket-row-header">
              <span className="advisor-basket-row-num">Order {idx + 1}</span>
              {rows.length > 1 && (
                <button className="advisor-basket-remove" onClick={() => removeRow(row.id)}>
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                </button>
              )}
            </div>

            <div className="input-group" style={{ marginBottom: 12 }}>
              <label className="input-label">Client</label>
              <div className="select-wrapper-po">
                <select className={`select${errors[`${row.id}_accountID`] ? ' input-error' : ''}`} value={row.accountID} onChange={e => updateRow(row.id, 'accountID', e.target.value)}>
                  <option value="">Choose client...</option>
                  {clients.map(c => <option key={c.accountID} value={c.accountID}>{c.accountID}</option>)}
                </select>
                <span className="select-arrow-po"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="6 9 12 15 18 9"/></svg></span>
              </div>
              {errors[`${row.id}_accountID`] && <p className="input-error-text">{errors[`${row.id}_accountID`]}</p>}
            </div>

            <div className="input-group" style={{ marginBottom: 12 }}>
              <label className="input-label">Fund</label>
              <div className="select-wrapper-po">
                <select className={`select${errors[`${row.id}_fundID`] ? ' input-error' : ''}`} value={row.fundID} onChange={e => updateRow(row.id, 'fundID', e.target.value)}>
                  <option value="">Choose fund...</option>
                  {funds.map(f => <option key={f.fundID} value={f.fundID}>{f.fundName || f.fundID} ({f.fundID})</option>)}
                </select>
                <span className="select-arrow-po"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="6 9 12 15 18 9"/></svg></span>
              </div>
              {errors[`${row.id}_fundID`] && <p className="input-error-text">{errors[`${row.id}_fundID`]}</p>}
            </div>

            <div className="input-group" style={{ marginBottom: 12 }}>
              <label className="input-label">Amount (₹)</label>
              <div className="amount-input-wrap">
                <span className="amount-prefix">₹</span>
                <input type="number" inputMode="decimal" className={`input amount-input${errors[`${row.id}_amount`] ? ' input-error' : ''}`} placeholder="0.00" value={row.amount} min="1" step="0.01" onChange={e => updateRow(row.id, 'amount', e.target.value)} />
              </div>
              {errors[`${row.id}_amount`] && <p className="input-error-text">{errors[`${row.id}_amount`]}</p>}
            </div>

            <div className="side-toggle">
              <button type="button" className={`side-btn${row.orderSide === 'BUY' ? ' side-btn-buy active' : ''}`} aria-pressed={row.orderSide === 'BUY'} onClick={() => updateRow(row.id, 'orderSide', 'BUY')}>BUY</button>
              <button type="button" className={`side-btn${row.orderSide === 'SELL' ? ' side-btn-sell active' : ''}`} aria-pressed={row.orderSide === 'SELL'} onClick={() => updateRow(row.id, 'orderSide', 'SELL')}>SELL</button>
            </div>
          </div>
        ))}

        <button className="btn btn-outline btn-full advisor-add-row" onClick={addRow}>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          Add Another Row
        </button>

        {totals.count > 0 && (
          <div className="card advisor-basket-summary">
            <p className="advisor-basket-summary-title">Basket Summary</p>
            <div className="advisor-basket-summary-row"><span>Orders</span><span className="font-mono">{totals.count}</span></div>
            <div className="advisor-basket-summary-row"><span>Total amount</span><span className="font-mono">{formatCurrency(totals.total)}</span></div>
            <div className="advisor-basket-summary-row"><span>BUY / SELL</span><span className="font-mono">{totals.buyCount} / {totals.sellCount}</span></div>
            <div className="advisor-basket-summary-row"><span>Clients</span><span className="font-mono">{totals.clientCount}</span></div>
          </div>
        )}
      </div>

      <div className="place-order-cta">
        <button className="btn btn-primary btn-full btn-lg" onClick={handleReview}>Review Basket</button>
      </div>
    </div>
  )
}
