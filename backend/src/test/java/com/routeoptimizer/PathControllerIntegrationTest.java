package com.routeoptimizer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.routeoptimizer.controller.PathRequest;
import com.routeoptimizer.model.City;
import com.routeoptimizer.model.Road;
import com.routeoptimizer.repository.CityRepository;
import com.routeoptimizer.repository.RoadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class PathControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private RoadRepository roadRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        roadRepository.deleteAll();
        cityRepository.deleteAll();
    }

    @Test
    void testLocationAllEmpty() throws Exception {
        mockMvc.perform(get("/api/cities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testShortestPathErrorWhenNoRoads() throws Exception {
        City c1 = cityRepository.save(new City(null, "A", 0, 0));
        City c2 = cityRepository.save(new City(null, "B", 1, 1));

        PathRequest req = new PathRequest();
        req.setStartCity(c1.getId());
        req.setEndCity(c2.getId());

        mockMvc.perform(post("/api/shortest-path")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testShortestPathSuccess() throws Exception {
        City c1 = cityRepository.save(new City(null, "A", 0, 0));
        City c2 = cityRepository.save(new City(null, "B", 0, 1));
        Road r = new Road();
        r.setFromCity(c1);
        r.setToCity(c2);
        r.setDistance(1);
        r.setTrafficLevel(0.1);
        r.setRoadType("NH");
        r.setSpeedLimit(80);
        r.setTravelTime(1.0/80);
        roadRepository.save(r);

        PathRequest req = new PathRequest();
        req.setStartCity(c1.getId());
        req.setEndCity(c2.getId());

        mockMvc.perform(post("/api/shortest-path")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path[0].name").value("A"))
                .andExpect(jsonPath("$.path[1].name").value("B"))
                .andExpect(jsonPath("$.distance").isNumber());
    }
}
