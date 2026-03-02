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
    const error = searchParams.get('error')
    const connected = searchParams.get('connected') // backend sends ?connected=soundcloud

    if (error) {
      const errorMessages = {
        invalid_state: 'OAuth session expired. Please try connecting again.',
        soundcloud_auth_failed: 'SoundCloud authorization failed. Please try again.',
      }
      setMessage(errorMessages[error] || `Connection failed: ${error}`)
      setMessageType('error')
      window.history.replaceState({}, '', window.location.pathname)
    } else if (connected) {
      setMessage(`${connected.charAt(0).toUpperCase() + connected.slice(1)} connected successfully!`)
      setMessageType('success')
      window.history.replaceState({}, '', window.location.pathname)
    }

    fetchConnections()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParams])

  const fetchConnections = async () => {
    const token = localStorage.getItem('token')
    if (!token) {
      setLoading(false)
      return
    }

    try {
      const response = await apiClient.get('/api/platforms/connections')
      setConnections(response.data)
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
        setMessage('Failed to get Spotify authorization URL')
        setMessageType('error')
      }
    } catch (error) {
      console.error('Failed to initiate Spotify connection:', error)
      const errorMsg = error.response?.data?.message || error.message || 'Failed to initiate Spotify connection'
      setMessage(errorMsg)
      setMessageType('error')
    }
  }

  const connectSoundCloud = async () => {
    try {
      setMessage(null)

      // Use the shared apiClient — it automatically adds the Authorization header
      const response = await apiClient.get('/api/platforms/soundcloud/connect')

      if (response.data?.url) {
        window.location.href = response.data.url
      } else {
        setMessage('Failed to get SoundCloud authorization URL')
        setMessageType('error')
      }
    } catch (error) {
      console.error('Failed to initiate SoundCloud connection:', error)
      const errorMsg = error.response?.data?.message || error.message || 'Failed to initiate SoundCloud connection'
      setMessage(errorMsg)
      setMessageType('error')
    }
  }

  const disconnectPlatform = async (platformName) => {
    try {
      setMessage(null)
      const connection = connections.find(c => c.platform === platformName)
      if (!connection) return

      if (!window.confirm(`Are you sure you want to disconnect ${platformName}?`)) {
        return
      }

      await apiClient.delete(`/api/platforms/connections/${connection.id}`)

      setMessage(`${platformName.charAt(0).toUpperCase() + platformName.slice(1).toLowerCase()} disconnected successfully`)
      setMessageType('success')

      // Refresh connections list
      fetchConnections()
    } catch (error) {
      console.error('Failed to disconnect platform:', error)
      const errorMsg = error.response?.data?.message || error.message || 'Failed to disconnect platform'
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
                {connections.some(c => c.platform === 'SPOTIFY') ? (
                  <button
                    className="btn-disconnect"
                    onClick={() => disconnectPlatform('SPOTIFY')}
                  >
                    Disconnect
                  </button>
                ) : (
                  <button onClick={connectSpotify}>Connect</button>
                )}
              </div>

              <div className="platform-item">
                <div className="platform-info">
                  <h3>SoundCloud</h3>
                  {connections.find(c => c.platform === 'SOUNDCLOUD') ? (
                    <span className="status-connected">Connected</span>
                  ) : (
                    <span className="status-disconnected">Not Connected</span>
                  )}
                </div>
                {connections.some(c => c.platform === 'SOUNDCLOUD') ? (
                  <button
                    className="btn-disconnect"
                    onClick={() => disconnectPlatform('SOUNDCLOUD')}
                  >
                    Disconnect
                  </button>
                ) : (
                  <button onClick={connectSoundCloud}>Connect</button>
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
