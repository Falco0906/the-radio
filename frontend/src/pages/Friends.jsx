import { useState, useEffect } from 'react'
import apiClient from '../api/axios'
import { useAuth } from '../contexts/AuthContext'
import { usePresence } from '../hooks/usePresence'
import Layout from '../components/Layout'
import Playbox from '../components/Playbox'
import './Friends.css'

const Friends = () => {
    const { user } = useAuth()
    const { presenceMap } = usePresence()
    const [friends, setFriends] = useState([])
    const [friendRequests, setFriendRequests] = useState({ sent: [], received: [] })
    const [searchUsername, setSearchUsername] = useState('')
    const [searchResults, setSearchResults] = useState([])
    const [sentIds, setSentIds] = useState(new Set())
    const [loading, setLoading] = useState(false)
    const [tab, setTab] = useState('friends')

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
            setSentIds(prev => new Set([...prev, recipientId]))
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
        } finally {
            setLoading(false)
        }
    }

    const pendingCount = friendRequests.received?.length || 0

    return (
        <Layout>
            <div className="friends-page">
                <h1>Friends</h1>

                <div className="friends-tabs">
                    <button
                        className={`tab-btn ${tab === 'friends' ? 'active' : ''}`}
                        onClick={() => setTab('friends')}
                    >
                        My Friends ({friends.length})
                    </button>
                    <button
                        className={`tab-btn ${tab === 'requests' ? 'active' : ''}`}
                        onClick={() => setTab('requests')}
                    >
                        Requests {pendingCount > 0 && <span className="badge">{pendingCount}</span>}
                    </button>
                    <button
                        className={`tab-btn ${tab === 'search' ? 'active' : ''}`}
                        onClick={() => setTab('search')}
                    >
                        Find People
                    </button>
                </div>

                {/* MY FRIENDS TAB */}
                {tab === 'friends' && (
                    <div className="tab-content">
                        {friends.length === 0 ? (
                            <div className="empty-state">
                                <p className="text-muted">No friends yet. Search for people to add!</p>
                                <button onClick={() => setTab('search')}>Find People</button>
                            </div>
                        ) : (
                            <div className="friends-grid">
                                {friends.map((friend) => {
                                    const presence = presenceMap[friend.id]
                                    const isLive = presence && presence.type !== 'PRESENCE_OFFLINE'

                                    return (
                                        <div key={friend.id} className="friend-card-full">
                                            <div className="friend-card-header">
                                                <div className="friend-identity">
                                                    <span className={`status-dot ${isLive ? 'online' : 'offline'}`}></span>
                                                    <div>
                                                        <div className="friend-name">{friend.displayName || friend.username}</div>
                                                        <div className="friend-username text-muted">@{friend.username}</div>
                                                    </div>
                                                </div>
                                                <button className="btn-remove" onClick={() => removeFriend(friend.id)}>
                                                    Remove
                                                </button>
                                            </div>
                                            {isLive && presence.trackName && (
                                                <div className="friend-now-playing">
                                                    {presence.albumArtUrl && (
                                                        <img src={presence.albumArtUrl} alt="" className="mini-album-art" />
                                                    )}
                                                    <div className="now-playing-info">
                                                        <div className="np-track">{presence.trackName}</div>
                                                        <div className="np-artist text-muted">{presence.artist}</div>
                                                    </div>
                                                    {presence.isPlaying && (
                                                        <div className="waveform small">
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
                )}

                {/* REQUESTS TAB */}
                {tab === 'requests' && (
                    <div className="tab-content">
                        {friendRequests.received?.length > 0 && (
                            <div className="requests-section">
                                <h3>Received</h3>
                                {friendRequests.received.map((request) => (
                                    <div key={request.id} className="request-card">
                                        <div className="request-info">
                                            <strong>{request.otherUsername}</strong>
                                            <span className="text-muted"> wants to be your friend</span>
                                        </div>
                                        <div className="request-actions">
                                            <button className="btn-accept" onClick={() => respondToRequest(request.id, true)}>
                                                Accept
                                            </button>
                                            <button className="btn-reject" onClick={() => respondToRequest(request.id, false)}>
                                                Decline
                                            </button>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}

                        {friendRequests.sent?.length > 0 && (
                            <div className="requests-section">
                                <h3>Sent</h3>
                                {friendRequests.sent.map((request) => (
                                    <div key={request.id} className="request-card">
                                        <div className="request-info">
                                            <strong>{request.otherUsername}</strong>
                                            <span className="text-muted"> — pending</span>
                                        </div>
                                        <span className="pending-badge">Pending</span>
                                    </div>
                                ))}
                            </div>
                        )}

                        {(!friendRequests.received?.length && !friendRequests.sent?.length) && (
                            <div className="empty-state">
                                <p className="text-muted">No pending friend requests.</p>
                            </div>
                        )}
                    </div>
                )}

                {/* SEARCH TAB */}
                {tab === 'search' && (
                    <div className="tab-content">
                        <div className="search-bar">
                            <input
                                type="text"
                                placeholder="Search by username..."
                                value={searchUsername}
                                onChange={(e) => setSearchUsername(e.target.value)}
                                onKeyDown={(e) => e.key === 'Enter' && searchUser()}
                            />
                            <button onClick={searchUser} disabled={loading}>
                                {loading ? '...' : 'Search'}
                            </button>
                        </div>

                        {searchResults.length > 0 && (
                            <div className="search-results-list">
                                {searchResults.map((result) => {
                                    const alreadyFriend = result.isFriend
                                    const requestSent = sentIds.has(result.id)

                                    return (
                                        <div key={result.id} className="search-result-card">
                                            <div className="search-result-info">
                                                <strong>{result.displayName || result.username}</strong>
                                                <span className="text-muted"> @{result.username}</span>
                                            </div>
                                            {alreadyFriend ? (
                                                <span className="status-badge friends">Friends ✓</span>
                                            ) : requestSent ? (
                                                <span className="status-badge pending">Request Sent</span>
                                            ) : (
                                                <button className="btn-add" onClick={() => sendFriendRequest(result.id)}>
                                                    Send Request
                                                </button>
                                            )}
                                        </div>
                                    )
                                })}
                            </div>
                        )}

                        {searchUsername && searchResults.length === 0 && !loading && (
                            <p className="text-muted" style={{ marginTop: '1rem' }}>No users found.</p>
                        )}
                    </div>
                )}
            </div>
        </Layout>
    )
}

export default Friends
