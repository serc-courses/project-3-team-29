import { STATUS_LABEL } from '../../constants/orderStatus'
import { STATUS_COLORS } from '../../constants/statusColors'
import './StatusPill.css'

export default function StatusPill({ status }) {
  const label = STATUS_LABEL[status] || status
  const colors = STATUS_COLORS[status] || { bg: '#F1F5F9', text: '#64748B', border: '#E2E8F0' }

  return (
    <span
      className="status-pill"
      style={{
        background: colors.bg,
        color: colors.text,
        borderColor: colors.border,
      }}
    >
      <span
        className="status-dot"
        style={{ background: colors.text }}
      />
      {label}
    </span>
  )
}
