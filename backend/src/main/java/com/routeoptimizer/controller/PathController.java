package com.routeoptimizer.controller;

import com.routeoptimizer.algorithm.ShortestPathResponse;
import com.routeoptimizer.model.Graph;
import com.routeoptimizer.service.GraphService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class PathController {

    private final GraphService graphService;

    public PathController(GraphService graphService) {
        this.graphService = graphService;
    }

    @PostMapping("/shortest-path")
    public ResponseEntity<?> findShortestPath(@RequestBody PathRequest request) {
        if (request.getStartCity() == null || request.getEndCity() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Start city and end city cannot be null"));
        }
        
        // Validation: Source and destination cannot be the same
        if (request.getStartCity().equals(request.getEndCity())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Source and destination cities must be different"));
        }

        ShortestPathResponse resp = graphService.findShortestPath(
                request.getStartCity(),
                request.getEndCity(),
                request.getTrafficLevel() > 0 ? request.getTrafficLevel() : 1.0);
        if (resp.getError() != null) {
            return ResponseEntity.badRequest().body(Map.of("error", resp.getError()));
        }
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/export-graph")
    public Graph exportGraph() {
        return graphService.getGraph();
    }
}
