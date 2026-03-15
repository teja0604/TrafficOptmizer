import React, { useEffect } from 'react';
import { MapContainer, TileLayer, Marker, Popup, Polyline, useMap, Tooltip } from 'react-leaflet';
import L from 'leaflet';
import './components.css';

delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
});

const createCustomIcon = (color) => {
  return L.divIcon({
    className: 'custom-city-marker',
    html: `<div style="background-color: ${color}; width: 20px; height: 20px; border-radius: 50%; border: 3px solid #fff; box-shadow: 0 0 15px ${color};"></div>`,
    iconSize: [20, 20],
    iconAnchor: [10, 10],
  });
};

const MapUpdater = ({ cities, startCity, endCity, shortestPath }) => {
  const map = useMap();
  useEffect(() => {
    // Collect all nodes that should be in view
    const pointsOfInterest = [];

    // Add path nodes
    if (shortestPath && shortestPath.length > 0) {
      shortestPath.forEach(node => pointsOfInterest.push([node.lat, node.lng]));
    } else {
      // If no path, at least show start and end if selected
      // Convert to String to ensure comparison works regardless of data type
      const start = cities.find(c => String(c.id) === String(startCity));
      const end = cities.find(c => String(c.id) === String(endCity));

      if (start) pointsOfInterest.push([start.lat, start.lng]);
      if (end) pointsOfInterest.push([end.lat, end.lng]);
    }

    // If we have points to focus on, fit bounds
    if (pointsOfInterest.length > 0) {
      const bounds = L.latLngBounds(pointsOfInterest);
      if (pointsOfInterest.length === 1) {
        map.setView(pointsOfInterest[0], 6);
      } else {
        map.fitBounds(bounds, { padding: [100, 100], maxZoom: 7 });
      }
    }
  }, [cities, startCity, endCity, shortestPath, map]);

  return null;
};

const MapView = ({ cities, shortestPath, shortestPathSequence, startCity, endCity }) => {
  const [isSatellite, setIsSatellite] = React.useState(false);
  const [animatedPath, setAnimatedPath] = React.useState([]);
  const center = [20.5937, 78.9629];

  // Smooth road path animation
  React.useEffect(() => {
    if (shortestPath && shortestPath.length > 0) {
      if (shortestPath.length === 1) {
        setAnimatedPath([shortestPath[0]]);
        return;
      }

      setAnimatedPath([shortestPath[0]]);
      
      // Calculate how many points to add per step to finish within ~1.5 seconds
      const totalPoints = shortestPath.length;
      const duration = 1500; // 1.5 seconds
      const framesPerSecond = 60;
      const totalFrames = (duration / 1000) * framesPerSecond;
      const pointsPerFrame = Math.max(1, Math.ceil(totalPoints / totalFrames));
      
      let currentPointIndex = 0;

      const interval = setInterval(() => {
        currentPointIndex += pointsPerFrame;
        
        if (currentPointIndex >= totalPoints) {
          setAnimatedPath(shortestPath);
          clearInterval(interval);
        } else {
          setAnimatedPath(shortestPath.slice(0, currentPointIndex));
        }
      }, 1000 / framesPerSecond);

      return () => clearInterval(interval);
    } else {
      setAnimatedPath([]);
    }
  }, [shortestPath]);

  const indiaBounds = [[6.4627, 68.1097], [35.5133, 97.3956]];

  return (
    <div style={{ height: '100%', width: '100%', position: 'relative' }}>
      <button
        onClick={() => setIsSatellite(!isSatellite)}
        style={{
          position: 'absolute',
          top: '10px',
          right: '10px',
          zIndex: 1000,
          padding: '8px 12px',
          backgroundColor: 'rgba(15, 23, 42, 0.8)',
          color: '#00e5ff',
          border: '1px solid #00e5ff',
          borderRadius: '4px',
          cursor: 'pointer',
          fontWeight: 'bold'
        }}
      >
        {isSatellite ? 'Switch to Map' : 'Switch to Satellite'}
      </button>
      <MapContainer 
        center={center} 
        zoom={5} 
        minZoom={5}
        maxBounds={indiaBounds}
        maxBoundsViscosity={1.0}
        style={{ height: '100%', width: '100%', backgroundColor: '#0f172a' }}
      >
        <TileLayer
          url={isSatellite
            ? "https://mt1.google.com/vt/lyrs=y&x={x}&y={y}&z={z}"
            : "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"}
          attribution={isSatellite
            ? '&copy; Google Maps'
            : '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>'}
        />
        <MapUpdater
          cities={cities}
          startCity={startCity}
          endCity={endCity}
          shortestPath={shortestPath.length > 0 ? [{lat: shortestPath[0][0], lng: shortestPath[0][1]}, {lat: shortestPath[shortestPath.length-1][0], lng: shortestPath[shortestPath.length-1][1]}] : []}
        />

        {/* Animated Shortest Path (Real Roads) */}
        {animatedPath.length > 1 && (
          <Polyline
            positions={animatedPath}
            color="#00e5ff"
            weight={5}
            className="path-animation glowing-border"
          />
        )}

        {cities.map(city => {
          const isStart = String(city.id) === String(startCity);
          const isEnd = String(city.id) === String(endCity);
          const isInPath = shortestPathSequence && shortestPathSequence.some(c => String(c.id) === String(city.id));

          // Only render markers for start, end, or cities in the final path
          if (!isStart && !isEnd && !isInPath) return null;

          let color = '#00e5ff'; // Default intermediate (cyan)
          if (isStart) color = '#3b82f6'; // Blue
          if (isEnd) color = '#ef4444'; // Red

          return (
            <Marker key={city.id} position={[city.lat, city.lng]} icon={createCustomIcon(color)}>
              <Tooltip direction="top" offset={[0, -10]} opacity={1}>
                <strong>{city.name}</strong>
              </Tooltip>
              <Popup>
                <strong style={{ color: '#0f172a' }}>{city.name}</strong>
              </Popup>
            </Marker>
          );
        })}
      </MapContainer>
    </div>
  );
};

export default MapView;
