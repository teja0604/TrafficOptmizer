package com.routeoptimizer.controller;

import com.routeoptimizer.model.City;
import com.routeoptimizer.service.GraphService;
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
    public Collection<City> getAllCities() {
        logger.info("[GET /api/cities] Fetching all cities.");
        return graphService.getAllCities();
    }

    @PostMapping
    public City addCity(@RequestBody City city) {
        logger.info("[POST /api/cities] Request received: name={}, lat={}, lng={}",
                city.getName(), city.getLatitude(), city.getLongitude());
        City saved = graphService.addCity(city);
        logger.info("[POST /api/cities] City saved successfully with id={}", saved.getId());
        return saved;
    }
}

