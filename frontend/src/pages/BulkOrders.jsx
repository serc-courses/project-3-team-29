import { useState, useEffect, useMemo, Fragment } from 'react'
import { Link } from 'react-router-dom'
import './BulkOrders.css'
import { getBulkOrderViews } from '../api/bulkOrdersApi'
import FilterBar from '../components/FilterBar/FilterBar'
import StatusBadge from '../components/StatusBadge/StatusBadge'
import { formatCurrency, formatQuantity } from '../utils/formatters'
import { BULK_ORDER_STATUS, ORDER_SIDE } from '../constants/orderStatus'

const STATUS_OPTIONS = Object.values(BULK_ORDER_STATUS).map((s) => ({ value: s, label: s }))
const SIDE_OPTIONS = Object.values(ORDER_SIDE).map((s) => ({ value: s, label: s }))

export default function BulkOrders() {
  const [bulkOrders, setBulkOrders] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [statusFilter, setStatusFilter] = useState('')
  const [sideFilter, setSideFilter] = useState('')
  const [search, setSearch] = useState('')
  const [expanded, setExpanded] = useState({})

  useEffect(() => {
    getBulkOrderViews()
      .then(setBulkOrders)
      .catch((err) => setError(err.message || 'Failed to load bulk orders'))
      .finally(() => setLoading(false))
  }, [])

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
      <div className="bulk-orders-count">
        Showing {filtered.length} of {bulkOrders.length} bulk orders
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
              </tr>
            </thead>
            <tbody>
              {filtered.length === 0 ? (
                <tr>
                  <td colSpan={10} className="data-table-empty">No bulk orders found</td>
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
                    </tr>
                    {expanded[b.bulkOrderID] && (
                      <tr className="matched-orders-row">
                        <td colSpan={10}>
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
    </div>
  )
}
