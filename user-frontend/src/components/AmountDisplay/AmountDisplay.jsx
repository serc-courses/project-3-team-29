import { formatCurrency } from '../../utils/formatters'
import './AmountDisplay.css'

export default function AmountDisplay({ amount, size = 'md', color }) {
  const formatted = formatCurrency(amount)
  const parts = formatted !== '—' ? formatted.split(/(?<=₹)/) : ['₹', '—']

  return (
    <span
      className={`amount-display amount-display--${size}`}
      style={color ? { color } : undefined}
    >
      {formatted}
    </span>
  )
}
