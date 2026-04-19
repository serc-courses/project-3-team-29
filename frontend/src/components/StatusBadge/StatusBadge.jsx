import './StatusBadge.css'
import { ORDER_STATUS_COLORS, BULK_STATUS_COLORS } from '../../constants/statusColors'

const FALLBACK = { bg: '#F3F4F6', text: '#6B7280', border: '#D1D5DB' }

export default function StatusBadge({ status, variant = 'order' }) {
  const colorMap = variant === 'bulk' ? BULK_STATUS_COLORS : ORDER_STATUS_COLORS
  const colors = colorMap[status] || FALLBACK

  return (
    <span
      className="status-badge"
      style={{
        backgroundColor: colors.bg,
        color: colors.text,
        borderColor: colors.border,
      }}
    >
      {status || '—'}
    </span>
  )
}
