package com.fptu.sep490.commonlibrary.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public <T> void saveValue(String key, T value, Duration ttl) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(value);
        redisTemplate.opsForValue().set(key, json, ttl);
    }

    public <T> T getValue(String key, Class<T> clazz) throws JsonProcessingException {
        Object raw = redisTemplate.opsForValue().get(key);
        if (raw == null) return null;
        return objectMapper.readValue(raw.toString(), clazz);
    }

    public <T> void addToList(String key, T value) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(value);
        redisTemplate.opsForList().rightPush(key, json);
    }

    public <T> List<T> getList(String key, Class<T> clazz) throws JsonProcessingException {
        List<Object> rawList = redisTemplate.opsForList().range(key, 0, -1);
        if (rawList == null) return Collections.emptyList();

        return rawList.stream()
                .map(obj -> {
                    try {
                        return objectMapper.readValue(obj.toString(), clazz);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }

    public <T> void addToSet(String key, T value) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(value);
        redisTemplate.opsForSet().add(key, json);
    }

    public <T> Set<T> getSet(String key, Class<T> clazz) throws JsonProcessingException {
        Set<Object> rawSet = redisTemplate.opsForSet().members(key);
        if (rawSet == null) return Collections.emptySet();

        Set<T> result = new HashSet<>();
        for (Object raw : rawSet) {
            result.add(objectMapper.readValue(raw.toString(), clazz));
        }
        return result;
    }

    public <T> void removeFromSet(String key, T value) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(value);
        redisTemplate.opsForSet().remove(key, json);
    }

    public void setTTL(String key, Duration ttl) {
        redisTemplate.expire(key, ttl);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}

