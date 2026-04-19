import './Dashboard.css'
import { getDashboard } from '../api/dashboardApi'
import { useFetch } from '../hooks/useFetch'
import SummaryCard from '../components/SummaryCard/SummaryCard'
import { StatusChart } from '../components/Charts'
import DataTable from '../components/DataTable/DataTable'
import StatusBadge from '../components/StatusBadge/StatusBadge'
import { formatCurrency } from '../utils/formatters'

/* ─── Summary card SVG icons ─────────────────────────────────────────────────── */
const IconTotalOrders = ({ color }) => (
  <svg width="18" height="18" viewBox="0 0 18 18" fill="none" aria-hidden="true">
    <rect x="3" y="2" width="12" height="14" rx="2"
      stroke={color} strokeWidth="1.5" />
    <line x1="5.5" y1="6.5" x2="12.5" y2="6.5"
      stroke={color} strokeWidth="1.5" strokeLinecap="round" />
    <line x1="5.5" y1="9.5" x2="12.5" y2="9.5"
      stroke={color} strokeWidth="1.5" strokeLinecap="round" />
    <line x1="5.5" y1="12.5" x2="9.5" y2="12.5"
      stroke={color} strokeWidth="1.5" strokeLinecap="round" />
  </svg>
)

const IconBulkOrders = ({ color }) => (
  <svg width="18" height="18" viewBox="0 0 18 18" fill="none" aria-hidden="true">
    <path d="M9 1.5L16.5 5.3L9 9.1L1.5 5.3Z"
      stroke={color} strokeWidth="1.5" strokeLinejoin="round" />
    <path d="M1.5 9.8L9 13.6L16.5 9.8"
      stroke={color} strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
    <path d="M1.5 13.5L9 17.3L16.5 13.5"
      stroke={color} strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
)

const IconBooked = ({ color }) => (
  <svg width="18" height="18" viewBox="0 0 18 18" fill="none" aria-hidden="true">
    <circle cx="9" cy="9" r="7.5" stroke={color} strokeWidth="1.5" />
    <path d="M5.5 9L7.8 11.5L12.5 6.5"
      stroke={color} strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
)

const IconErrored = ({ color }) => (
  <svg width="18" height="18" viewBox="0 0 18 18" fill="none" aria-hidden="true">
    <path d="M9 1.5L16.5 15.5H1.5Z"
      stroke={color} strokeWidth="1.5" strokeLinejoin="round" />
    <line x1="9" y1="7" x2="9" y2="11"
      stroke={color} strokeWidth="1.5" strokeLinecap="round" />
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

/* ─── Page ───────────────────────────────────────────────────────────────────── */
export default function Dashboard({ sseEventCount }) {
  const { data, loading, error } = useFetch(getDashboard, [sseEventCount])

  if (loading) {
    return (
      <div className="dashboard-loading">
        <span className="spinner" />
        Loading dashboard...
      </div>
    )
  }

  if (error) return <div className="dashboard-error">{error}</div>

  const ordersByStatus    = data?.ordersByStatus    || {}
  const bulkOrdersByStatus = data?.bulkOrdersByStatus || {}
  const recentOrders = [...(data?.orders || [])]
    .sort((a, b) => (b.orderID > a.orderID ? 1 : -1))
    .slice(0, 10)

  return (
    <div className="dashboard-page">
      <div className="dashboard-cards">
        <SummaryCard
          title="Total Orders"
          value={data?.totalOrders ?? 0}
          icon={<IconTotalOrders color="var(--color-primary-600)" />}
          accentColor="var(--color-primary-500)"
        />
        <SummaryCard
          title="Total Bulk Orders"
          value={data?.totalBulkOrders ?? 0}
          icon={<IconBulkOrders color="#6366F1" />}
          accentColor="#6366F1"
        />
        <SummaryCard
          title="Booked Orders"
          value={ordersByStatus.BOOKED ?? 0}
          icon={<IconBooked color="var(--color-success)" />}
          accentColor="var(--color-success)"
        />
        <SummaryCard
          title="Errored Orders"
          value={ordersByStatus.ERRORED ?? 0}
          icon={<IconErrored color="var(--color-error)" />}
          accentColor="var(--color-error)"
        />
      </div>

      <div className="dashboard-charts">
        <div className="card">
          <StatusChart data={ordersByStatus}     title="Order Status Distribution"      type="bar" />
        </div>
        <div className="card">
          <StatusChart data={bulkOrdersByStatus} title="Bulk Order Status Distribution" type="donut" />
        </div>
      </div>

      <div className="card">
        <div className="dashboard-section-title">Recent Orders</div>
        <DataTable columns={RECENT_ORDERS_COLUMNS} data={recentOrders} emptyMessage="No orders yet" />
      </div>
    </div>
  )
}
