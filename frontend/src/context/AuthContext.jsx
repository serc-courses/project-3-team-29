import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { getMeApi, loginApi, logoutApi } from '../api/authApi'

const TOKEN_KEY = 'oms_token'
const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [bootstrapping, setBootstrapping] = useState(true)

  useEffect(() => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (!token) {
      setBootstrapping(false)
      return
    }

    getMeApi()
      .then(setUser)
      .catch(() => {
        localStorage.removeItem(TOKEN_KEY)
        setUser(null)
      })
      .finally(() => setBootstrapping(false))
  }, [])

  useEffect(() => {
    const handleExpired = () => setUser(null)
    window.addEventListener('auth:expired', handleExpired)
    return () => window.removeEventListener('auth:expired', handleExpired)
  }, [])

  const login = useCallback(async (username, password) => {
    const data = await loginApi(username, password)
    localStorage.setItem(TOKEN_KEY, data.token)
    const { token: _token, ...userFields } = data
    setUser(userFields)
    return userFields
  }, [])

  const logout = useCallback(async () => {
    try {
      await logoutApi()
    } catch {
      // Ignore logout errors and clear client state anyway.
    }
    localStorage.removeItem(TOKEN_KEY)
    setUser(null)
  }, [])

  return (
    <AuthContext.Provider
      value={{
        user,
        bootstrapping,
        isAuthenticated: !!user,
        login,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used within AuthProvider')
  return context
}
