const BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

function getToken() {
  return localStorage.getItem('oms_token')
}

async function request(url, options = {}) {
  const token = getToken()
  const { headers: extraHeaders, ...restOptions } = options
  const config = {
    ...restOptions,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
      ...extraHeaders,
    },
  }

  const response = await fetch(`${BASE_URL}${url}`, config)

  if (response.status === 401) {
    localStorage.removeItem('oms_token')
    window.dispatchEvent(new Event('auth:expired'))
    throw new Error('Session expired. Please log in again.')
  }

  if (!response.ok) {
    const text = await response.text()
    let message = text || `Request failed: ${response.status}`
    try {
      const json = JSON.parse(text)
      if (json.message) message = json.message
    } catch (_) {}
    throw new Error(message)
  }

  const contentType = response.headers.get('Content-Type') || ''
  if (contentType.includes('application/json')) return response.json()
  return response.text()
}

export const api = {
  get:  (url) => request(url),
  post: (url, body, extraHeaders = {}) => request(url, { method: 'POST', body: JSON.stringify(body), headers: extraHeaders }),
}

