import React from 'react';

const CitySelector = ({ cities, onSelectStart, onSelectEnd, startCity, endCity }) => {
  return (
    <div className="city-selector">
      <div className="form-group">
        <label>Start City</label>
        <select className="input-field" value={startCity || ""} onChange={(e) => onSelectStart(e.target.value)}>
          <option value="">Select Start City</option>
          {cities.map(city => (
            <option key={city.id} value={city.id}>{city.name}</option>
          ))}
        </select>
      </div>
      <div className="form-group" style={{ marginTop: '15px' }}>
        <label>Destination City</label>
        <select className="input-field" value={endCity || ""} onChange={(e) => {
          if (e.target.value === startCity) {
            alert("Destination cannot be the same as Start City");
            return;
          }
          onSelectEnd(e.target.value);
        }}>
          <option value="">Select Destination</option>
          {cities.map(city => (
            <option key={city.id} value={city.id}>{city.name}</option>
          ))}
        </select>
      </div>
    </div>
  );
};

export default CitySelector;
