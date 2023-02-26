package com.sapwbd.kenbot.store;

public interface StoreService<K, V> {

    void insert(K key, V value);

    V get(K key);

}
