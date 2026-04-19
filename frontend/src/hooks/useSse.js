import { useEffect, useRef, useState, useCallback } from 'react'
import { CONFIG } from '../constants/config'

export function useSse(handlers) {
  const [connected, setConnected] = useState(false)
  const esRef = useRef(null)
  const retryDelay = useRef(1000)
  const handlersRef = useRef(handlers)
  handlersRef.current = handlers

  const connect = useCallback(() => {
    if (esRef.current) {
      esRef.current.close()
    }

    const es = new EventSource(CONFIG.SSE_ENDPOINT)
    esRef.current = es

    es.onopen = () => {
      setConnected(true)
      retryDelay.current = 1000
    }

    es.onerror = () => {
      setConnected(false)
      es.close()
      esRef.current = null
      const delay = Math.min(retryDelay.current, 30000)
      retryDelay.current = Math.min(delay * 2, 30000)
      setTimeout(connect, delay)
    }

    Object.keys(handlersRef.current).forEach((eventType) => {
      es.addEventListener(eventType, (e) => {
        try {
          const data = JSON.parse(e.data)
          handlersRef.current[eventType]?.(data)
        } catch {
          handlersRef.current[eventType]?.()
        }
      })
    })
  }, [])

  useEffect(() => {
    connect()
    return () => {
      esRef.current?.close()
      esRef.current = null
    }
  }, [connect])

  return { connected }
}
