import { api } from './client'

export function loginApi(username, password) {
  return api.post('/auth/login', { username, password })
}

export function getMeApi() {
  return api.get('/auth/me')
}

export function logoutApi() {
  return api.post('/auth/logout', {})
}

export function getAccountsList() {
  return api.get('/accounts')
}
