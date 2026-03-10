import React from 'react';
import './components.css';

const TrafficSlider = ({ trafficLevel, setTrafficLevel }) => {
  return (
    <div className="traffic-slider form-group" style={{ marginTop: '20px' }}>
      <label>Traffic Simulation Level</label>
      <input 
        type="range" 
        min="0" 
        max="1" 
        step="0.1"
        value={trafficLevel} 
        onChange={(e) => setTrafficLevel(parseFloat(e.target.value))} 
        className="slider"
      />
      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
        <span>Low</span>
        <span>High</span>
      </div>
    </div>
  );
};

export default TrafficSlider;
