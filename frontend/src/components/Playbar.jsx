import { useState, useEffect } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { usePresence } from '../hooks/usePresence'
import apiClient from '../api/axios'
import './Playbar.css'

const Playbar = () => {
    const { user } = useAuth()
    const { presenceMap, refreshPresence } = usePresence()
    const [isRefreshing, setIsRefreshing] = useState(false)
    const [isConnected, setIsConnected] = useState(false)

    useEffect(() => {
        if (user) {
            checkConnections()
        }
    }, [user])

    const checkConnections = async () => {
        try {
            const response = await apiClient.get('/api/platforms/connections/status')
            // If any platform is connected, we show the playbar
            const hasConnections = Object.values(response.data).some(v => v === true)
            setIsConnected(hasConnections)
        } catch (error) {
            console.error('Failed to fetch connections:', error)
        }
    }

    if (!user || !isConnected) return null

    const myPresence = presenceMap[user.id]

    const handleRefresh = async () => {
        setIsRefreshing(true)
        try {
            await refreshPresence()
        } catch (error) {
            console.error('Manual refresh failed:', error)
        } finally {
            // Add a small delay for better UX
            setTimeout(() => setIsRefreshing(false), 500)
        }
    }

    return (
        <div className="playbar">
            <div className="playbar-content">
                <div className="track-info">
                    {myPresence && myPresence.trackName ? (
                        <>
                            {myPresence.albumArtUrl ? (
                                <img src={myPresence.albumArtUrl} alt="Album Art" className="album-art" />
                            ) : (
                                <div className="album-art-fallback" />
                            )}
                            <div className="text-info">
                                <div className="track-name">{myPresence.trackName}</div>
                                <div className="artist-name">{myPresence.artist}</div>
                            </div>
                        </>
                    ) : (
                        <div className="text-info">
                            <div className="track-name">Not playing</div>
                            <div className="artist-name text-muted">Refresh to check for active music</div>
                        </div>
                    )}
                </div>

                <div className="playback-controls">
                    <button
                        className={`refresh-button ${isRefreshing ? 'spinning' : ''}`}
                        onClick={handleRefresh}
                        disabled={isRefreshing}
                        title="Refresh listening status"
                    >
                        <RefreshIcon />
                    </button>

                    {myPresence && myPresence.trackName && (
                        <div className="playback-status">
                            <span className={`platform-badge ${myPresence.platform.toLowerCase()}`}>
                                {myPresence.platform}
                            </span>
                            {myPresence.isPlaying && (
                                <div className="playing-indicator">
                                    <div className="bar"></div>
                                    <div className="bar"></div>
                                    <div className="bar"></div>
                                </div>
                            )}
                        </div>
                    )}
                </div>
            </div>
        </div>
    )
}

const RefreshIcon = () => (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M23 4v6h-6"></path>
        <path d="M1 20v-6h6"></path>
        <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"></path>
    </svg>
)

export default Playbar
