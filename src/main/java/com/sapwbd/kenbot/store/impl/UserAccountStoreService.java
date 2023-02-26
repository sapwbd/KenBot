package com.sapwbd.kenbot.store.impl;

import com.sapwbd.kenbot.store.StoreService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class UserAccountStoreService implements StoreService<String, String> {

    private final Map<String, String> keyValueMap = new HashMap<>();

    @Override
    public void insert(String key, String value) {
        keyValueMap.put(key, value);
    }

    @Override
    public String get(String key) {
        return keyValueMap.get(key);
    }
}
