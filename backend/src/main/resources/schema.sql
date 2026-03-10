CREATE TABLE IF NOT EXISTS city (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255),
    latitude DOUBLE,
    longitude DOUBLE
);

CREATE TABLE IF NOT EXISTS road (
    id VARCHAR(255) PRIMARY KEY,
    from_city VARCHAR(255),
    to_city VARCHAR(255),
    distance DOUBLE,
    traffic_level DOUBLE
);
