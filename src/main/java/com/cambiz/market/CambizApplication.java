package com.cambiz.market;  // ✅ CHANGED FROM com.cambiz.cambiz

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CambizApplication {
    public static void main(String[] args) {
        SpringApplication.run(CambizApplication.class, args);
        System.out.println("========================================");
        System.out.println("🚀 CamBiz Market Application Started!");
        System.out.println("🌐 http://localhost:8080/test");
        System.out.println("========================================");
    }
}