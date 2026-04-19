import { useState, useEffect, useMemo } from 'react'
import './Users.css'
import '../components/FilterBar/FilterBar.css'
import { getUsers } from '../api/usersApi'
import DataTable from '../components/DataTable/DataTable'

function RoleBadge({ role }) {
  const isAdvisor = role === 'ADVISOR'
  return (
    <span className={`role-badge role-badge--${isAdvisor ? 'advisor' : 'investor'}`}>
      {role}
    </span>
  )
}

function ClientList({ accounts = [] }) {
  if (accounts.length === 0) return <span className="text-muted text-sm">—</span>
  return (
    <div className="client-accounts-list">
      {accounts.map((id) => (
        <span key={id} className="client-account-chip">{id}</span>
      ))}
    </div>
  )
}

export default function Users() {
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [search, setSearch] = useState('')
  const [roleFilter, setRoleFilter] = useState('ALL')

  useEffect(() => {
    getUsers()
      .then(setUsers)
      .catch((err) => setError(err.message || 'Failed to load users'))
      .finally(() => setLoading(false))
  }, [])

  const filtered = useMemo(() => {
    let result = users
    if (roleFilter !== 'ALL') {
      result = result.filter((u) => u.role === roleFilter)
    }
    if (search) {
      const q = search.toLowerCase()
      result = result.filter(
        (u) =>
          u.username?.toLowerCase().includes(q) ||
          u.displayName?.toLowerCase().includes(q) ||
          u.userID?.toLowerCase().includes(q) ||
          u.accountID?.toLowerCase().includes(q) ||
          u.advisorID?.toLowerCase().includes(q)
      )
    }
    return result
  }, [users, search, roleFilter])

  const investorCount = users.filter((u) => u.role === 'INVESTOR').length
  const advisorCount = users.filter((u) => u.role === 'ADVISOR').length

  const columns = [
    { key: 'userID', label: 'User ID', render: (v) => <span className="font-mono text-sm">{v}</span> },
    { key: 'displayName', label: 'Name' },
    { key: 'username', label: 'Username', render: (v) => <span className="font-mono text-sm">{v}</span> },
    { key: 'role', label: 'Role', align: 'center', render: (v) => <RoleBadge role={v} /> },
    {
      key: 'accountID', label: 'Account / Advisor ID', render: (v, row) => {
        if (row.role === 'ADVISOR') {
          return <span className="font-mono text-sm">{row.advisorID}</span>
        }
        return v ? <span className="font-mono text-sm">{v}</span> : <span className="text-muted text-sm">—</span>
      }
    },
    {
      key: 'clientAccounts', label: 'Client Accounts', sortable: false,
      render: (v, row) => {
        if (row.role !== 'ADVISOR') return <span className="text-muted text-sm">—</span>
        return <ClientList accounts={v} />
      }
    },
  ]

  if (loading) {
    return (
      <div className="users-loading">
        <span className="spinner" />
        Loading users...
      </div>
    )
  }

  if (error) return <div className="users-error">{error}</div>

  return (
    <div className="users-page">
      <div className="users-summary">
        <div className="users-summary-card">
          <span className="users-summary-value">{users.length}</span>
          <span className="users-summary-label">Total Users</span>
        </div>
        <div className="users-summary-card users-summary-card--investor">
          <span className="users-summary-value">{investorCount}</span>
          <span className="users-summary-label">Investors</span>
        </div>
        <div className="users-summary-card users-summary-card--advisor">
          <span className="users-summary-value">{advisorCount}</span>
          <span className="users-summary-label">Advisors</span>
        </div>
      </div>

      <div className="users-toolbar">
        <div className="filter-bar-search">
          <span className="filter-bar-search-icon">
            <svg width="13" height="13" viewBox="0 0 13 13" fill="none" aria-hidden="true">
              <circle cx="5.5" cy="5.5" r="4" stroke="currentColor" strokeWidth="1.4"/>
              <line x1="8.5" y1="8.5" x2="12" y2="12" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round"/>
            </svg>
          </span>
          <input
            type="text"
            placeholder="Search users..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <div className="users-role-filter">
          {['ALL', 'INVESTOR', 'ADVISOR'].map((r) => (
            <button
              key={r}
              className={`users-role-btn ${roleFilter === r ? 'active' : ''}`}
              onClick={() => setRoleFilter(r)}
            >
              {r === 'ALL' ? 'All' : r.charAt(0) + r.slice(1).toLowerCase() + 's'}
            </button>
          ))}
        </div>
      </div>

      <div className="card" style={{ padding: 0 }}>
        <DataTable
          columns={columns}
          data={filtered}
          emptyMessage="No users found"
        />
      </div>
    </div>
  )
}
