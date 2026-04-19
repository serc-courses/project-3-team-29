import { useState, useMemo } from 'react'
import { getFunds } from '../../api/portfolioApi'
import { useFetch } from '../../hooks/useFetch'
import FundCard from '../../components/FundCard/FundCard'
import EmptyState from '../../components/EmptyState/EmptyState'
import './Funds.css'

function SkeletonFunds() {
  return (
    <div className="page funds-page">
      <div className="skeleton skeleton-heading" style={{ width: '50%', marginBottom: 4 }} />
      <div className="skeleton skeleton-text-sm" style={{ width: '60%', marginBottom: 20 }} />
      <div className="skeleton" style={{ height: 44, borderRadius: 8, marginBottom: 16 }} />
      {[1,2,3,4,5].map(i => <div key={i} className="skeleton skeleton-card" style={{ height: 100, marginBottom: 8 }} />)}
    </div>
  )
}

function NoFundsIcon() {
  return (
    <svg width="64" height="64" viewBox="0 0 64 64" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="8 40 22 26 32 36 46 20 56 28"/>
      <line x1="8" y1="52" x2="56" y2="52"/>
    </svg>
  )
}

export default function Funds() {
  const [search, setSearch] = useState('')
  const { data, loading } = useFetch(getFunds, [])
  const funds = data || []

  const filtered = useMemo(() => {
    if (!search.trim()) return [...funds].sort((a, b) => a.fundName?.localeCompare(b.fundName))
    const q = search.toLowerCase()
    return funds
      .filter(f => f.fundName?.toLowerCase().includes(q) || f.fundID?.toLowerCase().includes(q))
      .sort((a, b) => a.fundName?.localeCompare(b.fundName))
  }, [funds, search])

  if (loading) return <SkeletonFunds />

  return (
    <div className="page funds-page">
      <h1 className="page-title">Explore Funds</h1>
      <p className="page-subtitle">Browse mutual funds</p>

      <div className="funds-search">
        <span className="funds-search-icon">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
        </span>
        <input
          type="search"
          className="funds-search-input"
          placeholder="Search by name or fund ID"
          value={search}
          onChange={e => setSearch(e.target.value)}
        />
        {search && (
          <button className="funds-search-clear" onClick={() => setSearch('')} aria-label="Clear">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        )}
      </div>

      <p className="funds-count">
        {search
          ? `${filtered.length} fund${filtered.length !== 1 ? 's' : ''} matching "${search}"`
          : `${filtered.length} fund${filtered.length !== 1 ? 's' : ''} available`}
      </p>

      {filtered.length === 0 ? (
        <EmptyState
          icon={<NoFundsIcon />}
          title={search ? `No funds matching "${search}"` : 'No funds available'}
        />
      ) : (
        <div className="funds-list">
          {filtered.map(fund => (
            <FundCard key={fund.fundID} fund={fund} />
          ))}
        </div>
      )}
    </div>
  )
}
