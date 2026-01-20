import { useState, useEffect } from 'react'
import apiClient from '../api/axios'
import { useAuth } from '../contexts/AuthContext'
import Layout from '../components/Layout'
import { usePresence } from '../hooks/usePresence'
import './Dashboard.css'

const Dashboard = () => {
  const [friends, setFriends] = useState([])
  const [loading, setLoading] = useState(true)
  const { user } = useAuth()
  const presenceMap = usePresence()

  useEffect(() => {
    fetchFriends()
  }, [])

  const fetchFriends = async () => {
    try {
      const response = await apiClient.get('/api/friends')
      setFriends(response.data)
    } catch (error) {
      console.error('Failed to fetch friends:', error)
    } finally {
      setLoading(false)
    }
  }

  const tuneIn = (friend) => {
    const presence = presenceMap[friend.id]
    if (!presence || !presence.trackId) {
      return
    }

    // Create Spotify deep link
    const spotifyUrl = `https://open.spotify.com/track/${presence.trackId}`
    window.open(spotifyUrl, '_blank')
  }

  if (loading) {
    return (
      <Layout>
        <div className="container">Loading...</div>
      </Layout>
    )
  }

  return (
    <Layout>
      <div className="dashboard">
        <div className="dashboard-header">
          <h1>Dashboard</h1>
          <div className="live-toggle-container">
            <LiveToggle />
          </div>
        </div>

        {friends.length === 0 ? (
          <div className="empty-state">
            <p className="text-muted">No friends yet. Add friends to see what they're listening to.</p>
          </div>
        ) : (
          <div className="friends-list">
            {friends.map((friend) => {
              const presence = presenceMap[friend.id]
              const isLive = presence && presence.type !== 'PRESENCE_OFFLINE'

              return (
                <div key={friend.id} className="friend-card">
                  <div className="friend-header">
                    <div className="friend-info">
                      <span className={isLive ? 'status-live' : 'status-offline'}></span>
                      <div>
                        <div className="friend-name">{friend.displayName || friend.username}</div>
                        <div className="friend-username text-muted">@{friend.username}</div>
                      </div>
                    </div>
                    {isLive && (
                      <button onClick={() => tuneIn(friend)}>Tune In</button>
                    )}
                  </div>

                  {isLive && presence.trackName && (
                    <div className="friend-listening">
                      <div className="listening-track">
                        <strong>{presence.trackName}</strong>
                        <span className="text-muted"> by {presence.artist}</span>
                      </div>
                      {presence.isPlaying && (
                        <div className="waveform">
                          <div className="waveform-bar"></div>
                          <div className="waveform-bar"></div>
                          <div className="waveform-bar"></div>
                          <div className="waveform-bar"></div>
                          <div className="waveform-bar"></div>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              )
            })}
          </div>
        )}
      </div>
    </Layout>
  )
}

const LiveToggle = () => {
  const [isLive, setIsLive] = useState(false)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetchLiveStatus()
  }, [])

  const fetchLiveStatus = async () => {
    try {
      const response = await apiClient.get('/api/user/me')
      setIsLive(response.data.isLive)
    } catch (error) {
      console.error('Failed to fetch live status:', error)
    } finally {
      setLoading(false)
    }
  }

  const toggleLive = async () => {
    try {
      setLoading(true)
      const response = await apiClient.put('/api/presence/live', {
        isLive: !isLive
      })
      setIsLive(response.data.isLive)
    } catch (error) {
      console.error('Failed to toggle live status:', error)
    } finally {
      setLoading(false)
    }
  }

  if (loading && !isLive) {
    return <div>Loading...</div>
  }

  return (
    <button onClick={toggleLive} disabled={loading} className="live-toggle">
      {isLive ? '● Live' : '○ Invisible'}
    </button>
  )
}

export default Dashboard

