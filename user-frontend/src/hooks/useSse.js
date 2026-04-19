import { useState, useEffect, useRef, useCallback } from 'react'
import { CONFIG } from '../constants/config'

export function useSse() {
  const [connected, setConnected] = useState(false)
  const [eventCount, setEventCount] = useState(0)
  const [alerts, setAlerts] = useState([])
  const eventSourceRef = useRef(null)
  const retryTimeoutRef = useRef(null)
  const retryDelayRef = useRef(1000)

  const pushAlert = useCallback((type) => {
    setAlerts(prev => [
      { type, timestamp: Date.now(), label: type === 'order-updated' ? 'Order status updated' : type === 'bulk-order-updated' ? 'Bulk order updated' : 'Replay completed' },
      ...prev.slice(0, 4),
    ])
  }, [])

  const connect = useCallback(() => {
    if (eventSourceRef.current) eventSourceRef.current.close()

    const baseUrl = import.meta.env.VITE_API_BASE_URL || ''
    const es = new EventSource(`${baseUrl}${CONFIG.SSE_ENDPOINT}`)
    eventSourceRef.current = es

    es.onopen = () => {
      setConnected(true)
      retryDelayRef.current = 1000
    }

    es.addEventListener('order-updated', () => {
      setEventCount(c => c + 1)
      pushAlert('order-updated')
    })
    es.addEventListener('bulk-order-updated', () => {
      setEventCount(c => c + 1)
      pushAlert('bulk-order-updated')
    })
    es.addEventListener('replay-completed', () => {
      setEventCount(c => c + 1)
      pushAlert('replay-completed')
    })

    es.onerror = () => {
      setConnected(false)
      es.close()
      const delay = Math.min(retryDelayRef.current, 30000)
      retryTimeoutRef.current = setTimeout(() => {
        retryDelayRef.current = delay * 2
        connect()
      }, delay)
    }
  }, [pushAlert])

  useEffect(() => {
    connect()
    return () => {
      if (eventSourceRef.current) eventSourceRef.current.close()
      if (retryTimeoutRef.current) clearTimeout(retryTimeoutRef.current)
    }
  }, [connect])

  return { connected, eventCount, alerts }
}
