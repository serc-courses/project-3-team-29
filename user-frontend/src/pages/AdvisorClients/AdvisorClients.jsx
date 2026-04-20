import { useState, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { getAdvisorClients } from '../../api/advisorApi'
import { useFetch } from '../../hooks/useFetch'
import { ADVISOR_CONFIG } from '../../constants/advisorConfig'
import { formatCurrency, formatCompact } from '../../utils/formatters'
import './AdvisorClients.css'

const STATUS_BAR_GROUPS = [
  { key: 'c',  statuses: ['BOOKED'],                               color: '#059669', label: 'Done' },
  { key: 'pe', statuses: ['BULKED','CONFIRMED','CONTRACTED'],     color: '#D97706', label: 'Pending' },
  { key: 'pr', statuses: ['PLANNED','VALIDATED','ENRICHED','PLACED'], color: '#2563EB', label: 'Active' },
  { key: 'f',  statuses: ['ERRORED'],                              color: '#DC2626', label: 'Failed' },
]

function ClientStatusBar({ statuses }) {
  const counts = STATUS_BAR_GROUPS.map(g => ({
    ...g,
    count: g.statuses.reduce((s, k) => s + (statuses?.[k] || 0), 0),
  }))
  const total = counts.reduce((s, g) => s + g.count, 0)
  if (total === 0) return null
  const active = counts.filter(g => g.count > 0)

  return (
    <div style={{ marginTop: 8, width: '100%' }}>
      {/* Stacked bar */}
      <div style={{ display: 'flex', height: 6, borderRadius: 4, overflow: 'hidden', gap: 1, width: '100%' }}>
        {active.map((g, i) => (
          <div key={g.key} style={{
            width: `${(g.count / total) * 100}%`,
            background: g.color,
            borderRadius: i === 0 && active.length > 1 ? '4px 0 0 4px'
              : i === active.length - 1 && active.length > 1 ? '0 4px 4px 0'
              : active.length === 1 ? 4 : 0,
          }} />
        ))}
      </div>
      {/* Dot legend */}
      <div style={{ display: 'flex', gap: '6px 12px', marginTop: 5, flexWrap: 'wrap' }}>
        {active.map(g => (
          <span key={g.key} style={{ display: 'flex', alignItems: 'center', gap: 4, fontSize: 11, color: '#64748B' }}>
            <span style={{ width: 6, height: 6, borderRadius: '50%', background: g.color, flexShrink: 0 }} />
            {g.count}
          </span>
        ))}
      </div>
    </div>
  )
}

const SORT_OPTIONS = [
  { value: 'amount', label: 'Highest amount' },
  { value: 'orders', label: 'Most orders' },
  { value: 'active', label: 'Most active' },
  { value: 'issues', label: 'Most issues' },
]

export default function AdvisorClients({ advisorId, sseEventCount = 0 }) {
  const id = advisorId || ADVISOR_CONFIG.DEFAULT_ADVISOR_ID
  const navigate = useNavigate()
  const [search, setSearch] = useState('')
  const [sort, setSort] = useState('amount')

  const { data, loading } = useFetch(() => getAdvisorClients(id), [id, sseEventCount])
  const clients = data || []

  const filtered = useMemo(() => {
    let list = clients
    if (search.trim()) list = list.filter(c => c.accountID?.toLowerCase().includes(search.toLowerCase()))
    switch (sort) {
      case 'orders': list = [...list].sort((a, b) => (b.orderCount || 0) - (a.orderCount || 0)); break
      case 'active': list = [...list].sort((a, b) => {
        const active = c => ['PLANNED','VALIDATED','ENRICHED','PLACED'].reduce((s, k) => s + (c.statuses?.[k] || 0), 0)
        return active(b) - active(a)
      }); break
      case 'issues': list = [...list].sort((a, b) => (b.statuses?.ERRORED || 0) - (a.statuses?.ERRORED || 0)); break
      default: list = [...list].sort((a, b) => (b.totalAmount || 0) - (a.totalAmount || 0))
    }
    return list
  }, [clients, search, sort])

  if (loading) {
    return (
      <div className="page">
        <div className="skeleton skeleton-heading" style={{ width: '40%', marginBottom: 20 }} />
        <div className="skeleton" style={{ height: 44, borderRadius: 8, marginBottom: 16 }} />
        {[1,2,3,4,5].map(i => <div key={i} className="skeleton skeleton-card" style={{ height: 88, marginBottom: 8 }} />)}
      </div>
    )
  }

  return (
    <div className="page advisor-clients-page">
      <h1 className="page-title">Clients</h1>

      <div className="orders-search" style={{ marginBottom: 8 }}>
        <span className="orders-search-icon">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
        </span>
        <input type="search" className="orders-search-input" placeholder="Search by account ID" value={search} onChange={e => setSearch(e.target.value)} />
        {search && <button className="orders-search-clear" onClick={() => setSearch('')}>
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>}
      </div>

      <div className="advisor-clients-sort">
        <span className="advisor-clients-sort-label">Sort:</span>
        <select className="advisor-clients-sort-select" value={sort} onChange={e => setSort(e.target.value)}>
          {SORT_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
        </select>
      </div>

      {filtered.length === 0 ? (
        <div className="empty-state">
          <p className="empty-state-title">{search ? `No clients matching "${search}"` : 'No clients assigned'}</p>
        </div>
      ) : (
        <div className="advisor-clients-list">
          {filtered.map(c => (
            <button key={c.accountID} className="card card-interactive card-elevated advisor-client-card" onClick={() => navigate(`/advisor/clients/${c.accountID}`)}>
              <div className="advisor-client-card-top">
                <span className="font-mono advisor-client-id">{c.accountID}</span>
                <span className="font-mono advisor-client-amount">{c.totalAmount != null ? formatCurrency(c.totalAmount) : '—'}</span>
              </div>
              <div className="advisor-client-card-mid">
                <span>{c.orderCount ?? 0} orders</span>
                {c.totalQuantity != null && <span>{formatCompact(c.totalQuantity)} units</span>}
              </div>
              <div className="advisor-client-card-badges">
                {(c.statuses?.BOOKED || 0) > 0 && <span className="advisor-badge advisor-badge-green">{c.statuses.BOOKED} done</span>}
                {(['BULKED','CONFIRMED','CONTRACTED'].reduce((s, k) => s + (c.statuses?.[k] || 0), 0)) > 0 &&
                  <span className="advisor-badge advisor-badge-amber">{['BULKED','CONFIRMED','CONTRACTED'].reduce((s, k) => s + (c.statuses?.[k] || 0), 0)} pending</span>}
                {(c.statuses?.ERRORED || 0) > 0 && <span className="advisor-badge advisor-badge-red">{c.statuses.ERRORED} failed</span>}
              </div>
              <ClientStatusBar statuses={c.statuses} />
            </button>
          ))}
        </div>
      )}

      <button className="orders-fab" onClick={() => navigate('/advisor/orders/new')} aria-label="New Basket">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
          <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
        </svg>
      </button>
    </div>
  )
}
