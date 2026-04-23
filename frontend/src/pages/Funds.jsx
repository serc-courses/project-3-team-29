import { useState, useEffect, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import './Funds.css'
import '../components/FilterBar/FilterBar.css'
import { getFunds, updateFundNav } from '../api/fundApi'
import { formatCurrency, formatQuantity } from '../utils/formatters'

export default function Funds() {
  const [funds, setFunds] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [search, setSearch] = useState('')
  const [navModal, setNavModal] = useState(null)  // { fundID, fundName, currentNav }
  const [newNav, setNewNav] = useState('')
  const [updating, setUpdating] = useState(false)
  const [toast, setToast] = useState(null)
  const navigate = useNavigate()

  const loadFunds = () => {
    getFunds()
      .then(setFunds)
      .catch((err) => setError(err.message || 'Failed to load funds'))
      .finally(() => setLoading(false))
  }

  useEffect(() => { loadFunds() }, [])

  // Auto-dismiss toast
  useEffect(() => {
    if (!toast) return
    const t = setTimeout(() => setToast(null), 3000)
    return () => clearTimeout(t)
  }, [toast])

  const filtered = useMemo(() => {
    if (!search) return funds
    const q = search.toLowerCase()
    return funds.filter(
      (f) =>
        f.fundName?.toLowerCase().includes(q) ||
        f.fundID?.toLowerCase().includes(q)
    )
  }, [funds, search])

  const openNavModal = (fund, e) => {
    e.stopPropagation()
    setNavModal({ fundID: fund.fundID, fundName: fund.fundName, currentNav: fund.nav })
    setNewNav(fund.nav?.toString() || '')
  }

  const handleNavUpdate = async () => {
    if (!newNav || isNaN(Number(newNav)) || Number(newNav) <= 0) return
    setUpdating(true)
    try {
      const result = await updateFundNav(navModal.fundID, Number(newNav))
      setToast({ type: 'success', message: `NAV updated for ${navModal.fundName}: ₹${result.oldNav} → ₹${result.newNav}` })
      setNavModal(null)
      loadFunds() // Refresh
    } catch (err) {
      setToast({ type: 'error', message: err.message || 'Failed to update NAV' })
    } finally {
      setUpdating(false)
    }
  }

  if (loading) {
    return (
      <div className="funds-loading">
        <span className="spinner" />
        Loading funds...
      </div>
    )
  }

  if (error) return <div className="funds-error">{error}</div>

  return (
    <div className="funds-page">
      {/* Toast */}
      {toast && (
        <div className={`nav-toast nav-toast-${toast.type}`}>
          {toast.message}
        </div>
      )}

      <div className="funds-search">
        <div className="filter-bar-search">
          <span className="filter-bar-search-icon">
            🔍
          </span>
          <input
            type="text"
            placeholder="Search by fund name or ID..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
      </div>

      {filtered.length === 0 ? (
        <div className="funds-empty">No fund data available yet</div>
      ) : (
        <div className="funds-grid">
          {filtered.map((fund) => {
            const orderCount = fund.orderCount ?? 0
            const totalAmount = fund.totalAmount ?? 0
            const totalQuantity = fund.totalQuantity ?? 0
            const buyCount = fund.orderSides?.BUY ?? 0
            const sellCount = fund.orderSides?.SELL ?? 0

            return (
              <div
                key={fund.fundID}
                className="fund-card"
                onClick={() => navigate(`/orders?fundID=${fund.fundID}`)}
              >
                <div className="fund-card-id">{fund.fundID}</div>
                <div className='fund-family'>{fund.fundFamily}</div>
                <div className="fund-card-name">{fund.fundName}</div>

                <div className="fund-card-nav">
                  {formatCurrency(fund.nav ?? 0)}
                  <button
                    className="nav-edit-btn"
                    title="Change NAV"
                    onClick={(e) => openNavModal(fund, e)}
                  >
                    ✏️
                  </button>
                </div>

                <div className="fund-card-divider" />

                <div className="fund-card-stats">
                  <div className="fund-stat-row">
                    <span className="fund-stat-label">Orders</span>
                    <span className="fund-stat-value">{orderCount}</span>
                  </div>
                  <div className="fund-stat-row">
                    <span className="fund-stat-label">Total Amount</span>
                    <span className="fund-stat-value">
                      {formatCurrency(totalAmount)}
                    </span>
                  </div>
                  <div className="fund-stat-row">
                    <span className="fund-stat-label">Buy / Sell</span>
                    <div className="fund-side-badges">
                      <span className="fund-side-badge buy">B {buyCount}</span>
                      <span className="fund-side-badge sell">S {sellCount}</span>
                    </div>
                  </div>
                  <div className="fund-stat-row">
                    <span className="fund-stat-label">Total Qty</span>
                    <span className="fund-stat-value">
                      {formatQuantity(totalQuantity)}
                    </span>
                  </div>
                </div>
              </div>
            )
          })}
        </div>
      )}

      {/* Change NAV Modal */}
      {navModal && (
        <div className="nav-modal-overlay" onClick={() => setNavModal(null)}>
          <div className="nav-modal" onClick={e => e.stopPropagation()}>
            <h3 className="nav-modal-title">Change NAV</h3>
            <p className="nav-modal-sub">
              {navModal.fundName} ({navModal.fundID})
            </p>
            <div className="nav-modal-current">
              Current NAV: <strong>₹{navModal.currentNav}</strong>
            </div>
            <div className="nav-modal-input-group">
              <label htmlFor="new-nav-input">New NAV (₹)</label>
              <input
                id="new-nav-input"
                type="number"
                step="0.01"
                min="0.01"
                value={newNav}
                onChange={e => setNewNav(e.target.value)}
                autoFocus
                className="nav-modal-input"
              />
            </div>
            <div className="nav-modal-actions">
              <button className="nav-modal-cancel" onClick={() => setNavModal(null)}>Cancel</button>
              <button
                className="nav-modal-confirm"
                disabled={updating || !newNav || Number(newNav) <= 0}
                onClick={handleNavUpdate}
              >
                {updating ? 'Updating...' : 'Update NAV via Kafka'}
              </button>
            </div>
            <p className="nav-modal-hint">
              This will update the fund NAV in the database and publish a <code>oms.nav.updated</code> event to Kafka.
            </p>
          </div>
        </div>
      )}
    </div>
  )
}