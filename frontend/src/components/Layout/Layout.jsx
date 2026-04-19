import './Layout.css'
import Sidebar from './Sidebar'
import Header from './Header'

export default function Layout({ children, sseConnected }) {
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
