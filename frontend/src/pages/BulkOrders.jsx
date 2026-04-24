import { useState, useEffect, useMemo, Fragment } from 'react'
import { Link } from 'react-router-dom'
import './BulkOrders.css'
import { getBulkOrderViews } from '../api/bulkOrdersApi'
import FilterBar from '../components/FilterBar/FilterBar'
import StatusBadge from '../components/StatusBadge/StatusBadge'
import { formatCurrency, formatQuantity } from '../utils/formatters'
import { BULK_ORDER_STATUS, ORDER_SIDE } from '../constants/orderStatus'
import { CONFIG } from '../constants/config'
import { exportToCsv } from '../utils/exportCsv'

const STATUS_OPTIONS = Object.values(BULK_ORDER_STATUS).map((s) => ({ value: s, label: s }))
const SIDE_OPTIONS = Object.values(ORDER_SIDE).map((s) => ({ value: s, label: s }))

function ContractModal({ bulkOrder, onClose, onSuccess }) {
  const defaultNav = 10.0;
  const defaultShares = bulkOrder.amount ? (bulkOrder.amount / defaultNav).toFixed(4) : '';

  const [nav, setNav] = useState(defaultNav.toString())
  const [totalShares, setTotalShares] = useState(defaultShares.toString())
  const [contractRef, setContractRef] = useState(`CTR-${Date.now()}`)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState(null)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      const res = await fetch(`${CONFIG.API_BASE_URL}/transfer-agent/contract`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          bulkOrderId: bulkOrder.bulkOrderID,
          nav: parseFloat(nav),
          totalShares: parseFloat(totalShares),
          contractRef,
        }),
      })
      const data = await res.json()
      if (!res.ok) throw new Error(data.message || 'Contract submission failed')
      onSuccess(data)
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" onClick={(e) => e.stopPropagation()}>
        <h3>Simulate TA Contract Callback</h3>
        <p style={{ fontSize: '0.85rem', color: '#666' }}>Bulk ID: <strong>{bulkOrder.bulkOrderID}</strong></p>
        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          <label>
            NAV (per unit)
            <input
              type="number"
              step="0.0001"
              required
              value={nav}
              onChange={(e) => setNav(e.target.value)}
              placeholder="e.g. 47.23"
              style={{ display: 'block', width: '100%', marginTop: 4 }}
            />
          </label>
          <label>
            Total Allocated Shares
            <input
              type="number"
              step="0.0001"
              required
              value={totalShares}
              onChange={(e) => setTotalShares(e.target.value)}
              placeholder="e.g. 4447.28"
              style={{ display: 'block', width: '100%', marginTop: 4 }}
            />
          </label>
          <label>
            Contract Reference
            <input
              type="text"
              required
              value={contractRef}
              onChange={(e) => setContractRef(e.target.value)}
              style={{ display: 'block', width: '100%', marginTop: 4 }}
            />
          </label>
          {error && <div style={{ color: 'red', fontSize: '0.85rem' }}>{error}</div>}
          <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
            <button type="button" onClick={onClose} disabled={submitting}>Cancel</button>
            <button type="submit" disabled={submitting} className="btn-primary">
              {submitting ? 'Processing…' : 'Submit Contract'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default function BulkOrders({ sseEventCount = 0 }) {
  const [bulkOrders, setBulkOrders] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [statusFilter, setStatusFilter] = useState('')
  const [sideFilter, setSideFilter] = useState('')
  const [search, setSearch] = useState('')
  const [expanded, setExpanded] = useState({})
  const [contractModal, setContractModal] = useState(null)
  const [contractSuccess, setContractSuccess] = useState(null)

  useEffect(() => {
    // Only show full-screen loader on first load; use silent refresh on SSE events
    if (sseEventCount === 0) setLoading(true)
    getBulkOrderViews()
      .then(setBulkOrders)
      .catch((err) => setError(err.message || 'Failed to load bulk orders'))
      .finally(() => setLoading(false))
  }, [sseEventCount])

  const filtered = useMemo(() => {
    return bulkOrders.filter((b) => {
      if (statusFilter && b.bulkOrderStatus !== statusFilter) return false
      if (sideFilter && b.orderSide !== sideFilter) return false
      if (search && !b.bulkOrderID?.toLowerCase().includes(search.toLowerCase())) return false
      return true
    })
  }, [bulkOrders, statusFilter, sideFilter, search])

  const toggleExpand = (id) => {
    setExpanded((prev) => ({ ...prev, [id]: !prev[id] }))
  }

  if (loading) {
    return (
      <div className="bulk-orders-loading">
        <span className="spinner" />
        Loading bulk orders...
      </div>
    )
  }

  if (error) return <div className="bulk-orders-error">{error}</div>

  const filters = [
    { key: 'status', label: 'Status', options: STATUS_OPTIONS, value: statusFilter, onChange: setStatusFilter },
    { key: 'side', label: 'Side', options: SIDE_OPTIONS, value: sideFilter, onChange: setSideFilter },
  ]

  return (
    <div className="bulk-orders-page">
      <FilterBar
        filters={filters}
        searchPlaceholder="Search by Bulk ID..."
        searchValue={search}
        onSearchChange={setSearch}
      />
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
        <div className="bulk-orders-count" style={{ margin: 0 }}>
          Showing {filtered.length} of {bulkOrders.length} bulk orders
        </div>
        <button 
          className="recon-refresh-btn" 
          onClick={() => exportToCsv('bulk_orders.csv', filtered)} 
          disabled={filtered.length === 0}
          style={{ padding: '6px 12px', fontSize: '12px', background: 'white', border: '1px solid #ccc', borderRadius: '4px', cursor: filtered.length ? 'pointer' : 'not-allowed', opacity: filtered.length ? 1 : 0.6 }}
        >
          ⤓ Export CSV
        </button>
      </div>
      <div className="card" style={{ padding: 0 }}>
        <div style={{ overflowX: 'auto' }}>
          <table className="data-table">
            <thead>
              <tr>
                <th></th>
                <th>Bulk ID</th>
                <th>Fund</th>
                <th>Fund ID</th>
                <th style={{ textAlign: 'center' }}>Side</th>
                <th style={{ textAlign: 'right' }}>Total Amount</th>
                <th style={{ textAlign: 'right' }}>Total Qty</th>
                <th style={{ textAlign: 'right' }}>NAV</th>
                <th style={{ textAlign: 'center' }}>Status</th>
                <th style={{ textAlign: 'center' }}>Orders</th>
                <th style={{ textAlign: 'center' }}>Action</th>
              </tr>
            </thead>
            <tbody>
              {filtered.length === 0 ? (
                <tr>
                  <td colSpan={11} className="data-table-empty">No bulk orders found</td>
                </tr>
              ) : (
                filtered.map((b) => (
                  <Fragment key={b.bulkOrderID}>
                    <tr className="clickable" onClick={() => toggleExpand(b.bulkOrderID)}>
                      <td style={{ width: 32, textAlign: 'center' }}>
                        <button className="expand-btn">{expanded[b.bulkOrderID] ? '▼' : '▶'}</button>
                      </td>
                      <td><span className="font-mono text-sm">{b.bulkOrderID}</span></td>
                      <td>{b.fundName}</td>
                      <td><span className="font-mono text-sm">{b.fundID}</span></td>
                      <td style={{ textAlign: 'center' }}>
                        <span className={b.orderSide === 'BUY' ? 'side-buy' : 'side-sell'}>{b.orderSide}</span>
                      </td>
                      <td style={{ textAlign: 'right' }}>{formatCurrency(b.totalAmount)}</td>
                      <td style={{ textAlign: 'right' }}>{formatQuantity(b.totalQuantity)}</td>
                      <td style={{ textAlign: 'right' }}>{formatCurrency(b.nav)}</td>
                      <td style={{ textAlign: 'center' }}>
                        <StatusBadge status={b.bulkOrderStatus} variant="bulk" />
                      </td>
                      <td style={{ textAlign: 'center' }}>{b.matchedOrderCount ?? 0}</td>
                      <td style={{ textAlign: 'center' }}>
                        {(b.bulkOrderStatus === 'TRANSMITTED' || b.bulkOrderStatus === 'CONFIRMED') && (
                          <button
                            className="btn-simulate-ta"
                            onClick={(e) => { e.stopPropagation(); setContractModal(b) }}
                          >
                            Simulate TA Contract
                          </button>
                        )}
                      </td>
                    </tr>
                    {expanded[b.bulkOrderID] && (
                      <tr className="matched-orders-row">
                        <td colSpan={11}>
                          <div className="matched-orders-list">
                            <span>Matched Orders:</span>
                            {(b.matchedOrderIDs || []).map((id) => (
                              <Link
                                key={id}
                                to={`/orders?bulkOrderID=${id}`}
                                className="matched-order-id"
                                onClick={(e) => e.stopPropagation()}
                              >
                                {id}
                              </Link>
                            ))}
                          </div>
                        </td>
                      </tr>
                    )}
                  </Fragment>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
      {contractModal && (
        <ContractModal
          bulkOrder={contractModal}
          onClose={() => setContractModal(null)}
          onSuccess={(data) => {
            setContractModal(null)
            setContractSuccess(`Contract processed! ${data.bookedOrders} orders booked (ref: ${data.contractRef})`)
            getBulkOrderViews().then(setBulkOrders).catch(() => { })
          }}
        />
      )}
      {contractSuccess && (
        <div className="contract-success-toast" onClick={() => setContractSuccess(null)}>
          ✓ {contractSuccess}
        </div>
      )}
    </div>
  )
}
