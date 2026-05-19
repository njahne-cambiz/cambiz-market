package com.cambiz.market.service;

import com.cambiz.market.model.PlatformSetting;
import com.cambiz.market.repository.PlatformSettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PlatformSettingService {
    
    @Autowired
    private PlatformSettingRepository settingRepository;
    
    private Map<String, String> cache = new HashMap<>();
    
    @Transactional
    public void initDefaults() {
        setIfNotExists("site_name", "CamBiz Market", "Platform site name");
        setIfNotExists("currency", "XAF", "Default currency");
        setIfNotExists("commission_rate", "5.0", "Platform commission percentage");
        setIfNotExists("payout_threshold", "10000.0", "Minimum amount for seller withdrawal (XAF)");
        setIfNotExists("max_products_regular", "50", "Max products for regular sellers");
        setIfNotExists("max_products_premium", "500", "Max products for premium sellers");
        setIfNotExists("maintenance_mode", "false", "Maintenance mode on/off");
        setIfNotExists("allow_registration", "true", "Allow new user registration");
        loadCache();
    }
    
    private void setIfNotExists(String key, String value, String description) {
        if (settingRepository.findBySettingKey(key).isEmpty()) {
            PlatformSetting setting = new PlatformSetting();
            setting.setSettingKey(key);
            setting.setSettingValue(value);
            setting.setDescription(description);
            settingRepository.save(setting);
        }
    }
    
    public void loadCache() {
        cache.clear();
        List<PlatformSetting> all = settingRepository.findAll();
        for (PlatformSetting s : all) {
            cache.put(s.getSettingKey(), s.getSettingValue());
        }
    }
    
    public String get(String key, String defaultValue) {
        return cache.getOrDefault(key, defaultValue);
    }
    
    public double getDouble(String key, double defaultValue) {
        try { return Double.parseDouble(cache.getOrDefault(key, String.valueOf(defaultValue))); }
        catch (NumberFormatException e) { return defaultValue; }
    }
    
    public int getInt(String key, int defaultValue) {
        try { return Integer.parseInt(cache.getOrDefault(key, String.valueOf(defaultValue))); }
        catch (NumberFormatException e) { return defaultValue; }
    }
    
    public boolean getBoolean(String key, boolean defaultValue) {
        String val = cache.get(key);
        if (val == null) return defaultValue;
        return "true".equalsIgnoreCase(val) || "1".equals(val);
    }
    
    @Transactional
    public void set(String key, String value) {
        PlatformSetting setting = settingRepository.findBySettingKey(key)
            .orElse(new PlatformSetting());
        setting.setSettingKey(key);
        setting.setSettingValue(value);
        settingRepository.save(setting);
        cache.put(key, value);
    }
    
    public Map<String, String> getAll() {
        return new HashMap<>(cache);
    }
}