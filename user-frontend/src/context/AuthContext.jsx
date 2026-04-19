import { createContext, useContext, useState, useCallback, useEffect } from 'react'
import { loginApi, getMeApi, logoutApi } from '../api/authApi'

const TOKEN_KEY = 'oms_token'
const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [bootstrapping, setBootstrapping] = useState(true)

  // Restore session from stored token on mount
  useEffect(() => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (!token) { setBootstrapping(false); return }
    getMeApi()
      .then(setUser)
      .catch(() => { localStorage.removeItem(TOKEN_KEY); setUser(null) })
      .finally(() => setBootstrapping(false))
  }, [])

  // Handle 401 events from client.js
  useEffect(() => {
    const handler = () => setUser(null)
    window.addEventListener('auth:expired', handler)
    return () => window.removeEventListener('auth:expired', handler)
  }, [])

  const login = useCallback(async (username, password) => {
    const data = await loginApi(username, password)
    localStorage.setItem(TOKEN_KEY, data.token)
    const { token: _t, ...userFields } = data
    setUser(userFields)
    return userFields
  }, [])

  const logout = useCallback(async () => {
    try { await logoutApi() } catch {}
    localStorage.removeItem(TOKEN_KEY)
    setUser(null)
  }, [])

  return (
    <AuthContext.Provider value={{
      user,
      bootstrapping,
      isAuthenticated: !!user,
      isAdvisor: user?.role === 'ADVISOR',
      login,
      logout,
    }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
