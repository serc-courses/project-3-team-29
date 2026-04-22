import { api } from './client'

export function planOrders(orders) {
  const idempotencyKey = crypto.randomUUID()
  return api.post('/orders/plan', orders, { 'X-Idempotency-Key': idempotencyKey })
}


export function getOrderStatus(orderID) {
  return api.get(`/orders/status?orderID=${encodeURIComponent(orderID)}`)
}

export function getOrders(filters = {}) {
  const params = new URLSearchParams()
  if (filters.accountID) params.set('accountID', filters.accountID)
  if (filters.fundID) params.set('fundID', filters.fundID)
  if (filters.orderID) params.set('orderID', filters.orderID)
  if (filters.bulkOrderID) params.set('bulkOrderID', filters.bulkOrderID)
  const qs = params.toString()
  return api.get(`/view/orders${qs ? `?${qs}` : ''}`)
}
