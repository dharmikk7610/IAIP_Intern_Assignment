import axios from 'axios'

// In development Vite proxies /api to Spring Boot (see vite.config.js).
// VITE_API_BASE_URL can point somewhere else for other setups.
const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
})

let authToken = null
let onUnauthorized = () => {}

export function setAuthToken(token) {
  authToken = token
}

export function setUnauthorizedHandler(handler) {
  onUnauthorized = handler
}

client.interceptors.request.use((config) => {
  if (authToken) {
    config.headers.Authorization = `Bearer ${authToken}`
  }
  return config
})

client.interceptors.response.use(
  (response) => response,
  (error) => {
    // Expired or invalid token -> log out. A failed login is just a wrong password, not a logout.
    if (error.response?.status === 401 && !error.config?.url?.endsWith('/auth/login')) {
      onUnauthorized()
    }
    return Promise.reject(error)
  },
)

/** Human-readable message from the backend's ErrorResponse, or a sensible fallback. */
export function getErrorMessage(error, fallback = 'Something went wrong. Please try again.') {
  if (!error.response) {
    return 'Cannot reach the server. Please check that the backend is running.'
  }
  return error.response.data?.message ?? fallback
}

/** Field -> message map from a 400/409 response, used to show errors next to form fields. */
export function getFieldErrors(error) {
  return error.response?.data?.fieldErrors ?? {}
}

export default client
