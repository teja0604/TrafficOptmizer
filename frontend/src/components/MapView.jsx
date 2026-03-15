import React, { useEffect, useState } from 'react';
import { MapContainer, TileLayer, Marker, Popup, Polyline, useMap, useMapEvents, Tooltip } from 'react-leaflet';
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
    html: `<div style="background-color: ${color}; width: 22px; height: 22px; border-radius: 50%; border: 3px solid #fff; box-shadow: 0 0 15px ${color};"></div>`,
    iconSize: [22, 22],
    iconAnchor: [11, 11],
  });
};

const MapUpdater = ({ shortestPath, alternativePath }) => {
  const map = useMap();
  useEffect(() => {
    const points = [];
    if (shortestPath && shortestPath.length > 0) {
      shortestPath.forEach(p => points.push([p[0], p[1]]));
    }
    if (alternativePath && alternativePath.length > 0) {
      alternativePath.forEach(p => points.push([p[0], p[1]]));
    }

    if (points.length > 1) {
      const bounds = L.latLngBounds(points);
      map.fitBounds(bounds, { padding: [50, 50], maxZoom: 8 });
    }
  }, [shortestPath, alternativePath, map]);

  return null;
};

const ClickHandler = ({ onMapClick }) => {
  useMapEvents({
    click(e) {
      onMapClick(e.latlng.lat, e.latlng.lng);
    },
  });
  return null;
};

const MapView = ({ cities, shortestPath, alternativePath, shortestPathSequence, startCity, endCity, onMapClick }) => {
  const [isSatellite, setIsSatellite] = useState(false);
  const [animatedPath, setAnimatedPath] = useState([]);
  const [animatedAltPath, setAnimatedAltPath] = useState([]);
  const center = [20.5937, 78.9629];

  // Smooth path animation helper
  const animatePath = (path, setter) => {
    if (!path || path.length < 2) {
      setter(path || []);
      return;
    }

    setter([path[0]]);
    const totalPoints = path.length;
    const duration = 1200;
    const fps = 60;
    const totalFrames = (duration / 1000) * fps;
    const step = Math.max(1, Math.ceil(totalPoints / totalFrames));
    
    let index = 0;
    const interval = setInterval(() => {
      index += step;
      if (index >= totalPoints) {
        setter(path);
        clearInterval(interval);
      } else {
        setter(path.slice(0, index));
      }
    }, 1000 / fps);
    
    return () => clearInterval(interval);
  };

  useEffect(() => {
    const cleanup = animatePath(shortestPath, setAnimatedPath);
    return cleanup;
  }, [shortestPath]);

  useEffect(() => {
    const cleanup = animatePath(alternativePath, setAnimatedAltPath);
    return cleanup;
  }, [alternativePath]);

  const indiaBounds = [[6.4627, 68.1097], [35.5133, 97.3956]];

  return (
    <div id="map" style={{ height: '100%', width: '100%', position: 'relative' }}>
      <button
        onClick={() => setIsSatellite(!isSatellite)}
        className="btn-secondary"
        style={{
          position: 'absolute',
          top: '20px',
          right: '20px',
          zIndex: 1000,
          background: 'rgba(15, 23, 42, 0.9)',
          color: 'var(--accent-cyan)',
          borderColor: 'var(--accent-cyan)',
        }}
      >
        {isSatellite ? 'Standard View' : 'Satellite View'}
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
          attribution="&copy; OpenStreetMap contributors &copy; CARTO"
        />
        
        <MapUpdater shortestPath={shortestPath} alternativePath={alternativePath} />
        <ClickHandler onMapClick={onMapClick} />

        {/* Alternative Path (Orange) */}
        {animatedAltPath.length > 1 && (
          <Polyline
            positions={animatedAltPath}
            color="#f97316" // Orange
            weight={4}
            opacity={0.8}
            dashArray="10, 10"
            className="path-animation"
          >
            <Tooltip permanent={false}>Alternative Route</Tooltip>
          </Polyline>
        )}

        {/* Shortest Path (Blue) */}
        {animatedPath.length > 1 && (
          <Polyline
            positions={animatedPath}
            color="#3b82f6" // Blue
            weight={6}
            className="path-animation glowing-border"
          >
            <Tooltip permanent={false}>Shortest Route</Tooltip>
          </Polyline>
        )}

        {cities.map(city => {
          const isStart = String(city.id) === String(startCity);
          const isEnd = String(city.id) === String(endCity);
          const isInPath = shortestPathSequence && shortestPathSequence.some(c => String(c.id) === String(city.id));

          if (!isStart && !isEnd && !isInPath) return null;

          let color = '#3b82f6'; // Shortest route (blue)
          if (isEnd) color = '#ef4444'; // End (red)
          
          return (
            <Marker key={city.id} position={[city.lat, city.lng]} icon={createCustomIcon(color)}>
              <Tooltip direction="top" offset={[0, -10]} opacity={1} permanent={isStart || isEnd}>
                <strong>{isStart ? 'START: ' : isEnd ? 'DEST: ' : ''}{city.name}</strong>
              </Tooltip>
              <Popup>
                <div style={{ color: '#0f172a' }}>
                  <strong>{city.name}</strong>
                  <br />
                  Lat: {city.lat.toFixed(4)}, Lng: {city.lng.toFixed(4)}
                </div>
              </Popup>
            </Marker>
          );
        })}
      </MapContainer>
    </div>
  );
};

export default MapView;
