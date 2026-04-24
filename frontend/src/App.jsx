import { useState, useEffect } from 'react'
import { BrowserRouter, Navigate, Routes, Route } from 'react-router-dom'
import Layout from './components/Layout/Layout'
import ProtectedRoute from './components/ProtectedRoute/ProtectedRoute'
import { AuthProvider } from './context/AuthContext'
import Dashboard from './pages/Dashboard'
import Orders from './pages/Orders'
import BulkOrders from './pages/BulkOrders'
import Funds from './pages/Funds'
import Accounts from './pages/Accounts'
import Users from './pages/Users'
import NewOrder from './pages/NewOrder'
import { useSse } from './hooks/useSse'
import AggregateFund from './pages/AggregateFund'
import Reconciliation from './pages/Reconciliation'
import Portfolio from './pages/Portfolio'
import Login from './pages/Login'

function App() {
  const [sseConnected, setSseConnected] = useState(false)
  const [sseEventCount, setSseEventCount] = useState(0)

  const bumpEvent = () => setSseEventCount((c) => c + 1)

  const { connected } = useSse({
    'order-updated': bumpEvent,
    'bulk-order-updated': bumpEvent,
    'replay-completed': bumpEvent,
  })

  useEffect(() => {
    setSseConnected(connected)
  }, [connected])

  return (
    <BrowserRouter>
      <AuthProvider>
        <Layout sseConnected={sseConnected}>
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route path="/" element={<ProtectedRoute><Dashboard sseEventCount={sseEventCount} /></ProtectedRoute>} />
            <Route path="/orders" element={<ProtectedRoute><Orders sseEventCount={sseEventCount} /></ProtectedRoute>} />
            <Route path="/bulk-orders" element={<ProtectedRoute><BulkOrders sseEventCount={sseEventCount} /></ProtectedRoute>} />
            <Route path="/funds" element={<ProtectedRoute><Funds /></ProtectedRoute>} />
            <Route path="/accounts" element={<ProtectedRoute><Accounts /></ProtectedRoute>} />
            <Route path="/users" element={<ProtectedRoute><Users /></ProtectedRoute>} />
            <Route path="/orders/new" element={<ProtectedRoute><NewOrder /></ProtectedRoute>} />
            <Route path="/aggregate-funds" element={<ProtectedRoute><AggregateFund /></ProtectedRoute>} />
            <Route path="/reconciliation" element={<ProtectedRoute><Reconciliation /></ProtectedRoute>} />
            <Route path="/portfolio" element={<ProtectedRoute><Portfolio /></ProtectedRoute>} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </Layout>
      </AuthProvider>
    </BrowserRouter>
  )
}

export default App
