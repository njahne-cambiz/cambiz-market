package com.cambiz.market.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProductViewController {
    
    @GetMapping("/product")
    public String productDetail() {
        return "product-detail";
    }
}