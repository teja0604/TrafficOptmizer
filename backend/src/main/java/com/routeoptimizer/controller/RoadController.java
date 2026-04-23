package com.routeoptimizer.controller;

import com.routeoptimizer.model.City;
import com.routeoptimizer.model.Road;
import com.routeoptimizer.dto.RoadRequest;
import com.routeoptimizer.service.GraphService;
import com.routeoptimizer.repository.CityRepository;
import com.routeoptimizer.repository.RoadRepository;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roads")
public class RoadController {

    private static final Logger logger = LoggerFactory.getLogger(RoadController.class);

    private final GraphService graphService;
    private final CityRepository cityRepository;
    private final RoadRepository roadRepository;

    public RoadController(GraphService graphService, CityRepository cityRepository, RoadRepository roadRepository) {
        this.graphService = graphService;
        this.cityRepository = cityRepository;
        this.roadRepository = roadRepository;
    }

    @GetMapping
    public ResponseEntity<List<Road>> getAllRoads() {
        logger.info("[GET /api/roads] Fetching all roads.");
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(graphService.getAllRoads());
    }

    @PostMapping
    public Road addRoad(@RequestBody RoadRequest request) {
        logger.info("[POST /api/roads] Request received: fromCityId={}, toCityId={}, distance={}, roadType={}",
                request.getFromCityId(), request.getToCityId(), request.getDistance(), request.getRoadType());

        if (request.getDistance() <= 0 || request.getDistance() > 3000.0) {
            throw new IllegalArgumentException("Invalid road distance. Must be between 1 and 3000 km.");
        }
        if (request.getDistance() < 50.0) {
            logger.warn("Suspiciously short road distance: {} km. Proceeding carefully.", request.getDistance());
        }

        City fromCity = cityRepository.findById(request.getFromCityId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid fromCityId: " + request.getFromCityId()));
        City toCity = cityRepository.findById(request.getToCityId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid toCityId: " + request.getToCityId()));

        // Validation: Prevent duplicate roads
        if (roadRepository.existsByFromCityIdAndToCityId(fromCity.getId(), toCity.getId())) {
            throw new RuntimeException("Road already exists between " + fromCity.getName() + " and " + toCity.getName());
        }

        Road road = new Road();
        road.setFromCity(fromCity);
        road.setToCity(toCity);
        road.setDistance(request.getDistance());
        road.setRoadType(request.getRoadType());
        road.setTrafficLevel(0.1); // default

        Road saved = graphService.addRoad(road);
        logger.info("[POST /api/roads] Road saved successfully with id={}", saved.getId());

        // Create reverse road for undirected connection
        Road reverseRoad = new Road();
        reverseRoad.setFromCity(toCity);
        reverseRoad.setToCity(fromCity);
        reverseRoad.setDistance(request.getDistance());
        reverseRoad.setRoadType(request.getRoadType());
        reverseRoad.setTrafficLevel(0.1);
        graphService.addRoad(reverseRoad);

        return saved;
    }

    @PostMapping("/update-distances")
    public String updateAllDistances() {
        logger.info("[POST /api/roads/update-distances] Triggered full DB distance update.");
        return graphService.updateAllRoadDistances();
    }

    @PostMapping("/fix-graph")
    public String fixGraphData() {
        logger.info("[POST /api/roads/fix-graph] Triggered DB cleanup and missing connections fix.");
        return graphService.fixGraphData();
    }
}
