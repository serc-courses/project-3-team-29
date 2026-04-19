import { useState, useEffect, useMemo } from 'react'
import { Link } from 'react-router-dom'
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
  { key: 'quantity', label: 'Qty', align: 'right', render: (v) => formatQuantity(v) },
  { key: 'nav', label: 'NAV', align: 'right', render: (v) => formatCurrency(v) },
  { key: 'orderStatus', label: 'Status', align: 'center', render: (v) => <StatusBadge status={v} /> },
  { key: 'bulkOrderID', label: 'Bulk ID', render: (v) => v ? <span className="font-mono text-sm">{v}</span> : '—' },
]

export default function Orders() {
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [statusFilter, setStatusFilter] = useState('')
  const [accountFilter, setAccountFilter] = useState('')
  const [sideFilter, setSideFilter] = useState('')
  const [search, setSearch] = useState('')

  useEffect(() => {
    getOrderViews()
      .then(setOrders)
      .catch((err) => setError(err.message || 'Failed to load orders'))
      .finally(() => setLoading(false))
  }, [])

  const accountOptions = useMemo(() => {
    const ids = [...new Set(orders.map((o) => o.accountID).filter(Boolean))]
    return ids.map((id) => ({ value: id, label: id }))
  }, [orders])

  const filtered = useMemo(() => {
    return orders.filter((o) => {
      if (statusFilter && o.orderStatus !== statusFilter) return false
      if (accountFilter && o.accountID !== accountFilter) return false
      if (sideFilter && o.orderSide !== sideFilter) return false
      if (search && !o.orderID?.toLowerCase().includes(search.toLowerCase())) return false
      return true
    })
  }, [orders, statusFilter, accountFilter, sideFilter, search])

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
    { key: 'status', label: 'Status', options: STATUS_OPTIONS, value: statusFilter, onChange: setStatusFilter },
    { key: 'account', label: 'Account', options: accountOptions, value: accountFilter, onChange: setAccountFilter },
    { key: 'side', label: 'Side', options: SIDE_OPTIONS, value: sideFilter, onChange: setSideFilter },
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
        />
      </div>
    </div>
  )
}
