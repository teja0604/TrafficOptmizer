import React, { useState } from 'react';

const AddRoadModal = ({ isOpen, onClose, onAdd, cities }) => {
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [distance, setDistance] = useState('');
  const [roadType, setRoadType] = useState('SH');

  if (!isOpen) return null;

  const handleSubmit = (e) => {
    e.preventDefault();
    if (from && to && from !== to) {
      onAdd({ from, to, distance: distance ? parseFloat(distance) : 0, roadType });
      setFrom(''); setTo(''); setDistance(''); setRoadType('SH');
      onClose();
    }
  };

  return (
    <div className="modal-overlay fade-in">
      <div className="modal-content glass-card glowing-border" style={{ animation: 'fadeIn 0.3s ease' }}>
        <h3 className="glow-text">Add New Road</h3>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>From City</label>
            <select className="input-field" value={from} onChange={e => setFrom(e.target.value)} required>
              <option value="">Select city</option>
              {cities.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </div>
          <div className="form-group" style={{ marginTop: '15px' }}>
            <label>To City</label>
            <select className="input-field" value={to} onChange={e => setTo(e.target.value)} required>
              <option value="">Select city</option>
              {cities.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </div>
          <div className="form-group" style={{ marginTop: '15px' }}>
            <label>Distance (km) [Leave empty for auto-calculate]</label>
            <input className="input-field" type="number" value={distance} onChange={e => setDistance(e.target.value)} />
          </div>
          <div className="form-group" style={{ marginTop: '15px' }}>
            <label>Road Type</label>
            <select className="input-field" value={roadType} onChange={e => setRoadType(e.target.value)} required>
              <option value="NH">National Highway (80 km/h)</option>
              <option value="SH">State Highway (60 km/h)</option>
              <option value="CITY">City Road (40 km/h)</option>
              <option value="VILLAGE">Village Road (25 km/h)</option>
              <option value="BAD_SH">Bad State Highway (30 km/h)</option>
            </select>
          </div>
          <div className="modal-actions">
            <button type="button" className="btn-secondary" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn-primary">Add Road</button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default AddRoadModal;
