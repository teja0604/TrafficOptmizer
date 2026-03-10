import React from 'react';
import './components.css';

const ResultPanel = ({ shortestPath, totalDistance, travelTimes }) => {
  if (!shortestPath || shortestPath.length === 0) {
    return null;
  }

  return (
    <div className="result-panel" style={{ marginTop: '20px', padding: '15px 0', backgroundColor: 'transparent', border: 'none' }}>
      <h3 style={{ color: '#00e5ff', marginBottom: '15px', fontSize: '1.2rem', fontWeight: 'bold' }}>Final Path:</h3>
      
      <div style={{ color: '#ffffff', fontSize: '1.1rem', marginBottom: '25px', lineHeight: '1.5' }}>
        {shortestPath.map((c, index) => (
          <span key={c.id}>
            {c.name}
            {index < shortestPath.length - 1 && <span style={{ margin: '0 8px' }}>→</span>}
          </span>
        ))}
      </div>

      <h3 style={{ color: '#00e5ff', marginBottom: '15px', fontSize: '1.2rem', fontWeight: 'bold' }}>Total Distance:</h3>
      
      <div style={{ color: '#ffffff', fontSize: '1.1rem' }}>
        {totalDistance.toFixed(2)} km
      </div>

      {travelTimes && (
        <div style={{ marginTop: '20px', animation: 'fadeIn 0.5s ease' }}>
          <h3 style={{ color: '#00e5ff', marginBottom: '15px', fontSize: '1.2rem', fontWeight: 'bold' }}>Estimated Travel Time:</h3>
          <div style={{ display: 'grid', gap: '10px', color: '#ffffff', fontSize: '1.05rem' }}>
            <div style={{ padding: '8px 12px', background: 'rgba(255, 255, 255, 0.05)', borderRadius: '6px' }}>🚗 Car: <strong>{travelTimes.car.toFixed(2)} hrs</strong></div>
            <div style={{ padding: '8px 12px', background: 'rgba(255, 255, 255, 0.05)', borderRadius: '6px' }}>🚌 Bus: <strong>{travelTimes.bus.toFixed(2)} hrs</strong></div>
            <div style={{ padding: '8px 12px', background: 'rgba(255, 255, 255, 0.05)', borderRadius: '6px' }}>🏍️ Bike: <strong>{travelTimes.bike.toFixed(2)} hrs</strong></div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ResultPanel;
