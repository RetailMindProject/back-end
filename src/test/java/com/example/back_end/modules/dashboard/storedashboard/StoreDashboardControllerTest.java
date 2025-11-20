package com.example.back_end.modules.dashboard.storedashboard;

import com.example.back_end.modules.dashboard.storedashboard.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StoreDashboardControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl() {
        return "http://localhost:" + port + "/api/dashboard/store";
    }

    @Test
    void getSummary_shouldReturnStoreSummaryDTO() {
        ResponseEntity<StoreSummaryDTO> response =
            restTemplate.getForEntity(baseUrl() + "/summary", StoreSummaryDTO.class);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());

        StoreSummaryDTO summary = response.getBody();
        assertNotNull(summary.getTotalSales());
        assertNotNull(summary.getTotalOrders());
        assertNotNull(summary.getMostPopularProduct());
    }

    @Test
    void getSalesTrend_shouldReturnSevenDays() {
        ResponseEntity<SalesTrendDTO[]> response =
            restTemplate.getForEntity(baseUrl() + "/sales-trend", SalesTrendDTO[].class);

        assertEquals(200, response.getStatusCode().value());
        SalesTrendDTO[] trend = response.getBody();
        assertNotNull(trend);
        assertEquals(7, trend.length, "Sales trend should contain 7 days");
    }

    @Test
    void getCategoryCounts_shouldReturnList() {
        ResponseEntity<CategoryCountDTO[]> response =
            restTemplate.getForEntity(baseUrl() + "/category-counts", CategoryCountDTO[].class);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
    }

    @Test
    void getTopProducts_shouldReturnList() {
        ResponseEntity<TopProductDTO[]> response =
            restTemplate.getForEntity(baseUrl() + "/top-products", TopProductDTO[].class);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
    }

    @Test
    void getRecentDaily_shouldReturnList() {
        ResponseEntity<RecentDailyDTO[]> response =
            restTemplate.getForEntity(baseUrl() + "/recent-daily", RecentDailyDTO[].class);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
    }
}

