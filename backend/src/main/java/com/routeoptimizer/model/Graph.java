package com.routeoptimizer.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Graph {
    private final Map<String, City> cities = new HashMap<>();
    private final List<Road> roads = new ArrayList<>();

    // Adjacency list: cityId -> List of Roads originating from that city
    private final Map<String, List<Road>> adjacencyList = new HashMap<>();

    public Map<String, City> getCities() { return cities; }
    public List<Road> getRoads() { return roads; }
    public Map<String, List<Road>> getAdjacencyList() { return adjacencyList; }

    public void addCity(City city) {
        cities.put(city.getId(), city);
        adjacencyList.putIfAbsent(city.getId(), new ArrayList<>());
    }

    public void addRoad(Road road) {
        roads.add(road);

        adjacencyList.putIfAbsent(road.getFromCity(), new ArrayList<>());
        adjacencyList.get(road.getFromCity()).add(road);

        adjacencyList.putIfAbsent(road.getToCity(), new ArrayList<>());
        Road reverseRoad = new Road(null, road.getToCity(), road.getFromCity(), road.getDistance(),
                road.getTrafficLevel(), road.getRoadType(), road.getSpeedLimit(), road.getTravelTime());
        adjacencyList.get(road.getToCity()).add(reverseRoad);
    }

    public List<Road> getAdjacentRoads(String cityId) {
        return adjacencyList.getOrDefault(cityId, new ArrayList<>());
    }
}
