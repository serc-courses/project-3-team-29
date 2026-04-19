import { useState, useEffect } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import './Login.css'

function LogoIcon() {
  return (
    <svg width="40" height="40" viewBox="0 0 40 40" fill="none">
      <rect width="40" height="40" rx="12" fill="#059669"/>
      <polyline points="30 12 21 21 15 15 8 22" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"/>
      <polyline points="24 12 30 12 30 18" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"/>
    </svg>
  )
}

const TEST_CREDENTIALS = [
  { label: 'Investor',  username: 'john.miller',  password: 'invest123', hint: '(ACCT00001)' },
  { label: 'Investor',  username: 'emma.johnson', password: 'invest123', hint: '(ACCT00002)' },
  { label: 'Advisor',   username: 'advisor1',     password: 'advise123', hint: '(ADV001)' },
  { label: 'Advisor',   username: 'advisor2',     password: 'advise123', hint: '(ADV002)' },
]

export default function Login() {
  const navigate = useNavigate()
  const location = useLocation()
  const { login, isAuthenticated, isAdvisor } = useAuth()

  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [showHints, setShowHints] = useState(false)

  const from = location.state?.from?.pathname || null

  useEffect(() => {
    if (isAuthenticated) {
      const dest = from || (isAdvisor ? '/advisor' : '/')
      navigate(dest, { replace: true })
    }
  }, [isAuthenticated, isAdvisor, navigate, from])

  async function handleSubmit(e) {
    e.preventDefault()
    if (!username.trim() || !password.trim()) {
      setError('Username and password are required')
      return
    }
    setLoading(true)
    setError('')
    try {
      const user = await login(username.trim(), password)
      const dest = from || (user.role === 'ADVISOR' ? '/advisor' : '/')
      navigate(dest, { replace: true })
    } catch (err) {
      setError(err.message || 'Invalid username or password')
    } finally {
      setLoading(false)
    }
  }

  function fillCredential(cred) {
    setUsername(cred.username)
    setPassword(cred.password)
    setError('')
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="login-header">
          <LogoIcon />
          <h1 className="login-title">MF-OMS Invest</h1>
          <p className="login-subtitle">Sign in to your account</p>
        </div>

        <form onSubmit={handleSubmit} className="login-form">
          <div className="input-group">
            <label className="input-label" htmlFor="username-input">Username</label>
            <input
              id="username-input"
              type="text"
              className={`input${error ? ' input-error' : ''}`}
              placeholder="e.g. john.miller"
              value={username}
              onChange={e => { setUsername(e.target.value); setError('') }}
              autoComplete="username"
              autoCapitalize="none"
              spellCheck={false}
              disabled={loading}
            />
          </div>

          <div className="input-group">
            <label className="input-label" htmlFor="password-input">Password</label>
            <input
              id="password-input"
              type="password"
              className={`input${error ? ' input-error' : ''}`}
              placeholder="Enter password"
              value={password}
              onChange={e => { setPassword(e.target.value); setError('') }}
              autoComplete="current-password"
              disabled={loading}
            />
            {error && <p className="input-error-text">{error}</p>}
          </div>

          <button
            type="submit"
            className="btn btn-primary btn-full btn-lg login-btn"
            disabled={loading}
          >
            {loading ? <><span className="spinner" /> Signing in...</> : 'Sign In'}
          </button>
        </form>

        {/* Test credentials section */}
        <div className="login-hints">
          <button
            type="button"
            className="login-hints-toggle"
            onClick={() => setShowHints(v => !v)}
          >
            {showHints ? '▲' : '▼'} Demo credentials
          </button>
          {showHints && (
            <div className="login-hints-list">
              {TEST_CREDENTIALS.map(c => (
                <button
                  key={c.username}
                  type="button"
                  className="login-hint-row"
                  onClick={() => fillCredential(c)}
                >
                  <span className={`login-hint-role${c.label === 'Advisor' ? ' advisor' : ''}`}>{c.label}</span>
                  <span className="login-hint-user">{c.username}</span>
                  <span className="login-hint-acct">{c.hint}</span>
                </button>
              ))}
            </div>
          )}
        </div>

        <p className="login-footer">Mutual Fund Order Management System</p>
      </div>
    </div>
  )
}
