const BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

function getToken() {
  return localStorage.getItem('oms_token')
}

async function request(url, options = {}) {
  const token = getToken()
  const config = {
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
      ...options.headers,
    },
    ...options,
  }

  const response = await fetch(`${BASE_URL}${url}`, config)

  if (response.status === 401) {
    localStorage.removeItem('oms_token')
    window.dispatchEvent(new Event('auth:expired'))
    throw new Error('Session expired. Please log in again.')
  }

  if (!response.ok) {
    const text = await response.text()
    throw new Error(text || `Request failed: ${response.status}`)
  }

  const contentType = response.headers.get('Content-Type') || ''
  if (contentType.includes('application/json')) return response.json()
  return response.text()
}

export const api = {
  get:  (url)       => request(url),
  post: (url, body) => request(url, { method: 'POST', body: JSON.stringify(body) }),
}
