import { useState, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { getOrders } from '../../api/ordersApi'
import { useFetch } from '../../hooks/useFetch'
import { useAuth } from '../../context/AuthContext'
import OrderCard from '../../components/OrderCard/OrderCard'
import EmptyState from '../../components/EmptyState/EmptyState'
import { STATUS_GROUP } from '../../constants/orderStatus'
import './Orders.css'

const FILTERS = [
  { key: 'all', label: 'All', statuses: null },
  { key: 'active', label: 'Active', statuses: STATUS_GROUP.PROCESSING },
  { key: 'pending', label: 'Pending', statuses: STATUS_GROUP.PENDING },
  { key: 'completed', label: 'Completed', statuses: STATUS_GROUP.COMPLETED },
  { key: 'failed', label: 'Failed', statuses: STATUS_GROUP.FAILED },
]

const toOrderTime = (order) => {
  if ((order?.createdAt ?? 0) > 0) return order.createdAt
  if (order?.tradeDate) {
    const t = Date.parse(`${order.tradeDate}T00:00:00Z`)
    if (!Number.isNaN(t)) return t
  }
  return 0
}

function SkeletonOrders() {
  return (
    <div className="page orders-page">
      <div className="skeleton skeleton-heading" style={{ width: '40%', marginBottom: 4 }} />
      <div className="skeleton skeleton-text-sm" style={{ width: '30%', marginBottom: 20 }} />
      <div className="skeleton" style={{ height: 36, borderRadius: 9999, width: '100%', marginBottom: 16 }} />
      {[1,2,3,4].map(i => <div key={i} className="skeleton skeleton-card" style={{ marginBottom: 8 }} />)}
    </div>
  )
}

function NoOrdersIcon() {
  return (
    <svg width="64" height="64" viewBox="0 0 64 64" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
      <rect x="12" y="8" width="40" height="48" rx="4"/>
      <line x1="22" y1="24" x2="42" y2="24"/>
      <line x1="22" y1="32" x2="36" y2="32"/>
    </svg>
  )
}

export default function Orders({ sseEventCount = 0 }) {
  const navigate = useNavigate()
  const { user } = useAuth()
  const accountID = user?.accountID
  const [activeFilter, setActiveFilter] = useState('all')
  const [search, setSearch] = useState('')

  const { data, loading, refetch } = useFetch(
    () => getOrders(accountID ? { accountID } : {}),
    [sseEventCount, accountID]
  )
  const orders = data || []

  const handleCancelSuccess = () => {
    refetch()
  }

  const counts = useMemo(() => {
    const result = { all: orders.length }
    FILTERS.slice(1).forEach(f => {
      result[f.key] = orders.filter(o => f.statuses.includes(o.orderStatus)).length
    })
    return result
  }, [orders])

  const filtered = useMemo(() => {
    let list = orders
    const f = FILTERS.find(f => f.key === activeFilter)
    if (f?.statuses) list = list.filter(o => f.statuses.includes(o.orderStatus))
    if (search.trim()) list = list.filter(o => o.orderID?.toLowerCase().includes(search.toLowerCase()))
    return [...list].sort((a, b) => toOrderTime(b) - toOrderTime(a))
  }, [orders, activeFilter, search])

  if (loading) return <SkeletonOrders />

  return (
    <div className="page orders-page">
      <h1 className="page-title">Orders</h1>
      <p className="page-subtitle">{orders.length} total</p>

      {/* Filter chips */}
      <div className="filter-chips">
        {FILTERS.map(f => (
          <button
            key={f.key}
            className={`filter-chip${activeFilter === f.key ? ' active' : ''}`}
            onClick={() => setActiveFilter(f.key)}
          >
            {f.label} ({counts[f.key] ?? 0})
          </button>
        ))}
      </div>

      {/* Search */}
      <div className="orders-search">
        <span className="orders-search-icon">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
        </span>
        <input
          type="search"
          className="orders-search-input"
          placeholder="Search by Order ID"
          value={search}
          onChange={e => setSearch(e.target.value)}
        />
        {search && (
          <button className="orders-search-clear" onClick={() => setSearch('')} aria-label="Clear">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        )}
      </div>

      {/* Order list */}
      {filtered.length === 0 ? (
        orders.length === 0 ? (
          <EmptyState
            icon={<NoOrdersIcon />}
            title="No orders yet"
            message="Start investing to see your orders here"
            action="Start Investing"
            onAction={() => navigate('/orders/new')}
          />
        ) : search ? (
          <EmptyState icon={<NoOrdersIcon />} title={`No orders matching "${search}"`} />
        ) : (
          <EmptyState icon={<NoOrdersIcon />} title={`No ${activeFilter} orders`} />
        )
      ) : (
        <div className="orders-list">
          {filtered.map(order => (
            <OrderCard key={order.orderID} order={order} onCancelSuccess={handleCancelSuccess} />
          ))}
        </div>
      )}

      {/* FAB */}
      <button className="orders-fab" onClick={() => navigate('/orders/new')} aria-label="New Order">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
          <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
        </svg>
      </button>
    </div>
  )
}
