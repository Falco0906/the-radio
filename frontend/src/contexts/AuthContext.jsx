import React, { createContext, useContext, useState, useEffect, useRef } from 'react'
import apiClient from '../api/axios'

const AuthContext = createContext()

export const useAuth = () => {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return context
}

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null)
  const [token, setToken] = useState(localStorage.getItem('token'))
  const [loading, setLoading] = useState(true)
  const justLoggedIn = useRef(false)

  useEffect(() => {
    const storedToken = localStorage.getItem('token')
    if (storedToken && !token) {
      // If we have a stored token but no token in state, restore it
      setToken(storedToken)
      return // Exit early, will run again with new token
    }
    
    if (justLoggedIn.current) {
      // We just logged in, user is already set, just stop loading
      justLoggedIn.current = false
      setLoading(false)
      return
    }
    
    if (token && !user) {
      // Only fetch user if we have a token but no user data
      fetchUser()
    } else if (!token) {
      setLoading(false)
    } else {
      // We have both token and user, we're good
      setLoading(false)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token])

  const fetchUser = async () => {
    try {
      const response = await apiClient.get('/api/user/me')
      setUser(response.data)
      setLoading(false)
    } catch (error) {
      console.error('Failed to fetch user:', error)
      // Only logout if it's a 401 (unauthorized)
      if (error.response?.status === 401) {
        logout()
      } else {
        // For other errors (network, etc), don't clear user - just stop loading
        // This prevents clearing user data on temporary network issues
        setLoading(false)
      }
    }
  }

  const login = async (email, password) => {
    try {
      const response = await apiClient.post('/api/auth/login', { email, password })
      const { token: newToken, user: userData } = response.data
      localStorage.setItem('token', newToken)
      // Set user first, then token, and mark that we just logged in
      justLoggedIn.current = true
      setUser(userData)
      setToken(newToken)
      return { success: true }
    } catch (error) {
      console.error('Login error:', error.response?.data)
      const errorMessage = error.response?.data?.message || 
                          error.response?.data?.error || 
                          error.message || 
                          'Login failed'
      return {
        success: false,
        error: errorMessage
      }
    }
  }

  const register = async (email, password, username, displayName) => {
    try {
      const response = await apiClient.post('/api/auth/register', {
        email,
        password,
        username,
        displayName
      })
      const { token: newToken, user: userData } = response.data
      localStorage.setItem('token', newToken)
      // Set user first, then token, and mark that we just registered
      justLoggedIn.current = true
      setUser(userData)
      setToken(newToken)
      return { success: true }
    } catch (error) {
      console.error('Registration error:', error)
      
      let errorMessage = 'Registration failed. Please try again.'
      
      if (error.response) {
        // The request was made and the server responded with a status code
        // that falls out of the range of 2xx
        console.error('Response data:', error.response.data)
        console.error('Response status:', error.response.status)
        
        if (error.response.data) {
          errorMessage = error.response.data.message || 
                        error.response.data.error || 
                        JSON.stringify(error.response.data)
        }
      } else if (error.request) {
        // The request was made but no response was received
        console.error('No response received:', error.request)
        errorMessage = 'No response from server. Please check your connection.'
      } else {
        // Something happened in setting up the request
        console.error('Request setup error:', error.message)
        errorMessage = error.message || 'Error setting up registration request'
      }
      
      return {
        success: false,
        error: errorMessage
      }
    }
  }

  const logout = () => {
    setToken(null)
    setUser(null)
    localStorage.removeItem('token')
  }

  const value = {
    user,
    token,
    loading,
    login,
    register,
    logout,
    fetchUser
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

