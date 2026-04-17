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

const MapUpdater = ({ shortestPath, alternativePath, routeMarkers }) => {
  const map = useMap();
  useEffect(() => {
    const points = [];
    if (shortestPath && shortestPath.length > 0) {
      shortestPath.forEach(p => points.push([p[0], p[1]]));
    }
    if (alternativePath && alternativePath.length > 0) {
      alternativePath.forEach(p => points.push([p[0], p[1]]));
    }
    if (points.length === 0 && routeMarkers && routeMarkers.length > 1) {
      routeMarkers.forEach(c => {
        const lat = c.latitude ?? c.lat;
        const lng = c.longitude ?? c.lng;
        if (typeof lat === 'number' && typeof lng === 'number') {
          points.push([lat, lng]);
        }
      });
    }

    if (points.length > 1) {
      const bounds = L.latLngBounds(points);
      map.fitBounds(bounds, { padding: [50, 50], maxZoom: 8 });
    }
  }, [shortestPath, alternativePath, routeMarkers, map]);

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
  const selectedStart = (cities || []).find(c => String(c.id) === String(startCity));
  const selectedEnd = (cities || []).find(c => String(c.id) === String(endCity));
  const routeMarkers = (shortestPathSequence && shortestPathSequence.length > 0)
    ? shortestPathSequence
    : [selectedStart, selectedEnd].filter(Boolean);

  return (
    <div id="map" style={{ height: '100%', width: '100%', position: 'relative' }}>
      <button
        onClick={() => setIsSatellite(!isSatellite)}
        className="btn-secondary btn-overlay"
      >
        {isSatellite ? '📡 Standard' : '🛰️ Satellite'}
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
        
        <MapUpdater shortestPath={shortestPath} alternativePath={alternativePath} routeMarkers={routeMarkers} />
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

        {(routeMarkers || []).map((city, index, arr) => {
          if (!city) return null;
          const lat = city.latitude ?? city.lat;
          const lng = city.longitude ?? city.lng;
          if (typeof lat !== 'number' || typeof lng !== 'number') return null;

          const isStart = index === 0;
          const isEnd = index === arr.length - 1;

          let color = '#3b82f6'; // intermediate (blue)
          if (isStart) color = '#22c55e'; // start (green)
          if (isEnd) color = '#ef4444'; // end (red)

          return (
            <Marker key={city.id ?? `${index}-${city.name}`} position={[lat, lng]} icon={createCustomIcon(color)}>
              <Popup>
                <div style={{ color: '#0f172a' }}>
                  <strong>{index + 1}. {city.name}</strong>
                  <br />
                  Lat: {lat.toFixed(4)}, Lng: {lng.toFixed(4)}
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
