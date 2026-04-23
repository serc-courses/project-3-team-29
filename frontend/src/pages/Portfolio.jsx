import { useState, useEffect } from 'react'
import './Portfolio.css'
import { CONFIG } from '../constants/config'

const fmt = (v) => parseFloat(v || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
const fmtPct = (v) => parseFloat(v || 0).toFixed(2)

export default function Portfolio() {
    const [data, setData] = useState(null)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)
    const [accountFilter, setAccountFilter] = useState('')

    const fetchPortfolio = async () => {
        setError(null)
        try {
            const params = accountFilter ? `?accountID=${accountFilter}` : ''
            const res = await fetch(`${CONFIG.API_BASE_URL}/view/portfolio${params}`)
            if (!res.ok) throw new Error('Failed to load portfolio')
            const json = await res.json()
            setData(json)
        } catch (err) {
            setError(err.message)
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        fetchPortfolio()
    }, [accountFilter])



    if (loading && !data) {
        return (
            <div className="pf-loading">
                <span className="spinner" />
                Loading portfolio P/L...
            </div>
        )
    }

    if (error && !data) return <div className="pf-error">{error}</div>

    const totalPnl = parseFloat(data?.totalPnl || 0)
    const totalPnlPct = parseFloat(data?.totalPnlPercent || 0)
    const totalInvested = parseFloat(data?.totalInvested || 0)
    const totalCurrent = parseFloat(data?.totalCurrentValue || 0)
    const holdings = data?.holdings || []
    const pnlClass = totalPnl >= 0 ? 'pnl-positive' : 'pnl-negative'

    return (
        <div className="pf-page">
            <div className="pf-header">
                <h2>Portfolio P/L</h2>
                <div className="pf-controls">
                    <input
                        type="text"
                        className="pf-account-filter"
                        placeholder="Filter by Account ID..."
                        value={accountFilter}
                        onChange={(e) => setAccountFilter(e.target.value)}
                    />
                    <button className="pf-refresh-btn" onClick={fetchPortfolio}>↻ Refresh</button>
                </div>
            </div>

            {/* Summary cards */}
            <div className="pf-summary">
                <div className="pf-summary-card">
                    <div className="pf-summary-label">Total Invested</div>
                    <div className="pf-summary-value">${fmt(totalInvested)}</div>
                </div>
                <div className="pf-summary-card">
                    <div className="pf-summary-label">Current Value</div>
                    <div className="pf-summary-value">${fmt(totalCurrent)}</div>
                </div>
                <div className={`pf-summary-card pf-summary-pnl ${pnlClass}`}>
                    <div className="pf-summary-label">Total P/L</div>
                    <div className="pf-summary-value">
                        {totalPnl >= 0 ? '+' : ''}${fmt(totalPnl)}
                        <span className="pf-summary-pct"> ({totalPnl >= 0 ? '+' : ''}{fmtPct(totalPnlPct)}%)</span>
                    </div>
                </div>
                <div className="pf-summary-card">
                    <div className="pf-summary-label">Holdings</div>
                    <div className="pf-summary-value">{holdings.length}</div>
                </div>
            </div>

            <div className="pf-info">
                NAV is updated manually by admin. Go to the Funds page to change NAV values.
            </div>

            {holdings.length === 0 ? (
                <div className="pf-empty">
                    <div className="pf-empty-icon">📊</div>
                    <div className="pf-empty-text">No booked holdings found</div>
                    <div className="pf-empty-sub">Orders need to reach BOOKED status with a TA contract to show P/L</div>
                </div>
            ) : (
                <div className="card" style={{ padding: 0 }}>
                    <div style={{ overflowX: 'auto' }}>
                        <table className="data-table">
                            <thead>
                                <tr>
                                    <th>Account</th>
                                    <th>Fund</th>
                                    <th style={{ textAlign: 'right' }}>Invested ($)</th>
                                    <th style={{ textAlign: 'right' }}>Buy NAV</th>
                                    <th style={{ textAlign: 'right' }}>Shares</th>
                                    <th style={{ textAlign: 'right' }}>Current NAV</th>
                                    <th style={{ textAlign: 'right' }}>Current Value ($)</th>
                                    <th style={{ textAlign: 'right' }}>P/L ($)</th>
                                    <th style={{ textAlign: 'right' }}>P/L %</th>
                                </tr>
                            </thead>
                            <tbody>
                                {holdings.map((h) => {
                                    const pnl = parseFloat(h.pnl || 0)
                                    const pnlPct = parseFloat(h.pnlPercent || 0)
                                    const cls = pnl >= 0 ? 'pnl-positive' : 'pnl-negative'

                                    return (
                                        <tr key={h.orderID}>
                                            <td><span className="font-mono text-sm">{h.accountID}</span></td>
                                            <td>
                                                <div className="pf-fund-cell">
                                                    <span className="pf-fund-id">{h.fundID}</span>
                                                    <span className="pf-fund-name">{h.fundName}</span>
                                                </div>
                                            </td>
                                            <td style={{ textAlign: 'right' }}>${fmt(h.investedAmount)}</td>
                                            <td style={{ textAlign: 'right' }}>${fmt(h.buyNav)}</td>
                                            <td style={{ textAlign: 'right' }}>{parseFloat(h.allocatedShares || 0).toFixed(4)}</td>
                                            <td style={{ textAlign: 'right' }}>
                                                <span className="pf-live-nav">${fmt(h.currentNav)}</span>
                                            </td>
                                            <td style={{ textAlign: 'right' }}>${fmt(h.currentValue)}</td>
                                            <td style={{ textAlign: 'right' }}>
                                                <span className={cls}>
                                                    {pnl >= 0 ? '+' : ''}${fmt(pnl)}
                                                </span>
                                            </td>
                                            <td style={{ textAlign: 'right' }}>
                                                <span className={`pf-pct-badge ${cls}`}>
                                                    {pnl >= 0 ? '▲' : '▼'} {fmtPct(Math.abs(pnlPct))}%
                                                </span>
                                            </td>
                                        </tr>
                                    )
                                })}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}
        </div>
    )
}
