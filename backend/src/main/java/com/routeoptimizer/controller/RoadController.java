package com.routeoptimizer.controller;

import com.routeoptimizer.model.City;
import com.routeoptimizer.model.Road;
import com.routeoptimizer.dto.RoadRequest;
import com.routeoptimizer.service.GraphService;
import com.routeoptimizer.repository.CityRepository;
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

    public RoadController(GraphService graphService, CityRepository cityRepository) {
        this.graphService = graphService;
        this.cityRepository = cityRepository;
    }

    @GetMapping
    public List<Road> getAllRoads() {
        logger.info("[GET /api/roads] Fetching all roads.");
        return graphService.getAllRoads();
    }

    @PostMapping
    public Road addRoad(@RequestBody RoadRequest request) {
        logger.info("[POST /api/roads] Request received: fromCityId={}, toCityId={}, distance={}, roadType={}",
                request.getFromCityId(), request.getToCityId(), request.getDistance(), request.getRoadType());

        if (request.getDistance() < 0) {
            throw new IllegalArgumentException("Distance cannot be negative");
        }

        City fromCity = cityRepository.findById(request.getFromCityId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid fromCityId: " + request.getFromCityId()));
        City toCity = cityRepository.findById(request.getToCityId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid toCityId: " + request.getToCityId()));

        Road road = new Road();
        road.setFromCity(fromCity);
        road.setToCity(toCity);
        road.setDistance(request.getDistance());
        road.setRoadType(request.getRoadType());
        road.setTrafficLevel(0.1); // default

        Road saved = graphService.addRoad(road);
        logger.info("[POST /api/roads] Road saved successfully with id={}", saved.getId());
        return saved;
    }
}

