package com.routeoptimizer.algorithm;

import com.routeoptimizer.model.City;
import com.routeoptimizer.model.Graph;
import com.routeoptimizer.model.Road;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DijkstraAlgorithm {

    private static class NodeDistance implements Comparable<NodeDistance> {
        Long cityId;
        double distance;

        NodeDistance(Long cityId, double distance) {
            this.cityId = cityId;
            this.distance = distance;
        }

        @Override
        public int compareTo(NodeDistance other) {
            return Double.compare(this.distance, other.distance);
        }
    }

    public ShortestPathResponse findShortestPath(Graph graph, Long startCityId, Long endCityId, double trafficLevel) {
        Map<Long, Double> distances = new HashMap<>();
        Map<Long, Long> previous = new HashMap<>();
        PriorityQueue<NodeDistance> pq = new PriorityQueue<>();

        // 1. Initialize distances
        for (Long cityId : graph.getCities().keySet()) {
            distances.put(cityId, Double.POSITIVE_INFINITY);
            previous.put(cityId, null);
        }

        // 2. Start node
        distances.put(startCityId, 0.0);
        pq.add(new NodeDistance(startCityId, 0.0));

        while (!pq.isEmpty()) {
            NodeDistance current = pq.poll();
            Long u = current.cityId;

            if (current.distance > distances.get(u)) continue;
            if (u.equals(endCityId)) break;

            List<Road> adjacentRoads = graph.getAdjacentRoads(u);
            if (adjacentRoads == null) continue;

            if (!graph.getCities().containsKey(u)) {
                continue;
            }

            for (Road road : adjacentRoads) {
                if (road.getToCity() == null) continue;
                Long v = road.getToCity().getId();
                if (v == null) continue;

                // 3. Relax edges
                double weight = road.getEffectiveWeight(trafficLevel);
                double distanceThroughU = distances.get(u) + weight;

                if (distanceThroughU < distances.get(v)) {
                    distances.put(v, distanceThroughU);
                    previous.put(v, u);
                    pq.add(new NodeDistance(v, distanceThroughU));
                }
            }
        }

        // 4. Trace path and calculate distance
        List<City> path = new ArrayList<>();
        Long currId = endCityId;
        double finalDistance = 0.0;

        // User-Specified Traceback Fix
        if (previous.containsKey(currId) || currId.equals(startCityId)) {
            while (currId != null) {
                City city = graph.getCities().get(currId);
                if (city != null) {
                    path.add(0, city);
                }
                currId = previous.get(currId);
            }
        }

        // Safety: Ensure path starts with the source
        if (path.isEmpty() || !path.get(0).getId().equals(startCityId)) {
            ShortestPathResponse errorResp = new ShortestPathResponse();
            errorResp.setError("No route found between selected cities");
            return errorResp;
        }

        // Calculate actual road distance for the validated path
        for (int i = 0; i < path.size() - 1; i++) {
            Long u = path.get(i).getId();
            Long v = path.get(i + 1).getId();
            List<Road> roadsFromU = graph.getAdjacentRoads(u);
            if (roadsFromU != null) {
                Road connectingRoad = roadsFromU.stream()
                        .filter(r -> r.getToCity() != null && r.getToCity().getId().equals(v))
                        .min(Comparator.comparingDouble(r -> r.getEffectiveWeight(trafficLevel)))
                        .orElse(null);
                if (connectingRoad != null) {
                    finalDistance += connectingRoad.getDistance();
                }
            }
        }

        double finalTime = (distances.get(endCityId) == Double.POSITIVE_INFINITY) ? 0 : distances.get(endCityId);
        double totalTravelMinutes = finalTime * 60.0;

        ShortestPathResponse response = new ShortestPathResponse();
        response.setPath(path);
        response.setDistance(Math.round(finalDistance * 100.0) / 100.0);
        response.setTotalTravelMinutes(Math.round(totalTravelMinutes * 100.0) / 100.0);

        return response;
    }
}
