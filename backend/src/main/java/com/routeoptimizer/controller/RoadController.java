package com.routeoptimizer.controller;

import com.routeoptimizer.model.Road;
import com.routeoptimizer.service.GraphService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roads")
public class RoadController {

    private final GraphService graphService;

    public RoadController(GraphService graphService) {
        this.graphService = graphService;
    }
    
    @GetMapping
    public List<Road> getAllRoads() {
        return graphService.getAllRoads();
    }

    @PostMapping
    public Road addRoad(@RequestBody Road road) {
        if (road.getDistance() <= 0) {
            throw new IllegalArgumentException("Distance must be positive");
        }
        return graphService.addRoad(road);
    }
}
