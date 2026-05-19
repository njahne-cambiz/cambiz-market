package com.cambiz.market;

import com.cambiz.market.service.PlatformSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CambizApplication {

    @Autowired
    private PlatformSettingService settingService;

    public static void main(String[] args) {
        SpringApplication.run(CambizApplication.class, args);
        System.out.println("========================================");
        System.out.println("🚀 CamBiz Market Application Started!");
        System.out.println("🌐 http://localhost:8080/test");
        System.out.println("========================================");
    }

    @Bean
    public CommandLineRunner initSettings() {
        return args -> {
            settingService.initDefaults();
            System.out.println("✅ Platform settings initialized");
        };
    }
}