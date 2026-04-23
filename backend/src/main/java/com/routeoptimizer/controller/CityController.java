package com.routeoptimizer.controller;

import com.routeoptimizer.model.City;
import com.routeoptimizer.service.GraphService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping("/api/cities")
public class CityController {

    private static final Logger logger = LoggerFactory.getLogger(CityController.class);

    private final GraphService graphService;

    public CityController(GraphService graphService) {
        this.graphService = graphService;
    }

    @GetMapping
    public ResponseEntity<Collection<City>> getAllCities() {
        logger.info("[GET /api/cities] Fetching all cities.");
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(graphService.getAllCities());
    }

    @PostMapping
    public City addCity(@RequestBody City city) {
        logger.info("[POST /api/cities] Request received: name={}, lat={}, lng={}",
                city.getName(), city.getLatitude(), city.getLongitude());
        City saved = graphService.addCity(city);
        logger.info("[POST /api/cities] City saved successfully with id={}", saved.getId());
        return saved;
    }

    @PostMapping("/auto-fix")
    public String autoFixCityConnections() {
        logger.info("[POST /api/cities/auto-fix] Running auto-fix for graph connectivity.");
        return graphService.autoFixCityConnections();
    }
}
