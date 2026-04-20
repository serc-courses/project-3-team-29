import { useState } from 'react'
import './DataTable.css'

export default function DataTable({
  columns = [],
  data = [],
  emptyMessage = 'No data available',
  loading = false,
  onRowClick,
}) {
  const [sortKey, setSortKey] = useState(null)
  const [sortDir, setSortDir] = useState('asc')

  const handleSort = (col) => {
    if (!col.sortable) return
    if (sortKey === col.key) {
      setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'))
    } else {
      setSortKey(col.key)
      setSortDir('asc')
    }
  }

  const sorted = [...data].sort((a, b) => {
    if (!sortKey) return 0
    const av = a[sortKey]
    const bv = b[sortKey]
    if (av == null) return 1
    if (bv == null) return -1
    const cmp = av < bv ? -1 : av > bv ? 1 : 0
    return sortDir === 'asc' ? cmp : -cmp
  })

  return (
    <div className="data-table-wrapper">
      {loading ? (
        <div className="data-table-loading">
          <span className="spinner" />
          Loading...
        </div>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              {columns.map((col) => (
                <th
                  key={col.key}
                  className={[
                    col.sortable !== false ? 'sortable' : '',
                    col.align === 'right' ? 'align-right' : '',
                    col.align === 'center' ? 'align-center' : '',
                  ].join(' ')}
                  onClick={() => col.sortable !== false && handleSort(col)}
                >
                  {col.label}
                  {col.sortable !== false && sortKey === col.key && (
                    <span className="sort-indicator">
                      {sortDir === 'asc' ? '↑' : '↓'}
                    </span>
                  )}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {sorted.length === 0 ? (
              <tr>
                <td colSpan={columns.length} className="data-table-empty">
                  {emptyMessage}
                </td>
              </tr>
            ) : (
              sorted.map((row, i) => (
                <tr
                  key={row.id || i}
                  className={onRowClick ? 'clickable' : ''}
                  onClick={() => onRowClick?.(row)}
                >
                  {columns.map((col) => (
                    <td
                      key={col.key}
                      className={[
                        col.align === 'right' ? 'align-right' : '',
                        col.align === 'center' ? 'align-center' : '',
                      ].join(' ')}
                    >
                      {col.render ? col.render(row[col.key], row) : (row[col.key] ?? '—')}
                    </td>
                  ))}
                </tr>
              ))
            )}
          </tbody>
        </table>
      )}
    </div>
  )
}
