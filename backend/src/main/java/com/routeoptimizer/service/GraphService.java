package com.routeoptimizer.service;

import com.routeoptimizer.algorithm.DijkstraAlgorithm;
import com.routeoptimizer.algorithm.ShortestPathResponse;
import com.routeoptimizer.model.City;
import com.routeoptimizer.model.Graph;
import com.routeoptimizer.model.Road;
import com.routeoptimizer.repository.CityRepository;
import com.routeoptimizer.repository.RoadRepository;
import org.springframework.beans.factory.annotation.Value;
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

    private static final String VIRTUAL_ROAD_TYPE = "VIRTUAL";

    private final DijkstraAlgorithm dijkstraAlgorithm;
    private final CityRepository cityRepository;
    private final RoadRepository roadRepository;

    @Value("${app.osrm.enabled:true}")
    private boolean osrmEnabled;

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

        City city1 = road.getFromCity() != null ? cityRepository.findById(road.getFromCity().getId()).orElse(null)
                : null;
        City city2 = road.getToCity() != null ? cityRepository.findById(road.getToCity().getId()).orElse(null) : null;
        if (city1 != null && city2 != null && road.getDistance() == 0) {
            road.setDistance(getOSRMDrivingDistance(city1.getLatitude(), city1.getLongitude(), city2.getLatitude(),
                    city2.getLongitude()));
        }

        if (road.getRoadType() == null || road.getRoadType().trim().isEmpty()) {
            road.setRoadType("SH");
        }
        road.setRoadType(road.getRoadType().toUpperCase());

        if (road.getDistance() < 1.0 || road.getDistance() > 3000.0) {
            throw new IllegalArgumentException(
                    "Invalid road distance: " + road.getDistance() + " km. Must be realistic (1 to 3000 km).");
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
        List<Road> allRoads = roadRepository.findAll();
        int fixedCount = 0;

        for (Road r : allRoads) {
            if (r.getFromCity() != null && r.getToCity() != null) {
                double haversine = calculateHaversineDistance(
                        r.getFromCity().getLatitude(), r.getFromCity().getLongitude(),
                        r.getToCity().getLatitude(), r.getToCity().getLongitude());

                // If the distance is fundamentally impossible or a straight-line shortcut
                if (r.getDistance() < 10.0 || r.getDistance() < haversine * 1.05 || r.getDistance() > 3000.0) {
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

        // Ensure Mumbai (4) to Hyderabad (1) exists
        City mumbai = cityRepository.findById(4L).orElse(null);
        City hyderabad = cityRepository.findById(1L).orElse(null);
        if (mumbai != null && hyderabad != null) {
            boolean hasConnection = false;
            for (Road r : allRoads) {
                if ((r.getFromCity().getId().equals(4L) && r.getToCity().getId().equals(1L)) ||
                        (r.getFromCity().getId().equals(1L) && r.getToCity().getId().equals(4L))) {
                    hasConnection = true;
                    break;
                }
            }
            if (!hasConnection) {
                double dist = getOSRMDrivingDistance(mumbai.getLatitude(), mumbai.getLongitude(),
                        hyderabad.getLatitude(), hyderabad.getLongitude());

                Road forward = new Road();
                forward.setFromCity(mumbai);
                forward.setToCity(hyderabad);
                forward.setDistance(dist);
                forward.setRoadType("NH");
                forward.setTrafficLevel(0.4);
                forward.setSpeedLimit(ROAD_SPEEDS.getOrDefault("NH", 80.0));
                forward.setTravelTime(dist / 80.0);
                roadRepository.save(forward);

                Road backward = new Road();
                backward.setFromCity(hyderabad);
                backward.setToCity(mumbai);
                backward.setDistance(dist);
                backward.setRoadType("NH");
                backward.setTrafficLevel(0.4);
                backward.setSpeedLimit(ROAD_SPEEDS.getOrDefault("NH", 80.0));
                backward.setTravelTime(dist / 80.0);
                roadRepository.save(backward);

                fixedCount += 2;
                System.out.println("Fixed missing Mumbai-Hyderabad connection.");
            }
        }

        return "Successfully fixed " + fixedCount + " unrealistic or missing roads.";
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

        // 1. Connect weak/isolated cities (connections < 2)
        for (City city : allCities) {
            if (connections.get(city.getId()).size() < 2) {
                citiesFixed++;
                List<City> nearestCities = allCities.stream()
                        .filter(c -> !c.getId().equals(city.getId()))
                        .sorted(Comparator.comparingDouble(c -> estimateDrivingDistance(
                                city.getLatitude(), city.getLongitude(),
                                c.getLatitude(), c.getLongitude())))
                        .limit(3)
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
                        System.out.println("Added road: " + city.getName() + " -> " + neighbor.getName());
                        System.out.println("Added road: " + neighbor.getName() + " -> " + city.getName());
                    }
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
        if (!osrmEnabled) {
            return estimateDrivingDistance(lat1, lon1, lat2, lon2);
        }
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

    public List<City> cleanPath(List<City> path) {
        if (path == null || path.isEmpty()) return new ArrayList<>();
        List<City> cleaned = new ArrayList<>();
        for (int i = 0; i < path.size(); i++) {
            if (i == 0 || !path.get(i).getId().equals(path.get(i - 1).getId())) {
                cleaned.add(path.get(i));
            }
        }
        return cleaned;
    }

    private static final double VIRTUAL_EDGE_MAX_DISTANCE_KM = 250.0;
    private static final int VIRTUAL_EDGE_MAX_NEIGHBORS_PER_CITY = 4;
    private static final int VIRTUAL_EDGE_MIN_DEGREE_PER_CITY = 2;
    private static final double VIRTUAL_EDGE_TIME_PENALTY = 1.1; // slight penalty vs real DB roads (distance is already inflated)

    private boolean hasEdge(Graph graph, Long fromCityId, Long toCityId) {
        List<Road> existing = graph.getAdjacentRoads(fromCityId);
        return existing != null && existing.stream()
                .anyMatch(edge -> edge.getToCity() != null && toCityId.equals(edge.getToCity().getId()));
    }

    private boolean isVirtualRoad(Road road) {
        return road != null && road.getRoadType() != null && VIRTUAL_ROAD_TYPE.equalsIgnoreCase(road.getRoadType());
    }

    private boolean addVirtualEdgeIfMissing(Graph graph, City from, City to, double distanceKm) {
        if (from == null || to == null || from.getId() == null || to.getId() == null) return false;
        if (from.getId().equals(to.getId())) return false;
        if (hasEdge(graph, from.getId(), to.getId())) return false;

        Road road = new Road();
        road.setFromCity(from);
        road.setToCity(to);
        road.setDistance(distanceKm);
        road.setTrafficLevel(0.0);
        road.setRoadType(VIRTUAL_ROAD_TYPE);

        double speed = 60.0;
        road.setSpeedLimit(speed);
        road.setTravelTime((distanceKm / speed) * VIRTUAL_EDGE_TIME_PENALTY);

        graph.addRoad(road);
        return true;
    }

    /**
     * Auto-generate runtime-only "virtual" connections between cities that are geographically close.
     * This makes routing work even when the DB has incomplete road coverage.
     *
     * Note: Graph.addRoad already adds the reverse adjacency edge, so we add each pair once.
     */
    private int connectNearbyCities(Graph graph) {
        List<City> cities = new ArrayList<>(graph.getCities().values());
        int added = 0;

        for (City c1 : cities) {
            if (c1 == null || c1.getId() == null) continue;
            Long c1Id = c1.getId();

            List<Road> adjacent = graph.getAdjacentRoads(c1Id);
            long virtualDegree = adjacent == null ? 0 : adjacent.stream().filter(this::isVirtualRoad).count();
            long realDegree = adjacent == null ? 0 : adjacent.stream().filter(r -> !isVirtualRoad(r)).count();
            if (realDegree >= VIRTUAL_EDGE_MIN_DEGREE_PER_CITY) continue;
            int remaining = VIRTUAL_EDGE_MAX_NEIGHBORS_PER_CITY - (int) virtualDegree;
            if (remaining <= 0) continue;

            List<CityDistance> candidates = new ArrayList<>();
            for (City c2 : cities) {
                if (c2 == null || c2.getId() == null) continue;
                if (c1Id.equals(c2.getId())) continue;
                if (hasEdge(graph, c1Id, c2.getId())) continue;

                double haversineKm = calculateHaversineDistance(
                        c1.getLatitude(), c1.getLongitude(),
                        c2.getLatitude(), c2.getLongitude());
                if (haversineKm > VIRTUAL_EDGE_MAX_DISTANCE_KM) continue;

                double distanceKm = estimateDrivingDistance(
                        c1.getLatitude(), c1.getLongitude(),
                        c2.getLatitude(), c2.getLongitude());
                candidates.add(new CityDistance(c2, distanceKm));
            }

            candidates.sort(Comparator.comparingDouble(cd -> cd.distanceKm));

            for (CityDistance candidate : candidates) {
                if (remaining <= 0) break;
                if (addVirtualEdgeIfMissing(graph, c1, candidate.city, candidate.distanceKm)) {
                    added++;
                    remaining--;
                }
            }
        }

        return added;
    }

    private int ensureVirtualConnectivity(Graph graph) {
        int added = 0;
        added += connectNearbyCities(graph);
        added += addNearestVirtualEdges(graph);
        added += connectDisconnectedComponents(graph);
        return added;
    }

    private int addNearestVirtualEdges(Graph graph) {
        List<City> cities = new ArrayList<>(graph.getCities().values());
        int added = 0;

        for (City city : cities) {
            if (city == null || city.getId() == null) continue;

            int currentDegree = graph.getAdjacentRoads(city.getId()).size();
            if (currentDegree >= VIRTUAL_EDGE_MIN_DEGREE_PER_CITY) continue;
            int maxEdgesToAdd = Math.max(0, Math.min(VIRTUAL_EDGE_MAX_NEIGHBORS_PER_CITY, VIRTUAL_EDGE_MIN_DEGREE_PER_CITY - currentDegree));
            if (maxEdgesToAdd == 0) continue;

            List<CityDistance> candidates = new ArrayList<>();
            for (City other : cities) {
                if (other == null || other.getId() == null) continue;
                if (other.getId().equals(city.getId())) continue;
                double dist = estimateDrivingDistance(city.getLatitude(), city.getLongitude(),
                        other.getLatitude(), other.getLongitude());
                candidates.add(new CityDistance(other, dist));
            }
            candidates.sort(Comparator.comparingDouble(cd -> cd.distanceKm));

            int addedForCity = 0;

            // Pass 1: add nearby edges within the preferred threshold.
            for (CityDistance candidate : candidates) {
                if (addedForCity >= maxEdgesToAdd) break;
                if (candidate.distanceKm > VIRTUAL_EDGE_MAX_DISTANCE_KM) break;
                if (addVirtualEdgeIfMissing(graph, city, candidate.city, candidate.distanceKm)) {
                    added++;
                    addedForCity++;
                }
            }

            // Pass 2: if still isolated, allow the closest remaining edges (even if far).
            for (CityDistance candidate : candidates) {
                if (addedForCity >= maxEdgesToAdd) break;
                if (graph.getAdjacentRoads(city.getId()).size() >= VIRTUAL_EDGE_MIN_DEGREE_PER_CITY) break;
                if (addVirtualEdgeIfMissing(graph, city, candidate.city, candidate.distanceKm)) {
                    added++;
                    addedForCity++;
                }
            }
        }

        return added;
    }

    private int connectDisconnectedComponents(Graph graph) {
        List<Set<Long>> components = computeConnectedComponents(graph);
        if (components.size() <= 1) return 0;

        List<City> cities = new ArrayList<>(graph.getCities().values());
        Map<Long, City> byId = cities.stream()
                .filter(c -> c != null && c.getId() != null)
                .collect(Collectors.toMap(City::getId, c -> c));

        int added = 0;
        Set<Long> main = new HashSet<>(components.get(0));

        for (int i = 1; i < components.size(); i++) {
            Set<Long> component = components.get(i);
            double best = Double.POSITIVE_INFINITY;
            City bestA = null;
            City bestB = null;

            for (Long aId : main) {
                City a = byId.get(aId);
                if (a == null) continue;
                for (Long bId : component) {
                    City b = byId.get(bId);
                    if (b == null) continue;
                    double dist = estimateDrivingDistance(a.getLatitude(), a.getLongitude(), b.getLatitude(),
                            b.getLongitude());
                    if (dist < best) {
                        best = dist;
                        bestA = a;
                        bestB = b;
                    }
                }
            }

            if (bestA != null && bestB != null) {
                if (addVirtualEdgeIfMissing(graph, bestA, bestB, best)) {
                    added++;
                }
                // Graph.addRoad makes the reverse edge too.
                main.addAll(component);
            }
        }

        return added;
    }

    private List<Set<Long>> computeConnectedComponents(Graph graph) {
        Set<Long> unvisited = new HashSet<>(graph.getCities().keySet());
        List<Set<Long>> components = new ArrayList<>();

        while (!unvisited.isEmpty()) {
            Long start = unvisited.iterator().next();
            Queue<Long> q = new LinkedList<>();
            Set<Long> component = new HashSet<>();
            q.add(start);
            unvisited.remove(start);

            while (!q.isEmpty()) {
                Long current = q.poll();
                component.add(current);

                for (Road road : graph.getAdjacentRoads(current)) {
                    if (road.getToCity() == null || road.getToCity().getId() == null) continue;
                    Long next = road.getToCity().getId();
                    if (unvisited.remove(next)) {
                        q.add(next);
                    }
                }
            }

            components.add(component);
        }

        return components;
    }

    private static class CityDistance {
        final City city;
        final double distanceKm;

        private CityDistance(City city, double distanceKm) {
            this.city = city;
            this.distanceKm = distanceKm;
        }
    }

    public Graph getGraph() {
        Graph graph = new Graph();
        List<City> allCities = cityRepository.findAll();
        
        // Ensure all cities are initialized as nodes
        for (City city : allCities) {
            graph.addCity(city);
        }

        List<Road> allRoads = roadRepository.findAll();
        int validRoads = 0; // Initialize validRoads counter
        for (Road r : allRoads) {
            if (r.getFromCity() != null && r.getToCity() != null && 
                r.getFromCity().getId() != null && r.getToCity().getId() != null) {
                
                // Prevent duplicate edges in the runtime graph
                List<Road> existing = graph.getAdjacentRoads(r.getFromCity().getId());
                boolean exists = existing != null && existing.stream()
                        .anyMatch(edge -> edge.getToCity().getId().equals(r.getToCity().getId()));
                
                if (!exists) {
                    graph.addRoad(r);
                    validRoads++; // Increment validRoads only if added
                }
            } else {
                System.out.println("WARN: Skipping invalid/orphaned road in graph build: " + r.getId());
            }
        }
        int virtualRoads = ensureVirtualConnectivity(graph);
        System.out.println(
                "INFO: Graph built with " + graph.getCities().size() + " cities, " + validRoads + " DB roads, and "
                        + virtualRoads + " virtual roads.");
        return graph;
    }

    public ShortestPathResponse findShortestPath(Long startCityId, Long endCityId, double trafficLevel) {
        Graph graph = getGraph();
        if (!graph.getCities().containsKey(startCityId) || !graph.getCities().containsKey(endCityId)) {
            ShortestPathResponse errorResp = new ShortestPathResponse();
            errorResp.setError("Start or end city does not exist in graph.");
            errorResp.setPath(new ArrayList<>());
            errorResp.setEnrichedPath(new ArrayList<>());
            return errorResp;
        }
        
        ShortestPathResponse resp = dijkstraAlgorithm.findShortestPath(graph, startCityId, endCityId, trafficLevel);

        // Contract: always return a non-null list for path/enrichedPath.
        if (resp.getPath() == null) {
            resp.setPath(new ArrayList<>());
        }
        
        if (resp.getPath() != null) {
            // Apply cleaning to the raw path
            resp.setPath(cleanPath(resp.getPath()));

            // Always provide a non-null enrichedPath for client simplicity.
            if (resp.getPath().size() >= 2) {
                List<City> enriched = detectIntermediateWaypoints(resp.getPath());
                resp.setEnrichedPath(cleanPath(enriched));
            } else {
                resp.setEnrichedPath(resp.getPath());
            }
        }

        if (resp.getEnrichedPath() == null) {
            resp.setEnrichedPath(resp.getPath() != null ? resp.getPath() : new ArrayList<>());
        }
        
        return resp;
    }

    private List<City> detectIntermediateWaypoints(List<City> dijkstraPath) {
        // 1. Call OSRM to get detailed geometry
        List<double[]> geometry = fetchOSRMGeometry(dijkstraPath);
        if (geometry.isEmpty())
            return dijkstraPath;

        // 2. Rank ALL cities (including Dijkstra nodes) along the geometry
        List<City> allCities = cityRepository.findAll();
        List<CityWithRank> ranked = new ArrayList<>();

        // We'll use a 20km threshold for "important" intermediate cities
        double thresholdKm = 20.0;
        Set<Long> dijkstraIds = dijkstraPath.stream().map(City::getId).collect(Collectors.toSet());

        for (City city : allCities) {
            int bestIdx = -1;
            double minDist = Double.MAX_VALUE;

            // Check proximity to any sampled point on the OSRM geometry
            for (int i = 0; i < geometry.size(); i += 5) {
                double dist = calculateHaversineDistance(geometry.get(i)[0], geometry.get(i)[1], city.getLatitude(),
                        city.getLongitude());
                if (dist < minDist) {
                    minDist = dist;
                    bestIdx = i;
                }
            }

            // Include if it's a Dijkstra city OR very close to the road
            if (dijkstraIds.contains(city.getId()) || minDist < thresholdKm) {
                // Ignore placeholder testing cities if not explicitly in Dijkstra path
                if (!dijkstraIds.contains(city.getId()) && city.getName() != null
                        && city.getName().toLowerCase().contains("city a")) {
                    continue;
                }
                ranked.add(new CityWithRank(city, bestIdx));
            }
        }

        // 3. Sort by their progression along the road geometry
        ranked.sort(Comparator.comparingInt(r -> r.rank));

        // 4. Extract cities and ensure start/end are kept
        List<City> uniqueCities = ranked.stream()
                .map(r -> r.city)
                .distinct()
                .collect(Collectors.toList());

        if (uniqueCities.size() <= 15)
            return uniqueCities;

        // If too many, keep start, end, and some middle ones
        List<City> result = new ArrayList<>();
        result.add(uniqueCities.get(0)); // Start

        int step = (uniqueCities.size() - 2) / 13;
        for (int i = 1; i < uniqueCities.size() - 1; i += Math.max(1, step)) {
            result.add(uniqueCities.get(i));
            if (result.size() >= 14)
                break;
        }

        if (!result.contains(uniqueCities.get(uniqueCities.size() - 1))) {
            result.add(uniqueCities.get(uniqueCities.size() - 1)); // End
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private List<double[]> fetchOSRMGeometry(List<City> path) {
        if (!osrmEnabled) {
            return java.util.Collections.emptyList();
        }
        try {
            String coords = path.stream()
                    .map(c -> c.getLongitude() + "," + c.getLatitude())
                    .collect(Collectors.joining(";"));

            String url = "http://router.project-osrm.org/route/v1/driving/" + coords
                    + "?overview=full&geometries=geojson";

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
                            .map(c -> new double[] { c.get(1), c.get(0) }) // OSRM is [lng, lat]
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
