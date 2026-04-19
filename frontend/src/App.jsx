import { useState, useEffect } from 'react'
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import Layout from './components/Layout/Layout'
import Dashboard from './pages/Dashboard'
import Orders from './pages/Orders'
import BulkOrders from './pages/BulkOrders'
import Funds from './pages/Funds'
import Accounts from './pages/Accounts'
import NewOrder from './pages/NewOrder'
import { useSse } from './hooks/useSse'

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
      <Layout sseConnected={sseConnected}>
        <Routes>
          <Route path="/" element={<Dashboard sseEventCount={sseEventCount} />} />
          <Route path="/orders" element={<Orders />} />
          <Route path="/bulk-orders" element={<BulkOrders />} />
          <Route path="/funds" element={<Funds />} />
          <Route path="/accounts" element={<Accounts />} />
          <Route path="/orders/new" element={<NewOrder />} />
        </Routes>
      </Layout>
    </BrowserRouter>
  )
}

export default App
