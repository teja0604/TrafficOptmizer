package com.routeoptimizer.controller;

public class PathRequest {
    private Long startCity;
    private Long endCity;
    private double trafficLevel = 1.0;

    public Long getStartCity() { return startCity; }
    public void setStartCity(Long startCity) { this.startCity = startCity; }

    public Long getEndCity() { return endCity; }
    public void setEndCity(Long endCity) { this.endCity = endCity; }

    public double getTrafficLevel() { return trafficLevel; }
    public void setTrafficLevel(double trafficLevel) { this.trafficLevel = trafficLevel; }
}
