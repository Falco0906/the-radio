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
  const [showGuide, setShowGuide] = useState(false)
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

      const { authorizationUrl } = response.data

      if (authorizationUrl) {
        window.location.href = authorizationUrl
      } else {
        throw new Error('No authorization URL returned from backend')
      }
    } catch (error) {
      console.error('Spotify connection failed:', error)
      const errorMsg = error.response?.data?.message || error.message || 'Failed to get Spotify authorization URL'
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

            <div className="mt-3 bottom-actions">
              {connections.length > 0 && (
                <button onClick={handleContinue}>Continue to Dashboard</button>
              )}
              <button className="guide-btn-inline" onClick={() => setShowGuide(true)}>? Guide</button>
            </div>
          </>
        )}

        {showGuide && (
          <div className="guide-overlay" onClick={() => setShowGuide(false)}>
            <div className="guide-popup" onClick={(e) => e.stopPropagation()}>
              <div className="guide-header">
                <h2>Welcome to The Radio</h2>
                <button className="guide-close" onClick={() => setShowGuide(false)}>✕</button>
              </div>
              <div className="guide-content">
                <div className="guide-step">
                  <div className="step-number">1</div>
                  <div>
                    <strong>Connect Spotify</strong>
                    <p>Go to <strong>Platforms</strong> and connect your Spotify account. This lets The Radio detect what you're listening to.</p>
                  </div>
                </div>
                <div className="guide-step">
                  <div className="step-number">2</div>
                  <div>
                    <strong>Go Live</strong>
                    <p>On the <strong>Dashboard</strong>, toggle the <strong>Live</strong> button so your friends can see your music in realtime.</p>
                  </div>
                </div>
                <div className="guide-step">
                  <div className="step-number">3</div>
                  <div>
                    <strong>Add Friends</strong>
                    <p>Head to <strong>Friends → Find People</strong> and search for users. Send them a friend request!</p>
                  </div>
                </div>
                <div className="guide-step">
                  <div className="step-number">4</div>
                  <div>
                    <strong>Tune In</strong>
                    <p>Once you have friends, their currently playing tracks will appear on your Dashboard. Hit <strong>Tune In</strong> to listen along.</p>
                  </div>
                </div>
                <div className="guide-tip">
                  <p style={{ marginBottom: '0.5rem' }}><strong>Note:</strong> Before connecting Spotify, make sure to let <span className="highlight">tofu</span> know your email to get whitelisted (temporarily).</p>
                  <p><strong>Get started:</strong> Add <span className="highlight">tofutrash._</span> as a friend to see the app in action!</p>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </Layout>
  )
}

export default ConnectPlatform
