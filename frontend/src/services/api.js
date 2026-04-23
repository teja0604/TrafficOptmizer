import axios from 'axios';

const normalizeApiBaseUrl = (baseUrl) => {
  const trimmed = String(baseUrl || '').trim().replace(/\/+$/, '');
  if (!trimmed) return '';
  if (trimmed.endsWith('/api')) return trimmed;
  return `${trimmed}/api`;
};

const API_BASE_URL = normalizeApiBaseUrl(import.meta.env.VITE_API_URL);

if (!API_BASE_URL) {
  throw new Error('VITE_API_URL is missing in production');
}

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 60000,
});

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    console.error('API ERROR:', error?.response || error.message);
    if (error.code === 'ECONNABORTED') {
      console.warn('Backend still waking up (Render cold start)');
    }

    const config = error.config;

    if (!config || config.__retry) {
      return Promise.reject(error);
    }

    config.__retry = true;
    console.log('Retrying request...');

    return new Promise((resolve) => {
      setTimeout(() => resolve(api(config)), 8000);
    });
  }
);

api.addCity = async (cityData) => {
  const payload = {
    name: cityData.name,
    latitude: cityData.lat,
    longitude: cityData.lng,
  };
  const res = await api.post('/cities', payload);
  if (res.data) {
    res.data = { ...res.data, lat: res.data.latitude, lng: res.data.longitude };
  }
  return res;
};

api.addRoad = async (roadData) => {
  const payload = {
    fromCityId: roadData.from,
    toCityId: roadData.to,
    distance: roadData.distance,
    roadType: roadData.roadType,
  };
  const res = await api.post('/roads', payload);
  if (res.data) {
    res.data = {
      ...res.data,
      from: res.data.fromCity?.id || res.data.fromCity,
      to: res.data.toCity?.id || res.data.toCity,
    };
  }
  return res;
};

api.getCities = async () => {
  const res = await api.get('/cities');
  return {
    ...res,
    data: res.data.map((c) => ({
      ...c,
      lat: c.latitude,
      lng: c.longitude,
    })),
  };
};

api.getRoads = async () => {
  const res = await api.get('/roads');
  return {
    ...res,
    data: res.data.map((r) => ({
      ...r,
      from: r.fromCity?.id || r.fromCity,
      to: r.toCity?.id || r.toCity,
    })),
  };
};

api.shortestPath = async (startId, endId, trafficLevel) => {
  const res = await api.post('/shortest-path', {
    startCity: startId,
    endCity: endId,
    trafficLevel,
  });

  if (res.data && res.data.steps) {
    res.data.steps = res.data.steps.map((step) => ({
      ...step,
      visitedEdges: step.visitedEdges
        ? step.visitedEdges.map((e) => ({ ...e, from: e.fromCity, to: e.toCity }))
        : [],
      evaluatingEdge: step.evaluatingEdge
        ? { ...step.evaluatingEdge, from: step.evaluatingEdge.fromCity, to: step.evaluatingEdge.toCity }
        : null,
      shortestPath: step.shortestPath
        ? step.shortestPath.map((c) => ({ ...c, lat: c.latitude, lng: c.longitude }))
        : [],
    }));
  }

  if (res.data?.path) {
    res.data.path = res.data.path.map((c) => ({ ...c, lat: c.latitude, lng: c.longitude }));
  }
  if (res.data?.enrichedPath) {
    res.data.enrichedPath = res.data.enrichedPath.map((c) => ({ ...c, lat: c.latitude, lng: c.longitude }));
  }

  return res;
};

api.getRoadPath = async (pathCities) => {
  if (!pathCities || pathCities.length < 2) return [];

  try {
    const segmentPromises = [];
    for (let i = 0; i < pathCities.length - 1; i += 1) {
      const start = pathCities[i];
      const end = pathCities[i + 1];
      const coordinates = `${start.lng},${start.lat};${end.lng},${end.lat}`;
      segmentPromises.push(
        axios
          .get(`https://router.project-osrm.org/route/v1/driving/${coordinates}?overview=full&geometries=geojson`)
          .then((res) => {
            if (res.data?.routes?.[0]) {
              return res.data.routes[0].geometry.coordinates.map((coord) => [coord[1], coord[0]]);
            }
            return [[start.lat, start.lng], [end.lat, end.lng]];
          })
          .catch(() => [[start.lat, start.lng], [end.lat, end.lng]])
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
    console.error('OSRM Routing failed:', err);
    return pathCities.map((c) => [c.lat, c.lng]);
  }
};

export default api;
