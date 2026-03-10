import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

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

    // Normalize top-level path
    if (res.data && res.data.path) {
      res.data.path = res.data.path.map(c => ({ ...c, lat: c.latitude, lng: c.longitude }));
    }

    return res;
  }
};
