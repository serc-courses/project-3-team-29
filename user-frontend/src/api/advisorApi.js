import { api } from './client'
import { getAccounts } from './portfolioApi'
import { getOrders, planOrders } from './ordersApi'
import { ADVISOR_CONFIG } from '../constants/advisorConfig'

function getAdvisorId() {
  return localStorage.getItem(ADVISOR_CONFIG.STORAGE_KEYS.advisorId) || ADVISOR_CONFIG.DEFAULT_ADVISOR_ID
}

function advisorHeaders() {
  return { 'X-Advisor-ID': getAdvisorId() }
}

export async function getAdvisorMe(advisorId) {
  const id = advisorId || getAdvisorId()
  try {
    return await api.get(`/advisor/me?advisorID=${encodeURIComponent(id)}`)
  } catch {
    return { advisorID: id, name: `Advisor ${id}`, clientCount: ADVISOR_CONFIG.ADVISOR_CLIENT_MAP[id]?.length || 0 }
  }
}

export async function getAdvisorClients(advisorId) {
  const id = advisorId || getAdvisorId()
  try {
    return await fetch(`/advisor/clients?advisorID=${encodeURIComponent(id)}`, {
      headers: { 'X-Advisor-ID': id },
    }).then(r => r.ok ? r.json() : Promise.reject())
  } catch {
    // Fallback: compose from account aggregates
    const clientIds = ADVISOR_CONFIG.ADVISOR_CLIENT_MAP[id] || []
    let accounts = []
    try { accounts = await getAccounts() } catch {}
    return clientIds.map(accountID => {
      const found = accounts.find(a => (a.accountID || a) === accountID)
      return {
        accountID,
        orderCount: found?.orderCount ?? 0,
        totalAmount: found?.totalAmount ?? 0,
        totalQuantity: found?.totalQuantity ?? 0,
        statuses: found?.statuses ?? {},
      }
    })
  }
}

export async function getAdvisorClientOrders(accountID) {
  try {
    return await api.get(`/advisor/orders?accountID=${encodeURIComponent(accountID)}`)
  } catch {
    return getOrders({ accountID })
  }
}

export async function getAdvisorBookOverview(advisorId) {
  const id = advisorId || getAdvisorId()
  try {
    return await api.get(`/advisor/dashboard?advisorID=${encodeURIComponent(id)}`)
  } catch {
    // Compose client-side fallback
    const clients = await getAdvisorClients(id)
    const totalAmount = clients.reduce((s, c) => s + (c.totalAmount || 0), 0)
    const activeOrders = clients.reduce((s, c) => {
      const st = c.statuses || {}
      return s + ['PLANNED','VALIDATED','ENRICHED','PLACED','BULKED','CONFIRMED','CONTRACTED']
        .reduce((n, k) => n + (st[k] || 0), 0)
    }, 0)
    const failedOrders = clients.reduce((s, c) => s + (c.statuses?.ERRORED || 0), 0)
    return {
      advisorID: id,
      clientCount: clients.length,
      totalAmount,
      activeOrders,
      failedOrders,
      totalOrders: clients.reduce((s, c) => s + (c.orderCount || 0), 0),
    }
  }
}

export async function planAdvisorBasket(rows) {
  const advisorId = getAdvisorId()
  try {
    return await fetch('/advisor/orders/plan', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-Advisor-ID': advisorId },
      body: JSON.stringify(rows),
    }).then(async r => {
      if (!r.ok) throw new Error(await r.text())
      return r.json()
    })
  } catch {
    // Fallback: use regular plan endpoint
    const payload = rows.map(r => ({ productID: r.fundID, amount: r.amount, accountID: r.accountID, orderSide: r.orderSide }))
    return planOrders(payload)
  }
}

export function getBasketDraft() {
  try {
    const raw = localStorage.getItem(ADVISOR_CONFIG.STORAGE_KEYS.advisorDraftBasket)
    return raw ? JSON.parse(raw) : []
  } catch { return [] }
}

export function saveBasketDraft(rows) {
  localStorage.setItem(ADVISOR_CONFIG.STORAGE_KEYS.advisorDraftBasket, JSON.stringify(rows))
}

export function clearBasketDraft() {
  localStorage.removeItem(ADVISOR_CONFIG.STORAGE_KEYS.advisorDraftBasket)
}

export function groupBasketByClient(rows) {
  return rows.reduce((acc, row) => {
    const key = row.accountID
    if (!acc[key]) acc[key] = []
    acc[key].push(row)
    return acc
  }, {})
}

export function basketTotals(rows) {
  return {
    count: rows.length,
    total: rows.reduce((s, r) => s + (Number(r.amount) || 0), 0),
    buyCount: rows.filter(r => r.orderSide === 'BUY').length,
    sellCount: rows.filter(r => r.orderSide === 'SELL').length,
    clientCount: new Set(rows.map(r => r.accountID)).size,
  }
}
