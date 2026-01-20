import axios from 'axios'

// Create axios instance with base configuration
// In dev mode, use relative URLs (Vite proxy handles /api -> localhost:8081)
// In production, use the full base URL
const apiClient = axios.create({
  baseURL: import.meta.env.PROD ? (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081') : '',
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor to add auth token
apiClient.interceptors.request.use(
  (config) => {
    // Don't add Authorization header for public endpoints
    const publicEndpoints = ['/api/auth/login', '/api/auth/register', '/api/auth/logout']
    const isPublicEndpoint = publicEndpoints.some(endpoint => config.url?.includes(endpoint))
    
    if (!isPublicEndpoint) {
      const token = localStorage.getItem('token')
      if (token) {
        config.headers.Authorization = `Bearer ${token}`
        console.log('[API] Adding Authorization header:', config.url)
      } else {
        console.warn('[API] No token found in localStorage for request:', config.url)
      }
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
    
    // Don't handle auth errors for public endpoints
    const publicEndpoints = ['/api/auth/login', '/api/auth/register']
    const isPublicEndpoint = publicEndpoints.some(endpoint => url.includes(endpoint))
    
    if (!isPublicEndpoint && status === 401) {
      // Only treat Unauthorized as invalid session
      console.log(`[API] ${status} ${error.response?.statusText} - clearing token and redirecting to login`)
      localStorage.removeItem('token')
      if (!window.location.pathname.includes('/login')) {
        window.location.href = '/login'
      }
    } else if (!isPublicEndpoint && status === 403) {
      // Forbidden should not log the user out; surface error to caller/UI
      console.warn(`[API] 403 Forbidden on ${url}`)
    }
    
    return Promise.reject(error)
  }
)

export default apiClient

