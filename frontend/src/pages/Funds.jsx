import { useState, useEffect, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import './Funds.css'
import '../components/FilterBar/FilterBar.css'
import { getFunds } from '../api/fundApi'
import { formatCurrency, formatQuantity } from '../utils/formatters'

export default function Funds() {
  const [funds, setFunds] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [search, setSearch] = useState('')
  const navigate = useNavigate()

  useEffect(() => {
    getFunds()
      .then(setFunds)
      .catch((err) => setError(err.message || 'Failed to load funds'))
      .finally(() => setLoading(false))
  }, [])

  const filtered = useMemo(() => {
    if (!search) return funds
    const q = search.toLowerCase()
    return funds.filter(
      (f) =>
        f.fundName?.toLowerCase().includes(q) ||
        f.fundID?.toLowerCase().includes(q)
    )
  }, [funds, search])

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
            // SAFE DEFAULTS (important)
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

                {/* NAV always exists */}
                <div className="fund-card-nav">
                  {formatCurrency(fund.nav ?? 0)}
                </div>

                <div className="fund-card-divider" />

                <div className="fund-card-stats">
                  {/* <div className="fund-stat-row">
                    <span className="fund-stat-label">Orders</span>
                    <span className="fund-stat-value">{orderCount}</span>
                  </div> */}

                  {/* <div className="fund-stat-row">
                    <span className="fund-stat-label">Total Amount</span>
                    <span className="fund-stat-value">
                      {formatCurrency(totalAmount)}
                    </span>
                  </div> */}

                  {/* <div className="fund-stat-row">
                    <span className="fund-stat-label">Buy / Sell</span>
                    <div className="fund-side-badges">
                      <span className="fund-side-badge buy">B {buyCount}</span>
                      <span className="fund-side-badge sell">S {sellCount}</span>
                    </div>
                  </div> */}

                  {/* <div className="fund-stat-row">
                    <span className="fund-stat-label">Total Qty</span>
                    <span className="fund-stat-value">
                      {formatQuantity(totalQuantity)}
                    </span>
                  </div> */}
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}