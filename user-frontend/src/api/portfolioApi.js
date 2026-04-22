import { api } from './client'

export function getFunds() {
  return api.get('/funds')
}

export function getAccounts() {
  return api.get('/view/aggregates/accounts')
}

export function getDashboard() {
  return api.get('/view/dashboard')
}
