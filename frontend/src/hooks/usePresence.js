import { useState, useEffect, useRef } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useAuth } from '../contexts/AuthContext'

export const usePresence = () => {
  const [presenceMap, setPresenceMap] = useState({})
  const { token } = useAuth()
  const clientRef = useRef(null)

  useEffect(() => {
    if (!token) return

    const client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8081/ws'),
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
  }, [token])

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

  return presenceMap
}

