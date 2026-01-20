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
      // Note: This endpoint doesn't exist yet, but we can add it later
      // For now, just show an error
      alert('User search not implemented yet. Use user ID to send friend request.')
    } catch (error) {
      console.error('Failed to search user:', error)
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
          <h2>Add Friend</h2>
          <div className="add-friend-form">
            <input
              type="text"
              placeholder="Enter user ID"
              value={searchUsername}
              onChange={(e) => setSearchUsername(e.target.value)}
            />
            <button onClick={searchUser} disabled={loading}>
              Search
            </button>
          </div>
          <p className="text-muted mt-1">
            Note: User search by username will be added in a future update.
            For now, you need the user ID to send a friend request.
          </p>
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

