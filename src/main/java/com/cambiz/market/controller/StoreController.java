package com.cambiz.market.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StoreController {
    
    @GetMapping("/store")
    public String store(Model model) {
        model.addAttribute("pageTitle", "CamBiz Market - Shop Online");
        return "store";
    }
    
    @GetMapping("/shop")
    public String shop() {
        return "redirect:/store";
    }
    
    @GetMapping("/wishlist")
    public String wishlist() {
        return "wishlist";
    }
    
    @GetMapping("/order-tracking")
    public String orderTracking() {
        return "order-tracking";
    }
    
    @GetMapping("/track")
    public String track() {
        return "order-tracking";
    }
    
    @GetMapping("/orders")
    public String myOrders() {
        return "my-orders";
    }
    
    @GetMapping("/premium")
    public String premium() {
        return "premium";
    }
    
    @GetMapping("/register")
    public String register() {
        return "register";
    }
}