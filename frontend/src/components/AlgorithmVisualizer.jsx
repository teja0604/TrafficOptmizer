import React from 'react';
import './components.css';

const AlgorithmVisualizer = ({ shortestPathSequence, totalDistance, travelTimes }) => {
  if (!shortestPathSequence || shortestPathSequence.length === 0) {
    return (
      <div style={{ color: 'var(--text-secondary)', textAlign: 'center', marginTop: '20px' }}>
        Select a start and destination city, then click "Find Shortest Path" to see the optimal road route!
      </div>
    );
  }

  return (
    <div className="algorithm-visualizer">
      <h3 style={{ marginBottom: '15px', color: 'var(--accent-cyan)' }}>Shortest Route Details</h3>
      
      <div className="summary-card glass-card" style={{ padding: '15px', marginBottom: '20px', border: '1px solid rgba(0, 229, 255, 0.2)' }}>
        <h4 style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', textTransform: 'uppercase', marginBottom: '15px' }}>Itinerary</h4>
        <div style={{ 
          display: 'flex',
          flexWrap: 'wrap',
          alignItems: 'center',
          justifyContent: 'center',
          gap: '10px',
          color: 'var(--text-primary)', 
          fontWeight: 600,
          padding: '10px 0'
        }}>
          {shortestPathSequence.map((city, idx) => (
            <React.Fragment key={city.id}>
              <div style={{ 
                padding: '8px 16px', 
                background: 'rgba(0, 229, 255, 0.1)', 
                borderRadius: '8px',
                border: '1px solid rgba(0, 229, 255, 0.3)',
                textAlign: 'center',
                boxShadow: '0 0 10px rgba(0, 229, 255, 0.05)',
                whiteSpace: 'nowrap'
              }}>
                {idx === shortestPathSequence.length - 1 ? (
                  <span style={{ color: 'var(--accent-cyan)' }}>{city.name}</span>
                ) : (
                  city.name
                )}
              </div>
              {idx < shortestPathSequence.length - 1 && (
                <div style={{ color: 'var(--accent-cyan)', fontSize: '1.2rem' }}>→</div>
              )}
            </React.Fragment>
          ))}
        </div>
      </div>

      <div className="metrics-grid" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px', marginBottom: '20px' }}>
        <div className="metric-box" style={{ background: 'var(--bg-secondary)', padding: '12px', borderRadius: '8px', textAlign: 'center', border: '1px solid var(--border-color)' }}>
          <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>Total Distance</div>
          <div style={{ fontSize: '1.2rem', fontWeight: 'bold', color: 'var(--text-primary)' }}>{totalDistance.toFixed(1)} km</div>
        </div>
      </div>

      <div className="travel-times-summary">
        <h4 style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', textTransform: 'uppercase', marginBottom: '12px' }}>Estimated Travel Times</h4>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
          <div className="time-item" style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 12px', background: 'var(--bg-secondary)', borderRadius: '6px', border: '1px solid var(--border-color)' }}>
            <span style={{ color: 'var(--text-secondary)' }}>🚗 Car</span>
            <span style={{ fontWeight: 'bold', color: 'var(--text-primary)' }}>{travelTimes?.car}</span>
          </div>
          <div className="time-item" style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 12px', background: 'var(--bg-secondary)', borderRadius: '6px', border: '1px solid var(--border-color)' }}>
            <span style={{ color: 'var(--text-secondary)' }}>🏍️ Bike</span>
            <span style={{ fontWeight: 'bold', color: 'var(--text-primary)' }}>{travelTimes?.bike}</span>
          </div>
          <div className="time-item" style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 12px', background: 'var(--bg-secondary)', borderRadius: '6px', border: '1px solid var(--border-color)' }}>
            <span style={{ color: 'var(--text-secondary)' }}>🚌 Bus</span>
            <span style={{ fontWeight: 'bold', color: 'var(--text-primary)' }}>{travelTimes?.bus}</span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AlgorithmVisualizer;
