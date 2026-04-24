import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { useAuth } from './context/AuthContext'
import ProtectedRoute from './components/ProtectedRoute/ProtectedRoute'
import BottomNav from './components/BottomNav/BottomNav'
import Sidebar from './components/Sidebar/Sidebar'
import { useSse } from './hooks/useSse'

import Login from './pages/Login/Login'
import Home from './pages/Home/Home'
import Orders from './pages/Orders/Orders'
import OrderDetail from './pages/OrderDetail/OrderDetail'
import PlaceOrder from './pages/PlaceOrder/PlaceOrder'
import Transactions from './pages/Transactions/Transactions'
import Funds from './pages/Funds/Funds'
import FundDetail from './pages/FundDetail/FundDetail'
import Account from './pages/Account/Account'
import Portfolio from './pages/Portfolio/Portfolio'
import AdvisorHome from './pages/AdvisorHome/AdvisorHome'
import AdvisorClients from './pages/AdvisorClients/AdvisorClients'
import AdvisorClientDetail from './pages/AdvisorClientDetail/AdvisorClientDetail'
import AdvisorBasketOrder from './pages/AdvisorBasketOrder/AdvisorBasketOrder'
import AdvisorBasketReview from './pages/AdvisorBasketReview/AdvisorBasketReview'
import AdvisorActivity from './pages/AdvisorActivity/AdvisorActivity'

import './App.css'

function AppContent() {
  const { isAdvisor, user } = useAuth()
  const advisorId = user?.advisorID
  // Pass the stored token so useSse reconnects automatically after login
  const token = user ? localStorage.getItem('oms_token') : null
  const { connected, eventCount } = useSse(token)

  return (
    <div className={`app-shell${isAdvisor ? ' app-shell--advisor' : ''}`}>
      {isAdvisor && <Sidebar />}

      <div className="connection-indicator">
        <span className={`connection-dot${connected ? ' connected' : ''}`} />
      </div>

      <main className="app-main">
        <Routes>
          <Route path="/login" element={<Login />} />

          {/* Investor routes */}
          <Route path="/" element={<ProtectedRoute><Home sseEventCount={eventCount} /></ProtectedRoute>} />
          <Route path="/orders" element={<ProtectedRoute><Orders sseEventCount={eventCount} /></ProtectedRoute>} />
          <Route path="/orders/new" element={<ProtectedRoute><PlaceOrder /></ProtectedRoute>} />
          <Route path="/orders/:orderId" element={<ProtectedRoute><OrderDetail sseEventCount={eventCount} /></ProtectedRoute>} />
          <Route path="/transactions" element={<ProtectedRoute><Transactions /></ProtectedRoute>} />
          <Route path="/funds" element={<ProtectedRoute><Funds /></ProtectedRoute>} />
          <Route path="/funds/:fundId" element={<ProtectedRoute><FundDetail /></ProtectedRoute>} />
          <Route path="/portfolio" element={<ProtectedRoute><Portfolio sseEventCount={eventCount} /></ProtectedRoute>} />
          <Route path="/account" element={<ProtectedRoute><Account /></ProtectedRoute>} />

          {/* Advisor routes */}
          <Route path="/advisor" element={<ProtectedRoute><AdvisorHome sseEventCount={eventCount} advisorId={advisorId} /></ProtectedRoute>} />
          <Route path="/advisor/clients" element={<ProtectedRoute><AdvisorClients advisorId={advisorId} sseEventCount={eventCount} /></ProtectedRoute>} />
          <Route path="/advisor/clients/:accountId" element={<ProtectedRoute><AdvisorClientDetail advisorId={advisorId} sseEventCount={eventCount} /></ProtectedRoute>} />
          <Route path="/advisor/orders/new" element={<ProtectedRoute><AdvisorBasketOrder advisorId={advisorId} /></ProtectedRoute>} />
          <Route path="/advisor/orders/review" element={<ProtectedRoute><AdvisorBasketReview /></ProtectedRoute>} />
          <Route path="/advisor/activity" element={<ProtectedRoute><AdvisorActivity advisorId={advisorId} sseEventCount={eventCount} /></ProtectedRoute>} />

          <Route path="*" element={<Navigate to={isAdvisor ? '/advisor' : '/'} replace />} />
        </Routes>
      </main>

      <BottomNav isAdvisor={isAdvisor} />
    </div>
  )
}

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppContent />
      </AuthProvider>
    </BrowserRouter>
  )
}

export default App
