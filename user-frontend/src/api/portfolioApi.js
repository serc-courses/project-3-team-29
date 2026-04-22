import { api } from './client'

export async function getFunds() {
  const [funds, aggregates] = await Promise.all([
    api.get('/funds'),
    api.get('/view/aggregates/funds').catch(() => []),
  ])
  const aggMap = {}
  for (const agg of (aggregates || [])) {
    aggMap[agg.fundID] = agg
  }
  return funds.map(f => ({
    ...f,
    orderCount: aggMap[f.fundID]?.orderCount ?? 0,
    totalAmount: aggMap[f.fundID]?.totalAmount ?? 0,
    totalQuantity: aggMap[f.fundID]?.totalQuantity ?? 0,
    orderSides: aggMap[f.fundID]?.orderSides ?? {},
  }))
}

export function getAccounts() {
  return api.get('/view/aggregates/accounts')
}

export function getDashboard() {
  return api.get('/view/dashboard')
}
