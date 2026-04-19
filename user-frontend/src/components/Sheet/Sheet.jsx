import { useEffect, useRef } from 'react'
import './Sheet.css'

export default function Sheet({ isOpen, onClose, title, children }) {
  const closeRef = useRef(null)

  useEffect(() => {
    if (isOpen && closeRef.current) {
      closeRef.current.focus()
    }
  }, [isOpen])

  if (!isOpen) return null

  return (
    <div className="sheet-overlay" onClick={onClose}>
      <div
        className="sheet"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-label={title}
      >
        <div className="sheet-handle" />
        <div className="sheet-header">
          <span className="sheet-title">{title}</span>
          <button
            ref={closeRef}
            className="sheet-close"
            onClick={onClose}
            aria-label="Close"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
        <div className="sheet-content">{children}</div>
      </div>
    </div>
  )
}
