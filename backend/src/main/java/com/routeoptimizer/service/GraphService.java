package com.routeoptimizer.service;

import com.routeoptimizer.algorithm.DijkstraAlgorithm;
import com.routeoptimizer.algorithm.ShortestPathResponse;
import com.routeoptimizer.model.City;
import com.routeoptimizer.model.Graph;
import com.routeoptimizer.model.Road;
import com.routeoptimizer.repository.CityRepository;
import com.routeoptimizer.repository.RoadRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Set;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import org.springframework.web.client.RestTemplate;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GraphService {

    public static final Map<String, Double> ROAD_SPEEDS = Map.of(
            "NH", 80.0,
            "SH", 60.0,
            "CITY", 40.0,
            "VILLAGE", 25.0,
            "BAD_SH", 30.0);

    private final DijkstraAlgorithm dijkstraAlgorithm;
    private final CityRepository cityRepository;
    private final RoadRepository roadRepository;

    public GraphService(DijkstraAlgorithm dijkstraAlgorithm, CityRepository cityRepository,
            RoadRepository roadRepository) {
        this.dijkstraAlgorithm = dijkstraAlgorithm;
        this.cityRepository = cityRepository;
        this.roadRepository = roadRepository;
    }

    public City addCity(City city) {
        City savedCity = cityRepository.save(city);

        List<City> allCities = cityRepository.findAll();
        for (City existingCity : allCities) {
            if (!existingCity.getId().equals(savedCity.getId())) {
                double distance = estimateDrivingDistance(
                        savedCity.getLatitude(), savedCity.getLongitude(),
                        existingCity.getLatitude(), existingCity.getLongitude());

                if (distance < 100.0) {
                    Road road = new Road();
                    road.setFromCity(savedCity);
                    road.setToCity(existingCity);
                    road.setDistance(distance);
                    road.setTrafficLevel(0.1);
                    if (distance <= 20.0) {
                        road.setRoadType("CITY");
                    } else if (distance <= 50.0) {
                        road.setRoadType("SH");
                    } else {
                        road.setRoadType("NH");
                    }
                    addRoad(road);

                    // Also add the reverse road for consistency in DB
                    Road reverseRoad = new Road();
                    reverseRoad.setFromCity(existingCity);
                    reverseRoad.setToCity(savedCity);
                    reverseRoad.setDistance(distance);
                    reverseRoad.setTrafficLevel(0.1);
                    reverseRoad.setRoadType(road.getRoadType());
                    addRoad(reverseRoad);
                }
            }
        }

        return savedCity;
    }

    public Collection<City> getAllCities() {
        return cityRepository.findAll();
    }

    public Road addRoad(Road road) {
        if (road.getTrafficLevel() < 0.0) {
            road.setTrafficLevel(0.0);
        }

        City city1 = road.getFromCity() != null ? cityRepository.findById(road.getFromCity().getId()).orElse(null) : null;
        City city2 = road.getToCity() != null ? cityRepository.findById(road.getToCity().getId()).orElse(null) : null;
        
        if (city1 == null || city2 == null) {
            throw new IllegalArgumentException("Both cities must exist to add a road.");
        }

        // VALIDATION: Prevent duplicate roads between same city IDs
        List<Road> existingRoads = roadRepository.findAll();
        boolean roadExists = existingRoads.stream().anyMatch(r -> 
            r.getFromCity() != null && r.getToCity() != null &&
            r.getFromCity().getId().equals(city1.getId()) && 
            r.getToCity().getId().equals(city2.getId())
        );
        
        if (roadExists) {
            System.out.println("INFO: Road already exists between " + city1.getName() + " and " + city2.getName() + ". Skipping.");
            return existingRoads.stream().filter(r -> 
                r.getFromCity().getId().equals(city1.getId()) && 
                r.getToCity().getId().equals(city2.getId())).findFirst().orElse(null);
        }

        if (road.getDistance() == 0) {
            road.setDistance(getOSRMDrivingDistance(city1.getLatitude(), city1.getLongitude(), city2.getLatitude(),
                    city2.getLongitude()));
        }

        if (road.getRoadType() == null || road.getRoadType().trim().isEmpty()) {
            road.setRoadType("SH");
        }
        road.setRoadType(road.getRoadType().toUpperCase());
        
        if (road.getDistance() < 1.0 || road.getDistance() > 3000.0) {
            throw new IllegalArgumentException("Invalid road distance: " + road.getDistance() + " km. Must be realistic (1 to 3000 km).");
        }

        double speed = ROAD_SPEEDS.getOrDefault(road.getRoadType(), 60.0);
        road.setSpeedLimit(speed);
        if (road.getDistance() > 0) {
            road.setTravelTime(road.getDistance() / speed);
        } else {
            road.setTravelTime(0.0);
        }

        return roadRepository.save(road);
    }

    // Multiplies straight-line distance by 1.3 to approximate winding roads
    private double estimateDrivingDistance(double lat1, double lon1, double lat2, double lon2) {
        return Math.round((calculateHaversineDistance(lat1, lon1, lat2, lon2) * 1.3) * 100.0) / 100.0;
    }

    // Haversine formula to calculate real spherical distance in KM
    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        int R = 6371; // Earth's radius in kilometers
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round((R * c) * 100.0) / 100.0; // Round to 2 decimals
    }

    public List<Road> getAllRoads() {
        return roadRepository.findAll();
    }

    public String updateAllRoadDistances() {
        List<Road> allRoads = roadRepository.findAll();
        int count = 0;
        for (Road r : allRoads) {
            if (r.getFromCity() != null && r.getToCity() != null) {
                double dist = getOSRMDrivingDistance(r.getFromCity().getLatitude(), r.getFromCity().getLongitude(), 
                                                     r.getToCity().getLatitude(), r.getToCity().getLongitude());
                if (dist > 0 && Math.abs(dist - r.getDistance()) > 1.0) {
                    r.setDistance(dist);
                    double speed = ROAD_SPEEDS.getOrDefault(r.getRoadType(), 60.0);
                    r.setTravelTime(dist / speed);
                    roadRepository.save(r);
                    count++;
                }
            }
        }
        return "Successfully updated " + count + " road distances to real-world driving limits.";
    }

    @Transactional
    public String fixGraphData() {
        // 0. CLEANUP: Delete placeholder testing cities
        List<City> citiesToClean = cityRepository.findAll().stream()
                .filter(c -> c.getName() != null && (c.getName().equalsIgnoreCase("City A") || c.getName().equalsIgnoreCase("City B")))
                .collect(Collectors.toList());
        
        if (!citiesToClean.isEmpty()) {
            for (City c : citiesToClean) {
                List<Road> roadsToCity = roadRepository.findAll().stream()
                        .filter(r -> r.getFromCity().getId().equals(c.getId()) || r.getToCity().getId().equals(c.getId()))
                        .collect(Collectors.toList());
                roadRepository.deleteAll(roadsToCity);
                cityRepository.delete(c);
            }
        }

        List<Road> allRoads = roadRepository.findAll();
        int fixedCount = 0;
        int deletedCount = 0;
        
        // 1. DEDUPLICATION: Remove multiple roads between same cities
        Map<String, Road> uniqueRoads = new HashMap<>();
        List<Road> toDelete = new ArrayList<>();
        
        for (Road r : allRoads) {
            if (r.getFromCity() == null || r.getToCity() == null) {
                toDelete.add(r);
                continue;
            }
            String key = r.getFromCity().getId() + "_" + r.getToCity().getId();
            if (uniqueRoads.containsKey(key)) {
                toDelete.add(r);
            } else {
                uniqueRoads.put(key, r);
            }
        }
        
        if (!toDelete.isEmpty()) {
            roadRepository.deleteAll(toDelete);
            deletedCount = toDelete.size();
            // Refresh list for subsequent fixes
            allRoads = roadRepository.findAll();
        }

        // 2. SANITIZATION: Fix unrealistic distances
        for (Road r : allRoads) {
            if (r.getFromCity() != null && r.getToCity() != null) {
                double haversine = calculateHaversineDistance(
                    r.getFromCity().getLatitude(), r.getFromCity().getLongitude(),
                    r.getToCity().getLatitude(), r.getToCity().getLongitude());
                
                boolean isSuspicious = r.getDistance() < 1.0 || (r.getDistance() < haversine * 0.5 && r.getDistance() > 0);
                
                if (isSuspicious || r.getDistance() > 4000.0) {
                    double newDist = getOSRMDrivingDistance(
                        r.getFromCity().getLatitude(), r.getFromCity().getLongitude(),
                        r.getToCity().getLatitude(), r.getToCity().getLongitude());
                    
                    if (newDist > 0 && Math.abs(newDist - r.getDistance()) > 1.0) {
                        r.setDistance(newDist);
                        r.setTravelTime(newDist / ROAD_SPEEDS.getOrDefault(r.getRoadType(), 60.0));
                        roadRepository.save(r);
                        fixedCount++;
                    }
                }
            }
        }
        
        // 3. CORE CONNECTIONS: Ensure major highways exist
        fixedCount += ensureConnection(7L, 35L); // Kolkata - Bhubaneswar
        fixedCount += ensureConnection(35L, 12L); // Bhubaneswar - Visakhapatnam
        fixedCount += ensureConnection(12L, 18L); // Visakhapatnam - Vijayawada
        fixedCount += ensureConnection(18L, 32L); // Vijayawada - Nellore
        fixedCount += ensureConnection(32L, 3L);  // Nellore - Chennai
        fixedCount += ensureConnection(1L, 44L); // Hyderabad - Warangal
        fixedCount += ensureConnection(44L, 12L); // Warangal - Visakhapatnam
        fixedCount += ensureConnection(27L, 19L); // Palwal - Agra

        return "Successfully audited graph. Fixed: " + fixedCount + ", Deleted Duplicates: " + deletedCount;
    }

    private int ensureConnection(Long city1Id, Long city2Id) {
        City c1 = cityRepository.findById(city1Id).orElse(null);
        City c2 = cityRepository.findById(city2Id).orElse(null);
        if (c1 == null || c2 == null) return 0;

        List<Road> existing = roadRepository.findAll();
        boolean hasConn = existing.stream().anyMatch(r -> 
            (r.getFromCity().getId().equals(city1Id) && r.getToCity().getId().equals(city2Id)) ||
            (r.getFromCity().getId().equals(city2Id) && r.getToCity().getId().equals(city1Id))
        );

        if (!hasConn) {
            double dist = getOSRMDrivingDistance(c1.getLatitude(), c1.getLongitude(), c2.getLatitude(), c2.getLongitude());
            
            Road r1 = new Road();
            r1.setFromCity(c1); r1.setToCity(c2); r1.setDistance(dist);
            r1.setRoadType("NH"); r1.setTrafficLevel(0.2); 
            r1.setSpeedLimit(80.0); r1.setTravelTime(dist/80.0);
            roadRepository.save(r1);

            Road r2 = new Road();
            r2.setFromCity(c2); r2.setToCity(c1); r2.setDistance(dist);
            r2.setRoadType("NH"); r2.setTrafficLevel(0.2);
            r2.setSpeedLimit(80.0); r2.setTravelTime(dist/80.0);
            roadRepository.save(r2);
            return 2;
        }
        return 0;
    }

    @Transactional
    public String autoFixCityConnections() {
        List<City> allCities = cityRepository.findAll();
        List<Road> allRoads = roadRepository.findAll();
        
        Map<Long, Set<Long>> connections = new HashMap<>();
        for (City c : allCities) {
            connections.put(c.getId(), new HashSet<>());
        }
        
        for (Road r : allRoads) {
            if (r.getFromCity() != null && r.getToCity() != null) {
                if (connections.containsKey(r.getFromCity().getId())) {
                    connections.get(r.getFromCity().getId()).add(r.getToCity().getId());
                }
                if (connections.containsKey(r.getToCity().getId())) {
                    connections.get(r.getToCity().getId()).add(r.getFromCity().getId());
                }
            }
        }
        
        int addedRoadsCount = 0;
        int citiesFixed = 0;
        
        // 1. Audit and Connect all cities for dense graph connectivity
        for (City city : allCities) {
            // Logic: Connect EVERY city to its nearest logical neighbors regardless of current degree
            // This ensures a more realistic and redundant road network.
            List<City> nearestCities = allCities.stream()
                    .filter(c -> !c.getId().equals(city.getId()))
                    .sorted(Comparator.comparingDouble(c -> estimateDrivingDistance(
                            city.getLatitude(), city.getLongitude(),
                            c.getLatitude(), c.getLongitude())))
                    .limit(5) // Increased limit to 5 neighbors for better density
                    .collect(Collectors.toList());
                    
            for (City neighbor : nearestCities) {
                if (!connections.get(city.getId()).contains(neighbor.getId())) {
                    double distance = estimateDrivingDistance(
                            city.getLatitude(), city.getLongitude(),
                            neighbor.getLatitude(), neighbor.getLongitude());
                            
                    Road forward = new Road();
                    forward.setFromCity(city);
                    forward.setToCity(neighbor);
                    forward.setDistance(distance);
                    forward.setTrafficLevel(0.1);
                    forward.setRoadType("SH");
                    forward.setSpeedLimit(ROAD_SPEEDS.getOrDefault("SH", 60.0));
                    forward.setTravelTime(distance / 60.0);
                    roadRepository.save(forward);
                    
                    Road backward = new Road();
                    backward.setFromCity(neighbor);
                    backward.setToCity(city);
                    backward.setDistance(distance);
                    backward.setTrafficLevel(0.1);
                    backward.setRoadType("SH");
                    backward.setSpeedLimit(ROAD_SPEEDS.getOrDefault("SH", 60.0));
                    backward.setTravelTime(distance / 60.0);
                    roadRepository.save(backward);
                    
                    connections.get(city.getId()).add(neighbor.getId());
                    connections.get(neighbor.getId()).add(city.getId());
                    
                    addedRoadsCount += 2;
                    System.out.println("Graph Audit - Added road: " + city.getName() + " -> " + neighbor.getName());
                }
            }
        }
        
        // 2. Ensure Full Connectivity (BFS to find components)
        List<Set<Long>> components = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        for (City city : allCities) {
            if (!visited.contains(city.getId())) {
                Set<Long> component = new HashSet<>();
                Queue<Long> queue = new LinkedList<>();
                
                queue.add(city.getId());
                visited.add(city.getId());
                
                while (!queue.isEmpty()) {
                    Long current = queue.poll();
                    component.add(current);
                    for (Long neighbor : connections.get(current)) {
                        if (!visited.contains(neighbor)) {
                            visited.add(neighbor);
                            queue.add(neighbor);
                        }
                    }
                }
                components.add(component);
            }
        }
        
        // Connect disconnected components
        if (components.size() > 1) {
            for (int i = 0; i < components.size() - 1; i++) {
                Set<Long> compA = components.get(i);
                Set<Long> compB = components.get(i + 1);
                
                City cityA = allCities.stream().filter(c -> compA.contains(c.getId())).findFirst().orElse(null);
                City cityB = allCities.stream().filter(c -> compB.contains(c.getId())).findFirst().orElse(null);
                
                if (cityA != null && cityB != null && !connections.get(cityA.getId()).contains(cityB.getId())) {
                    double distance = estimateDrivingDistance(
                            cityA.getLatitude(), cityA.getLongitude(),
                            cityB.getLatitude(), cityB.getLongitude());
                            
                    Road forward = new Road();
                    forward.setFromCity(cityA);
                    forward.setToCity(cityB);
                    forward.setDistance(distance);
                    forward.setTrafficLevel(0.1);
                    forward.setRoadType("NH"); 
                    forward.setSpeedLimit(ROAD_SPEEDS.getOrDefault("NH", 80.0));
                    forward.setTravelTime(distance / 80.0);
                    roadRepository.save(forward);
                    
                    Road backward = new Road();
                    backward.setFromCity(cityB);
                    backward.setToCity(cityA);
                    backward.setDistance(distance);
                    backward.setTrafficLevel(0.1);
                    backward.setRoadType("NH");
                    backward.setSpeedLimit(ROAD_SPEEDS.getOrDefault("NH", 80.0));
                    backward.setTravelTime(distance / 80.0);
                    roadRepository.save(backward);
                    
                    connections.get(cityA.getId()).add(cityB.getId());
                    connections.get(cityB.getId()).add(cityA.getId());
                    
                    addedRoadsCount += 2;
                    System.out.println("Connected component: " + cityA.getName() + " -> " + cityB.getName());
                    System.out.println("Connected component: " + cityB.getName() + " -> " + cityA.getName());
                }
            }
        }

        return "Total roads added: " + addedRoadsCount + "\nCities fixed: " + citiesFixed;
    }

    public double getOSRMDrivingDistance(double lat1, double lon1, double lat2, double lon2) {
        try {
            String coords = lon1 + "," + lat1 + ";" + lon2 + "," + lat2;
            String url = "http://router.project-osrm.org/route/v1/driving/" + coords + "?overview=false";

            org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(3000);
            factory.setReadTimeout(3000);
            RestTemplate restTemplate = new RestTemplate(factory);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("routes")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> routes = (List<Map<String, Object>>) response.get("routes");
                if (!routes.isEmpty()) {
                    Number distanceMeters = (Number) routes.get(0).get("distance");
                    if (distanceMeters != null) {
                        return Math.round((distanceMeters.doubleValue() / 1000.0) * 100.0) / 100.0;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("OSRM distance call failed: " + e.getMessage());
        }
        return estimateDrivingDistance(lat1, lon1, lat2, lon2);
    }

    public Graph getGraph() {
        Graph graph = new Graph();
        for (City c : cityRepository.findAll()) {
            graph.addCity(c);
        }
        int validRoads = 0;
        for (Road r : roadRepository.findAll()) {
            if (r.getFromCity() != null && r.getToCity() != null && r.getFromCity().getId() != null && r.getToCity().getId() != null &&
                    graph.getCities().containsKey(r.getFromCity().getId()) &&
                    graph.getCities().containsKey(r.getToCity().getId())) {
                graph.addRoad(r);
                validRoads++;
            } else {
                System.out.println("WARN: Skipping invalid/orphaned road in graph build: " + r.getId());
            }
        }
        System.out.println(
                "INFO: Graph built with " + graph.getCities().size() + " cities and " + validRoads + " valid roads.");
        return graph;
    }

    private List<City> cleanPath(List<City> path) {
        if (path == null || path.isEmpty()) return new ArrayList<>();
        
        List<City> cleaned = new ArrayList<>();
        String lastSeenName = null;
        Long lastSeenId = null;
        
        for (City city : path) {
            if (city == null || city.getName() == null) continue;
            
            // Logic: Remove consecutive cities with same ID or same Name
            if (!city.getId().equals(lastSeenId) && !city.getName().equalsIgnoreCase(lastSeenName)) {
                cleaned.add(city);
                lastSeenId = city.getId();
                lastSeenName = city.getName();
            }
        }
        
        return cleaned;
    }

    public ShortestPathResponse findShortestPath(Long startCityId, Long endCityId, double trafficLevel) {
        if (roadRepository.count() == 0) {
            ShortestPathResponse errorResp = new ShortestPathResponse();
            errorResp.setError("No road connections exist between cities");
            return errorResp;
        }
        Graph graph = getGraph();
        if (!graph.getCities().containsKey(startCityId) || !graph.getCities().containsKey(endCityId)) {
            ShortestPathResponse errorResp = new ShortestPathResponse();
            errorResp.setError("Start or end city does not exist in graph.");
            return errorResp;
        }
        ShortestPathResponse resp = dijkstraAlgorithm.findShortestPath(graph, startCityId, endCityId, trafficLevel);
        
        if (resp.getPath() != null && resp.getPath().size() >= 2) {
            List<City> cleanedDijkstra = cleanPath(resp.getPath());
            resp.setPath(cleanedDijkstra);
            
            List<City> enriched = detectIntermediateWaypoints(cleanedDijkstra);
            resp.setEnrichedPath(cleanPath(enriched)); // Ensure enriched path is also clean
        }
        
        return resp;
    }

    private List<City> detectIntermediateWaypoints(List<City> dijkstraPath) {
        // 1. Call OSRM to get detailed geometry
        List<double[]> geometry = fetchOSRMGeometry(dijkstraPath);
        if (geometry.isEmpty()) return dijkstraPath;

        // 2. Rank ALL cities (including Dijkstra nodes) along the geometry
        List<City> allCities = cityRepository.findAll();
        List<CityWithRank> ranked = new ArrayList<>();
        
        // We'll use a tighter threshold for cleaning unwanted/random cities
        double thresholdKm = 7.0; // Reduced from 10km for cleaner route
        Set<Long> dijkstraIds = dijkstraPath.stream().map(City::getId).collect(Collectors.toSet());

        for (City city : allCities) {
            int bestIdx = -1;
            double minDist = Double.MAX_VALUE;
            
            // Check proximity to any sampled point on the OSRM geometry
            for (int i = 0; i < geometry.size(); i += 5) {
                double dist = calculateHaversineDistance(geometry.get(i)[0], geometry.get(i)[1], city.getLatitude(), city.getLongitude());
                if (dist < minDist) {
                    minDist = dist;
                    bestIdx = i;
                }
            }

            // Representation Logic Audit: Only include if it's actually near the driving route
            // And EXCLUDE placeholder cities
            if (dijkstraIds.contains(city.getId()) || (minDist < thresholdKm && !city.getName().toLowerCase().contains("city "))) {
                ranked.add(new CityWithRank(city, bestIdx));
            }
        }

        // 3. Sort by their progression along the road geometry
        ranked.sort(Comparator.comparingInt(r -> r.rank));

        // 4. Extract cities and ensure no consecutive duplicates
        List<City> result = new ArrayList<>();
        String lastSeenName = null;
        for (CityWithRank r : ranked) {
            if (lastSeenName == null || !r.city.getName().equalsIgnoreCase(lastSeenName)) {
                result.add(r.city);
                lastSeenName = r.city.getName();
            }
        }

        // Keep path manageable
        if (result.size() <= 12) return result;

        List<City> finalPath = new ArrayList<>();
        finalPath.add(result.get(0)); // Start
        
        int step = (result.size() - 2) / 10;
        for (int i = 1; i < result.size() - 1; i += Math.max(1, step)) {
            finalPath.add(result.get(i));
            if (finalPath.size() >= 11) break;
        }
        
        if (!finalPath.contains(result.get(result.size() - 1))) {
            finalPath.add(result.get(result.size() - 1)); // End
        }
        
        return finalPath;
    }

    @SuppressWarnings("unchecked")
    private List<double[]> fetchOSRMGeometry(List<City> path) {
        try {
            String coords = path.stream()
                .map(c -> c.getLongitude() + "," + c.getLatitude())
                .collect(Collectors.joining(";"));
            
            String url = "http://router.project-osrm.org/route/v1/driving/" + coords + "?overview=full&geometries=geojson";
            
            org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(3000);
            factory.setReadTimeout(3000);
            RestTemplate restTemplate = new RestTemplate(factory);
            
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null && response.containsKey("routes")) {
                List<Map<String, Object>> routes = (List<Map<String, Object>>) response.get("routes");
                if (!routes.isEmpty()) {
                    Map<String, Object> geometry = (Map<String, Object>) routes.get(0).get("geometry");
                    List<List<Double>> coordsList = (List<List<Double>>) geometry.get("coordinates");
                    return coordsList.stream()
                        .map(c -> new double[]{c.get(1), c.get(0)}) // OSRM is [lng, lat]
                        .collect(Collectors.toList());
                }
            }
        } catch (Exception e) {
            System.err.println("OSRM call failed: " + e.getMessage());
        }
        return java.util.Collections.emptyList();
    }

    private static class CityWithRank {
        City city;
        int rank;
        CityWithRank(City city, int rank) {
            this.city = city;
            this.rank = rank;
        }
    }
}
