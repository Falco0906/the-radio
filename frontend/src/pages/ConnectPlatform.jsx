import { useState, useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import apiClient from '../api/axios'
import { useAuth } from '../contexts/AuthContext'
import Layout from '../components/Layout'
import './ConnectPlatform.css'

const ConnectPlatform = () => {
  const [connections, setConnections] = useState([])
  const [loading, setLoading] = useState(true)
  const [message, setMessage] = useState(null)
  const [messageType, setMessageType] = useState(null)
  const { user } = useAuth()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()

  useEffect(() => {
    // Check for callback messages
    const error = searchParams.get('error')
    const success = searchParams.get('success')
    
    if (error) {
      setMessage(error)
      setMessageType('error')
      // Clear URL params
      window.history.replaceState({}, '', window.location.pathname)
    } else if (success) {
      setMessage(success)
      setMessageType('success')
      // Clear URL params and refresh connections
      window.history.replaceState({}, '', window.location.pathname)
      fetchConnections()
    }
    
    fetchConnections()
  }, [searchParams])

  const fetchConnections = async () => {
    try {
      const response = await apiClient.get('/api/platforms/connections')
      setConnections(response.data)
      
      // If user has at least one connection, allow them to proceed
      if (response.data.length > 0) {
        // Optionally auto-redirect after a delay
      }
    } catch (error) {
      console.error('Failed to fetch connections:', error)
    } finally {
      setLoading(false)
    }
  }

  const connectSpotify = async () => {
    try {
      setMessage(null)
      const response = await apiClient.post('/api/platforms/spotify/connect')
      if (response.data && response.data.authUrl) {
        window.location.href = response.data.authUrl
      } else {
        setMessage('Failed to get authorization URL')
        setMessageType('error')
      }
    } catch (error) {
      console.error('Failed to initiate Spotify connection:', error)
      const errorMsg = error.response?.data?.message || error.message || 'Failed to initiate Spotify connection'
      setMessage(errorMsg)
      setMessageType('error')
    }
  }

  const handleContinue = () => {
    if (connections.length > 0) {
      navigate('/dashboard')
    }
  }

  return (
    <Layout>
      <div className="connect-platform">
        <h1>Connect Music Platform</h1>
        <p className="text-muted mb-3">
          Connect at least one music platform to start sharing your listening activity.
        </p>

        {message && (
          <div className={`message ${messageType === 'error' ? 'error' : 'success'}`}>
            {message}
          </div>
        )}

        {loading ? (
          <div>Loading...</div>
        ) : (
          <>
            <div className="platform-list">
              <div className="platform-item">
                <div className="platform-info">
                  <h3>Spotify</h3>
                  {connections.find(c => c.platform === 'SPOTIFY') ? (
                    <span className="status-connected">Connected</span>
                  ) : (
                    <span className="status-disconnected">Not Connected</span>
                  )}
                </div>
                {!connections.find(c => c.platform === 'SPOTIFY') && (
                  <button onClick={connectSpotify}>Connect</button>
                )}
              </div>

              <div className="platform-item">
                <div className="platform-info">
                  <h3>Apple Music</h3>
                  <span className="text-muted">Coming Soon</span>
                </div>
              </div>

              <div className="platform-item">
                <div className="platform-info">
                  <h3>YouTube Music</h3>
                  <span className="text-muted">Coming Soon</span>
                </div>
              </div>
            </div>

            {connections.length > 0 && (
              <div className="mt-3">
                <button onClick={handleContinue}>Continue to Dashboard</button>
              </div>
            )}
          </>
        )}
      </div>
    </Layout>
  )
}

export default ConnectPlatform

