package com.routeoptimizer.controller;

import com.routeoptimizer.algorithm.ShortestPathResponse;
import com.routeoptimizer.model.City;
import com.routeoptimizer.model.Graph;
import com.routeoptimizer.service.GraphService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PathController {

    private final GraphService graphService;

    public PathController(GraphService graphService) {
        this.graphService = graphService;
    }

    @PostMapping("/shortest-path")
    public ResponseEntity<?> findShortestPath(@RequestBody PathRequest request) {
        // Always return a consistent response body (never throw client-side 400s for routing errors).
        if (request.getStartCity() == null || request.getEndCity() == null) {
            ShortestPathResponse resp = new ShortestPathResponse();
            resp.setPath(List.of());
            resp.setEnrichedPath(List.of());
            resp.setError("Start city and end city cannot be null");
            return ResponseEntity.ok(resp);
        }

        // If source == destination, return a trivial path with that single city.
        if (request.getStartCity().equals(request.getEndCity())) {
            Graph graph = graphService.getGraph();
            City city = graph.getCities().get(request.getStartCity());
            ShortestPathResponse resp = new ShortestPathResponse();
            if (city != null) {
                resp.setPath(List.of(city));
                resp.setEnrichedPath(List.of(city));
            } else {
                resp.setPath(List.of());
                resp.setEnrichedPath(List.of());
                resp.setError("Start city does not exist in graph.");
            }
            resp.setDistance(0.0);
            resp.setTotalTravelMinutes(0.0);
            return ResponseEntity.ok(resp);
        }

        ShortestPathResponse resp = graphService.findShortestPath(
                request.getStartCity(),
                request.getEndCity(),
                request.getTrafficLevel() > 0 ? request.getTrafficLevel() : 1.0);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/export-graph")
    public Graph exportGraph() {
        return graphService.getGraph();
    }
}
