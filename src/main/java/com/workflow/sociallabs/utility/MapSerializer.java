package com.workflow.sociallabs.utility;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class MapSerializer {

    private final static ObjectMapper objectMapper = new ObjectMapper();

    public static String serializeToJson(Object obj) {
        if (obj == null) {
            return "{}";
        }

        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize data to JSON", e);

            return null;
        }
    }

    public static Map<String, Object> deserializeFromJson(String json) {
        try {
          return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Error deserializing data to JSON", e);

            return null;
        }
    }
}
