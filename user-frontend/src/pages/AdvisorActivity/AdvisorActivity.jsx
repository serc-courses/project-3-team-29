import { useState, useMemo, useEffect } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell } from 'recharts'
import { getAdvisorClients, getAdvisorClientOrders } from '../../api/advisorApi'
import { ADVISOR_CONFIG } from '../../constants/advisorConfig'
import { STATUS_GROUP } from '../../constants/orderStatus'
import StatusPill from '../../components/StatusPill/StatusPill'
import EmptyState from '../../components/EmptyState/EmptyState'
import { formatCurrency } from '../../utils/formatters'
import { SIDE_COLORS } from '../../constants/statusColors'
import './AdvisorActivity.css'

const TOOLTIP_STYLE = {
  background: '#fff',
  border: '1px solid #E2E8F0',
  borderRadius: 8,
  boxShadow: '0 4px 12px rgba(0,0,0,0.08)',
  fontSize: 13,
  fontFamily: 'Inter, sans-serif',
  padding: '6px 10px',
}

const STATUS_FILTERS = [
  { key: 'all', label: 'All' },
  { key: 'active', label: 'Active', statuses: STATUS_GROUP.PROCESSING },
  { key: 'pending', label: 'Pending', statuses: STATUS_GROUP.PENDING },
  { key: 'completed', label: 'Completed', statuses: STATUS_GROUP.COMPLETED },
  { key: 'failed', label: 'Failed', statuses: STATUS_GROUP.FAILED },
]

const CHART_GROUPS = [
  { name: 'Processing', color: '#2563EB', key: 'processing' },
  { name: 'Pending',    color: '#D97706', key: 'pending'    },
  { name: 'Completed',  color: '#059669', key: 'completed'  },
  { name: 'Failed',     color: '#DC2626', key: 'failed'     },
]

const toOrderTime = (order) => {
  if ((order?.createdAt ?? 0) > 0) return order.createdAt
  if (order?.tradeDate) {
    const t = Date.parse(`${order.tradeDate}T00:00:00Z`)
    if (!Number.isNaN(t)) return t
  }
  return 0
}

export default function AdvisorActivity({ advisorId, sseEventCount = 0 }) {
  const id = advisorId || ADVISOR_CONFIG.DEFAULT_ADVISOR_ID
  const navigate = useNavigate()
  const location = useLocation()

  const prefilterAccount = new URLSearchParams(location.search).get('accountID') || 'all'
  const [clientFilter, setClientFilter] = useState(prefilterAccount)
  const [statusFilter, setStatusFilter] = useState('all')
  const [sideFilter, setSideFilter] = useState('all')
  const [fundSearch, setFundSearch] = useState('')
  const [allOrders, setAllOrders] = useState([])
  const [clients, setClients] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setLoading(true)
    getAdvisorClients(id)
      .then(async cl => {
        setClients(cl)
        const orderArrays = await Promise.all(cl.map(c => getAdvisorClientOrders(c.accountID).catch(() => [])))
        setAllOrders(orderArrays.flat())
      })
      .catch(() => setAllOrders([]))
      .finally(() => setLoading(false))
  }, [id, sseEventCount])

  const filtered = useMemo(() => {
    let list = allOrders
    if (clientFilter !== 'all') list = list.filter(o => o.accountID === clientFilter)
    const sf = STATUS_FILTERS.find(f => f.key === statusFilter)
    if (sf?.statuses) list = list.filter(o => sf.statuses.includes(o.orderStatus))
    if (sideFilter !== 'all') list = list.filter(o => o.orderSide === sideFilter)
    if (fundSearch.trim()) list = list.filter(o => o.fundID?.toLowerCase().includes(fundSearch.toLowerCase()) || o.fundName?.toLowerCase().includes(fundSearch.toLowerCase()))
    return [...list].sort((a, b) => toOrderTime(b) - toOrderTime(a))
  }, [allOrders, clientFilter, statusFilter, sideFilter, fundSearch])

  /* Summary strip derived from filtered list */
  const summary = useMemo(() => ({
    total:     filtered.length,
    buyAmt:    filtered.filter(o => o.orderSide === 'BUY').reduce((s, o)  => s + (o.amount || 0), 0),
    sellAmt:   filtered.filter(o => o.orderSide === 'SELL').reduce((s, o) => s + (o.amount || 0), 0),
    processing:filtered.filter(o => STATUS_GROUP.PROCESSING.includes(o.orderStatus)).length,
    pending:   filtered.filter(o => STATUS_GROUP.PENDING.includes(o.orderStatus)).length,
    completed: filtered.filter(o => STATUS_GROUP.COMPLETED.includes(o.orderStatus)).length,
    failed:    filtered.filter(o => STATUS_GROUP.FAILED.includes(o.orderStatus)).length,
  }), [filtered])

  /* Bar chart data (responds to active filters) */
  const chartData = useMemo(() => CHART_GROUPS.map(g => ({
    name:  g.name,
    value: summary[g.key],
    color: g.color,
  })), [summary])

  if (loading) {
    return (
      <div className="page">
        <div className="skeleton skeleton-heading" style={{ width: '40%', marginBottom: 20 }} />
        {[1,2,3,4,5].map(i => <div key={i} className="skeleton skeleton-card" style={{ height: 88, marginBottom: 8 }} />)}
      </div>
    )
  }

  return (
    <div className="page advisor-activity-page">
      <h1 className="page-title">Activity</h1>

      {/* Desktop top area: filters left, chart right */}
      <div className="advisor-activity-top">
        {/* Filters */}
        <div className="advisor-activity-filters">
          <div className="advisor-activity-filter-row">
            <select className="advisor-filter-select" value={clientFilter} onChange={e => setClientFilter(e.target.value)}>
              <option value="all">All Clients</option>
              {clients.map(c => <option key={c.accountID} value={c.accountID}>{c.accountID}</option>)}
            </select>
            <select className="advisor-filter-select" value={sideFilter} onChange={e => setSideFilter(e.target.value)}>
              <option value="all">BUY + SELL</option>
              <option value="BUY">BUY only</option>
              <option value="SELL">SELL only</option>
            </select>
          </div>

          <div className="filter-chips" style={{ marginBottom: 8 }}>
            {STATUS_FILTERS.map(f => (
              <button key={f.key} className={`filter-chip${statusFilter === f.key ? ' active' : ''}`} onClick={() => setStatusFilter(f.key)}>{f.label}</button>
            ))}
          </div>

          <div className="orders-search" style={{ marginBottom: 0 }}>
            <span className="orders-search-icon"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg></span>
            <input type="search" className="orders-search-input" placeholder="Search by fund..." value={fundSearch} onChange={e => setFundSearch(e.target.value)} />
            {fundSearch && <button className="orders-search-clear" onClick={() => setFundSearch('')}><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg></button>}
          </div>
        </div>

        {/* Status distribution mini chart */}
        {summary.total > 0 && (
          <div className="card card-elevated advisor-activity-chart">
            <p className="advisor-chart-title" style={{ fontSize: 11, fontWeight: 600, color: 'var(--color-text-400)', textTransform: 'uppercase', letterSpacing: '.05em', marginBottom: 8 }}>
              Status Distribution
            </p>
            <ResponsiveContainer width="100%" height={130}>
              <BarChart data={chartData} margin={{ top: 4, right: 8, left: -16, bottom: 0 }}>
                <XAxis dataKey="name" tick={{ fontSize: 11, fill: '#94A3B8' }} />
                <YAxis tick={{ fontSize: 11, fill: '#94A3B8' }} allowDecimals={false} />
                <Tooltip contentStyle={TOOLTIP_STYLE} formatter={v => [v, 'orders']} />
                <Bar dataKey="value" radius={[4, 4, 0, 0]} isAnimationActive animationDuration={600} animationEasing="ease-out">
                  {chartData.map(d => <Cell key={d.name} fill={d.color} />)}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        )}
      </div>

      {/* Summary strip */}
      <div className="advisor-activity-summary">
        <div className="activity-pill">
          <span className="activity-pill-value font-mono">{summary.total}</span>
          <span className="activity-pill-label">orders</span>
        </div>
        <div className="activity-pill">
          <span className="activity-pill-dot" style={{ background: '#059669' }} />
          <span className="activity-pill-value font-mono">{formatCurrency(summary.buyAmt)}</span>
          <span className="activity-pill-label">BUY</span>
        </div>
        <div className="activity-pill">
          <span className="activity-pill-dot" style={{ background: '#DC2626' }} />
          <span className="activity-pill-value font-mono">{formatCurrency(summary.sellAmt)}</span>
          <span className="activity-pill-label">SELL</span>
        </div>
        <div className="activity-pill">
          <span className="activity-pill-dot" style={{ background: '#2563EB' }} />
          <span className="activity-pill-value font-mono">{summary.processing}</span>
          <span className="activity-pill-label">active</span>
        </div>
        <div className="activity-pill">
          <span className="activity-pill-dot" style={{ background: '#D97706' }} />
          <span className="activity-pill-value font-mono">{summary.pending}</span>
          <span className="activity-pill-label">pending</span>
        </div>
        <div className="activity-pill">
          <span className="activity-pill-dot" style={{ background: '#DC2626' }} />
          <span className="activity-pill-value font-mono">{summary.failed}</span>
          <span className="activity-pill-label">failed</span>
        </div>
      </div>

      {filtered.length === 0
        ? <EmptyState title="No matching activity" message="Try adjusting your filters" />
        : (
          <div className="advisor-activity-list">
            {filtered.map(o => {
              const sc = SIDE_COLORS[o.orderSide] || {}
              return (
                <button key={o.orderID} className="card card-interactive card-elevated advisor-activity-card" onClick={() => navigate(`/orders/${o.orderID}`)}>
                  <div className="advisor-activity-card-top">
                    <span className="font-mono advisor-activity-account">{o.accountID}</span>
                    <span style={{ fontSize: 'var(--text-xs)', fontWeight: 700, color: sc.text, background: sc.bg, padding: '2px 6px', borderRadius: 9999 }}>{o.orderSide}</span>
                  </div>
                  <div className="advisor-activity-card-mid">
                    <span className="advisor-activity-fund">{o.fundName || o.fundID}</span>
                    <span className="font-mono advisor-activity-amount">{formatCurrency(o.amount)}</span>
                  </div>
                  <div className="advisor-activity-card-bottom">
                    <span className="font-mono" style={{ fontSize: 'var(--text-xs)', color: 'var(--color-text-400)' }}>{o.orderID}</span>
                    <StatusPill status={o.orderStatus} />
                  </div>
                </button>
              )
            })}
          </div>
        )
      }
    </div>
  )
}
