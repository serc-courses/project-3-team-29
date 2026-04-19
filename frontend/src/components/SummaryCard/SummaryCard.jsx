import './SummaryCard.css'

export default function SummaryCard({ title, value, subtitle, icon, accentColor }) {
  return (
    <div className="summary-card">
      {accentColor && (
        <div className="summary-card-accent" style={{ background: accentColor }} />
      )}
      <div className="summary-card-header">
        <span className="summary-card-title">{title}</span>
        {icon && <span className="summary-card-icon">{icon}</span>}
      </div>
      <div className="summary-card-value font-mono">{value ?? '—'}</div>
      {subtitle && <div className="summary-card-subtitle">{subtitle}</div>}
    </div>
  )
}
