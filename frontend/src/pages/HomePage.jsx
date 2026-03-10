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
];

const HomePage = () => {
  const [cities, setCities] = useState([]);
  const [roads, setRoads] = useState([]);

  const [startCity, setStartCity] = useState('');
  const [endCity, setEndCity] = useState('');
  const [trafficLevel, setTrafficLevel] = useState(1);

  const [shortestPath, setShortestPath] = useState([]);
  const [visitedNodes, setVisitedNodes] = useState([]);
  const [visitedEdges, setVisitedEdges] = useState([]);
  const [evaluatingEdge, setEvaluatingEdge] = useState(null);
  const [totalDistance, setTotalDistance] = useState(0);
  const [travelTimes, setTravelTimes] = useState(null);
  const [algoSteps, setAlgoSteps] = useState([]);
  const [currentStepIdx, setCurrentStepIdx] = useState(-1);
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
        setCities(citiesRes.data);
        const roadsRes = await api.getRoads();
        setRoads(roadsRes.data);
      } catch (err) {
        console.error("Failed to load graph data:", err);
        // Fallback to initial mock data if backend not running
        setCities(initialCities);
        setRoads(initialRoads);
      }
    };
    fetchData();
  }, []);

  const handleAddCity = async (cityData) => {
    const res = await api.addCity(cityData);
    setCities([...cities, res.data]);
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
    // reset state
    setShortestPath([]);
    setVisitedNodes([]);
    setVisitedEdges([]);
    setEvaluatingEdge(null);
    setTotalDistance(0);
    setTravelTimes(null);
    setCurrentStepIdx(-1);
    setIsVisualizing(true);

    try {
      // Pull frames from the Spring Boot API
      const res = await api.shortestPath(startCity, endCity, trafficLevel);
      const frames = res.data.steps || [];
      const finalPathData = res.data.path || [];
      const finalDistanceData = res.data.distance || 0;
      // Calculate specific transport times using the optimal totalTravelMinutes (based on Car logic 60km/h baseline)
      const baseMinutes = res.data.totalTravelMinutes || 0;
      const finalTravelTimes = {
        car: baseMinutes / 60,                // Baseline
        bus: (baseMinutes * 1.5) / 60,        // Buses are 50% slower
        bike: (baseMinutes * 0.8) / 60        // Bikes are 20% faster through traffic
      };

      setAlgoSteps(frames.map(f => ({ message: f.message })));

      let step = 0;
      const interval = setInterval(() => {
        if (step < frames.length) {
          const frame = frames[step];
          setCurrentStepIdx(step);
          setVisitedNodes(frame.visitedNodes);
          setVisitedEdges(frame.visitedEdges);
          setEvaluatingEdge(frame.evaluatingEdge);

          if (frame.isComplete) {
            setShortestPath(finalPathData);
            setTotalDistance(finalDistanceData);
            setTravelTimes(finalTravelTimes);
            clearInterval(interval);
            setIsVisualizing(false);
          }

          step++;
        } else {
          clearInterval(interval);
          setShortestPath(finalPathData);
          setTotalDistance(finalDistanceData);
          setTravelTimes(finalTravelTimes);
          setIsVisualizing(false);
        }
      }, 800 / animationSpeed);
    } catch (error) {
      console.error("Failed to fetch path from backend:", error);
      alert("Error: Could not connect to the Backend API. Make sure the Spring Boot server is running on port 8080.");
      setIsVisualizing(false);
    }
  };

  const resetSimulation = () => {
    setShortestPath([]);
    setVisitedNodes([]);
    setVisitedEdges([]);
    setEvaluatingEdge(null);
    setTotalDistance(0);
    setTravelTimes(null);
    setCurrentStepIdx(-1);
    setAlgoSteps([]);
    setSimulationStarted(false);
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
            startCity={startCity}
            endCity={endCity}
            visitedNodes={visitedNodes}
            visitedEdges={visitedEdges}
            evaluatingEdge={evaluatingEdge}
            simulationStarted={simulationStarted}
            animationSpeed={animationSpeed}
          />
        </section>

        {/* Right Algorithm Info Panel */}
        <aside className="right-panel glass-card slide-in-right">
          <h2>Algorithm Visualizer</h2>
          <div className="panel-content">
            <AlgorithmVisualizer steps={algoSteps} currentStepIdx={currentStepIdx} />
            {!isVisualizing && shortestPath.length > 0 && (
              <ResultPanel shortestPath={shortestPath} totalDistance={totalDistance} travelTimes={travelTimes} />
            )}
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
