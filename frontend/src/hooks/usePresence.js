import { useState, useEffect, useRef, useCallback } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useAuth } from '../contexts/AuthContext'
import apiClient from '../api/axios'

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
    if (!token) return

    fetchCurrentPresence()

    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        client.subscribe(`/user/queue/presence`, (message) => {
          const data = JSON.parse(message.body)
          handlePresenceUpdate(data)
        })
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame)
      }
    })

    clientRef.current = client
    client.activate()

    return () => {
      if (clientRef.current) {
        clientRef.current.deactivate()
      }
    }
  }, [token, fetchCurrentPresence])

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

