CREATE TABLE IF NOT EXISTS city (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    latitude DOUBLE,
    longitude DOUBLE,
    INDEX idx_lat_lng (latitude, longitude)
);

CREATE TABLE IF NOT EXISTS road (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    from_city BIGINT,
    to_city BIGINT,
    distance DOUBLE,
    traffic_level DOUBLE,
    road_type VARCHAR(50),
    speed_limit DOUBLE,
    travel_time DOUBLE
);
