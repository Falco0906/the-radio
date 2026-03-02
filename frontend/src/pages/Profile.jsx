import { useState, useEffect } from 'react'
import apiClient from '../api/axios'
import { useAuth } from '../contexts/AuthContext'
import Layout from '../components/Layout'
import './Profile.css'

const Profile = () => {
  const { user } = useAuth()
  const [friends, setFriends] = useState([])
  const [friendRequests, setFriendRequests] = useState({ sent: [], received: [] })
  const [searchUsername, setSearchUsername] = useState('')
  const [searchResults, setSearchResults] = useState([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    fetchFriends()
    fetchFriendRequests()
  }, [])

  const fetchFriends = async () => {
    try {
      const response = await apiClient.get('/api/friends')
      setFriends(response.data)
    } catch (error) {
      console.error('Failed to fetch friends:', error)
    }
  }

  const fetchFriendRequests = async () => {
    try {
      const response = await apiClient.get('/api/friends/requests')
      setFriendRequests(response.data)
    } catch (error) {
      console.error('Failed to fetch friend requests:', error)
    }
  }

  const sendFriendRequest = async (recipientId) => {
    try {
      await apiClient.post('/api/friends/requests', { recipientId })
      setSearchResults(prev => prev.filter(u => u.id !== recipientId))
      fetchFriendRequests()
    } catch (error) {
      console.error('Failed to send friend request:', error)
      alert(error.response?.data?.message || 'Failed to send friend request')
    }
  }

  const respondToRequest = async (requestId, accept) => {
    try {
      await apiClient.put(`/api/friends/requests/${requestId}`, {
        action: accept ? 'ACCEPT' : 'REJECT'
      })
      fetchFriendRequests()
      fetchFriends()
    } catch (error) {
      console.error('Failed to respond to friend request:', error)
    }
  }

  const removeFriend = async (friendId) => {
    if (!window.confirm('Remove this friend?')) return

    try {
      await apiClient.delete(`/api/friends/${friendId}`)
      fetchFriends()
    } catch (error) {
      console.error('Failed to remove friend:', error)
    }
  }

  const searchUser = async () => {
    if (!searchUsername.trim()) return

    setLoading(true)
    try {
      const response = await apiClient.get(`/api/friends/search?query=${searchUsername}`)
      setSearchResults(response.data)
    } catch (error) {
      console.error('Failed to search user:', error)
      alert('Search failed. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Layout>
      <div className="profile">
        <h1>Profile</h1>

        <div className="profile-section">
          <h2>Your Info</h2>
          <div className="info-item">
            <strong>Username:</strong> {user?.username}
          </div>
          <div className="info-item">
            <strong>Display Name:</strong> {user?.displayName || user?.username}
          </div>
          <div className="info-item">
            <strong>Email:</strong> {user?.email}
          </div>
        </div>

        <div className="profile-section">
          <h2>Find Friends</h2>
          <div className="add-friend-form">
            <input
              type="text"
              placeholder="Search by username..."
              value={searchUsername}
              onChange={(e) => setSearchUsername(e.target.value)}
              onKeyPress={(e) => e.key === 'Enter' && searchUser()}
            />
            <button onClick={searchUser} disabled={loading}>
              {loading ? '...' : 'Search'}
            </button>
          </div>

          {searchResults.length > 0 && (
            <div className="search-results mt-2">
              {searchResults.map((result) => (
                <div key={result.id} className="friend-request-item">
                  <div>
                    <strong>{result.displayName || result.username}</strong>
                    <span className="text-muted"> @{result.username}</span>
                  </div>
                  <button
                    className="btn-sm"
                    onClick={() => sendFriendRequest(result.id)}
                  >
                    Add Friend
                  </button>
                </div>
              ))}
            </div>
          )}

          {searchUsername && searchResults.length === 0 && !loading && (
            <p className="text-muted mt-1 small">No users found.</p>
          )}
        </div>

        {friendRequests.received.length > 0 && (
          <div className="profile-section">
            <h2>Friend Requests</h2>
            <div className="friend-requests-list">
              {friendRequests.received.map((request) => (
                <div key={request.id} className="friend-request-item">
                  <div>
                    <strong>{request.otherUsername}</strong>
                  </div>
                  <div className="friend-request-actions">
                    <button onClick={() => respondToRequest(request.id, true)}>
                      Accept
                    </button>
                    <button onClick={() => respondToRequest(request.id, false)}>
                      Reject
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        <div className="profile-section">
          <h2>Friends ({friends.length})</h2>
          {friends.length === 0 ? (
            <p className="text-muted">No friends yet.</p>
          ) : (
            <div className="friends-list">
              {friends.map((friend) => (
                <div key={friend.id} className="friend-item">
                  <div>
                    <strong>{friend.displayName || friend.username}</strong>
                    <span className="text-muted"> @{friend.username}</span>
                    {friend.isLive && <span className="status-live"></span>}
                  </div>
                  <button onClick={() => removeFriend(friend.id)}>
                    Remove
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </Layout>
  )
}

export default Profile

