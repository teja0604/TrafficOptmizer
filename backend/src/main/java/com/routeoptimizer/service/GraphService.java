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
                
                if (r.getRoadType() == null || r.getRoadType().trim().isEmpty()) {
                    r.setRoadType("SH");
                }
                r.setRoadType(r.getRoadType().toUpperCase());
                
                if (r.getSpeedLimit() <= 0) {
                    double speed = ROAD_SPEEDS.getOrDefault(r.getRoadType(), 60.0);
                    r.setSpeedLimit(speed);
                }
                
                if (r.getTravelTime() <= 0 && r.getDistance() > 0) {
                    r.setTravelTime(r.getDistance() / r.getSpeedLimit());
                }

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

    public ShortestPathResponse findShortestPath(String startCityId, String endCityId, double trafficLevel) {
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
        return dijkstraAlgorithm.findShortestPath(graph, startCityId, endCityId, trafficLevel);
    }
}
