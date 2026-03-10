package com.routeoptimizer.controller;

import com.routeoptimizer.model.City;
import com.routeoptimizer.service.GraphService;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping("/api/cities")
public class CityController {

    private final GraphService graphService;

    public CityController(GraphService graphService) {
        this.graphService = graphService;
    }

    @GetMapping
    public Collection<City> getAllCities() {
        return graphService.getAllCities();
    }

    @PostMapping
    public City addCity(@RequestBody City city) {
        return graphService.addCity(city);
    }
}
