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
}