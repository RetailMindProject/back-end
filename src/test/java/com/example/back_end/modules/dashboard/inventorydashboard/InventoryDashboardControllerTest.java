package com.example.back_end.modules.dashboard.inventorydashboard;

import com.example.back_end.modules.dashboard.inventorydashboard.dto.WeeklyInventoryMovementDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InventoryDashboardControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void weeklyTrend_shouldReturnSevenDays() {
        String url = "http://localhost:" + port + "/api/dashboard/inventory/weekly-trend";
        ResponseEntity<WeeklyInventoryMovementDTO[]> response = restTemplate.getForEntity(url, WeeklyInventoryMovementDTO[].class);
        assertEquals(200, response.getStatusCode().value());
        WeeklyInventoryMovementDTO[] body = response.getBody();
        assertNotNull(body, "Response body should not be null");
        assertEquals(7, body.length, "Weekly trend should contain 7 days");
    }
}
