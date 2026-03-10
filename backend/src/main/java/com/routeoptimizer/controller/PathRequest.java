package com.routeoptimizer.controller;

public class PathRequest {
    private String startCity;
    private String endCity;
    private double trafficLevel = 1.0;

    public String getStartCity() { return startCity; }
    public void setStartCity(String startCity) { this.startCity = startCity; }

    public String getEndCity() { return endCity; }
    public void setEndCity(String endCity) { this.endCity = endCity; }

    public double getTrafficLevel() { return trafficLevel; }
    public void setTrafficLevel(double trafficLevel) { this.trafficLevel = trafficLevel; }
}
