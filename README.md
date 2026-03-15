# Traffic Route Optimizer

A full-stack application for optimizing traffic routes using Dijkstra's algorithm.

## Features
- Interactive map for visualizing cities and roads.
- Shortest path calculation considering traffic levels.
- Real-time city and road management.

## Prerequisites
- **Node.js** (v18+)
- **Java** (v17+)
- **MySQL** (v8+)

## Setup & Running

### Backend
1. Create a MySQL database named `route_optimizer`.
2. Configure environment variables (optional, defaults provided):
   - `DB_URL`, `DB_USER`, `DB_PASSWORD`, `ALLOWED_ORIGINS`
3. Run with Maven:
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```

### Frontend
1. Install dependencies:
   ```bash
   cd frontend
   npm install
   ```
2. Configure environment variables (optional, defaults provided):
   - `VITE_API_URL`
3. Run in development:
   ```bash
   npm run dev
   ```

## Deployment
1. **Frontend**: Run `npm run build` and deploy the `dist` folder to any static hosting.
2. **Backend**: Run `./mvnw package` and deploy the generated JAR to a Java-compatible server.

