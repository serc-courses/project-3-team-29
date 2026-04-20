import { useState, useEffect, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import './Accounts.css'
import '../components/FilterBar/FilterBar.css'
import { getAccountAggregates } from '../api/aggregatesApi'
import DataTable from '../components/DataTable/DataTable'
import { formatCurrency, formatQuantity } from '../utils/formatters'
import { ORDER_STATUS_COLORS } from '../constants/statusColors'

function StatusBreakdown({ statuses = {} }) {
  const entries = Object.entries(statuses).filter(([, count]) => count > 0)
  if (entries.length === 0) return <span className="text-muted text-sm">—</span>
  return (
    <div className="status-breakdown">
      {entries.map(([status, count]) => {
        const colors = ORDER_STATUS_COLORS[status] || { bg: '#F3F4F6', text: '#6B7280', border: '#D1D5DB' }
        return (
          <span
            key={status}
            className="status-mini-badge"
            style={{ background: colors.bg, color: colors.text, borderColor: colors.border }}
          >
            {status}: {count}
          </span>
        )
      })}
    </div>
  )
}

export default function Accounts() {
  const [accounts, setAccounts] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [search, setSearch] = useState('')
  const navigate = useNavigate()

  useEffect(() => {
    getAccountAggregates()
      .then(setAccounts)
      .catch((err) => setError(err.message || 'Failed to load accounts'))
      .finally(() => setLoading(false))
  }, [])

  const filtered = useMemo(() => {
    if (!search) return accounts
    const q = search.toLowerCase()
    return accounts.filter((a) => a.accountID?.toLowerCase().includes(q))
  }, [accounts, search])

  const columns = [
    { key: 'accountID', label: 'Account ID', render: (v) => <span className="font-mono text-sm">{v}</span> },
    { key: 'orderCount', label: 'Orders', align: 'center' },
    { key: 'totalAmount', label: 'Total Amount', align: 'right', render: (v) => formatCurrency(v) },
    { key: 'totalQuantity', label: 'Total Qty', align: 'right', render: (v) => formatQuantity(v) },
    {
      key: 'statuses', label: 'Status Breakdown', sortable: false,
      render: (v) => <StatusBreakdown statuses={v} />,
    },
  ]

  if (loading) {
    return (
      <div className="accounts-loading">
        <span className="spinner" />
        Loading accounts...
      </div>
    )
  }

  if (error) return <div className="accounts-error">{error}</div>

  return (
    <div className="accounts-page">
      <div className="accounts-search">
        <div className="filter-bar-search">
          <span className="filter-bar-search-icon">
            <svg width="13" height="13" viewBox="0 0 13 13" fill="none" aria-hidden="true">
              <circle cx="5.5" cy="5.5" r="4" stroke="currentColor" strokeWidth="1.4"/>
              <line x1="8.5" y1="8.5" x2="12" y2="12" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round"/>
            </svg>
          </span>
          <input
            type="text"
            placeholder="Search by Account ID..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
      </div>
      <div className="card" style={{ padding: 0 }}>
        <DataTable
          columns={columns}
          data={filtered}
          emptyMessage="No accounts found"
          onRowClick={(row) => navigate(`/orders?accountID=${row.accountID}`)}
        />
      </div>
    </div>
  )
}
