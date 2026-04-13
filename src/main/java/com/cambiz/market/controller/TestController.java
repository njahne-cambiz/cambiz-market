package com.cambiz.market.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    
    @GetMapping("/test")
    public String test() {
        return "Hello from CamBiz Market! 🚀";
    }
    
    @GetMapping("/api")
    public String api() {
        return "Welcome to CamBiz Market API! 🇨🇲";
    }
}