import './FilterBar.css'

export default function FilterBar({ filters = [], searchPlaceholder, searchValue, onSearchChange }) {
  return (
    <div className="filter-bar">
      {filters.map((f) => (
        <div key={f.key} className="filter-bar-group">
          <span className="filter-bar-label">{f.label}</span>
          <select value={f.value} onChange={(e) => f.onChange(e.target.value)}>
            <option value="">All</option>
            {f.options.map((opt) => (
              <option key={opt.value} value={opt.value}>{opt.label}</option>
            ))}
          </select>
        </div>
      ))}
      {onSearchChange && (
        <div className="filter-bar-search" style={{ marginLeft: 'auto' }}>
          <span className="filter-bar-search-icon">
            <svg width="13" height="13" viewBox="0 0 13 13" fill="none" aria-hidden="true">
              <circle cx="5.5" cy="5.5" r="4" stroke="currentColor" strokeWidth="1.4"/>
              <line x1="8.5" y1="8.5" x2="12" y2="12" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round"/>
            </svg>
          </span>
          <input
            type="text"
            placeholder={searchPlaceholder || 'Search...'}
            value={searchValue}
            onChange={(e) => onSearchChange(e.target.value)}
          />
        </div>
      )}
    </div>
  )
}
