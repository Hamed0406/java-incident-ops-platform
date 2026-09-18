package com.networkincident.incident;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.networkincident.IncidentPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = IncidentPlatformApplication.class)
@AutoConfigureMockMvc
class IncidentControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsRetrievesAndResolvesAnIncident() throws Exception {
        String response = mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"siteId":"STH-001","assetId":"RTR-1001","severity":"CRITICAL","description":"Link unavailable"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andReturn().getResponse().getContentAsString();

        String id = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response).get("id").asText();

        mockMvc.perform(get("/api/incidents/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        mockMvc.perform(post("/api/incidents/{id}/resolve", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolvedAt").exists());
    }

    @Test
    void returnsProblemDetailsForInvalidOrMissingIncidents() throws Exception {
        mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"siteId":"","assetId":"RTR-1001","severity":"CRITICAL","description":"Link unavailable"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(get("/api/incidents/{id}", "00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void advancesLifecycleAndRejectsInvalidTransitions() throws Exception {
        String id = new com.fasterxml.jackson.databind.ObjectMapper().readTree(mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"siteId":"STH-001","assetId":"RTR-1001","severity":"MAJOR","description":"Link unavailable"}
                                """))
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/incidents/{id}/investigate", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
        mockMvc.perform(post("/api/incidents/{id}/acknowledge", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"));
        mockMvc.perform(post("/api/incidents/{id}/investigate", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INVESTIGATING"));
        mockMvc.perform(put("/api/incidents/{id}/owner", id)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"owner\":\"NOC\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner").value("NOC"));
    }

    @Test
    void filtersAndPaginatesIncidents() throws Exception {
        create("LIST-001", "RTR-1", "MINOR");
        create("LIST-001", "RTR-2", "CRITICAL");
        create("LIST-002", "RTR-3", "CRITICAL");

        mockMvc.perform(get("/api/incidents")
                        .param("siteId", "LIST-001")
                        .param("severity", "CRITICAL")
                        .param("page", "1")
                        .param("pageSize", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].assetId").value("RTR-2"));

        mockMvc.perform(get("/api/incidents").param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    private void create(String siteId, String assetId, String severity) throws Exception {
        mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"siteId":"%s","assetId":"%s","severity":"%s","description":"Link unavailable"}
                                """.formatted(siteId, assetId, severity)))
                .andExpect(status().isCreated());
    }
}
