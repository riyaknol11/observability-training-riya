package org.observability.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class InventoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/inventory/health returns 200 UP")
    void health_returns200() throws Exception {
        mockMvc.perform(get("/api/inventory/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("inventory-service"));
    }

    @Test
    @DisplayName("GET /api/inventory/{itemId} returns 200 or 500")
    void getInventory_returns200or500() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/inventory/test-item-1"))
                .andReturn();

        int status = result.getResponse().getStatus();
        assertThat(status).isIn(200, 500);
    }

    @Test
    @DisplayName("GET /api/inventory/{itemId} 200 response contains itemId and quantity")
    void getInventory_successResponse_hasCorrectFields() throws Exception {
        // Retry until we get a success response
        for (int i = 0; i < 20; i++) {
            MvcResult result = mockMvc.perform(get("/api/inventory/item-" + i))
                    .andReturn();
            if (result.getResponse().getStatus() == 200) {
                String body = result.getResponse().getContentAsString();
                assertThat(body).contains("itemId");
                assertThat(body).contains("quantity");
                assertThat(body).contains("available");
                return;
            }
        }
    }

    @Test
    @DisplayName("GET /api/inventory/{itemId} 500 response contains error field")
    void getInventory_errorResponse_hasErrorField() throws Exception {
        for (int i = 0; i < 30; i++) {
            MvcResult result = mockMvc.perform(get("/api/inventory/item-" + i))
                    .andReturn();
            if (result.getResponse().getStatus() == 500) {
                String body = result.getResponse().getContentAsString();
                assertThat(body).contains("error");
                assertThat(body).contains("itemId");
                return;
            }
        }
    }

    @Test
    @DisplayName("GET /actuator/prometheus exposes metrics endpoint")
    void prometheus_endpointIsExposed() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/plain"));
    }

    @Test
    @DisplayName("Prometheus endpoint contains custom inventory counter")
    void prometheus_containsCustomCounter() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/inventory/item-" + i)).andReturn();
        }

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("inventory_requests_total")));
    }

    @Test
    @DisplayName("GET /api/inventory/{itemId}/validate returns 200 or 500")
    void validateStock_returns200or500() throws Exception {
        MvcResult result = mockMvc.perform(
                        get("/api/inventory/item-1/validate").param("quantity", "5"))
                .andReturn();
        assertThat(result.getResponse().getStatus()).isIn(200, 500);
    }
}