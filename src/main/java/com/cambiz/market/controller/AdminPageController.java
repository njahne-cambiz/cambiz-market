package com.cambiz.market.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminPageController {
    
    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        // JWT auth filter already validates the user
        // The frontend will handle role-based access
        return "admin/dashboard";
    }
}