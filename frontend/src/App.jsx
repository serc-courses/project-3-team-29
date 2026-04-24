import { useState } from 'react'
import { BrowserRouter, Navigate, Routes, Route } from 'react-router-dom'
import Layout from './components/Layout/Layout'
import ProtectedRoute from './components/ProtectedRoute/ProtectedRoute'
import { AuthProvider, useAuth } from './context/AuthContext'
import Dashboard from './pages/Dashboard'
import Orders from './pages/Orders'
import BulkOrders from './pages/BulkOrders'
import Funds from './pages/Funds'
import Accounts from './pages/Accounts'
import Users from './pages/Users'
import NewOrder from './pages/NewOrder'
import { useSse } from './hooks/useSse'
import Reconciliation from './pages/Reconciliation'
import Login from './pages/Login'

function AppContent() {
  const { user } = useAuth()
  const [sseEventCount, setSseEventCount] = useState(0)

  const bumpEvent = () => setSseEventCount((c) => c + 1)

  // Pass the token so useSse reconnects automatically after login
  const token = user ? localStorage.getItem('oms_token') : null
  const { connected } = useSse({
    'order-updated': bumpEvent,
    'bulk-order-updated': bumpEvent,
    'replay-completed': bumpEvent,
  }, token)

  return (
    <Layout sseConnected={connected}>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/" element={<ProtectedRoute><Dashboard sseEventCount={sseEventCount} /></ProtectedRoute>} />
        <Route path="/orders" element={<ProtectedRoute><Orders sseEventCount={sseEventCount} /></ProtectedRoute>} />
        <Route path="/bulk-orders" element={<ProtectedRoute><BulkOrders sseEventCount={sseEventCount} /></ProtectedRoute>} />
        <Route path="/funds" element={<ProtectedRoute><Funds /></ProtectedRoute>} />
        <Route path="/accounts" element={<ProtectedRoute><Accounts /></ProtectedRoute>} />
        <Route path="/users" element={<ProtectedRoute><Users /></ProtectedRoute>} />
        <Route path="/orders/new" element={<ProtectedRoute><NewOrder /></ProtectedRoute>} />
        <Route path="/reconciliation" element={<ProtectedRoute><Reconciliation /></ProtectedRoute>} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Layout>
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
