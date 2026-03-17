package com.routeoptimizer.dto;

public class RoadRequest {
    private Long fromCityId;
    private Long toCityId;
    private double distance;
    private String roadType;

    public RoadRequest() {}

    public Long getFromCityId() { return fromCityId; }
    public void setFromCityId(Long fromCityId) { this.fromCityId = fromCityId; }

    public Long getToCityId() { return toCityId; }
    public void setToCityId(Long toCityId) { this.toCityId = toCityId; }

    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }

    public String getRoadType() { return roadType; }
    public void setRoadType(String roadType) { this.roadType = roadType; }
}
