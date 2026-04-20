import { useEffect } from 'react'
import './Toast.css'

export default function Toast({ message, type = 'success', visible, onClose }) {
  useEffect(() => {
    if (!visible) return
    const timer = setTimeout(onClose, 4000)
    return () => clearTimeout(timer)
  }, [visible, onClose])

  if (!visible) return null

  return (
    <div className="toast-container">
      <div className={`toast-item ${type}`}>
        <span>{message}</span>
        <button className="toast-close" onClick={onClose}>×</button>
      </div>
    </div>
  )
}
