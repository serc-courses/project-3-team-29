import { useEffect, useRef, useState, useCallback } from 'react'
import { CONFIG } from '../constants/config'

export function useSse(handlers, token) {
  const [connected, setConnected] = useState(false)
  const esRef = useRef(null)
  const retryDelay = useRef(1000)
  const retryTimeoutRef = useRef(null)
  const handlersRef = useRef(handlers)
  handlersRef.current = handlers

  const connect = useCallback(() => {
    if (esRef.current) {
      esRef.current.close()
    }
    if (retryTimeoutRef.current) clearTimeout(retryTimeoutRef.current)

    if (!token) return

    const sseUrl = `${CONFIG.SSE_ENDPOINT}?access_token=${encodeURIComponent(token)}`
    const es = new EventSource(sseUrl)
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
      retryTimeoutRef.current = setTimeout(connect, delay)
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
  }, [token])

  useEffect(() => {
    connect()
    return () => {
      esRef.current?.close()
      esRef.current = null
      if (retryTimeoutRef.current) clearTimeout(retryTimeoutRef.current)
    }
  }, [connect])

  return { connected }
}
