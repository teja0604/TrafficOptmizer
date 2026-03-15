package com.routeoptimizer.model;

import jakarta.persistence.*;

@Entity
public class Road {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long fromCity;
    private Long toCity;
    private double distance;
    private double trafficLevel;

    private String roadType;
    private double speedLimit;
    private double travelTime;

    public Road() {}

    public Road(Long id, Long fromCity, Long toCity, double distance, double trafficLevel,
                String roadType, double speedLimit, double travelTime) {
        this.id = id;
        this.fromCity = fromCity;
        this.toCity = toCity;
        this.distance = distance;
        this.trafficLevel = trafficLevel;
        this.roadType = roadType;
        this.speedLimit = speedLimit;
        this.travelTime = travelTime;
    }

    // getters/setters for all fields...
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFromCity() { return fromCity; }
    public void setFromCity(Long fromCity) { this.fromCity = fromCity; }
    public Long getToCity() { return toCity; }
    public void setToCity(Long toCity) { this.toCity = toCity; }
    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }
    public double getTrafficLevel() { return trafficLevel; }
    public void setTrafficLevel(double trafficLevel) { this.trafficLevel = trafficLevel; }
    public String getRoadType() { return roadType; }
    public void setRoadType(String roadType) { this.roadType = roadType; }
    public double getSpeedLimit() { return speedLimit; }
    public void setSpeedLimit(double speedLimit) { this.speedLimit = speedLimit; }
    public double getTravelTime() { return travelTime; }
    public void setTravelTime(double travelTime) { this.travelTime = travelTime; }

    public double getEffectiveWeight(double globalTrafficLevel) {
        double weight = travelTime * (1.0 + (trafficLevel * globalTrafficLevel));
        if ("BAD_SH".equalsIgnoreCase(roadType)) {
            weight *= 2.5;
        }
        return weight;
    }
}
