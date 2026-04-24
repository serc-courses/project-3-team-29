import { useState, useEffect } from 'react'
import './Reconciliation.css'
import { api } from '../api/client'
import { exportToCsv } from '../utils/exportCsv'

export default function Reconciliation() {
    const [breaks, setBreaks] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)
    const [showAll, setShowAll] = useState(false)
    const [resolving, setResolving] = useState(null)
    const [confirmModal, setConfirmModal] = useState(null)

    const fetchBreaks = async () => {
        setLoading(true)
        setError(null)
        try {
            const url = showAll
                ? '/view/reconciliation?all=true'
                : '/view/reconciliation'
            const data = await api.get(url)
            setBreaks(data)
        } catch (err) {
            setError(err.message)
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        fetchBreaks()
    }, [showAll])

    const handleResolve = async (breakId, action) => {
        setResolving(breakId)
        setConfirmModal(null)
        try {
            await api.post('/view/reconciliation/resolve', { breakId, action })
            await fetchBreaks()
        } catch (err) {
            alert('Error: ' + err.message)
        } finally {
            setResolving(null)
        }
    }

    const ACTION_CONFIG = {
        ACCEPT: {
            title: '✓ Accept & Book',
            description: (m) => (
                <>
                    <p>
                        You are <strong>accepting the Transfer Agent's values</strong> for bulk order{' '}
                        <strong>{m.bulkOrderId}</strong>.
                    </p>
                    <p>
                        This will advance all <strong>frozen CONTRACTED orders → BOOKED</strong> using
                        the TA's provided NAV and share allocation. The dollar difference will be absorbed.
                    </p>
                    <div className="recon-modal-detail">
                        <div>Expected: <strong>₹{m.expected.toLocaleString(undefined, { minimumFractionDigits: 2 })}</strong></div>
                        <div>Received: <strong>₹{m.received.toLocaleString(undefined, { minimumFractionDigits: 2 })}</strong></div>
                        <div className="diff-value">
                            Difference: ₹{m.diff.toLocaleString(undefined, { minimumFractionDigits: 2 })} ({m.pct}%)
                        </div>
                    </div>
                    <p className="recon-modal-warn">
                        ⚠ Common for legitimate TA fees or rounding. Verify with the Transfer Agent before accepting.
                    </p>
                </>
            ),
            confirmLabel: '✓ Confirm Accept & Book',
            btnClass: 'recon-btn-accept',
        },
        RETRANSMIT: {
            title: '↻ Retransmit',
            description: (m) => (
                <>
                    <p>
                        You are <strong>disputing the Transfer Agent's contract</strong> for bulk order{' '}
                        <strong>{m.bulkOrderId}</strong>.
                    </p>
                    <p>
                        This will <strong>roll all orders back to TRANSMITTED</strong> status and clear the
                        invalid contract data (NAV, shares, contract ref). The bulk order returns to TRANSMITTED.
                    </p>
                    <p>
                        After this, you can submit a <strong>new corrected contract</strong> via the
                        "Simulate TA Contract" button on the Bulk Orders page with the correct values from the TA.
                    </p>
                    <p className="recon-modal-info">
                        💡 Use this when the TA acknowledges their error and will re-issue a corrected contract.
                    </p>
                </>
            ),
            confirmLabel: '↻ Confirm Retransmit',
            btnClass: 'recon-btn-retransmit',
        },
        CANCEL: {
            title: '✕ Cancel Orders',
            description: (m) => (
                <>
                    <p>
                        You are <strong>cancelling all orders</strong> for bulk order{' '}
                        <strong>{m.bulkOrderId}</strong>.
                    </p>
                    <p>
                        All constituent orders will be marked as <strong>ERRORED</strong> and permanently
                        abandoned. This cannot be undone.
                    </p>
                    <p className="recon-modal-danger">
                        🚨 Only use this for unresolvable breaks — suspected fraud, severe data corruption,
                        or when both you and the TA agree the transaction should be voided.
                    </p>
                </>
            ),
            confirmLabel: '✕ Confirm Cancel',
            btnClass: 'recon-btn-cancel-action',
        },
    }

    if (loading) {
        return (
            <div className="recon-loading">
                <span className="spinner" />
                Loading reconciliation breaks...
            </div>
        )
    }

    if (error) return <div className="recon-error">{error}</div>

    return (
        <div className="recon-page">
            <div className="recon-header">
                <h2>Reconciliation Breaks</h2>
                <div className="recon-controls">
                    <label className="recon-toggle">
                        <input
                            type="checkbox"
                            checked={showAll}
                            onChange={(e) => setShowAll(e.target.checked)}
                        />
                        Show resolved
                    </label>
                    <button className="btn btn-primary" onClick={() => exportToCsv('reconciliation_breaks.csv', breaks)} disabled={breaks.length === 0}>
                        ⤓ Export CSV
                    </button>
                    <button className="btn btn-outline" onClick={fetchBreaks}>
                        ↻ Refresh
                    </button>
                </div>
            </div>

            <div className="recon-count">
                Showing {breaks.length} break{breaks.length !== 1 ? 's' : ''}
            </div>

            {breaks.length === 0 ? (
                <div className="recon-empty card">
                    <div className="recon-empty-icon">✓</div>
                    <div className="recon-empty-text">No reconciliation breaks found</div>
                    <div className="recon-empty-sub">All contract callbacks are within tolerance</div>
                </div>
            ) : (
                <div className="card" style={{ padding: 0 }}>
                    <div style={{ overflowX: 'auto' }}>
                        <table className="data-table">
                            <thead>
                                <tr>
                                    <th>Break ID</th>
                                    <th>Bulk Order</th>
                                    <th>Type</th>
                                    <th style={{ textAlign: 'right' }}>Expected (₹)</th>
                                    <th style={{ textAlign: 'right' }}>Received (₹)</th>
                                    <th style={{ textAlign: 'right' }}>Difference (₹)</th>
                                    <th>Detected At</th>
                                    <th style={{ textAlign: 'center' }}>Status</th>
                                    <th style={{ textAlign: 'center' }}>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {breaks.map((b) => {
                                    const expected = parseFloat(b.expectedValue) || 0
                                    const received = parseFloat(b.receivedValue) || 0
                                    const diff = Math.abs(expected - received)
                                    const pct = expected > 0 ? ((diff / expected) * 100).toFixed(4) : '0'
                                    const isResolving = resolving === b.breakId
                                    const modalData = { breakId: b.breakId, expected, received, diff, pct, bulkOrderId: b.bulkOrderId }

                                    return (
                                        <tr key={b.breakId} className={b.escalated && !b.resolved ? 'escalated-row' : ''}>
                                            <td>
                                                <span className="font-mono text-sm">{b.breakId}</span>
                                            </td>
                                            <td>
                                                <span className="font-mono text-sm">{b.bulkOrderId}</span>
                                            </td>
                                            <td>
                                                <span className={`break-type break-type-${b.breakType?.toLowerCase()}`}>
                                                    {b.breakType}
                                                </span>
                                            </td>
                                            <td style={{ textAlign: 'right' }}>
                                                ₹{expected.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                                            </td>
                                            <td style={{ textAlign: 'right' }}>
                                                ₹{received.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                                            </td>
                                            <td style={{ textAlign: 'right' }}>
                                                <span className="diff-value">
                                                    ₹{diff.toLocaleString(undefined, { minimumFractionDigits: 2 })} ({pct}%)
                                                </span>
                                            </td>
                                            <td>
                                                {b.detectedAt
                                                    ? new Date(b.detectedAt).toLocaleString()
                                                    : '—'}
                                            </td>
                                            <td style={{ textAlign: 'center' }}>
                                                {b.resolved ? (
                                                    <span className="recon-badge recon-badge-resolved">Resolved</span>
                                                ) : b.escalated ? (
                                                    <span className="recon-badge recon-badge-escalated">⚠ Escalated</span>
                                                ) : (
                                                    <span className="recon-badge recon-badge-open">Open</span>
                                                )}
                                            </td>
                                            <td style={{ textAlign: 'center' }}>
                                                {b.resolved ? (
                                                    <span className="recon-resolved-label">—</span>
                                                ) : (
                                                    <div className="recon-actions">
                                                        <button
                                                            className="recon-btn recon-btn-accept"
                                                            disabled={isResolving}
                                                            onClick={() => setConfirmModal({ ...modalData, action: 'ACCEPT' })}
                                                            title="Accept TA's values and book orders"
                                                        >
                                                            {isResolving ? '...' : '✓ Accept'}
                                                        </button>
                                                        <button
                                                            className="recon-btn recon-btn-retransmit"
                                                            disabled={isResolving}
                                                            onClick={() => setConfirmModal({ ...modalData, action: 'RETRANSMIT' })}
                                                            title="Roll back to TRANSMITTED for a new corrected contract"
                                                        >
                                                            {isResolving ? '...' : '↻ Retransmit'}
                                                        </button>
                                                        <button
                                                            className="recon-btn recon-btn-cancel-action"
                                                            disabled={isResolving}
                                                            onClick={() => setConfirmModal({ ...modalData, action: 'CANCEL' })}
                                                            title="Cancel all orders — mark as ERRORED"
                                                        >
                                                            {isResolving ? '...' : '✕ Cancel'}
                                                        </button>
                                                    </div>
                                                )}
                                            </td>
                                        </tr>
                                    )
                                })}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}

            {/* Confirmation Modal */}
            {confirmModal && (
                <div className="recon-modal-overlay" onClick={() => setConfirmModal(null)}>
                    <div className="recon-modal" onClick={(e) => e.stopPropagation()}>
                        <h3 className="recon-modal-title">
                            {ACTION_CONFIG[confirmModal.action].title}
                        </h3>
                        <div className="recon-modal-body">
                            {ACTION_CONFIG[confirmModal.action].description(confirmModal)}
                        </div>
                        <div className="recon-modal-actions">
                            <button
                                className="recon-btn recon-btn-dismiss"
                                onClick={() => setConfirmModal(null)}
                            >
                                Go Back
                            </button>
                            <button
                                className={`recon-btn ${ACTION_CONFIG[confirmModal.action].btnClass}`}
                                onClick={() => handleResolve(confirmModal.breakId, confirmModal.action)}
                            >
                                {ACTION_CONFIG[confirmModal.action].confirmLabel}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    )
}
