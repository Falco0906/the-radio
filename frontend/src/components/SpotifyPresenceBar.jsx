import React, { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { usePresence } from '../hooks/usePresence';
import apiClient from '../api/axios';
import Playbox from './Playbox';
import './SpotifyPresenceBar.css';

const SpotifyPresenceBar = () => {
    const { user } = useAuth();
    const { presenceMap, refreshPresence } = usePresence();
    const [isRefreshing, setIsRefreshing] = useState(false);
    const [isConnected, setIsConnected] = useState(false);

    useEffect(() => {
        if (user) {
            checkConnections();
        }
    }, [user]);

    const checkConnections = async () => {
        try {
            const response = await apiClient.get('/api/platforms/connections/status');
            const hasConnections = Object.values(response.data).some(v => v === true);
            setIsConnected(hasConnections);
        } catch (error) {
            console.error('Failed to fetch connections:', error);
        }
    };

    if (!user || !isConnected) return null;

    const myPresence = presenceMap[user.id];

    const handleRefresh = async () => {
        setIsRefreshing(true);
        try {
            await refreshPresence();
        } catch (error) {
            console.error('Manual refresh failed:', error);
        } finally {
            setTimeout(() => setIsRefreshing(false), 500);
        }
    };

    return (
        <div className="spotify-presence-bar">
            <div className="bar-container">
                <Playbox presence={myPresence} />

                <button
                    className={`refresh-btn-mini ${isRefreshing ? 'spinning' : ''}`}
                    onClick={handleRefresh}
                    disabled={isRefreshing}
                    title="Sync with Spotify"
                >
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M23 4v6h-6"></path>
                        <path d="M1 20v-6h6"></path>
                        <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"></path>
                    </svg>
                </button>
            </div>
        </div>
    );
};

export default SpotifyPresenceBar;
