import { useState, useEffect, useRef, useCallback } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useAuth } from '../contexts/AuthContext'
import apiClient from '../api/axios'
import { API_BASE } from '../config/api'

export const usePresence = () => {
  const [presenceMap, setPresenceMap] = useState({})
  const { token, user } = useAuth()
  const clientRef = useRef(null)

  const fetchCurrentPresence = useCallback(async () => {
    if (!token) return
    try {
      const response = await apiClient.get('/api/presence/current')
      if (response.data && response.data.userId) {
        handlePresenceUpdate(response.data)
      }
    } catch (error) {
      console.error('Failed to fetch current presence:', error)
    }
  }, [token])

  const refreshPresence = useCallback(async () => {
    if (!token) return
    try {
      const response = await apiClient.post('/api/presence/refresh')
      if (response.data && response.data.userId) {
        handlePresenceUpdate(response.data)
      }
      return response.data
    } catch (error) {
      console.error('Failed to refresh presence:', error)
      throw error
    }
  }, [token])

  useEffect(() => {
    if (!token || !user?.id) {
      console.log('User or token not ready for WebSocket setup', { token: !!token, userId: user?.id });
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
        console.log('STOMP Debug:', str);
      },
      onConnect: () => {
        const userId = user.id;
        console.log("WS connected. current user ID:", userId);

        const topic = `/topic/presence/${userId}`;
        console.log("Subscribing to dynamic topic:", topic);
        client.subscribe(topic, (message) => {
          console.log("WS presence update (dynamic):", message.body);
          const data = JSON.parse(message.body);
          handlePresenceUpdate(data);
        });

        // TEST TOPIC: Hardcoded subscription to rule out ID mismatches
        console.log("Subscribing to: /topic/presence-test");
        client.subscribe("/topic/presence-test", (message) => {
          console.log("WS presence update (test-topic):", message.body);
        });
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame);
      }
    });

    clientRef.current = client;
    client.activate();

    return () => {
      if (clientRef.current) {
        console.log('Deactivating STOMP client');
        clientRef.current.deactivate();
      }
    };
  }, [token, user?.id, fetchCurrentPresence])

  const handlePresenceUpdate = (data) => {
    setPresenceMap((prev) => {
      const newMap = { ...prev }

      if (data.type === 'PRESENCE_OFFLINE') {
        delete newMap[data.userId]
      } else {
        newMap[data.userId] = data
      }

      return newMap
    })
  }

  return { presenceMap, refreshPresence, fetchCurrentPresence }
}

