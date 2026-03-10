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
      const start = cities.find(c => c.id === startCity);
      const end = cities.find(c => c.id === endCity);

      if (start) pointsOfInterest.push([start.lat, start.lng]);
      if (end) pointsOfInterest.push([end.lat, end.lng]);
    }

    // If we have points to focus on, fit bounds
    if (pointsOfInterest.length > 0) {
      const bounds = L.latLngBounds(pointsOfInterest);
      map.fitBounds(bounds, { padding: [50, 50], maxZoom: 8 });
    } else if (cities.length > 0) {
      // Fallback to showing all cities
      const allCityCoords = cities.map(c => [c.lat, c.lng]);
      const bounds = L.latLngBounds(allCityCoords);
      map.fitBounds(bounds, { padding: [50, 50] });
    }
  }, [cities, startCity, endCity, shortestPath, map]);

  return null;
};

const MapView = ({ cities, roads, shortestPath, startCity, endCity, visitedNodes = [], visitedEdges = [], evaluatingEdge = null, simulationStarted = false, animationSpeed = 3 }) => {
  const [isSatellite, setIsSatellite] = React.useState(false);
  const [animatedPath, setAnimatedPath] = React.useState([]);
  const center = [20.5937, 78.9629];

  // Animated shortest path drawing just like Google Maps
  React.useEffect(() => {
    if (shortestPath && shortestPath.length > 0) {
      if (shortestPath.length === 1) {
        setAnimatedPath([[shortestPath[0].lat, shortestPath[0].lng]]);
        return;
      }

      setAnimatedPath([[shortestPath[0].lat, shortestPath[0].lng]]);
      let index = 1;

      const interval = setInterval(() => {
        setAnimatedPath(prev => {
          if (index < shortestPath.length) {
            return [...prev, [shortestPath[index].lat, shortestPath[index].lng]];
          }
          return prev;
        });

        index++;

        if (index >= shortestPath.length) {
          clearInterval(interval);
        }
      }, 800 / animationSpeed);

      return () => clearInterval(interval);
    } else {
      setAnimatedPath([]);
    }
  }, [shortestPath, animationSpeed]);

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
      <MapContainer center={center} zoom={5} style={{ height: '100%', width: '100%', backgroundColor: '#0f172a' }}>
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
          shortestPath={shortestPath}
        />

        {roads.map((road, idx) => {
          const fromCity = cities.find(c => c.id === road.from);
          const toCity = cities.find(c => c.id === road.to);
          if (!fromCity || !toCity) return null;

          const isShortestPath = shortestPath && shortestPath.length > 0 && shortestPath.some((node, i) => {
            if (i === 0) return false;
            const prevNode = shortestPath[i - 1];
            return (node.id === road.from && prevNode.id === road.to) ||
              (node.id === road.to && prevNode.id === road.from);
          });

          // Check if visited or evaluating
          const isVisited = !isShortestPath && visitedEdges.some(e =>
            (e.from === road.from && e.to === road.to) || (e.from === road.to && e.to === road.from)
          );

          const isEvaluating = !isShortestPath && evaluatingEdge &&
            ((evaluatingEdge.from === road.from && evaluatingEdge.to === road.to) ||
              (evaluatingEdge.from === road.to && evaluatingEdge.to === road.from));

          // Determine traffic color mapping (Low = Green, Med = Yellow, High = Red)
          let baseRoadColor = '#22c55e'; // default green (low traffic)
          if (road.trafficLevel >= 0.7) {
            baseRoadColor = '#ef4444'; // red (heavy traffic)
          } else if (road.trafficLevel >= 0.4) {
            baseRoadColor = '#eab308'; // yellow (moderate traffic)
          }

          let color = baseRoadColor;
          let weight = 2;
          let className = '';
          let dashArray = '';

          // We defer shortest path drawing to the animated polyline below,
          // so we don't draw it solid here right away
          if (isShortestPath) {
            // Just draw the underlying road structure lightly underneath
            color = baseRoadColor;
            weight = 2;
          } else if (isEvaluating) {
            color = '#eab308'; // Yellow (exploring edge)
            weight = 4;
            className = 'glowing-border';
          } else if (isVisited) {
            color = baseRoadColor; // Revert to base after visit
            weight = 3;
            className = 'path-animation';
          }

          if (!simulationStarted) {
            return null; // hide on initial empty load
          }

          return (
            <Polyline
              key={`road-${idx}`}
              positions={[[fromCity.lat, fromCity.lng], [toCity.lat, toCity.lng]]}
              color={color}
              weight={weight}
              className={className}
              dashArray={dashArray}
            >
              <Tooltip sticky>
                <strong>Distance:</strong> {road.distance.toFixed(1)} km<br />
                <strong>Type:</strong> {road.roadType || 'SH'}<br />
                <strong>Traffic:</strong> {road.trafficLevel < 0.4 ? 'Low' : road.trafficLevel < 0.7 ? 'Moderate' : 'Heavy'}
              </Tooltip>
            </Polyline>
          );
        })}

        {animatedPath.length > 1 && (
          <Polyline
            positions={animatedPath}
            color="lime"
            weight={6}
            className="path-animation glowing-border"
          />
        )}

        {cities.map(city => {
          let color = '#9ca3af'; // Gray (unvisited node)

          const isShortestPathNode = shortestPath && shortestPath.length > 0 && shortestPath.some(n => n.id === city.id);
          const isVisitedNode = visitedNodes.includes(city.id);

          const isEvaluatingNode = evaluatingEdge &&
            (evaluatingEdge.from === city.id || evaluatingEdge.to === city.id);

          if (isShortestPathNode) {
            color = '#ef4444'; // Red (final shortest path)
          } else if (isEvaluatingNode) {
            color = '#eab308'; // Yellow (currently being explored)
          } else if (isVisitedNode) {
            color = '#22c55e'; // Green (visited)
          } else if (city.id === startCity) {
            color = '#3b82f6'; // Keep start distinct if desired (or change if user insists)
          } else if (city.id === endCity) {
            color = '#a855f7'; // Keep dest distinct if desired
          }

          // Removed null check so all nodes show by default

          if (!simulationStarted && city.id !== startCity && city.id !== endCity) {
            return null;
          }

          return (
            <Marker key={city.id} position={[city.lat, city.lng]} icon={createCustomIcon(color)}>
              <Tooltip direction="top" offset={[0, -10]} opacity={1}>
                <strong>{city.name}</strong><br />
                Lat: {city.lat.toFixed(4)}<br />
                Lng: {city.lng.toFixed(4)}
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
