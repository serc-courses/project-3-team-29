import { useState, useEffect } from 'react'
import { useAuth } from '../../context/AuthContext'
import { getPortfolio } from '../../api'
import './Portfolio.css'

const fmt = (v) => parseFloat(v || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
const fmtPct = (v) => parseFloat(v || 0).toFixed(2)

export default function Portfolio({ sseEventCount }) {
    const { user } = useAuth()
    const [data, setData] = useState(null)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)

    const fetchPortfolio = async () => {
        setError(null)
        try {
            const json = await getPortfolio(user?.accountID)
            setData(json)
        } catch (err) {
            setError(err.message)
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        fetchPortfolio()
    }, [])

    // Re-fetch when SSE events arrive (e.g. nav-updated from admin)
    useEffect(() => {
        if (sseEventCount > 0) {
            fetchPortfolio()
        }
    }, [sseEventCount])

    if (loading && !data) {
        return (
            <div className="page pf-user-page">
                <div className="skeleton skeleton-heading" style={{ width: '40%', marginBottom: 24 }} />
                <div className="pf-summary-grid">
                    {[1, 2, 3, 4].map(i => <div key={i} className="skeleton" style={{ height: 80, borderRadius: 12 }} />)}
                </div>
            </div>
        )
    }

    if (error && !data) return <div className="page pf-user-page"><div className="home-error-banner">{error}</div></div>

    const totalPnl = parseFloat(data?.totalPnl || 0)
    const totalPnlPct = parseFloat(data?.totalPnlPercent || 0)
    const totalInvested = parseFloat(data?.totalInvested || 0)
    const totalCurrent = parseFloat(data?.totalCurrentValue || 0)
    const holdings = data?.holdings || []
    const isPositive = totalPnl >= 0

    return (
        <div className="page pf-user-page">
            <div className="pf-user-header">
                <h1 className="pf-user-title">My P/L</h1>
                <button className="pf-refresh-btn" onClick={fetchPortfolio}>↻ Refresh</button>
            </div>

            <div className={`pf-hero-card ${isPositive ? 'pf-hero-positive' : 'pf-hero-negative'}`}>
                <p className="pf-hero-label">Total Profit / Loss</p>
                <div className="pf-hero-value-wrap">
                    <span className="pf-hero-value">
                        {isPositive ? '+' : ''}${fmt(totalPnl)}
                    </span>
                    <span className="pf-hero-pct badge">
                        {isPositive ? '▲' : '▼'} {fmtPct(Math.abs(totalPnlPct))}%
                    </span>
                </div>
                <p className="pf-hero-meta">
                    Current Value: ${fmt(totalCurrent)} &middot; Invested: ${fmt(totalInvested)}
                </p>
            </div>

            <div className="pf-disclaimer">
                NAV is updated by admin. Portfolio refreshes automatically via SSE when NAV changes.
            </div>

            <div className="section">
                <h2 className="section-title">Your Holdings</h2>

                {holdings.length === 0 ? (
                    <div className="card text-center" style={{ padding: '40px 20px', color: 'var(--color-text-500)' }}>
                        No booked holdings. (Orders must reach BOOKED status via the TA callback to show P/L).
                    </div>
                ) : (
                    <div className="pf-user-list">
                        {holdings.map(h => {
                            const hPnl = parseFloat(h.pnl || 0)
                            const hPct = parseFloat(h.pnlPercent || 0)
                            const hPos = hPnl >= 0

                            return (
                                <div key={h.orderID} className="card pf-holding-card">
                                    <div className="pf-holding-header">
                                        <div>
                                            <div className="pf-holding-fund">{h.fundName}</div>
                                            <div className="pf-holding-id">{h.fundID}</div>
                                        </div>
                                        <div className={`pf-holding-pnl ${hPos ? 'text-green' : 'text-red'}`}>
                                            {hPos ? '+' : ''}${fmt(hPnl)}
                                            <div className="pf-holding-pnl-pct">{hPos ? '▲' : '▼'} {fmtPct(Math.abs(hPct))}%</div>
                                        </div>
                                    </div>

                                    <div className="pf-holding-divider" />

                                    <div className="pf-holding-stats">
                                        <div className="pf-holding-stat">
                                            <span className="pf-hs-label">Shares</span>
                                            <span className="pf-hs-val">{parseFloat(h.allocatedShares || 0).toFixed(4)}</span>
                                        </div>
                                        <div className="pf-holding-stat">
                                            <span className="pf-hs-label">Buy NAV</span>
                                            <span className="pf-hs-val">${fmt(h.buyNav)}</span>
                                        </div>
                                        <div className="pf-holding-stat">
                                            <span className="pf-hs-label">Current NAV</span>
                                            <span className="pf-hs-val font-mono">${fmt(h.currentNav)}</span>
                                        </div>
                                    </div>
                                </div>
                            )
                        })}
                    </div>
                )}
            </div>
        </div>
    )
}
