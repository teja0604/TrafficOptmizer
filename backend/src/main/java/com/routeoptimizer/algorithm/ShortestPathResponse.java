package com.routeoptimizer.algorithm;

import com.routeoptimizer.model.City;
import java.util.List;

/**
 * Simplified response DTO for shortest path calculations.
 * Only essential values are exposed to avoid circular serialization and
 * keep the API contract concise.
 */
public class ShortestPathResponse {
    private List<City> path;
    private double distance;
    private double totalTravelMinutes;
    private String error; // in case something went wrong (eg. no roads)

    public ShortestPathResponse() {}

    public ShortestPathResponse(List<City> path, double distance, double totalTravelMinutes) {
        this.path = path;
        this.distance = distance;
        this.totalTravelMinutes = totalTravelMinutes;
    }

    // getters and setters
    public List<City> getPath() { return path; }
    public void setPath(List<City> path) { this.path = path; }
    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }
    public double getTotalTravelMinutes() { return totalTravelMinutes; }
    public void setTotalTravelMinutes(double totalTravelMinutes) { this.totalTravelMinutes = totalTravelMinutes; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
