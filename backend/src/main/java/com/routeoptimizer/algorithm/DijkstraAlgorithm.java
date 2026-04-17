package com.routeoptimizer.algorithm;

import com.routeoptimizer.model.City;
import com.routeoptimizer.model.Graph;
import com.routeoptimizer.model.Road;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DijkstraAlgorithm {

    private static final String VIRTUAL_ROAD_TYPE = "VIRTUAL";
    private static final double VIRTUAL_EDGE_MAX_EXTRA_PENALTY_MINUTES = 50.0;
    private static final double VIRTUAL_EDGE_PENALTY_MINUTES_PER_KM = 0.1;
    private static final double DIRECTION_PENALTY_SCALE = 0.5;
    private static final double HUB_TARGET_DEGREE = 4.0;
    private static final double HUB_PENALTY_HOURS_PER_LOG_UNIT = 0.12;

    private static class NodeDistance implements Comparable<NodeDistance> {
        Long cityId;
        double priority;
        double gScore;

        NodeDistance(Long cityId, double priority, double gScore) {
            this.cityId = cityId;
            this.priority = priority;
            this.gScore = gScore;
        }

        @Override
        public int compareTo(NodeDistance other) {
            return Double.compare(this.priority, other.priority);
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
        pq.add(new NodeDistance(startCityId, heuristicHours(graph, startCityId, endCityId), 0.0));

        while (!pq.isEmpty()) {
            NodeDistance current = pq.poll();
            Long u = current.cityId;

            // If this entry is stale (we found a better g-score already), skip it.
            // Priority queue uses f-score (g + h), so we can't directly compare priority to g.
            if (current.gScore > distances.getOrDefault(u, Double.POSITIVE_INFINITY)) continue;
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
                weight += adaptiveVirtualEdgePenaltyHours(road);
                weight += smoothDirectionPenaltyHours(graph, u, v, endCityId, road);
                weight += hubPenaltyHours(graph, v);
                if (weight <= 0.0) weight = 1e-9;
                double distanceThroughU = distances.get(u) + weight;

                if (distanceThroughU < distances.get(v)) {
                    distances.put(v, distanceThroughU);
                    previous.put(v, u);
                    double fScore = distanceThroughU + heuristicHours(graph, v, endCityId);
                    pq.add(new NodeDistance(v, fScore, distanceThroughU));
                }
            }
        }

        if (distances.get(endCityId) == null || distances.get(endCityId) == Double.POSITIVE_INFINITY) {
            ShortestPathResponse errorResp = new ShortestPathResponse();
            errorResp.setPath(new ArrayList<>());
            errorResp.setError("No route found - graph disconnected");
            return errorResp;
        }

        // 4. Trace path and calculate distance
        List<City> path = new ArrayList<>();
        Long currId = endCityId;
        double finalDistance = 0.0;

        while (currId != null) {
            City city = graph.getCities().get(currId);
            if (city != null) {
                path.add(0, city);
            }
            currId = previous.get(currId);
        }

        path = dedupeConsecutiveCities(path);

        // Safety: Ensure path starts with the source
        if (path.isEmpty() || !path.get(0).getId().equals(startCityId)) {
            ShortestPathResponse errorResp = new ShortestPathResponse();
            errorResp.setPath(new ArrayList<>());
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

    private static double heuristicHours(Graph graph, Long fromCityId, Long toCityId) {
        City from = graph.getCities().get(fromCityId);
        City to = graph.getCities().get(toCityId);
        if (from == null || to == null) return 0.0;
        double km = haversineKm(from.getLatitude(), from.getLongitude(), to.getLatitude(), to.getLongitude());
        double maxSpeedKmph = 80.0; // optimistic bound so heuristic stays admissible
        return km / maxSpeedKmph;
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * (2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a)));
    }

    private static double adaptiveVirtualEdgePenaltyHours(Road road) {
        if (road == null || road.getRoadType() == null) return 0.0;
        if (!VIRTUAL_ROAD_TYPE.equalsIgnoreCase(road.getRoadType())) return 0.0;
        // Adaptive penalty: short virtual hops are allowed, long jumps are discouraged.
        // Minutes penalty = min(50, distanceKm * 0.1)
        double distanceKm = Math.max(0.0, road.getDistance());
        double penaltyMinutes = Math.min(VIRTUAL_EDGE_MAX_EXTRA_PENALTY_MINUTES, distanceKm * VIRTUAL_EDGE_PENALTY_MINUTES_PER_KM);
        return penaltyMinutes / 60.0;
    }

    private static double smoothDirectionPenaltyHours(Graph graph, Long u, Long v, Long endCityId, Road road) {
        if (graph == null || u == null || v == null || endCityId == null || road == null) return 0.0;
        if (road.getRoadType() == null || !VIRTUAL_ROAD_TYPE.equalsIgnoreCase(road.getRoadType())) return 0.0;

        City cu = graph.getCities().get(u);
        City cv = graph.getCities().get(v);
        City cd = graph.getCities().get(endCityId);
        if (cu == null || cv == null || cd == null) return 0.0;

        double distToDestFromU = haversineKm(cu.getLatitude(), cu.getLongitude(), cd.getLatitude(), cd.getLongitude());
        double distToDestFromV = haversineKm(cv.getLatitude(), cv.getLongitude(), cd.getLatitude(), cd.getLongitude());

        double deltaKm = distToDestFromV - distToDestFromU;
        if (deltaKm <= 0.0) return 0.0;

        // Smooth penalty (avoids dead-end bias): weight += deltaKm * 0.5 (converted to hours scale).
        // Convert km to hours using a stable scale so the penalty is comparable to travel time.
        return (deltaKm * DIRECTION_PENALTY_SCALE) / 80.0;
    }

    private static double hubPenaltyHours(Graph graph, Long cityId) {
        if (graph == null || cityId == null) return 0.0;
        List<Road> adjacent = graph.getAdjacentRoads(cityId);
        int degree = adjacent == null ? 0 : adjacent.size();

        // Prefer higher-degree nodes in a smooth way (generic “hub” bias).
        double centrality = Math.log(degree + 1.0);
        double targetCentrality = Math.log(HUB_TARGET_DEGREE + 1.0);
        double missing = Math.max(0.0, targetCentrality - centrality);
        return missing * HUB_PENALTY_HOURS_PER_LOG_UNIT;
    }

    private static List<City> dedupeConsecutiveCities(List<City> path) {
        if (path == null || path.isEmpty()) return path;
        List<City> cleaned = new ArrayList<>();
        for (City city : path) {
            if (city == null || city.getId() == null) continue;
            if (cleaned.isEmpty() || !city.getId().equals(cleaned.get(cleaned.size() - 1).getId())) {
                cleaned.add(city);
            }
        }
        return cleaned;
    }
}
