import './Layout.css'
import { useLocation } from 'react-router-dom'
import Sidebar from './Sidebar'
import Header from './Header'

export default function Layout({ children, sseConnected }) {
  const location = useLocation()
  const isLoginPage = location.pathname === '/login'

  if (isLoginPage) {
    return children
  }

  return (
    <div className="layout">
      <Sidebar />
      <div className="main-content">
        <Header sseConnected={sseConnected} />
        <main className="page-content">
          {children}
        </main>
      </div>
    </div>
  )
}
