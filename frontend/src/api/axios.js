import axios from 'axios'
import { API_BASE } from '../config/api'

// Create axios instance with base configuration
const apiClient = axios.create({
  baseURL: API_BASE,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor to add auth token
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response interceptor to handle errors
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const url = error.config?.url || ''
    const status = error.response?.status

    // These endpoints should never trigger auto-logout on 401:
    // - /api/user/me  (used to restore session on page load)
    // - /api/platforms/ (OAuth and platform management — 401 here is not a session issue)
    const noAutoLogoutPrefixes = ['/api/user/me', '/api/platforms/']
    const shouldSkipAutoLogout = noAutoLogoutPrefixes.some(prefix => url.includes(prefix))

    // Don't handle auth errors for public endpoints
    const publicEndpoints = ['/api/auth/login', '/api/auth/register']
    const isPublicEndpoint = publicEndpoints.some(endpoint => url.includes(endpoint))

    if (!isPublicEndpoint && status === 401 && !shouldSkipAutoLogout) {
      localStorage.removeItem('token')
      if (!window.location.pathname.includes('/login')) {
        window.location.href = '/login'
      }
    }

    return Promise.reject(error)
  }
)

export default apiClient

