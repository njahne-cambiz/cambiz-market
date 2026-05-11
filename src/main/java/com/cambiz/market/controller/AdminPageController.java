package com.cambiz.market.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.servlet.http.HttpSession;
import com.cambiz.market.model.User;

@Controller
public class AdminPageController {
    
    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model, HttpSession session) {
        User admin = (User) session.getAttribute("user");
        if (admin == null || !"ADMIN".equals(admin.getUserType().name())) {
            return "redirect:/login";
        }
        model.addAttribute("admin", admin);
        return "admin/dashboard";
    }
    
    @GetMapping("/admin")
    public String adminRedirect() {
        return "redirect:/admin/dashboard";
    }
}