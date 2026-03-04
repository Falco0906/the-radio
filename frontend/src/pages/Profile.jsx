import { useState } from 'react'
import { useAuth } from '../contexts/AuthContext'
import Layout from '../components/Layout'
import './Profile.css'

const Profile = () => {
  const { user } = useAuth()
  const [showGuide, setShowGuide] = useState(false)

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

        <button className="guide-btn" onClick={() => setShowGuide(true)}>
          ? Guide
        </button>

        {showGuide && (
          <div className="guide-overlay" onClick={() => setShowGuide(false)}>
            <div className="guide-popup" onClick={(e) => e.stopPropagation()}>
              <div className="guide-header">
                <h2>Welcome to The Radio</h2>
                <button className="guide-close" onClick={() => setShowGuide(false)}>✕</button>
              </div>

              <div className="guide-content">
                <div className="guide-step">
                  <div className="step-number">1</div>
                  <div>
                    <strong>Connect Spotify</strong>
                    <p>Go to <strong>Platforms</strong> and connect your Spotify account. This lets The Radio detect what you're listening to.</p>
                  </div>
                </div>

                <div className="guide-step">
                  <div className="step-number">2</div>
                  <div>
                    <strong>Go Live</strong>
                    <p>On the <strong>Dashboard</strong>, toggle the <strong>Live</strong> button so your friends can see your music in realtime.</p>
                  </div>
                </div>

                <div className="guide-step">
                  <div className="step-number">3</div>
                  <div>
                    <strong>Add Friends</strong>
                    <p>Head to <strong>Friends → Find People</strong> and search for users. Send them a friend request!</p>
                  </div>
                </div>

                <div className="guide-step">
                  <div className="step-number">4</div>
                  <div>
                    <strong>Tune In</strong>
                    <p>Once you have friends, their currently playing tracks will appear on your Dashboard. Hit <strong>Tune In</strong> to listen along.</p>
                  </div>
                </div>

                <div className="guide-tip">
                  <strong>Get started:</strong> Add <span className="highlight">tofutrash._</span> as a friend to see the app in action!
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </Layout>
  )
}

export default Profile
