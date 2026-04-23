import { useState } from 'react'
import { NavLink, Link } from 'react-router-dom'
import { NAV_ITEMS, ROUTES } from '../../constants/routes'
import { confirmOrders, bookOrders } from '../../api/operationsApi'

/* ─── Inline SVG icons ───────────────────────────────────────────────────────── */
const LogoIcon = () => (
  <svg width="18" height="18" viewBox="0 0 18 18" fill="none" aria-hidden="true">
    <polyline
      points="2,13 5.5,8 9,10.5 16,4"
      stroke="currentColor" strokeWidth="1.8"
      strokeLinecap="round" strokeLinejoin="round"
    />
    <circle cx="16" cy="4" r="1.5" fill="currentColor" />
  </svg>
)

const IconDashboard = () => (
  <svg width="15" height="15" viewBox="0 0 15 15" fill="none" aria-hidden="true">
    <rect x="1" y="8.5" width="3.2" height="5.5" rx="0.8" fill="currentColor" />
    <rect x="5.9" y="5" width="3.2" height="9" rx="0.8" fill="currentColor" />
    <rect x="10.8" y="1.5" width="3.2" height="12.5" rx="0.8" fill="currentColor" />
  </svg>
)

const IconOrders = () => (
  <svg width="15" height="15" viewBox="0 0 15 15" fill="none" aria-hidden="true">
    <rect x="2" y="1.5" width="11" height="12" rx="1.5"
      stroke="currentColor" strokeWidth="1.3" />
    <line x1="4.5" y1="5" x2="10.5" y2="5" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" />
    <line x1="4.5" y1="7.5" x2="10.5" y2="7.5" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" />
    <line x1="4.5" y1="10" x2="8" y2="10" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" />
  </svg>
)

const IconBulk = () => (
  <svg width="15" height="15" viewBox="0 0 15 15" fill="none" aria-hidden="true">
    <path d="M7.5 1L14 4.3L7.5 7.6L1 4.3Z"
      stroke="currentColor" strokeWidth="1.3" strokeLinejoin="round" />
    <path d="M1 8L7.5 11.3L14 8"
      stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" strokeLinejoin="round" />
    <path d="M1 11.2L7.5 14.5L14 11.2"
      stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
)

const IconFunds = () => (
  <svg width="15" height="15" viewBox="0 0 15 15" fill="none" aria-hidden="true">
    <circle cx="7.5" cy="7.5" r="6" stroke="currentColor" strokeWidth="1.3" />
    <path d="M7.5 3.2V4" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" />
    <path d="M7.5 11V11.8" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" />
    <path
      d="M5.3 6C5.3 5.17 6.3 4.5 7.5 4.5C8.7 4.5 9.7 5.17 9.7 6C9.7 6.83 8.7 7.5 7.5 7.5C6.3 7.5 5.3 8.17 5.3 9C5.3 9.83 6.3 10.5 7.5 10.5C8.7 10.5 9.7 9.83 9.7 9"
      stroke="currentColor" strokeWidth="1.3" strokeLinecap="round"
    />
  </svg>
)

const IconAccounts = () => (
  <svg width="15" height="15" viewBox="0 0 15 15" fill="none" aria-hidden="true">
    <circle cx="7.5" cy="5" r="2.7" stroke="currentColor" strokeWidth="1.3" />
    <path d="M1.5 14C1.5 11.1 4.2 8.8 7.5 8.8C10.8 8.8 13.5 11.1 13.5 14"
      stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" />
  </svg>
)

const IconUsers = () => (
  <svg width="15" height="15" viewBox="0 0 15 15" fill="none" aria-hidden="true">
    <circle cx="5.5" cy="4.5" r="2.2" stroke="currentColor" strokeWidth="1.3" />
    <path d="M0.5 13C0.5 10.8 2.7 9 5.5 9C6.4 9 7.3 9.2 8 9.6"
      stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" />
    <circle cx="11" cy="6" r="2" stroke="currentColor" strokeWidth="1.3" />
    <path d="M7.5 14C7.5 12 9 10.5 11 10.5C13 10.5 14.5 12 14.5 14"
      stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" />
  </svg>
)

const IconRecon = () => (
  <svg width="15" height="15" viewBox="0 0 15 15" fill="none" aria-hidden="true">
    <path d="M7.5 1.5L2 7.5L7.5 13.5" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" strokeLinejoin="round" />
    <path d="M7.5 1.5L13 7.5L7.5 13.5" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" strokeLinejoin="round" />
    <line x1="2" y1="7.5" x2="13" y2="7.5" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" />
  </svg>
)

const IconPortfolio = () => (
  <svg width="15" height="15" viewBox="0 0 15 15" fill="none" aria-hidden="true">
    <polyline points="1,13 4,9 7,10 10,5 14,2" stroke="currentColor" strokeWidth="1.3" fill="none" strokeLinecap="round" strokeLinejoin="round" />
    <polyline points="10,2 14,2 14,5" stroke="currentColor" strokeWidth="1.3" fill="none" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
)

const NAV_ICONS = {
  dashboard: <IconDashboard />,
  orders: <IconOrders />,
  bulk: <IconBulk />,
  funds: <IconFunds />,
  accounts: <IconAccounts />,
  users: <IconUsers />,
  aggregateFunds: <IconFunds />,
  reconciliation: <IconRecon />,
  portfolio: <IconPortfolio />,
}

/* ─── Toast ──────────────────────────────────────────────────────────────────── */
function Toast({ message, type, onClose }) {
  return (
    <div className={`toast toast-${type}`} onClick={onClose}>
      {message}
    </div>
  )
}

/* ─── Sidebar ────────────────────────────────────────────────────────────────── */
export default function Sidebar() {
  const [loadingConfirm, setLoadingConfirm] = useState(false)
  const [loadingBook, setLoadingBook] = useState(false)
  const [toast, setToast] = useState(null)

  const showToast = (message, type) => {
    setToast({ message, type })
    setTimeout(() => setToast(null), 3000)
  }

  const handleConfirm = async () => {
    setLoadingConfirm(true)
    try {
      await confirmOrders()
      showToast('Orders confirmed successfully', 'success')
    } catch (err) {
      showToast(err.message || 'Failed to confirm orders', 'error')
    } finally {
      setLoadingConfirm(false)
    }
  }

  const handleBook = async () => {
    setLoadingBook(true)
    try {
      await bookOrders()
      showToast('Orders booked successfully', 'success')
    } catch (err) {
      showToast(err.message || 'Failed to book orders', 'error')
    } finally {
      setLoadingBook(false)
    }
  }

  return (
    <>
      <aside className="sidebar">
        <div className="sidebar-logo">
          <span className="sidebar-logo-icon">
            <LogoIcon />
          </span>
          <div>
            <div className="sidebar-logo-text">MF-OMS</div>
            <div className="sidebar-logo-sub">Mutual Fund OMS</div>
          </div>
        </div>

        <nav className="sidebar-nav">
          {NAV_ITEMS.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              end={item.path === '/'}
              className={({ isActive }) =>
                `sidebar-nav-item${isActive ? ' active' : ''}`
              }
            >
              <span className="sidebar-nav-icon">{NAV_ICONS[item.icon]}</span>
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="sidebar-divider" />

        <div className="sidebar-operations">
          <div className="sidebar-ops-title">Operations</div>
          <Link to={ROUTES.NEW_ORDER} className="sidebar-btn sidebar-btn-new">
            + New Order
          </Link>
          <button
            className="sidebar-btn sidebar-btn-confirm"
            onClick={handleConfirm}
            disabled={loadingConfirm}
          >
            {loadingConfirm
              ? <span className="spinner" style={{ width: 13, height: 13, borderWidth: 2 }} />
              : null}
            Confirm Orders
          </button>
          <button
            className="sidebar-btn sidebar-btn-book"
            onClick={handleBook}
            disabled={loadingBook}
          >
            {loadingBook
              ? <span className="spinner" style={{ width: 13, height: 13, borderWidth: 2 }} />
              : null}
            Book Orders
          </button>
        </div>
      </aside>

      {toast && (
        <Toast
          message={toast.message}
          type={toast.type}
          onClose={() => setToast(null)}
        />
      )}
    </>
  )
}
