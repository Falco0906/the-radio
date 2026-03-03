import React from 'react';
import './Playbox.css';

const Playbox = ({ presence, title }) => {
    if (!presence || (!presence.trackName && !presence.isPlaying)) {
        return (
            <div className="playbox empty">
                {title && <h3 className="playbox-title">{title}</h3>}
                <div className="track-info">
                    <div className="text-info">
                        <div className="track-name">Not playing</div>
                        <div className="artist-name text-muted">Radio is silent...</div>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className={`playbox ${presence.isPlaying ? 'playing' : ''}`}>
            {title && <h3 className="playbox-title">{title}</h3>}
            <div className="track-info">
                {presence.albumArtUrl ? (
                    <img src={presence.albumArtUrl} alt="Album Art" className="album-art" />
                ) : (
                    <div className="album-art-fallback" />
                )}
                <div className="text-info">
                    <div className="track-name">{presence.trackName}</div>
                    <div className="artist-name">{presence.artist}</div>
                </div>

                {presence.isPlaying && (
                    <div className="waveform">
                        <div className="waveform-bar"></div>
                        <div className="waveform-bar"></div>
                        <div className="waveform-bar"></div>
                        <div className="waveform-bar"></div>
                        <div className="waveform-bar"></div>
                    </div>
                )}
            </div>

            <div className="platform-info">
                <span className={`platform-badge ${presence.platform?.toLowerCase() || ''}`}>
                    {presence.platform}
                </span>
            </div>
        </div>
    );
};

export default Playbox;
