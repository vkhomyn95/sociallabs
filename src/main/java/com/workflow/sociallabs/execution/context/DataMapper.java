package com.workflow.sociallabs.execution.context;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class DataMapper {

    public static final String NAME = "name";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    public static Map<String, Object> convert(Object source) {
        if (source == null) {
            return Map.of();
        }

        Map<String, Object> values = OBJECT_MAPPER.convertValue(
                source,
                new TypeReference<Map<String, Object>>() {
                }
        );
        values.put(NAME, source.getClass().getSimpleName());
        return values;
    }

    public static Object convert(Object values, Class<?> toValue) {
       return OBJECT_MAPPER.convertValue(values, toValue);
    }
}
