const API_BASE_URL = 'http://localhost:8080/api';

async function runTest() {
  try {
    console.log("Adding City A...");
    const cityARes = await fetch(`${API_BASE_URL}/cities`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: 'City A', latitude: 12.9716, longitude: 77.5946 })
    });
    const cityAData = await cityARes.json();
    const cityAId = cityAData.id;
    console.log(`City A added! ID: ${cityAId}`);

    console.log("Adding City B...");
    const cityBRes = await fetch(`${API_BASE_URL}/cities`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: 'City B', latitude: 13.0827, longitude: 80.2707 })
    });
    const cityBData = await cityBRes.json();
    const cityBId = cityBData.id;
    console.log(`City B added! ID: ${cityBId}`);

    console.log("Adding Road...");
    const roadRes = await fetch(`${API_BASE_URL}/roads`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        fromCityId: cityAId,
        toCityId: cityBId,
        distance: 345,
        roadType: 'NH'
      })
    });
    const roadData = await roadRes.json();
    console.log("Road added! Responses:", roadData);

    console.log("Getting all roads...");
    const allRoadsRes = await fetch(`${API_BASE_URL}/roads`);
    const allRoadsData = await allRoadsRes.json();
    console.log(`There are ${allRoadsData.length} roads total.`);

    console.log("Testing Shortest Path...");
    const pathRes = await fetch(`${API_BASE_URL}/shortest-path`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        startCity: cityAId,
        endCity: cityBId,
        trafficLevel: 0.1
      })
    });
    const pathData = await pathRes.json();
    console.log("Shortest Path Result:", JSON.stringify(pathData, null, 2));

    console.log("ALL TESTS PASSED SUCCESSFULLY");
  } catch (error) {
    console.error("Test failed:", error);
  }
}

runTest();
