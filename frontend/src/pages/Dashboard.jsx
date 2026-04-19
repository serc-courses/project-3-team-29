import './Dashboard.css'
import { useMemo } from 'react'
import {
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell, Legend, AreaChart, Area,
} from 'recharts'
import { getDashboard } from '../api/dashboardApi'
import { useFetch } from '../hooks/useFetch'
import SummaryCard from '../components/SummaryCard/SummaryCard'
import { StatusChart } from '../components/Charts'
import DataTable from '../components/DataTable/DataTable'
import StatusBadge from '../components/StatusBadge/StatusBadge'
import { formatCurrency } from '../utils/formatters'

const TOOLTIP_STYLE = {
  background: '#ffffff',
  border: '1px solid #E2E8F0',
  borderRadius: 8,
  boxShadow: '0 4px 12px rgba(0,0,0,0.08)',
  fontSize: 13,
  fontFamily: 'Inter, sans-serif',
  padding: '6px 10px',
}

const AMOUNT_BUCKETS = [
  { label: '<₹5K',    min: 0,       max: 5000       },
  { label: '₹5K-25K', min: 5000,    max: 25000      },
  { label: '₹25K-1L', min: 25000,   max: 100000     },
  { label: '₹1L-5L',  min: 100000,  max: 500000     },
  { label: '>₹5L',    min: 500000,  max: Infinity   },
]

const PIPELINE_STAGES = [
  { name: 'PLANNED',    color: '#93C5FD' },
  { name: 'VALIDATED',  color: '#60A5FA' },
  { name: 'ENRICHED',   color: '#3B82F6' },
  { name: 'PLACED',     color: '#2563EB' },
  { name: 'BULKED',     color: '#FCD34D' },
  { name: 'CONFIRMED',  color: '#F59E0B' },
  { name: 'CONTRACTED', color: '#D97706' },
  { name: 'BOOKED',     color: '#059669' },
  { name: 'ERRORED',    color: '#DC2626' },
]

/* ─── Summary card SVG icons ─────────────────────────────────────────────────── */
const IconTotalOrders = ({ color }) => (
  <svg width="18" height="18" viewBox="0 0 18 18" fill="none" aria-hidden="true">
    <rect x="3" y="2" width="12" height="14" rx="2" stroke={color} strokeWidth="1.5" />
    <line x1="5.5" y1="6.5" x2="12.5" y2="6.5" stroke={color} strokeWidth="1.5" strokeLinecap="round" />
    <line x1="5.5" y1="9.5" x2="12.5" y2="9.5" stroke={color} strokeWidth="1.5" strokeLinecap="round" />
    <line x1="5.5" y1="12.5" x2="9.5" y2="12.5" stroke={color} strokeWidth="1.5" strokeLinecap="round" />
  </svg>
)
const IconBulkOrders = ({ color }) => (
  <svg width="18" height="18" viewBox="0 0 18 18" fill="none" aria-hidden="true">
    <path d="M9 1.5L16.5 5.3L9 9.1L1.5 5.3Z" stroke={color} strokeWidth="1.5" strokeLinejoin="round" />
    <path d="M1.5 9.8L9 13.6L16.5 9.8" stroke={color} strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
    <path d="M1.5 13.5L9 17.3L16.5 13.5" stroke={color} strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
)
const IconBooked = ({ color }) => (
  <svg width="18" height="18" viewBox="0 0 18 18" fill="none" aria-hidden="true">
    <circle cx="9" cy="9" r="7.5" stroke={color} strokeWidth="1.5" />
    <path d="M5.5 9L7.8 11.5L12.5 6.5" stroke={color} strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
)
const IconErrored = ({ color }) => (
  <svg width="18" height="18" viewBox="0 0 18 18" fill="none" aria-hidden="true">
    <path d="M9 1.5L16.5 15.5H1.5Z" stroke={color} strokeWidth="1.5" strokeLinejoin="round" />
    <line x1="9" y1="7" x2="9" y2="11" stroke={color} strokeWidth="1.5" strokeLinecap="round" />
    <circle cx="9" cy="13.5" r="0.8" fill={color} />
  </svg>
)

/* ─── Table columns ──────────────────────────────────────────────────────────── */
const RECENT_ORDERS_COLUMNS = [
  { key: 'orderID',    label: 'Order ID', render: (v) => <span className="font-mono text-sm">{v}</span> },
  { key: 'accountID',  label: 'Account' },
  { key: 'fundName',   label: 'Fund' },
  { key: 'amount',     label: 'Amount', align: 'right', render: (v) => formatCurrency(v) },
  { key: 'orderStatus', label: 'Status', sortable: false, render: (v) => <StatusBadge status={v} /> },
]

function ChartCard({ title, children, height }) {
  return (
    <div className="card dashboard-chart-card">
      <div className="dashboard-section-title">{title}</div>
      {children}
    </div>
  )
}

function NoData({ height = 200 }) {
  return (
    <div style={{ height, display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#94A3B8', fontSize: 13, fontStyle: 'italic' }}>
      No data
    </div>
  )
}

/* ─── Page ───────────────────────────────────────────────────────────────────── */
export default function Dashboard({ sseEventCount }) {
  const { data, loading, error } = useFetch(getDashboard, [sseEventCount])

  const orders          = useMemo(() => data?.orders          || [], [data])
  const ordersByStatus  = useMemo(() => data?.ordersByStatus  || {}, [data])
  const bulkOrdersByStatus = useMemo(() => data?.bulkOrdersByStatus || {}, [data])

  const recentOrders = useMemo(() =>
    [...orders].sort((a, b) => (b.orderID > a.orderID ? 1 : -1)).slice(0, 10),
  [orders])

  /* BUY vs SELL pie */
  const buySellData = useMemo(() => {
    const buy  = orders.filter(o => o.orderSide === 'BUY').length
    const sell = orders.filter(o => o.orderSide === 'SELL').length
    return [
      { name: 'BUY',  value: buy,  fill: '#059669' },
      { name: 'SELL', value: sell, fill: '#DC2626'  },
    ]
  }, [orders])

  /* Top 10 funds by order count */
  const topFundsData = useMemo(() => {
    const counts = {}
    orders.forEach(o => {
      const name = o.fundName || o.fundID || 'Unknown'
      counts[name] = (counts[name] || 0) + 1
    })
    return Object.entries(counts)
      .sort((a, b) => b[1] - a[1])
      .slice(0, 10)
      .map(([name, count]) => ({
        name: name.length > 20 ? name.slice(0, 18) + '…' : name,
        fullName: name,
        count,
      }))
  }, [orders])

  /* Amount histogram */
  const histogramData = useMemo(() =>
    AMOUNT_BUCKETS.map(b => ({
      label: b.label,
      count: orders.filter(o => (o.amount || 0) >= b.min && (o.amount || 0) < b.max).length,
    })),
  [orders])

  /* Pipeline funnel */
  const pipelineData = useMemo(() =>
    PIPELINE_STAGES.map(s => ({
      name:  s.name,
      count: ordersByStatus[s.name] || 0,
      fill:  s.color,
    })),
  [ordersByStatus])

  /* Orders by Account — stacked by BUY/SELL */
  const ordersByAccountData = useMemo(() => {
    const acctMap = {}
    orders.forEach(o => {
      const acct = o.accountID || 'Unknown'
      if (!acctMap[acct]) acctMap[acct] = { name: acct, BUY: 0, SELL: 0, total: 0 }
      if (o.orderSide === 'SELL') acctMap[acct].SELL += 1
      else acctMap[acct].BUY += 1
      acctMap[acct].total += 1
    })
    return Object.values(acctMap).sort((a, b) => b.total - a.total).slice(0, 10)
  }, [orders])

  /* Amount by Account — horizontal bars */
  const amountByAccountData = useMemo(() => {
    const acctMap = {}
    orders.forEach(o => {
      const acct = o.accountID || 'Unknown'
      acctMap[acct] = (acctMap[acct] || 0) + (o.amount || 0)
    })
    return Object.entries(acctMap)
      .sort((a, b) => b[1] - a[1])
      .slice(0, 10)
      .map(([name, amount]) => ({ name, amount }))
  }, [orders])

  if (loading) {
    return (
      <div className="dashboard-loading">
        <span className="spinner" />
        Loading dashboard...
      </div>
    )
  }

  if (error) return <div className="dashboard-error">{error}</div>

  return (
    <div className="dashboard-page">
      <div className="dashboard-cards">
        <SummaryCard title="Total Orders"      value={data?.totalOrders ?? 0}       icon={<IconTotalOrders color="var(--color-primary-600)" />} accentColor="var(--color-primary-500)" />
        <SummaryCard title="Total Bulk Orders" value={data?.totalBulkOrders ?? 0}   icon={<IconBulkOrders  color="#6366F1" />}                 accentColor="#6366F1" />
        <SummaryCard title="Booked Orders"     value={ordersByStatus.BOOKED ?? 0}   icon={<IconBooked      color="var(--color-success)" />}    accentColor="var(--color-success)" />
        <SummaryCard title="Errored Orders"    value={ordersByStatus.ERRORED ?? 0}  icon={<IconErrored     color="var(--color-error)" />}      accentColor="var(--color-error)" />
      </div>

      {/* Existing charts */}
      <div className="dashboard-charts">
        <div className="card">
          <StatusChart data={ordersByStatus}     title="Order Status Distribution"      type="bar" />
        </div>
        <div className="card">
          <StatusChart data={bulkOrdersByStatus} title="Bulk Order Status Distribution" type="donut" />
        </div>
      </div>

      {/* New charts row 1: BUY/SELL + Top Funds */}
      <div className="dashboard-charts" style={{ marginBottom: 'var(--spacing-4)' }}>
        <ChartCard title="Order Volume by Type (BUY vs SELL)">
          {buySellData.every(d => d.value === 0) ? <NoData /> : (
            <div style={{ position: 'relative' }}>
              <ResponsiveContainer width="100%" height={280}>
                <PieChart>
                  <Pie
                    data={buySellData}
                    cx="50%"
                    cy="50%"
                    innerRadius={70}
                    outerRadius={110}
                    dataKey="value"
                    strokeWidth={0}
                    isAnimationActive
                    animationDuration={600}
                    animationEasing="ease-out"
                  >
                    {buySellData.map(d => <Cell key={d.name} fill={d.fill} />)}
                  </Pie>
                  <Tooltip contentStyle={TOOLTIP_STYLE} formatter={(v, n) => [v + ' orders', n]} separator="" />
                </PieChart>
              </ResponsiveContainer>
              <div style={{ position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%,-50%)', textAlign: 'center', pointerEvents: 'none' }}>
                <div style={{ fontSize: 26, fontWeight: 700, color: '#0F172A', lineHeight: 1 }}>{orders.length}</div>
                <div style={{ fontSize: 11, color: '#94A3B8', marginTop: 3 }}>total</div>
              </div>
              <div style={{ display: 'flex', justifyContent: 'center', gap: 24, marginTop: 8 }}>
                {buySellData.map(d => (
                  <span key={d.name} style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 13, color: '#475569' }}>
                    <span style={{ width: 10, height: 10, borderRadius: '50%', background: d.fill }} />
                    {d.name} ({d.value})
                  </span>
                ))}
              </div>
            </div>
          )}
        </ChartCard>

        <ChartCard title="Top 10 Funds by Order Count">
          {topFundsData.length === 0 ? <NoData height={320} /> : (
            <ResponsiveContainer width="100%" height={topFundsData.length * 36 + 20}>
              <BarChart data={topFundsData} layout="vertical" margin={{ left: 8, right: 24, top: 4, bottom: 4 }}>
                <XAxis type="number" tick={{ fontSize: 11, fill: '#94A3B8' }} allowDecimals={false} />
                <YAxis type="category" dataKey="name" tick={{ fontSize: 11, fill: '#64748B' }} width={140} />
                <Tooltip contentStyle={TOOLTIP_STYLE}
                  formatter={(v, _n, props) => [v + ' orders', props.payload.fullName || props.payload.name]} />
                <Bar dataKey="count" fill="#059669" radius={[0, 4, 4, 0]}
                  isAnimationActive animationDuration={600} animationEasing="ease-out" />
              </BarChart>
            </ResponsiveContainer>
          )}
        </ChartCard>
      </div>

      {/* New charts row 2: Amount histogram + Pipeline funnel */}
      <div className="dashboard-charts" style={{ marginBottom: 'var(--spacing-4)' }}>
        <ChartCard title="Order Amount Distribution">
          {histogramData.every(d => d.count === 0) ? <NoData /> : (
            <ResponsiveContainer width="100%" height={280}>
              <BarChart data={histogramData} margin={{ top: 8, right: 16, left: -8, bottom: 0 }}>
                <XAxis dataKey="label" tick={{ fontSize: 11, fill: '#64748B' }} />
                <YAxis tick={{ fontSize: 11, fill: '#94A3B8' }} allowDecimals={false} />
                <Tooltip contentStyle={TOOLTIP_STYLE} formatter={v => [v, 'orders']} />
                <Bar dataKey="count" fill="#3B82F6" radius={[4, 4, 0, 0]}
                  isAnimationActive animationDuration={600} animationEasing="ease-out" />
              </BarChart>
            </ResponsiveContainer>
          )}
        </ChartCard>

        <ChartCard title="Order Pipeline (by Lifecycle Stage)">
          {pipelineData.every(d => d.count === 0) ? <NoData /> : (
            <ResponsiveContainer width="100%" height={pipelineData.length * 32 + 16}>
              <BarChart data={pipelineData} layout="vertical" margin={{ left: 8, right: 24, top: 4, bottom: 4 }}>
                <XAxis type="number" tick={{ fontSize: 11, fill: '#94A3B8' }} allowDecimals={false} />
                <YAxis type="category" dataKey="name" tick={{ fontSize: 11, fill: '#64748B' }} width={84} />
                <Tooltip contentStyle={TOOLTIP_STYLE} formatter={v => [v, 'orders']} />
                <Bar dataKey="count" radius={[0, 4, 4, 0]}
                  isAnimationActive animationDuration={600} animationEasing="ease-out">
                  {pipelineData.map(d => <Cell key={d.name} fill={d.fill} />)}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          )}
        </ChartCard>
      </div>

      {/* New charts row 3: Orders by Account + Amount by Account */}
      <div className="dashboard-charts" style={{ marginBottom: 'var(--spacing-4)' }}>
        <ChartCard title="Orders by Account (BUY vs SELL)">
          {ordersByAccountData.length === 0 ? <NoData /> : (
            <ResponsiveContainer width="100%" height={280}>
              <BarChart data={ordersByAccountData} margin={{ top: 8, right: 16, left: -8, bottom: 0 }}>
                <XAxis dataKey="name" tick={{ fontSize: 10, fill: '#64748B', angle: -30 }} height={50} />
                <YAxis tick={{ fontSize: 11, fill: '#94A3B8' }} allowDecimals={false} />
                <Tooltip contentStyle={TOOLTIP_STYLE} />
                <Legend wrapperStyle={{ fontSize: 12 }} />
                <Bar dataKey="BUY" stackId="a" fill="#059669" radius={[0, 0, 0, 0]}
                  isAnimationActive animationDuration={600} animationEasing="ease-out" />
                <Bar dataKey="SELL" stackId="a" fill="#DC2626" radius={[4, 4, 0, 0]}
                  isAnimationActive animationDuration={600} animationEasing="ease-out" />
              </BarChart>
            </ResponsiveContainer>
          )}
        </ChartCard>

        <ChartCard title="Investment by Account (Top 10)">
          {amountByAccountData.length === 0 ? <NoData height={320} /> : (
            <ResponsiveContainer width="100%" height={amountByAccountData.length * 36 + 20}>
              <BarChart data={amountByAccountData} layout="vertical" margin={{ left: 8, right: 24, top: 4, bottom: 4 }}>
                <XAxis type="number" tick={{ fontSize: 11, fill: '#94A3B8' }}
                  tickFormatter={v => v >= 1000 ? `₹${(v/1000).toFixed(0)}K` : `₹${v}`} />
                <YAxis type="category" dataKey="name" tick={{ fontSize: 11, fill: '#64748B' }} width={80} />
                <Tooltip contentStyle={TOOLTIP_STYLE} formatter={v => [formatCurrency(v), 'Amount']} />
                <Bar dataKey="amount" fill="#6366F1" radius={[0, 4, 4, 0]}
                  isAnimationActive animationDuration={600} animationEasing="ease-out" />
              </BarChart>
            </ResponsiveContainer>
          )}
        </ChartCard>
      </div>

      <div className="card">
        <div className="dashboard-section-title">Recent Orders</div>
        <DataTable columns={RECENT_ORDERS_COLUMNS} data={recentOrders} emptyMessage="No orders yet" />
      </div>
    </div>
  )
}
