import React, { createContext, useContext, useState, useEffect, useRef, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useAuth } from './AuthContext';
import apiClient from '../api/axios';
import { API_BASE } from '../config/api';

const PresenceContext = createContext();

export const PresenceProvider = ({ children }) => {
    const [presenceMap, setPresenceMap] = useState({});
    const [isConnected, setIsConnected] = useState(false);
    const { token, user } = useAuth();
    const clientRef = useRef(null);

    const handlePresenceUpdate = useCallback((data) => {
        setPresenceMap((prev) => {
            const newMap = { ...prev };
            if (data.type === 'PRESENCE_OFFLINE') {
                delete newMap[data.userId];
            } else {
                newMap[data.userId] = data;
            }
            return newMap;
        });
    }, []);

    const fetchCurrentPresence = useCallback(async () => {
        if (!token) return;
        try {
            const response = await apiClient.get('/api/presence/current');
            if (response.data && response.data.userId) {
                handlePresenceUpdate(response.data);
            }
        } catch (error) {
            console.error('Failed to fetch current presence:', error);
        }
    }, [token, handlePresenceUpdate]);

    const refreshPresence = useCallback(async () => {
        if (!token) return;
        try {
            const response = await apiClient.post('/api/presence/refresh');
            if (response.data && response.data.userId) {
                handlePresenceUpdate(response.data);
            }
            return response.data;
        } catch (error) {
            console.error('Failed to refresh presence:', error);
            throw error;
        }
    }, [token, handlePresenceUpdate]);

    useEffect(() => {
        if (!token || !user?.id) {
            if (clientRef.current) {
                clientRef.current.deactivate();
                clientRef.current = null;
                setIsConnected(false);
            }
            return;
        }

        fetchCurrentPresence();

        const client = new Client({
            webSocketFactory: () => new SockJS(API_BASE + '/ws'),
            connectHeaders: {
                Authorization: `Bearer ${localStorage.getItem('token') || token}`
            },
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
            debug: (str) => {
                // console.log('STOMP Debug:', str);
            },
            onConnect: () => {
                setIsConnected(true);
                const userId = user.id;
                const topic = `/topic/presence/${userId}`;

                client.subscribe(topic, (message) => {
                    const data = JSON.parse(message.body);
                    handlePresenceUpdate(data);
                });

                // Test topic for global debugging if needed
                client.subscribe("/topic/presence-test", (message) => {
                    const data = JSON.parse(message.body);
                    handlePresenceUpdate(data);
                });
            },
            onDisconnect: () => {
                setIsConnected(false);
            },
            onStompError: (frame) => {
                console.error('STOMP error:', frame);
                setIsConnected(false);
            }
        });

        clientRef.current = client;
        client.activate();

        return () => {
            if (clientRef.current) {
                clientRef.current.deactivate();
                clientRef.current = null;
            }
        };
    }, [token, user?.id, fetchCurrentPresence, handlePresenceUpdate]);

    return (
        <PresenceContext.Provider value={{ presenceMap, isConnected, refreshPresence, fetchCurrentPresence }}>
            {children}
        </PresenceContext.Provider>
    );
};

export const usePresence = () => {
    const context = useContext(PresenceContext);
    if (!context) {
        throw new Error('usePresence must be used within a PresenceProvider');
    }
    return context;
};
