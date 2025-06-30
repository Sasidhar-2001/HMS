package com.hostel.controller;

import com.hostel.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/warden")
@CrossOrigin(origins = "*")
public class WardenController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/dashboard")
    public ResponseEntity<?> getWardenDashboard(HttpServletRequest request) {
        try {
            String userId = (String) request.getAttribute("userId");
            Map<String, Object> dashboardData = dashboardService.getWardenDashboardStats();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", dashboardData);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Failed to fetch warden dashboard: " + e.getMessage()));
        }
    }
}