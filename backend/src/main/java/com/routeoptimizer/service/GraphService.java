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
import org.springframework.web.client.RestTemplate;

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
                double distance = calculateHaversineDistance(
                        savedCity.getLatitude(), savedCity.getLongitude(),
                        existingCity.getLatitude(), existingCity.getLongitude());

                if (distance < 80.0) {
                    Road road = new Road();
                    road.setFromCity(savedCity.getId());
                    road.setToCity(existingCity.getId());
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
                    reverseRoad.setFromCity(existingCity.getId());
                    reverseRoad.setToCity(savedCity.getId());
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

        City city1 = cityRepository.findById(road.getFromCity()).orElse(null);
        City city2 = cityRepository.findById(road.getToCity()).orElse(null);
        if (city1 != null && city2 != null && road.getDistance() == 0) {
            road.setDistance(calculateHaversineDistance(city1.getLatitude(), city1.getLongitude(), city2.getLatitude(),
                    city2.getLongitude()));
        }

        if (road.getRoadType() == null || road.getRoadType().trim().isEmpty()) {
            road.setRoadType("SH");
        }
        road.setRoadType(road.getRoadType().toUpperCase());
        double speed = ROAD_SPEEDS.getOrDefault(road.getRoadType(), 60.0);
        road.setSpeedLimit(speed);
        if (road.getDistance() > 0) {
            road.setTravelTime(road.getDistance() / speed);
        } else {
            road.setTravelTime(0.0);
        }

        return roadRepository.save(road);
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

    public Graph getGraph() {
        Graph graph = new Graph();
        for (City c : cityRepository.findAll()) {
            graph.addCity(c);
        }
        int validRoads = 0;
        for (Road r : roadRepository.findAll()) {
            if (r.getFromCity() != null && r.getToCity() != null &&
                    graph.getCities().containsKey(r.getFromCity()) &&
                    graph.getCities().containsKey(r.getToCity())) {
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
            List<City> enriched = detectIntermediateWaypoints(resp.getPath());
            resp.setEnrichedPath(enriched);
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
        
        // We'll use a 20km threshold for "important" intermediate cities
        double thresholdKm = 20.0;
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

            // Include if it's a Dijkstra city OR very close to the road
            if (dijkstraIds.contains(city.getId()) || minDist < thresholdKm) {
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

        if (uniqueCities.size() <= 15) return uniqueCities;

        // If too many, keep start, end, and some middle ones
        List<City> result = new ArrayList<>();
        result.add(uniqueCities.get(0)); // Start
        
        int step = (uniqueCities.size() - 2) / 13;
        for (int i = 1; i < uniqueCities.size() - 1; i += Math.max(1, step)) {
            result.add(uniqueCities.get(i));
            if (result.size() >= 14) break;
        }
        
        if (!result.contains(uniqueCities.get(uniqueCities.size() - 1))) {
            result.add(uniqueCities.get(uniqueCities.size() - 1)); // End
        }
        
        return result;
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
