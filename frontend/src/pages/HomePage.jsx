import React, { useState, useEffect } from 'react';
import './HomePage.css';
import MapView from '../components/MapView';
import CitySelector from '../components/CitySelector';
import AlgorithmVisualizer from '../components/AlgorithmVisualizer';
import ResultPanel from '../components/ResultPanel';
import TrafficSlider from '../components/TrafficSlider';
import AddCityModal from '../components/AddCityModal';
import AddRoadModal from '../components/AddRoadModal';
import { api } from '../services/api';


// Initial Mock Data
// Initial Mock Data Expanded
const initialCities = [
  { id: '1', name: 'Hyderabad', lat: 17.3850, lng: 78.4867 },
  { id: '2', name: 'Bangalore', lat: 12.9716, lng: 77.5946 },
  { id: '3', name: 'Chennai', lat: 13.0827, lng: 80.2707 },
  { id: '4', name: 'Mumbai', lat: 19.0760, lng: 72.8777 },
  { id: '5', name: 'Delhi', lat: 28.7041, lng: 77.1025 },
  { id: '6', name: 'Ahmedabad', lat: 23.0225, lng: 72.5714 },
  { id: '7', name: 'Kolkata', lat: 22.5726, lng: 88.3639 },
  { id: '8', name: 'Pune', lat: 18.5204, lng: 73.8567 },
  { id: '9', name: 'Jaipur', lat: 26.9124, lng: 75.7873 },
  { id: '10', name: 'Nagpur', lat: 21.1458, lng: 79.0882 },
  { id: '11', name: 'Lucknow', lat: 26.8467, lng: 80.9462 },
  { id: '12', name: 'Visakhapatnam', lat: 17.6868, lng: 83.2185 },
  { id: '13', name: 'Patna', lat: 25.5941, lng: 85.1376 },
  { id: '14', name: 'Bhopal', lat: 23.2599, lng: 77.4126 },
  { id: '15', name: 'Ludhiana', lat: 30.9010, lng: 75.8573 },
  { id: '16', name: 'Agra', lat: 27.1767, lng: 78.0081 },
  { id: '17', name: 'Nashik', lat: 19.9975, lng: 73.7898 },
  { id: '18', name: 'Vijayawada', lat: 16.5062, lng: 80.6480 },
  { id: '19', name: 'Madurai', lat: 9.9252, lng: 78.1198 },
  { id: '20', name: 'Varanasi', lat: 25.3176, lng: 82.9739 },
  { id: '21', name: 'Coimbatore', lat: 11.0168, lng: 76.9558 },
  { id: '22', name: 'Kochi', lat: 9.9312, lng: 76.2673 },
  { id: '23', name: 'Thiruvananthapuram', lat: 8.5241, lng: 76.9366 },
  { id: '24', name: 'Mysuru', lat: 12.2958, lng: 76.6394 },
  { id: '25', name: 'Guwahati', lat: 26.1158, lng: 91.7086 },
  { id: '26', name: 'Bhubaneswar', lat: 20.2961, lng: 85.8245 },
  { id: '27', name: 'Raipur', lat: 21.2514, lng: 81.6296 },
  { id: '28', name: 'Chandigarh', lat: 30.7333, lng: 76.7794 },
  { id: '29', name: 'Indore', lat: 22.7196, lng: 75.8577 },
  { id: '30', name: 'Surat', lat: 21.1702, lng: 72.8311 },
];

const initialRoads = [
  { from: '1', to: '2', distance: 570 },
  { from: '2', to: '3', distance: 350 },
  { from: '1', to: '8', distance: 590 },
  { from: '1', to: '10', distance: 500 },
  { from: '4', to: '8', distance: 150 },
  { from: '4', to: '6', distance: 530 },
  { from: '6', to: '9', distance: 680 },
  { from: '9', to: '5', distance: 280 },
  { from: '10', to: '5', distance: 1060 },
  { from: '10', to: '7', distance: 1130 },
  { from: '3', to: '7', distance: 1670 },
  { from: '4', to: '5', distance: 1420 },
  { from: '3', to: '12', distance: 800 },
  { from: '12', to: '7', distance: 880 },
  { from: '7', to: '13', distance: 580 },
  { from: '13', to: '11', distance: 500 },
  { from: '11', to: '5', distance: 500 },
];

const HomePage = () => {
  const [cities, setCities] = useState([]);
  const [roads, setRoads] = useState([]);

  const [startCity, setStartCity] = useState('');
  const [endCity, setEndCity] = useState('');
  const [trafficLevel, setTrafficLevel] = useState(1);

  const [shortestPath, setShortestPath] = useState([]); // This will hold the road geometry [lat, lng]
  const [shortestPathSequence, setShortestPathSequence] = useState([]); // This will hold the city objects
  const [totalDistance, setTotalDistance] = useState(0);
  const [travelTimes, setTravelTimes] = useState(null);
  const [isVisualizing, setIsVisualizing] = useState(false);
  const [simulationStarted, setSimulationStarted] = useState(false);
  const [animationSpeed, setAnimationSpeed] = useState(3);
  const [theme, setTheme] = useState('dark');

  const [isCityModalOpen, setCityModalOpen] = useState(false);
  const [isRoadModalOpen, setRoadModalOpen] = useState(false);

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
  }, [theme]);

  const toggleTheme = () => {
    setTheme(prev => prev === 'dark' ? 'light' : 'dark');
  };

  useEffect(() => {
    const fetchData = async () => {
      try {
        const citiesRes = await api.getCities();
        const finalCities = citiesRes.data.length > 0 ? citiesRes.data : initialCities;
        setCities(finalCities);
        
        const roadsRes = await api.getRoads();
        setRoads(roadsRes.data.length > 0 ? roadsRes.data : initialRoads);
      } catch (err) {
        console.error("Failed to load graph data:", err);
        setCities(initialCities);
        setRoads(initialRoads);
      }
    };
    fetchData();
  }, []);

  const handleAddCity = async (cityData) => {
    try {
      const res = await api.addCity(cityData);
      setCities([...cities, res.data]);
      
      // Refresh roads as adding a city might auto-generate connections
      const roadsRes = await api.getRoads();
      setRoads(roadsRes.data);
    } catch (err) {
      console.error("Failed to add city:", err);
      alert("Error adding city. Please check the backend.");
    }
  };

  const handleAddRoad = async (roadData) => {
    const res = await api.addRoad(roadData);
    setRoads([...roads, res.data]);
  };


  const findShortestPath = async () => {
    if (!startCity || !endCity) {
      alert("Please select both a start and destination city.");
      return;
    }

    setSimulationStarted(true);
    setShortestPath([]);
    setShortestPathSequence([]);
    setTotalDistance(0);
    setTravelTimes(null);
    setIsVisualizing(true);

    try {
      const res = await api.shortestPath(startCity, endCity, trafficLevel);
      const finalPathData = res.data.path || [];
      
      if (finalPathData.length === 0) {
        alert("No route found between these cities. Please try adding more roads!");
        setIsVisualizing(false);
        setShortestPath([]);
        setShortestPathSequence([]);
        return;
      }

      const finalDistanceData = res.data.distance || 0;
      const baseMinutes = res.data.totalTravelMinutes || 0;
      
      const formatTime = (totalMinutes) => {
        const h = Math.floor(totalMinutes / 60);
        const m = Math.round(totalMinutes % 60);
        return `${h > 0 ? `${h} hours ` : ''}${m} minutes`;
      };

      const finalTravelTimes = {
        car: formatTime(baseMinutes),
        bus: formatTime(baseMinutes * 1.5),
        bike: formatTime(baseMinutes * 0.8)
      };

      const roadGeometry = await api.getRoadPath(finalPathData);
      
      // Use backend enriched path if available, otherwise fallback to dijkstra path
      const enrichedSequence = res.data.enrichedPath || finalPathData;

      setShortestPathSequence(enrichedSequence);
      setShortestPath(roadGeometry.length > 0 ? roadGeometry : finalPathData.map(c => [c.lat, c.lng]));
      setTotalDistance(finalDistanceData);
      setTravelTimes(finalTravelTimes);
      setIsVisualizing(false);

    } catch (error) {
      console.error("Failed to fetch path from backend:", error);
      const errorMsg = error.response?.data?.error || "Could not connect to the Backend API. Make sure the Spring Boot server is running on port 8080.";
      alert(`Error: ${errorMsg}`);
      setIsVisualizing(false);
    }
  };

  const resetSimulation = () => {
    setShortestPath([]);
    setShortestPathSequence([]);
    setTotalDistance(0);
    setTravelTimes(null);
    setSimulationStarted(false);
    setStartCity('');
    setEndCity('');
  };

  return (
    <div className="homepage-layout">
      {/* Header Section */}
      <header className="app-header glass-card slide-in-top">
        <div className="header-content">
          <div className="logo-container">
            <div className="glowing-icon">🗺️</div>
            <div>
              <h1 className="glow-text">Traffic Route Optimizer</h1>
              <p className="subtitle">Visualizing Dijkstra’s Shortest Path Algorithm</p>
            </div>
          </div>
        </div>
        <button
          className="btn-secondary"
          onClick={toggleTheme}
          style={{ width: '45px', height: '45px', padding: '0', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '1.2rem', borderRadius: '50%' }}
          title={theme === 'dark' ? "Switch to Light Mode" : "Switch to Dark Mode"}
        >
          {theme === 'dark' ? '☀️' : '🌙'}
        </button>
      </header>

      {/* Main Content Area */}
      <main className="main-content">
        {/* Left Control Panel */}
        <aside className="left-panel glass-card slide-in-left">
          <h2>Controls</h2>
          <div className="panel-content">
            <CitySelector
              cities={cities}
              onSelectStart={setStartCity}
              onSelectEnd={setEndCity}
            />

            <TrafficSlider
              trafficLevel={trafficLevel}
              setTrafficLevel={setTrafficLevel}
            />

            <div className="form-group" style={{ marginTop: '20px' }}>
              <label>Animation Speed</label>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <input
                  type="range"
                  min="1"
                  max="10"
                  value={animationSpeed}
                  onChange={(e) => setAnimationSpeed(Number(e.target.value))}
                  className="slider"
                  style={{ flex: 1 }}
                />
                <span style={{ fontWeight: 'bold', color: 'var(--accent-cyan)' }}>{animationSpeed}x</span>
              </div>
            </div>

            <div style={{ marginTop: '30px', display: 'flex', flexDirection: 'column', gap: '15px' }}>
              <button className="btn-primary" onClick={findShortestPath} disabled={isVisualizing}>
                {isVisualizing ? 'Visualizing...' : 'Find Shortest Path'}
              </button>
              <button className="btn-secondary" onClick={resetSimulation} disabled={isVisualizing}>
                Reset
              </button>
              <div style={{ display: 'flex', gap: '10px', marginTop: '10px' }}>
                <button className="btn-secondary" style={{ flex: 1, fontSize: '0.85rem', padding: '8px' }} onClick={() => setCityModalOpen(true)}>
                  + Add City
                </button>
                <button className="btn-secondary" style={{ flex: 1, fontSize: '0.85rem', padding: '8px' }} onClick={() => setRoadModalOpen(true)}>
                  + Add Road
                </button>
              </div>
            </div>
          </div>
        </aside>

        {/* Center Map Area */}
        <section className="map-area glass-card fade-in">
          <MapView
            cities={cities}
            roads={roads}
            shortestPath={shortestPath}
            shortestPathSequence={shortestPathSequence}
            startCity={startCity}
            endCity={endCity}
            simulationStarted={simulationStarted}
            animationSpeed={animationSpeed}
          />
        </section>

        {/* Right Algorithm Info Panel */}
        <aside className="right-panel glass-card slide-in-right">
          <h2>Summary</h2>
          <div className="panel-content">
            <AlgorithmVisualizer 
              shortestPathSequence={shortestPathSequence} 
              totalDistance={totalDistance} 
              travelTimes={travelTimes} 
            />
          </div>
        </aside>
      </main>

      {/* Modals */}
      <AddCityModal isOpen={isCityModalOpen} onClose={() => setCityModalOpen(false)} onAdd={handleAddCity} />
      <AddRoadModal isOpen={isRoadModalOpen} onClose={() => setRoadModalOpen(false)} onAdd={handleAddRoad} cities={cities} />
    </div>
  );
};

export default HomePage;
