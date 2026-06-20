import axios from 'axios'

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'
const STORAGE_KEY = 'library_auth'

export function getStoredAuth() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export function storeAuth(auth) {
  if (auth) localStorage.setItem(STORAGE_KEY, JSON.stringify(auth))
  else localStorage.removeItem(STORAGE_KEY)
}

const client = axios.create({ baseURL: API_URL })

// Attach the JWT to every request.
client.interceptors.request.use((config) => {
  const auth = getStoredAuth()
  if (auth?.token) config.headers.Authorization = `Bearer ${auth.token}`
  return config
})

// Allow the auth layer to react to expired/invalid sessions.
let onUnauthorized = () => {}
export function setUnauthorizedHandler(fn) {
  onUnauthorized = fn
}

client.interceptors.response.use(
  (res) => res,
  (error) => {
    if (error.response?.status === 401) {
      storeAuth(null)
      onUnauthorized()
    }
    return Promise.reject(error)
  },
)

// Extracts a user-friendly message from a backend ErrorResponse.
export function apiErrorMessage(error, fallback = 'Something went wrong') {
  const data = error?.response?.data
  if (data?.errors?.length) return data.errors.join(', ')
  return data?.message || fallback
}

export default client
