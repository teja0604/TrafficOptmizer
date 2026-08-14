# TrafficOptimizer

<p align="center">
  <strong>Intelligent Traffic-Aware Route Optimization and Interactive Path Visualization</strong>
</p>

<p align="center">
  A full-stack route optimization platform that combines graph algorithms, traffic-aware routing, real-world road geometry, interactive maps, and persistent city-road data.
</p>

<p align="center">

<img src="https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 17"/>
<img src="https://img.shields.io/badge/Spring_Boot-3.3.2-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot"/>
<img src="https://img.shields.io/badge/React-19-61DAFB?style=for-the-badge&logo=react&logoColor=black" alt="React"/>
<img src="https://img.shields.io/badge/Vite-7-646CFF?style=for-the-badge&logo=vite&logoColor=white" alt="Vite"/>
<img src="https://img.shields.io/badge/MySQL-8-4479A1?style=for-the-badge&logo=mysql&logoColor=white" alt="MySQL"/>
<img src="https://img.shields.io/badge/Leaflet-1.9.4-199900?style=for-the-badge&logo=leaflet&logoColor=white" alt="Leaflet"/>
<img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker"/>

</p>

<p align="center">

<img src="https://img.shields.io/badge/Axios-5A29E4?style=flat-square&logo=axios&logoColor=white" alt="Axios"/>
<img src="https://img.shields.io/badge/React_Leaflet-199900?style=flat-square&logo=leaflet&logoColor=white" alt="React Leaflet"/>
<img src="https://img.shields.io/badge/Dijkstra-Algorithm-4CAF50?style=flat-square" alt="Dijkstra"/>
<img src="https://img.shields.io/badge/OSRM-Routing-2E7D32?style=flat-square" alt="OSRM"/>
<img src="https://img.shields.io/badge/Haversine-Geospatial-1565C0?style=flat-square" alt="Haversine"/>

</p>

---

## Overview

TrafficOptimizer is a full-stack route-planning and visualization system designed to find efficient paths between cities while taking traffic and road characteristics into account.

The platform combines:

* Graph-based shortest-path computation
* Traffic-aware route weighting
* Road-type speed models
* Geographic distance calculations
* OSRM real-world driving distances
* Interactive Leaflet maps
* Dynamic city and road management
* Animated route visualization
* Persistent backend graph data
* REST API integration
* Docker-ready backend deployment

The frontend provides the interactive user experience, while the Spring Boot backend manages the graph, calculates routes, persists data, and exposes REST APIs.

---

# Key Features

## Traffic-Aware Route Optimization

Users select:

* Starting city
* Destination city
* Traffic level

The backend calculates a route using the graph representation of the road network.

The result includes:

* Shortest route
* Total distance
* Estimated travel time
* Path city sequence
* Enriched route information

The frontend then renders the route on an interactive map.

---

## Advanced Dijkstra-Based Routing

The routing engine is implemented in `DijkstraAlgorithm.java`.

The algorithm uses:

* Priority queue
* Distance map
* Previous-node tracking
* Edge relaxation
* Heuristic estimation
* Traffic-adjusted road weights
* Virtual-road penalties
* Directional penalties
* Hub penalties
* Haversine distance

The resulting architecture can be viewed as:

```text id="traffic-algorithm"
                        Graph
                          |
                          v
                  Start City / End City
                          |
                          v
                  Priority Queue
                          |
                          v
                  Edge Relaxation
                          |
        +-----------------+-----------------+
        |                 |                 |
        v                 v                 v
   Traffic Weight    Direction Bias    Hub Penalty
        |                 |                 |
        +-----------------+-----------------+
                          |
                          v
                    Best Path
                          |
                          v
                 Route Reconstruction
```

---

# Traffic Modeling

Road travel cost is not treated as simple geometric distance.

The backend incorporates traffic and road characteristics into route weighting.

Road speeds are configured according to road type:

| Road Type | Default Speed |
| --------- | ------------: |
| `NH`      |       80 km/h |
| `SH`      |       60 km/h |
| `CITY`    |       40 km/h |
| `VILLAGE` |       25 km/h |
| `BAD_SH`  |       30 km/h |

The graph service converts distance and road type into travel time used by the routing system.

---

# Geographic Distance Calculation

The backend uses the Haversine formula to calculate geographic distance between coordinates.

```text
Latitude / Longitude
        |
        v
Haversine Distance
        |
        v
Distance in KM
        |
        v
Route / Road Weight
```

This provides a geographic estimate even when real-world road geometry is not immediately available.

---

# OSRM Integration

TrafficOptimizer also integrates with the **Open Source Routing Machine (OSRM)** for real-world road distances and route geometry.

The backend can use OSRM-derived driving distance when building or correcting road data, while the frontend requests detailed road geometry for the final route.

```text
City A
  |
  v
OSRM
  |
  v
Real Driving Distance
  |
  v
Road Model
  |
  v
Graph
```

For visualization:

```text
Selected Cities
      |
      v
OSRM Routing API
      |
      v
Road Geometry
      |
      v
Leaflet Polyline
```

---

# Interactive Map Visualization

The frontend uses **React-Leaflet** and Leaflet for map-based visualization.

The application can:

* Display cities
* Display road connections
* Visualize the calculated route
* Add cities by map location
* Animate route/path information
* Display route statistics
* Switch between themes
* Adjust visualization speed

---

# Algorithm Visualization

The frontend contains a dedicated algorithm visualization component:

```text id="visualizer"
Graph
  |
  v
Dijkstra Execution
  |
  +--> Visited Nodes
  +--> Evaluated Edges
  +--> Current Shortest Path
  |
  v
Animated Visualization
```

The homepage integrates:

* `MapView`
* `CitySelector`
* `AlgorithmVisualizer`
* `ResultPanel`
* `TrafficSlider`
* `AddCityModal`
* `AddRoadModal`

This makes the routing algorithm visible rather than presenting it as a black-box result.

---

# City Management

Users can add cities to the graph.

A city contains information such as:

```text
City
├── ID
├── Name
├── Latitude
└── Longitude
```

When a new city is added, the backend can evaluate its proximity to existing cities and automatically construct candidate roads where the distance satisfies the configured threshold.

---

# Road Management

Users can also create roads manually.

Road information includes:

```text
Road
├── From City
├── To City
├── Distance
├── Road Type
├── Traffic Level
├── Speed Limit
└── Travel Time
```

The backend validates road distances and calculates travel time based on the configured road type speed.

---

# Route Calculation Workflow

```text id="route-flow"
┌───────────────────┐
│ Select Start City │
└─────────┬─────────┘
          |
          v
┌───────────────────┐
│ Select Destination│
└─────────┬─────────┘
          |
          v
┌───────────────────┐
│ Set Traffic Level │
└─────────┬─────────┘
          |
          v
┌───────────────────┐
│   Frontend API    │
└─────────┬─────────┘
          |
          v
┌───────────────────┐
│ Spring Boot API   │
└─────────┬─────────┘
          |
          v
┌───────────────────┐
│   Graph Service   │
└─────────┬─────────┘
          |
          v
┌───────────────────┐
│ Dijkstra Routing  │
└─────────┬─────────┘
          |
          v
┌───────────────────┐
│ Shortest Path     │
│ + Distance        │
│ + Travel Time     │
└─────────┬─────────┘
          |
          v
┌───────────────────┐
│ OSRM Route Shape  │
└─────────┬─────────┘
          |
          v
┌───────────────────┐
│ Leaflet Map       │
└───────────────────┘
```

---

# System Design Architecture

```text id="system-design"
                         TRAFFICOPTIMIZER
                                |
             ┌──────────────────┴──────────────────┐
             |                                     |
             v                                     v
     React Frontend                         Spring Boot Backend
       React 19                                  Java 17
       Vite 7                              Spring Boot 3.3.2
             |                                     |
             v                                     v
      Axios API Layer                     REST Controllers
             |                                     |
             |                            ┌────────┴─────────┐
             |                            |                  |
             |                            v                  v
             |                       Graph Service       Security
             |                            |
             |                            v
             |                      Dijkstra Engine
             |                            |
             |                   ┌────────┼─────────┐
             |                   |        |         |
             |                   v        v         v
             |               Traffic  Haversine   OSRM
             |                   |        |         |
             |                   └────────┼─────────┘
             |                            |
             |                            v
             |                         Graph
             |                            |
             |                            v
             |                    JPA Repositories
             |                            |
             |                            v
             |                       MySQL / DB
             |
             v
       React-Leaflet
             |
             v
        Interactive Map
```

---

# Backend Architecture

The backend follows a layered Spring Boot architecture.

```text
backend/
│
├── controller/
│   ├── CityController
│   ├── PathController
│   ├── RoadController
│   ├── PathRequest
│   └── GlobalExceptionHandler
│
├── algorithm/
│   ├── DijkstraAlgorithm
│   └── ShortestPathResponse
│
├── service/
│   └── GraphService
│
├── model/
│   ├── City
│   ├── Road
│   └── Graph
│
├── repository/
│   ├── CityRepository
│   └── RoadRepository
│
└── config/
    ├── WebConfig
    └── RequestLoggingConfig
```

The actual repository contains these controller, algorithm, model, repository, service, and configuration packages.

---

# Frontend Architecture

The frontend is structured around a single main experience with reusable visualization and control components.

```text
frontend/
│
├── src/
│   ├── components/
│   │   ├── MapView
│   │   ├── CitySelector
│   │   ├── AlgorithmVisualizer
│   │   ├── ResultPanel
│   │   ├── TrafficSlider
│   │   ├── AddCityModal
│   │   └── AddRoadModal
│   │
│   ├── pages/
│   │   └── HomePage
│   │
│   ├── services/
│   │   └── api.js
│   │
│   ├── App.jsx
│   └── main.jsx
│
└── package.json
```

The frontend package configuration confirms React 19, Vite 7, Axios, Leaflet, React-Leaflet, Lucide, React Icons, and React Toastify.

---

# Frontend Technology Stack

## Core

<p>
<img src="https://img.shields.io/badge/React_19-61DAFB?style=for-the-badge&logo=react&logoColor=black" alt="React"/>
<img src="https://img.shields.io/badge/Vite_7-646CFF?style=for-the-badge&logo=vite&logoColor=white" alt="Vite"/>
<img src="https://img.shields.io/badge/JavaScript-F7DF1E?style=for-the-badge&logo=javascript&logoColor=black" alt="JavaScript"/>
</p>

## Mapping

<p>
<img src="https://img.shields.io/badge/Leaflet-199900?style=for-the-badge&logo=leaflet&logoColor=white" alt="Leaflet"/>
<img src="https://img.shields.io/badge/React_Leaflet-199900?style=flat-square&logo=leaflet&logoColor=white" alt="React Leaflet"/>
</p>

## API and UI

<p>
<img src="https://img.shields.io/badge/Axios-5A29E4?style=for-the-badge&logo=axios&logoColor=white" alt="Axios"/>
<img src="https://img.shields.io/badge/Lucide-000000?style=flat-square" alt="Lucide"/>
<img src="https://img.shields.io/badge/React_Icons-61DAFB?style=flat-square" alt="React Icons"/>
<img src="https://img.shields.io/badge/React_Toastify-FF6B6B?style=flat-square" alt="React Toastify"/>
</p>

These dependencies are directly listed in the frontend package manifest.

---

# Backend Technology Stack

## Runtime and Framework

<p>
<img src="https://img.shields.io/badge/Java_17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 17"/>
<img src="https://img.shields.io/badge/Spring_Boot_3.3.2-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot"/>
<img src="https://img.shields.io/badge/Spring_Web-6DB33F?style=flat-square&logo=spring&logoColor=white" alt="Spring Web"/>
<img src="https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=flat-square&logo=spring&logoColor=white" alt="Spring Data JPA"/>
</p>

## Security

<p>
<img src="https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white" alt="Spring Security"/>
</p>

## Database

<p>
<img src="https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white" alt="MySQL"/>
<img src="https://img.shields.io/badge/PostgreSQL-4169E1?style=flat-square&logo=postgresql&logoColor=white" alt="PostgreSQL"/>
<img src="https://img.shields.io/badge/H2_Test_DB-59666C?style=flat-square" alt="H2"/>
</p>

The Maven configuration uses Java 17 and Spring Boot 3.3.2, with Spring Web, Spring Security, Spring Data JPA, MySQL, PostgreSQL, and H2 for testing.

> **Primary database configuration:** The repository README identifies MySQL 8+ as the required database and `route_optimizer` as the database name. The Maven file also retains a PostgreSQL runtime dependency, so PostgreSQL appears to be supported at the dependency level even though the documented setup is MySQL.

---

# Geospatial and Routing Technology

<p>
<img src="https://img.shields.io/badge/Dijkstra-4CAF50?style=flat-square" alt="Dijkstra"/>
<img src="https://img.shields.io/badge/Haversine-1565C0?style=flat-square" alt="Haversine"/>
<img src="https://img.shields.io/badge/OSRM-2E7D32?style=flat-square" alt="OSRM"/>
<img src="https://img.shields.io/badge/GeoJSON-111111?style=flat-square" alt="GeoJSON"/>
</p>

---

# Data Model

The core graph consists of three main domain concepts:

```text id="domain-model"
              ┌──────────────┐
              │     City     │
              │              │
              │ id           │
              │ name         │
              │ latitude     │
              │ longitude    │
              └──────┬───────┘
                     |
                     | connected by
                     v
              ┌──────────────┐
              │     Road     │
              │              │
              │ fromCity     │
              │ toCity       │
              │ distance     │
              │ roadType     │
              │ trafficLevel │
              │ speedLimit   │
              │ travelTime   │
              └──────┬───────┘
                     |
                     v
              ┌──────────────┐
              │     Graph    │
              │              │
              │ cities       │
              │ adjacency    │
              └──────────────┘
```

The backend model layer includes `City`, `Road`, and `Graph`, with repositories for city and road persistence.

---

# Routing Cost Model

The effective routing cost combines multiple factors.

Conceptually:

```text id="weight-model"
Effective Route Weight
        |
        +--> Base Road Travel Time
        |
        +--> Traffic Adjustment
        |
        +--> Virtual Road Penalty
        |
        +--> Direction Penalty
        |
        +--> Hub Penalty
        |
        v
Final Edge Weight
```

The Dijkstra implementation explicitly applies these penalty mechanisms before relaxing an edge.

---

# Road Geometry Workflow

The frontend does not simply draw straight lines between cities.

After receiving the backend route:

```text
Backend Path
     |
     v
Sequence of Cities
     |
     v
OSRM Segment Requests
     |
     v
GeoJSON Road Geometry
     |
     v
Merged Coordinates
     |
     v
Leaflet Polyline
```

The frontend's API service requests each road segment from OSRM and merges the returned GeoJSON coordinates into one continuous route.

---

# REST API

## City APIs

```http
GET  /api/cities
POST /api/cities
```

These endpoints support city retrieval and city creation.

---

## Road APIs

```http
GET  /api/roads
POST /api/roads
```

These endpoints support graph road retrieval and road creation.

---

## Routing APIs

```http
POST /api/shortest-path
GET  /api/export-graph
```

The shortest-path endpoint accepts the source city, destination city, and traffic level.

Example request:

```json
{
  "startCity": 1,
  "endCity": 5,
  "trafficLevel": 1.5
}
```

Example response structure:

```json
{
  "path": [],
  "enrichedPath": [],
  "distance": 0,
  "totalTravelMinutes": 0
}
```

The actual response model is implemented through `ShortestPathResponse`.

---

# User Workflow

```text id="user-flow"
                    TrafficOptimizer
                           |
                           v
                  Choose Start City
                           |
                           v
                Choose Destination City
                           |
                           v
                  Adjust Traffic Level
                           |
                           v
                 Find Shortest Path
                           |
                           v
                   Backend Routing
                           |
                           v
                  Dijkstra Calculation
                           |
                           v
                  Route + Statistics
                           |
                           v
                    OSRM Geometry
                           |
                           v
                   Interactive Map
                           |
                  +--------+--------+
                  |                 |
                  v                 v
             Route Details    Algorithm Animation
```

---

# Frontend User Interface

The main page combines:

* Header
* Route controls
* City selection
* Traffic slider
* Animation speed
* Map visualization
* Route results
* Algorithm visualization
* Add City dialog
* Add Road dialog
* Theme switching

The homepage source confirms these are composed as individual React components.

---

# Project Structure

```text id="project-structure"
TrafficOptmizer/
│
├── backend/
│   ├── .mvn/
│   │   └── wrapper/
│   │
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/
│   │       │       └── routeoptimizer/
│   │       │           │
│   │       │           ├── RouteOptimizerApplication.java
│   │       │           ├── SecurityConfig.java
│   │       │           │
│   │       │           ├── algorithm/
│   │       │           │   ├── DijkstraAlgorithm.java
│   │       │           │   └── ShortestPathResponse.java
│   │       │           │
│   │       │           ├── config/
│   │       │           │   ├── RequestLoggingConfig.java
│   │       │           │   └── WebConfig.java
│   │       │           │
│   │       │           ├── controller/
│   │       │           │   ├── CityController.java
│   │       │           │   ├── PathController.java
│   │       │           │   ├── PathRequest.java
│   │       │           │   ├── RoadController.java
│   │       │           │   └── GlobalExceptionHandler.java
│   │       │           │
│   │       │           ├── dto/
│   │       │           │   └── RoadRequest.java
│   │       │           │
│   │       │           ├── model/
│   │       │           │   ├── City.java
│   │       │           │   ├── Graph.java
│   │       │           │   └── Road.java
│   │       │           │
│   │       │           ├── repository/
│   │       │           │   ├── CityRepository.java
│   │       │           │   └── RoadRepository.java
│   │       │           │
│   │       │           └── service/
│   │       │               └── GraphService.java
│   │       │
│   │       └── resources/
│   │
│   ├── Dockerfile
│   ├── pom.xml
│   ├── mvnw
│   └── mvnw.cmd
│
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   │   ├── MapView.jsx
│   │   │   ├── CitySelector.jsx
│   │   │   ├── AlgorithmVisualizer.jsx
│   │   │   ├── ResultPanel.jsx
│   │   │   ├── TrafficSlider.jsx
│   │   │   ├── AddCityModal.jsx
│   │   │   └── AddRoadModal.jsx
│   │   │
│   │   ├── pages/
│   │   │   └── HomePage.jsx
│   │   │
│   │   ├── services/
│   │   │   └── api.js
│   │   │
│   │   ├── App.jsx
│   │   └── main.jsx
│   │
│   ├── package.json
│   └── vite.config.js
│
└── README.md
```

The repository currently has dedicated `backend` and `frontend` applications.

---

# Installation

## Prerequisites

Install:

* Node.js 18+
* Java 17+
* Maven
* MySQL 8+
* Git
* Docker (optional)

The repository's existing setup documentation specifies Node.js 18+, Java 17+, and MySQL 8+.

---

# Backend Setup

## 1. Create Database

Create a MySQL database:

```sql
CREATE DATABASE route_optimizer;
```

## 2. Configure Environment

The backend supports configuration values including:

```text
DB_URL
DB_USER
DB_PASSWORD
ALLOWED_ORIGINS
```

The existing README identifies these variables for backend configuration.

Typical Spring configuration:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/route_optimizer
spring.datasource.username=your_username
spring.datasource.password=your_password
```

## 3. Run Backend

Linux/macOS:

```bash
cd backend
./mvnw spring-boot:run
```

Windows:

```bash
cd backend
mvnw.cmd spring-boot:run
```

---

# Frontend Setup

Install dependencies:

```bash
cd frontend
npm install
```

Configure the backend URL:

```env
VITE_API_URL=http://localhost:8080
```

The frontend API service normalizes this value and uses `/api` as the API path.

Run the development server:

```bash
npm run dev
```

---

# Build for Production

## Frontend

```bash
cd frontend
npm run build
```

Preview:

```bash
npm run preview
```

Lint:

```bash
npm run lint
```

The current frontend package provides `dev`, `build`, `lint`, and `preview` scripts.

## Backend

```bash
cd backend
./mvnw clean package
```

The generated JAR can then be deployed to a Java-compatible server.

---

# Docker

The backend contains a Dockerfile for containerized deployment.

Build:

```bash
cd backend
docker build -t trafficoptimizer-backend .
```

Run:

```bash
docker run -p 8080:8080 trafficoptimizer-backend
```

---

# Development Architecture

TrafficOptimizer is organized around a clean separation of concerns.

```text
Frontend
   |
   | HTTP
   v
REST API
   |
   v
Controllers
   |
   v
Graph Service
   |
   +----------+
   |          |
   v          v
Dijkstra    Persistence
   |          |
   v          v
Route      MySQL
   |
   v
OSRM / Geometry
   |
   v
Leaflet
```

This keeps graph algorithms, application logic, persistence, and visualization independent enough to evolve separately.

---

# Technical Highlights

TrafficOptimizer demonstrates implementation of:

* Graph data structures
* Dijkstra shortest-path algorithm
* Priority queues
* Heuristic route estimation
* Traffic-aware routing
* Road-type weighting
* Haversine geospatial calculations
* OSRM integration
* REST API design
* Spring Boot
* Spring Data JPA
* React
* React-Leaflet
* Axios
* Persistent graph data
* Interactive algorithm visualization
* Docker-based backend deployment

---

# Algorithmic Complexity

For the underlying Dijkstra-style graph traversal with a binary heap priority queue, the typical complexity is:

```text
Time:  O((V + E) log V)
Space: O(V + E)
```

where:

* `V` = number of cities/nodes
* `E` = number of road connections

The implementation adds heuristic and penalty calculations to the edge cost model, so the practical runtime also depends on graph size and the additional route-weight computations.

---

# Design Decisions

## Why Dijkstra?

Dijkstra provides a reliable shortest-path foundation for non-negative graph weights and maps naturally to road-network routing.

## Why Haversine?

Haversine provides geographic distance between latitude/longitude coordinates without requiring an external routing service for every calculation.

## Why OSRM?

Haversine represents geographic distance, but actual driving routes are rarely straight lines. OSRM provides realistic road-network geometry and driving distances.

## Why React-Leaflet?

It provides an interactive map layer that integrates naturally with React state and route visualization.

---

# Future Improvements

## Routing

* Multi-route generation
* A* optimization
* Turn restrictions
* One-way road support
* More sophisticated traffic models
* Time-dependent traffic weights
* Toll-aware routing
* Fuel-aware route optimization

## Geographic Data

* OpenStreetMap integration
* Automated road-network import
* Real-time map updates
* Better road geometry caching
* Geospatial database support

## Frontend

* Better mobile map controls
* Route comparison
* Trip history
* Saved routes
* ETA charts
* Traffic heatmap visualization
* Enhanced route animation

## Backend

* Route caching
* Redis
* Asynchronous OSRM requests
* Distributed graph processing
* Improved authentication
* API rate limiting
* Monitoring and metrics

---

# Known Scope

The current implementation is best described as a **traffic-aware route optimization and visualization platform**, rather than a production navigation service.

The route engine uses modeled traffic values and graph data, while OSRM is used for real-world driving geometry and distance calculations. It does not itself provide a live traffic data ingestion pipeline comparable to commercial navigation providers.

---

# Author

**Krishna Teja**

[GitHub](https://github.com/teja0604)

[TrafficOptimizer Repository](https://github.com/teja0604/TrafficOptmizer)

---

# License

This project is currently intended for educational, portfolio, and development purposes.

A formal open-source license can be added before redistribution or commercial deployment.
