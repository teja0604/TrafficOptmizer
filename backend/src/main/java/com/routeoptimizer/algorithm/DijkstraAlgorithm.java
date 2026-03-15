package com.routeoptimizer.algorithm;

import com.routeoptimizer.model.City;
import com.routeoptimizer.model.Graph;
import com.routeoptimizer.model.Road;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DijkstraAlgorithm {

    public ShortestPathResponse findShortestPath(Graph graph, Long startCityId, Long endCityId, double trafficLevel) {
        Map<Long, Double> distances = new HashMap<>();
        Map<Long, Long> previous = new HashMap<>();
        PriorityQueue<Long> pq = new PriorityQueue<>(Comparator.comparingDouble(distances::get));

        Set<Long> visited = new HashSet<>();

        for (Long cityId : graph.getCities().keySet()) {
            distances.put(cityId, Double.POSITIVE_INFINITY);
            previous.put(cityId, null);
        }
        distances.put(startCityId, 0.0);
        pq.add(startCityId);

        while (!pq.isEmpty()) {
            Long current = pq.poll();

            if (current.equals(endCityId)) {
                break;
            }

            if (visited.contains(current)) {
                continue;
            }
            visited.add(current);

            List<Road> adjacentRoads = graph.getAdjacentRoads(current);
            if (adjacentRoads == null)
                continue;

            for (Road road : adjacentRoads) {
                Long neighbor = road.getToCity();
                if (neighbor == null)
                    continue;

                double effectiveWeight = road.getEffectiveWeight(trafficLevel);
                double newDist = distances.get(current) + effectiveWeight;

                if (newDist < distances.get(neighbor)) {
                    distances.put(neighbor, newDist);
                    previous.put(neighbor, current);
                    pq.add(neighbor);
                }
            }
        }

        List<City> path = new ArrayList<>();
        Long curr = endCityId;
        double finalDistance = 0.0;

        if (previous.get(curr) != null || curr.equals(startCityId)) {
            while (curr != null) {
                City currentPathCity = graph.getCities().get(curr);
                if (currentPathCity != null) {
                    path.add(0, currentPathCity);
                }

                Long prevNode = previous.get(curr);
                if (prevNode != null) {
                    final Long c = curr;
                    List<Road> adjacentPrev = graph.getAdjacentRoads(prevNode);
                    if (adjacentPrev != null) {
                        Road connectingRoad = adjacentPrev.stream()
                                .filter(r -> r.getToCity() != null && r.getToCity().equals(c))
                                .findFirst().orElse(null);
                        if (connectingRoad != null) {
                            finalDistance += connectingRoad.getDistance();
                        }
                    }
                }
                curr = prevNode;
            }
        }

        double finalTime = distances.get(endCityId) == Double.POSITIVE_INFINITY ? 0 : distances.get(endCityId);
        double totalTravelMinutes = finalTime * 60.0;

        ShortestPathResponse response = new ShortestPathResponse();
        response.setPath(path);
        response.setDistance(Math.round(finalDistance * 100.0) / 100.0);
        response.setTotalTravelMinutes(Math.round(totalTravelMinutes * 100.0) / 100.0);

        return response;
    }
}
