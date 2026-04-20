export default function EmptyState({ icon, title, message, action, onAction }) {
  return (
    <div className="empty-state">
      {icon && <div className="empty-state-icon">{icon}</div>}
      <p className="empty-state-title">{title}</p>
      {message && <p className="empty-state-text">{message}</p>}
      {action && onAction && (
        <button className="btn btn-primary" onClick={onAction}>
          {action}
        </button>
      )}
    </div>
  )
}
