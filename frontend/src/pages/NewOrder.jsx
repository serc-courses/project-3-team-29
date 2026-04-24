import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import './NewOrder.css'
import { planOrders } from '../api/ordersApi'
import { getFundAggregates, getAccountAggregates } from '../api/aggregatesApi'
import { getFunds } from '../api/fundApi'
import {getUsers} from '../api/usersApi'
import { formatCurrency } from '../utils/formatters'
import { ORDER_SIDE } from '../constants/orderStatus'
import { ROUTES } from '../constants/routes'

const SIDE_OPTIONS = Object.values(ORDER_SIDE)

const FALLBACK_ACCOUNTS = Array.from({ length: 10 }, (_, i) =>
  `ACCT${String(i + 1).padStart(5, '0')}`
)

const FALLBACK_FUNDS = Array.from({ length: 50 }, (_, i) => ({
  fundID: `FND${String(i + 1).padStart(3, '0')}`,
  fundName: `Fund ${String(i + 1).padStart(3, '0')}`,
}))

function newOrderRow() {
  return { id: Date.now(), productID: '', accountID: '', amount: '', orderSide: 'BUY', errors: {} }
}

function validateRow(row) {
  const errors = {}
  if (!row.productID) errors.productID = 'Fund is required'
  if (!row.accountID) errors.accountID = 'Account is required'
  if (!row.amount || Number(row.amount) <= 0) errors.amount = 'Amount must be > 0'
  if (!row.orderSide) errors.orderSide = 'Side is required'
  return errors
}

export default function NewOrder() {
  const [rows, setRows] = useState([newOrderRow()])
  const [funds, setFunds] = useState([])
  const [accounts, setAccounts] = useState([])
  const [submitting, setSubmitting] = useState(false)
  const [banner, setBanner] = useState(null)
  const navigate = useNavigate()

  useEffect(() => {
    getFunds()
      .then((data) => setFunds(data.length > 0 ? data : FALLBACK_FUNDS))
      .catch(() => setFunds(FALLBACK_FUNDS))

    getUsers()
      .then((data) =>
        setAccounts(
          data.length > 0 ? data.map((a) => a.accountID) : FALLBACK_ACCOUNTS
        )
      )
      .catch(() => setAccounts(FALLBACK_ACCOUNTS))
  }, [])

  const updateRow = (id, field, value) => {
    setRows((prev) =>
      prev.map((r) =>
        r.id === id
          ? { ...r, [field]: value, errors: { ...r.errors, [field]: undefined } }
          : r
      )
    )
  }

  const addRow = () => setRows((prev) => [...prev, newOrderRow()])

  const removeRow = (id) => setRows((prev) => prev.filter((r) => r.id !== id))

  const totalAmount = rows.reduce((sum, r) => sum + (Number(r.amount) || 0), 0)

  const handleSubmit = async () => {
    const validated = rows.map((r) => ({ ...r, errors: validateRow(r) }))
    setRows(validated)
    const hasErrors = validated.some((r) => Object.keys(r.errors).length > 0)
    if (hasErrors) return

    setSubmitting(true)
    setBanner(null)
    try {
      const payload = rows.map(({ productID, amount, accountID, orderSide }) => ({
        productID,
        amount: Number(amount),
        accountID,
        orderSide,
      }))
      const result = await planOrders(payload)
      setBanner({
        type: 'success',
        message: `${result.count} order(s) planned. IDs: ${result.orderIDs?.join(', ')}`,
      })
      setTimeout(() => navigate(ROUTES.ORDERS), 2000)
    } catch (err) {
      setBanner({ type: 'error', message: err.message || 'Failed to submit orders' })
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="new-order-page">
      <h2 className="new-order-title">Plan New Orders</h2>

      {banner && (
        <div className={banner.type === 'success' ? 'success-banner' : 'error-banner'}>
          {banner.message}
        </div>
      )}

      <div className="order-rows">
        {rows.map((row, idx) => (
          <div key={row.id} className="order-row">
            <div className="order-row-header">
              <span className="order-row-number">Order {idx + 1}</span>
              <button
                className="remove-btn"
                onClick={() => removeRow(row.id)}
                disabled={rows.length === 1}
              >
                Remove
              </button>
            </div>
            <div className="order-row-fields">
              <div>
                <label className="form-label">Fund</label>
                <select
                  className="form-select"
                  value={row.productID}
                  onChange={(e) => updateRow(row.id, 'productID', e.target.value)}
                >
                  <option value="">Select fund...</option>
                  {funds.map((f) => (
                    <option key={f.fundID} value={f.fundID}>
                      {f.fundID} — {f.fundName}
                    </option>
                  ))}
                </select>
                {row.errors.productID && <div className="field-error">{row.errors.productID}</div>}
              </div>
              <div>
                <label className="form-label">Account</label>
                <select
                  className="form-select"
                  value={row.accountID}
                  onChange={(e) => updateRow(row.id, 'accountID', e.target.value)}
                >
                  <option value="">Select account...</option>
                  {accounts.map((id) => (
                    <option key={id} value={id}>{id}</option>
                  ))}
                </select>
                {row.errors.accountID && <div className="field-error">{row.errors.accountID}</div>}
              </div>
              <div>
                <label className="form-label">Amount (₹)</label>
                <input
                  className="form-input"
                  type="number"
                  min="1"
                  step="100"
                  placeholder="1000"
                  value={row.amount}
                  onChange={(e) => updateRow(row.id, 'amount', e.target.value)}
                />
                {row.errors.amount && <div className="field-error">{row.errors.amount}</div>}
              </div>
              <div>
                <label className="form-label">Side</label>
                <select
                  className="form-select"
                  value={row.orderSide}
                  onChange={(e) => updateRow(row.id, 'orderSide', e.target.value)}
                >
                  {SIDE_OPTIONS.map((s) => (
                    <option key={s} value={s}>{s}</option>
                  ))}
                </select>
              </div>
            </div>
          </div>
        ))}
      </div>

      <button className="add-order-btn" onClick={addRow}>
        + Add Another Order
      </button>

      <div className="new-order-footer">
        <div className="new-order-summary">
          <strong>{rows.length}</strong> order(s) &nbsp;·&nbsp; Total: <strong>{formatCurrency(totalAmount)}</strong>
        </div>
        <div className="new-order-actions">
          <button className="btn btn-outline" onClick={() => navigate(ROUTES.ORDERS)}>
            Cancel
          </button>
          <button
            className="btn btn-primary"
            onClick={handleSubmit}
            disabled={submitting}
          >
            {submitting ? <span className="spinner" style={{ width: 14, height: 14, borderWidth: 2 }} /> : null}
            Submit Orders
          </button>
        </div>
      </div>
    </div>
  )
}
