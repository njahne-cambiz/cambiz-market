package com.cambiz.market.controller;

import com.cambiz.market.dto.*;
import com.cambiz.market.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DashboardController {
    
    private final DashboardService dashboardService;
    
    /**
     * BUYER DASHBOARD
     * GET /api/dashboard/buyer
     */
    @GetMapping("/buyer")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<ApiResponse> getBuyerDashboard() {
        try {
            BuyerDashboardResponse dashboard = dashboardService.getBuyerDashboard();
            return ResponseEntity.ok(new ApiResponse(true, "Buyer dashboard retrieved", dashboard));
        } catch (Exception e) {
            log.error("Error fetching buyer dashboard: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, e.getMessage(), null));
        }
    }
    
    /**
     * SELLER DASHBOARD
     * GET /api/dashboard/seller
     */
    @GetMapping("/seller")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ApiResponse> getSellerDashboard() {
        try {
            SellerDashboardResponse dashboard = dashboardService.getSellerDashboard();
            return ResponseEntity.ok(new ApiResponse(true, "Seller dashboard retrieved", dashboard));
        } catch (Exception e) {
            log.error("Error fetching seller dashboard: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, e.getMessage(), null));
        }
    }
    
    /**
     * ADMIN DASHBOARD
     * GET /api/dashboard/admin
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getAdminDashboard() {
        try {
            AdminDashboardResponse dashboard = dashboardService.getAdminDashboard();
            return ResponseEntity.ok(new ApiResponse(true, "Admin dashboard retrieved", dashboard));
        } catch (Exception e) {
            log.error("Error fetching admin dashboard: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, e.getMessage(), null));
        }
    }
    
    /**
     * QUICK STATS - For mobile app header
     * GET /api/dashboard/quick-stats
     */
    @GetMapping("/quick-stats")
    public ResponseEntity<ApiResponse> getQuickStats() {
        try {
            var stats = dashboardService.getQuickStats();
            return ResponseEntity.ok(new ApiResponse(true, "Quick stats retrieved", stats));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, e.getMessage(), null));
        }
    }
    
    /**
     * SELLER ANALYTICS - Detailed reports
     * GET /api/dashboard/seller/analytics?period=week
     */
    @GetMapping("/seller/analytics")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ApiResponse> getSellerAnalytics(
            @RequestParam(defaultValue = "week") String period) {
        try {
            var analytics = dashboardService.getSellerAnalytics(period);
            return ResponseEntity.ok(new ApiResponse(true, "Analytics retrieved", analytics));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, e.getMessage(), null));
        }
    }
    
    /**
     * ADMIN REPORTS - Export data
     * GET /api/dashboard/admin/report?type=revenue&format=csv
     */
    @GetMapping("/admin/report")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> generateReport(
            @RequestParam String type,
            @RequestParam(defaultValue = "json") String format) {
        try {
            var report = dashboardService.generateReport(type, format);
            return ResponseEntity.ok(new ApiResponse(true, "Report generated", report));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, e.getMessage(), null));
        }
    }
}