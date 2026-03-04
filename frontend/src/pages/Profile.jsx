import { useState } from 'react'
import { useAuth } from '../contexts/AuthContext'
import Layout from '../components/Layout'
import './Profile.css'

const Profile = () => {
  const { user } = useAuth()

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
      </div>
    </Layout>
  )
}

export default Profile
