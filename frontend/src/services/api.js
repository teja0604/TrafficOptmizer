import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

export const api = {
  addCity: async (cityData) => {
    const payload = {
      name: cityData.name,
      latitude: cityData.lat,
      longitude: cityData.lng
    };
    const res = await axios.post(`${API_BASE_URL}/cities`, payload);
    if (res.data) {
      res.data = { ...res.data, lat: res.data.latitude, lng: res.data.longitude };
    }
    return res;
  },
  addRoad: async (roadData) => {
    return await axios.post(`${API_BASE_URL}/roads`, roadData);
  },
  getCities: async () => {
    const res = await axios.get(`${API_BASE_URL}/cities`);
    res.data = res.data.map(c => ({ ...c, lat: c.latitude, lng: c.longitude }));
    return res;
  },
  getRoads: async () => {
    const res = await axios.get(`${API_BASE_URL}/roads`);
    res.data = res.data.map(r => ({ ...r, from: r.fromCity, to: r.toCity }));
    return res;
  },
  shortestPath: async (startId, endId, trafficLevel) => {
    const res = await axios.post(`${API_BASE_URL}/shortest-path`, {
      startCity: startId,
      endCity: endId,
      trafficLevel: trafficLevel
    });

    if (res.data && res.data.steps) {
      res.data.steps = res.data.steps.map(step => ({
        ...step,
        visitedEdges: step.visitedEdges ? step.visitedEdges.map(e => ({ ...e, from: e.fromCity, to: e.toCity })) : [],
        evaluatingEdge: step.evaluatingEdge ? { ...step.evaluatingEdge, from: step.evaluatingEdge.fromCity, to: step.evaluatingEdge.toCity } : null,
        shortestPath: step.shortestPath ? step.shortestPath.map(c => ({ ...c, lat: c.latitude, lng: c.longitude })) : []
      }));
    }

    // Normalize top-level path and enriched path
    if (res.data) {
      if (res.data.path) {
        res.data.path = res.data.path.map(c => ({ ...c, lat: c.latitude, lng: c.longitude }));
      }
      if (res.data.enrichedPath) {
        res.data.enrichedPath = res.data.enrichedPath.map(c => ({ ...c, lat: c.latitude, lng: c.longitude }));
      }
    }

    return res;
  },

  /**
   * Fetches the real road path geometry from OSRM between a sequence of cities.
   * Fetches segment by segment to ensure completion and merges them.
   * @param {Array} pathCities - Array of city objects with lat and lng.
   * @returns {Promise<Array>} - Array of [lat, lng] coordinates.
   */
  getRoadPath: async (pathCities) => {
    if (!pathCities || pathCities.length < 2) return [];

    try {
      const segmentPromises = [];
      for (let i = 0; i < pathCities.length - 1; i++) {
        const start = pathCities[i];
        const end = pathCities[i + 1];
        const coordinates = `${start.lng},${start.lat};${end.lng},${end.lat}`;
        segmentPromises.push(
          axios.get(`http://router.project-osrm.org/route/v1/driving/${coordinates}?overview=full&geometries=geojson`)
            .then(res => {
              if (res.data && res.data.routes && res.data.routes[0]) {
                return res.data.routes[0].geometry.coordinates.map(coord => [coord[1], coord[0]]);
              }
              return [[start.lat, start.lng], [end.lat, end.lng]]; // Segment fallback
            })
            .catch(() => [[start.lat, start.lng], [end.lat, end.lng]]) // Segment error fallback
        );
      }

      const segments = await Promise.all(segmentPromises);
      
      let fullPath = [];
      segments.forEach((segment, idx) => {
        if (idx === 0) {
          fullPath = segment;
        } else {
          fullPath = [...fullPath, ...segment.slice(1)];
        }
      });

      return fullPath;
    } catch (err) {
      console.error("OSRM Routing failed:", err);
      return pathCities.map(c => [c.lat, c.lng]);
    }
  }
};
