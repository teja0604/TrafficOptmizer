import React, { useState } from 'react';

const AddCityModal = ({ isOpen, onClose, onAdd }) => {
  const [name, setName] = useState('');
  const [lat, setLat] = useState('');
  const [lng, setLng] = useState('');

  if (!isOpen) return null;

  const parseCoordinate = (str) => {
    const cleanStr = String(str).trim();
    if (!isNaN(cleanStr) && !isNaN(parseFloat(cleanStr))) return parseFloat(cleanStr);

    const regex = /(\d+(?:\.\d+)?)[°\s]*(\d+(?:\.\d+)?)?['\s]*(\d+(?:\.\d+)?)?["\s]*([NSEWnsew])?/i;
    const match = cleanStr.match(regex);
    if (!match) return NaN;

    let degrees = parseFloat(match[1] || 0);
    let minutes = parseFloat(match[2] || 0);
    let seconds = parseFloat(match[3] || 0);
    let direction = (match[4] || '').toUpperCase();

    let dd = degrees + (minutes / 60) + (seconds / 3600);
    if (direction === 'S' || direction === 'W') {
      dd = dd * -1;
    }
    return dd;
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (name && lat && lng) {
      const parsedLat = parseCoordinate(lat);
      const parsedLng = parseCoordinate(lng);

      if (isNaN(parsedLat) || isNaN(parsedLng)) {
        alert("Invalid coordinate format. Please use Decimals or Degrees (e.g. 28.6139 or 28°36'50\"N)");
        return;
      }

      onAdd({ name, lat: parsedLat, lng: parsedLng });
      setName(''); setLat(''); setLng('');
      onClose();
    }
  };

  return (
    <div className="modal-overlay fade-in">
      <div className="modal-content glass-card glowing-border" style={{ animation: 'fadeIn 0.3s ease' }}>
        <h3 className="glow-text">Add New City</h3>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>City Name</label>
            <input className="input-field" value={name} onChange={e => setName(e.target.value)} required />
          </div>
          <div className="form-group" style={{ marginTop: '15px' }}>
            <label>Latitude</label>
            <input className="input-field" type="text" placeholder="e.g. 17.3850 or 17°23'6&quot;N" value={lat} onChange={e => setLat(e.target.value)} required />
          </div>
          <div className="form-group" style={{ marginTop: '15px' }}>
            <label>Longitude</label>
            <input className="input-field" type="text" placeholder="e.g. 78.4867 or 78°29'12&quot;E" value={lng} onChange={e => setLng(e.target.value)} required />
          </div>
          <div className="modal-actions">
            <button type="button" className="btn-secondary" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn-primary">Add City</button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default AddCityModal;
