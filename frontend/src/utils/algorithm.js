export const generateDijkstraFrames = (cities, roads, startCityId, endCityId, trafficLevel) => {
  const frames = [];
  const distances = {};
  const previous = {};
  const unvisited = new Set(cities.map(c => c.id));
  const visitedNodes = new Set();
  const visitedEdges = new Set();
  
  // Initialize
  cities.forEach(c => {
    distances[c.id] = Infinity;
    previous[c.id] = null;
  });
  distances[startCityId] = 0;

  const pushFrame = (msg, evalEdge = null) => {
    frames.push({
      visitedNodes: Array.from(visitedNodes),
      visitedEdges: Array.from(visitedEdges),
      evaluatingEdge: evalEdge,
      message: msg,
      shortestPath: [],
      isComplete: false
    });
  };

  pushFrame(`Initialized distances from Start City.`);

  while (unvisited.size > 0) {
    // Find node with minimum distance
    let current = null;
    let minDistance = Infinity;
    
    unvisited.forEach(nodeId => {
      if (distances[nodeId] < minDistance) {
        minDistance = distances[nodeId];
        current = nodeId;
      }
    });

    if (current === null || minDistance === Infinity) {
      break; // No reachable unvisited nodes left
    }

    if (current === endCityId) {
      pushFrame(`Destination reached! Reconstructing shortest path...`);
      break;
    }

    unvisited.delete(current);
    visitedNodes.add(current);
    
    if (current !== startCityId) {
       pushFrame(`Visiting node ${cities.find(c => c.id === current).name}. Distance: ${distances[current].toFixed(0)}`);
    }

    // Get outgoing roads
    const adjacentRoads = roads.filter(r => r.from === current || r.to === current);

    for (const road of adjacentRoads) {
      const neighbor = road.from === current ? road.to : road.from;
      
      if (!unvisited.has(neighbor)) continue;

      // Create a unique edge signature regardless of direction
      // unique edge key (unused for now)
      // const edgeKey = [current, neighbor].sort().join('-');
      
      pushFrame(`Evaluating path to ${cities.find(c => c.id === neighbor).name}...`, road);
      
      // Calculate new distance adding traffic level multiplier (mock logic)
      const newDist = distances[current] + (road.distance * trafficLevel);
      
      if (newDist < distances[neighbor]) {
        distances[neighbor] = newDist;
        previous[neighbor] = current;
        visitedEdges.add(road);
        pushFrame(`Found shorter path to ${cities.find(c => c.id === neighbor).name}. Updating distance to ${newDist.toFixed(0)}`, road);
      }
    }
  }

  // Reconstruct path with segments details
  const path = [];
  const pathSegments = [];
  let curr = endCityId;
  
  if (previous[curr] !== null || curr === startCityId) {
    while (curr !== null) {
      path.unshift(cities.find(c => c.id === curr));
      
      const prevNode = previous[curr];
      if (prevNode !== null) {
        // Find the road connecting prevNode and curr
        const connectingRoad = roads.find(r => 
          (r.from === prevNode && r.to === curr) ||
          (r.from === curr && r.to === prevNode)
        );
        if (connectingRoad) {
           pathSegments.unshift({
             from: cities.find(c => c.id === prevNode),
             to: cities.find(c => c.id === curr),
             distance: connectingRoad.distance
           });
        }
      }
      
      curr = prevNode;
    }
  }

  frames.push({
    visitedNodes: Array.from(visitedNodes).concat(endCityId),
    visitedEdges: Array.from(visitedEdges),
    evaluatingEdge: null,
    message: `Algorithm complete. Found shortest path!`,
    shortestPath: path,
    pathSegments: pathSegments,
    isComplete: true,
    totalDistance: distances[endCityId] === Infinity ? 0 : distances[endCityId]
  });

  return frames;
};
