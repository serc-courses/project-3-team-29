import { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { planOrders } from '../../api/ordersApi'
import { getFunds, getPortfolio } from '../../api/portfolioApi'
import { useAuth } from '../../context/AuthContext'
import Sheet from '../../components/Sheet/Sheet'
import Toast from '../../components/Toast/Toast'
import { formatCurrency } from '../../utils/formatters'
import './PlaceOrder.css'

function BackIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="15 18 9 12 15 6" />
    </svg>
  )
}

export default function PlaceOrder() {
  const navigate = useNavigate()
  const redirectTimerRef = useRef(null)
  const { user } = useAuth()
  const accountID = user?.accountID

  const [funds, setFunds] = useState([])
  const [fundID, setFundID] = useState('')
  const [amount, setAmount] = useState('')
  const [side, setSide] = useState('BUY')
  const [portfolio, setPortfolio] = useState(null)
  const [errors, setErrors] = useState({})
  const [loading, setLoading] = useState(false)
  const [toast, setToast] = useState(null)
  const [successSheet, setSuccessSheet] = useState(null)

  useEffect(() => {
    getFunds()
      .then(data => setFunds(data && data.length > 0 ? data : generateSeedFunds()))
      .catch(() => setFunds(generateSeedFunds()))

    if (accountID) {
      getPortfolio(accountID)
        .then(data => setPortfolio(data))
        .catch(console.error)
    }
  }, [accountID])

  useEffect(() => {
    return () => {
      if (redirectTimerRef.current) clearTimeout(redirectTimerRef.current)
    }
  }, [])

  function generateSeedFunds() {
    return Array.from({ length: 50 }, (_, i) => ({
      fundID: `FND${String(i + 1).padStart(3, '0')}`,
      fundName: `Fund ${i + 1}`,
    }))
  }

  function validate() {
    const errs = {}
    if (!fundID) errs.fundID = 'Please select a fund'
    if (!amount || Number(amount) <= 0) errs.amount = 'Amount must be greater than 0'

    if (side === 'SELL' && fundID && amount) {
      const holding = portfolio?.holdings?.find(h => h.fundID === fundID)
      if (!holding || holding.allocatedShares <= 0) {
        errs.fundID = 'You do not own any shares of this fund to sell'
      } else {
        const maxValue = holding.currentValue
        // Allow tiny buffer for decimal rounding
        if (Number(amount) > Number(maxValue) * 1.01) {
          errs.amount = `Insufficient holdings. Max sell value: ₹${formatCurrency(maxValue)}`
        }
      }
    }

    return errs
  }

  async function handleSubmit(e) {
    e.preventDefault()
    const errs = validate()
    if (Object.keys(errs).length > 0) {
      setErrors(errs)
      return
    }
    setLoading(true)
    try {
      const result = await planOrders([{
        productID: fundID,
        amount: Number(amount),
        accountID,
        orderSide: side,
      }])
      const orderID = result.orderIDs?.[0]
      setSuccessSheet({ orderID })
      redirectTimerRef.current = setTimeout(() => {
        navigate('/orders')
      }, 3000)
    } catch (err) {
      setToast({ message: err.message || 'Failed to place order', type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  const allFilled = fundID && amount && Number(amount) > 0
  const selectedFundName = funds.find(f => f.fundID === fundID)?.fundName

  return (
    <div className="place-order-page">
      <div className="place-order-topbar">
        <button className="place-order-back" onClick={() => navigate(-1)}><BackIcon /></button>
        <span className="place-order-topbar-title">Place Order</span>
        <span style={{ width: 36 }} />
      </div>

      {toast && (
        <Toast message={toast.message} type={toast.type} onClose={() => setToast(null)} />
      )}

      <form className="place-order-form" onSubmit={handleSubmit}>
        <div className="input-group">
          <label className="input-label" htmlFor="fund-select">Select Fund</label>
          <div className="select-wrapper-po">
            <select
              id="fund-select"
              className={`select${errors.fundID ? ' input-error' : ''}`}
              value={fundID}
              onChange={e => { setFundID(e.target.value); setErrors(p => ({ ...p, fundID: '' })) }}
            >
              <option value="">Choose a fund...</option>
              {funds.map(f => (
                <option key={f.fundID} value={f.fundID}>
                  {f.fundName} ({f.fundID})
                </option>
              ))}
            </select>
            <span className="select-arrow-po">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="6 9 12 15 18 9" />
              </svg>
            </span>
          </div>
          {errors.fundID && <p className="input-error-text">{errors.fundID}</p>}
        </div>

        <div className="input-group">
          <label className="input-label" htmlFor="amount-input">Amount (₹)</label>
          <div className="amount-input-wrap">
            <span className="amount-prefix">₹</span>
            <input
              id="amount-input"
              type="number"
              inputMode="decimal"
              className={`input amount-input${errors.amount ? ' input-error' : ''}`}
              placeholder="0.00"
              value={amount}
              min="1"
              step="100"
              onChange={e => { setAmount(e.target.value); setErrors(p => ({ ...p, amount: '' })) }}
            />
          </div>
          {errors.amount && <p className="input-error-text">{errors.amount}</p>}
          {side === 'SELL' && fundID && portfolio?.holdings?.find(h => h.fundID === fundID) && (
            <p style={{ fontSize: '12px', color: '#64748B', marginTop: 4 }}>
              Available to sell: ~₹{formatCurrency(portfolio.holdings.find(h => h.fundID === fundID).currentValue)}
            </p>
          )}
        </div>

        <div className="input-group">
          <label className="input-label">Order Side</label>
          <div className="side-toggle">
            <button
              type="button"
              className={`side-btn${side === 'BUY' ? ' side-btn-buy active' : ''}`}
              aria-pressed={side === 'BUY'}
              onClick={() => setSide('BUY')}
            >BUY</button>
            <button
              type="button"
              className={`side-btn${side === 'SELL' ? ' side-btn-sell active' : ''}`}
              aria-pressed={side === 'SELL'}
              onClick={() => setSide('SELL')}
            >SELL</button>
          </div>
        </div>

        {allFilled && (
          <>
            <div className="divider" />
            <div className="order-summary card">
              <p className="order-summary-title">Order Summary</p>
              <div className="order-summary-row">
                <span>Fund</span><span>{selectedFundName || fundID}</span>
              </div>
              <div className="order-summary-row">
                <span>Account</span><span className="font-mono">{accountID}</span>
              </div>
              <div className="order-summary-row">
                <span>Amount</span><span className="font-mono">{formatCurrency(Number(amount))}</span>
              </div>
              <div className="order-summary-row">
                <span>Side</span>
                <span style={{ color: side === 'BUY' ? '#059669' : '#DC2626', fontWeight: 600 }}>{side}</span>
              </div>
            </div>
          </>
        )}

        <div className="place-order-cta">
          <button
            type="submit"
            className="btn btn-primary btn-full btn-lg"
            disabled={loading || !allFilled}
          >
            {loading ? (
              <><span className="spinner" /> Placing order...</>
            ) : (
              'Place Order'
            )}
          </button>
        </div>
      </form>

      {/* Success sheet */}
      <Sheet isOpen={!!successSheet} onClose={() => { setSuccessSheet(null); navigate('/orders') }} title="">
        <div className="success-content">
          <div className="success-check">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
              <polyline points="20 6 9 17 4 12" />
            </svg>
          </div>
          <h2 className="success-title">Order Placed!</h2>
          {successSheet?.orderID && (
            <p className="success-order-id">
              Order ID: <span className="font-mono">{successSheet.orderID}</span>
            </p>
          )}
          <button
            className="btn btn-primary btn-full"
            onClick={() => {
              if (redirectTimerRef.current) clearTimeout(redirectTimerRef.current)
              navigate(`/orders/${successSheet?.orderID}`)
            }}
          >
            View Order
          </button>
          <p className="success-redirect">Redirecting to orders in 3s...</p>
        </div>
      </Sheet>
    </div>
  )
}
