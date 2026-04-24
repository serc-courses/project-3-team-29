import { useState, useEffect, useMemo } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import './Orders.css'
import { getOrderViews } from '../api/ordersApi'
import DataTable from '../components/DataTable/DataTable'
import StatusBadge from '../components/StatusBadge/StatusBadge'
import FilterBar from '../components/FilterBar/FilterBar'
import { formatCurrency, formatQuantity } from '../utils/formatters'
import { ORDER_STATUS, ORDER_SIDE } from '../constants/orderStatus'
import { ROUTES } from '../constants/routes'

const STATUS_OPTIONS = Object.values(ORDER_STATUS).map((s) => ({ value: s, label: s }))
const SIDE_OPTIONS = Object.values(ORDER_SIDE).map((s) => ({ value: s, label: s }))

const COLUMNS = [
  { key: 'orderID', label: 'Order ID', render: (v) => <span className="font-mono text-sm">{v}</span> },
  { key: 'accountID', label: 'Account', render: (v) => <span className="font-mono text-sm">{v}</span> },
  { key: 'fundName', label: 'Fund', render: (v) => <span className="fund-name-cell" title={v}>{v}</span> },
  {
    key: 'orderSide', label: 'Side', align: 'center',
    render: (v) => <span className={v === 'BUY' ? 'side-buy' : 'side-sell'}>{v}</span>,
  },
  { key: 'amount', label: 'Amount', align: 'right', render: (v) => formatCurrency(v) },
  {
    key: 'qty_display', label: 'Qty', align: 'right',
    sortable: false,
    render: (_, row) => {
      const qty = row.allocatedShares ?? row.quantity
      return qty ? formatQuantity(qty) : '—'
    },
  },
  { key: 'nav', label: 'NAV', align: 'right', render: (v) => v ? formatCurrency(v) : '—' },
  { key: 'orderStatus', label: 'Status', align: 'center', render: (v) => <StatusBadge status={v} /> },
]

const toOrderTime = (order) => {
  if ((order?.createdAt ?? 0) > 0) return order.createdAt
  if (order?.tradeDate) {
    const t = Date.parse(`${order.tradeDate}T00:00:00Z`)
    if (!Number.isNaN(t)) return t
  }
  return 0
}

function Field({ label, value, mono }) {
  return (
    <div className="drawer-field">
      <span className="drawer-field-label">{label}</span>
      <span className={`drawer-field-value${mono ? ' mono' : ''}${!value || value === '—' ? ' muted' : ''}`}>
        {value ?? '—'}
      </span>
    </div>
  )
}

function OrderDrawer({ order, onClose }) {
  if (!order) return null
  return (
    <>
      <div className="order-drawer-overlay" onClick={onClose} />
      <div className="order-drawer">
        <div className="order-drawer-header">
          <span className="order-drawer-title">Order Detail</span>
          <button className="order-drawer-close" onClick={onClose} aria-label="Close">✕</button>
        </div>
        <div className="order-drawer-body">
          <div className="drawer-fund-hero">
            <p className="drawer-fund-name">{order.fundName || order.fundID}</p>
            <p className="drawer-amount">{formatCurrency(order.amount)}</p>
            <div className="drawer-badges">
              <StatusBadge status={order.orderStatus} />
              <span className={order.orderSide === 'BUY' ? 'side-buy' : 'side-sell'} style={{ fontWeight: 600, fontSize: '0.82rem' }}>
                {order.orderSide}
              </span>
            </div>
          </div>

          <div className="drawer-section-label">Order Info</div>
          <Field label="Order ID"   value={order.orderID}   mono />
          <Field label="Account"    value={order.accountID} mono />
          <Field label="Bulk ID"    value={order.bulkOrderID || '—'} mono />
          <Field label="Status"     value={order.orderStatus} />

          <div className="drawer-section-label">Fund Details</div>
          <Field label="Fund"          value={order.fundName || order.fundID} />
          <Field label="Fund Family"   value={order.fundFamily || '—'} />
          <Field label="Transfer Agent" value={order.transferAgent || '—'} />
          <Field label="NAV"           value={order.nav ? formatCurrency(order.nav) : '—'} mono />

          <div className="drawer-section-label">Financials</div>
          <Field label="Amount"       value={formatCurrency(order.amount)} mono />
          <Field label="Expected Qty" value={order.quantity ? formatQuantity(order.quantity) : '—'} mono />
          <Field label="Allocated Shares" value={order.allocatedShares ? formatQuantity(order.allocatedShares) : '—'} mono />
          <Field label="Contract Ref" value={order.contractRef || '—'} mono />

          <div className="drawer-section-label">Dates</div>
          <Field label="Trade Date"      value={order.tradeDate || '—'} mono />
          <Field label="Settlement Date" value={order.settlementDate || '—'} mono />
        </div>
      </div>
    </>
  )
}

export default function Orders({ sseEventCount = 0 }) {
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [statusFilter, setStatusFilter] = useState('')
  const [accountFilter, setAccountFilter] = useState('')
  const [sideFilter, setSideFilter] = useState('')
  const [fundFilter, setFundFilter] = useState('')
  const [search, setSearch] = useState('')
  const [selectedOrder, setSelectedOrder] = useState(null)
  const [searchParams] = useSearchParams()

  // Pre-fill filters from URL params (e.g., from Accounts or Funds page click)
  useEffect(() => {
    const acc = searchParams.get('accountID')
    const fund = searchParams.get('fundID')
    if (acc) setAccountFilter(acc)
    if (fund) setFundFilter(fund)
  }, []) // run once on mount

  useEffect(() => {
    if (sseEventCount === 0) setLoading(true)
    getOrderViews()
      .then((data) => {
        setOrders(data)
        // Keep selected order in sync with live data
        if (selectedOrder) {
          const fresh = data.find((o) => o.orderID === selectedOrder.orderID)
          if (fresh) setSelectedOrder(fresh)
        }
      })
      .catch((err) => setError(err.message || 'Failed to load orders'))
      .finally(() => setLoading(false))
  }, [sseEventCount])

  const accountOptions = useMemo(() => {
    const ids = [...new Set(orders.map((o) => o.accountID).filter(Boolean))]
    return ids.map((id) => ({ value: id, label: id }))
  }, [orders])

  const fundOptions = useMemo(() => {
    const names = [...new Set(orders.map((o) => o.fundID).filter(Boolean))]
    return names.map((id) => {
      const o = orders.find((x) => x.fundID === id)
      return { value: id, label: o?.fundName || id }
    })
  }, [orders])

  const filtered = useMemo(() => {
    return orders.filter((o) => {
      if (statusFilter && o.orderStatus !== statusFilter) return false
      if (accountFilter && o.accountID !== accountFilter) return false
      if (sideFilter && o.orderSide !== sideFilter) return false
      if (fundFilter && o.fundID !== fundFilter) return false
      if (search && !o.orderID?.toLowerCase().includes(search.toLowerCase())) return false
      return true
    }).sort((a, b) => toOrderTime(b) - toOrderTime(a))
  }, [orders, statusFilter, accountFilter, sideFilter, fundFilter, search])

  if (loading) {
    return (
      <div className="orders-loading">
        <span className="spinner" />
        Loading orders...
      </div>
    )
  }

  if (error) return <div className="orders-error">{error}</div>

  const filters = [
    { key: 'status',   label: 'Status',   options: STATUS_OPTIONS,  value: statusFilter,  onChange: setStatusFilter },
    { key: 'account',  label: 'Account',  options: accountOptions,  value: accountFilter, onChange: setAccountFilter },
    { key: 'side',     label: 'Side',     options: SIDE_OPTIONS,    value: sideFilter,    onChange: setSideFilter },
    { key: 'fund',     label: 'Fund',     options: fundOptions,     value: fundFilter,    onChange: setFundFilter },
  ]

  return (
    <div className="orders-page">
      <div className="orders-page-header">
        <h2>Orders</h2>
        <Link to={ROUTES.NEW_ORDER} className="btn btn-primary">+ New Order</Link>
      </div>
      <FilterBar
        filters={filters}
        searchPlaceholder="Search by Order ID..."
        searchValue={search}
        onSearchChange={setSearch}
      />
      <div className="orders-count">
        Showing {filtered.length} of {orders.length} orders
      </div>
      <div className="card" style={{ padding: 0 }}>
        <DataTable
          columns={COLUMNS}
          data={filtered}
          emptyMessage="No orders found"
          onRowClick={setSelectedOrder}
        />
      </div>

      <OrderDrawer order={selectedOrder} onClose={() => setSelectedOrder(null)} />
    </div>
  )
}

