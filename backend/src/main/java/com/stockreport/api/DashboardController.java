package com.stockreport.api;

import com.stockreport.dto.response.DashboardDto;
import com.stockreport.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<?> getDashboard() {
        DashboardDto dashboard = dashboardService.getDashboard();
        return ResponseEntity.ok(Map.of("data", dashboard, "meta", Map.of("timestamp", Instant.now())));
    }
}
