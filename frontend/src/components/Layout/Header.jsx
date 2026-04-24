import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'

const ReplayIcon = () => (
  <svg width="13" height="13" viewBox="0 0 13 13" fill="none" aria-hidden="true">
    <path d="M11.5 6.5A5 5 0 1 1 6.5 1.5"
      stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
    <path d="M6.5 1.5H10.5V5.5"
      stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
)
import { replayProjections } from '../../api/operationsApi'
import { ROUTES } from '../../constants/routes'
import { useAuth } from '../../context/AuthContext'

const PAGE_TITLES = {
  [ROUTES.DASHBOARD]: 'Dashboard',
  [ROUTES.ORDERS]: 'Orders',
  [ROUTES.NEW_ORDER]: 'New Order',
  [ROUTES.BULK_ORDERS]: 'Bulk Orders',
  [ROUTES.FUNDS]: 'Funds',
  [ROUTES.ACCOUNTS]: 'Accounts',
}

export default function Header({ sseConnected }) {
  const location = useLocation()
  const navigate = useNavigate()
  const { logout } = useAuth()
  const [replaying, setReplaying] = useState(false)
  const [toast, setToast] = useState(null)

  const title = PAGE_TITLES[location.pathname] || 'OMS'

  const showToast = (message, type) => {
    setToast({ message, type })
    setTimeout(() => setToast(null), 3000)
  }

  const handleReplay = async () => {
    setReplaying(true)
    try {
      await replayProjections()
      showToast('Projections replayed', 'success')
    } catch (err) {
      showToast(err.message || 'Replay failed', 'error')
    } finally {
      setReplaying(false)
    }
  }

  const handleLogout = async () => {
    await logout()
    navigate('/login', { replace: true })
  }

  return (
    <>
      <header className="header">
        <h1 className="header-title">{title}</h1>
        <div className="header-right">
          <div className="connection-status">
            <span className={`connection-dot ${sseConnected ? 'connected' : 'disconnected'}`} />
            {sseConnected ? 'Live' : 'Offline'}
          </div>
          <button
            className="btn btn-outline"
            onClick={handleReplay}
            disabled={replaying}
            style={{ fontSize: 'var(--text-xs)', padding: '4px 10px' }}
          >
            {replaying
              ? <span className="spinner" style={{ width: 12, height: 12, borderWidth: 2 }} />
              : <ReplayIcon />}
            Replay
          </button>
          <button
            className="btn btn-outline"
            onClick={handleLogout}
            style={{ fontSize: 'var(--text-xs)', padding: '4px 10px' }}
          >
            Logout
          </button>
        </div>
      </header>
      {toast && (
        <div className={`toast toast-${toast.type}`} onClick={() => setToast(null)}>
          {toast.message}
        </div>
      )}
    </>
  )
}
