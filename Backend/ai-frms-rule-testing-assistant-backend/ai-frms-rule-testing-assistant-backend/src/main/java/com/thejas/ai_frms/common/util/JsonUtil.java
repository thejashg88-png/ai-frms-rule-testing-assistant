package com.thejas.ai_frms.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thejas.ai_frms.common.exception.BadRequestException;

public final class JsonUtil {

    // findAndRegisterModules() registers jackson-datatype-jsr310 (LocalDateTime, LocalDate, etc.)
    // and any other Jackson modules on the classpath — required for LocalDateTime serialization.
    // Without this, plain new ObjectMapper() cannot serialize LocalDateTime and throws JsonProcessingException.
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    private JsonUtil() {
    }

    public static String toJson(Object object) {
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException exception) {
            throw new BadRequestException("Unable to convert object to JSON: " + exception.getOriginalMessage());
        }
    }

    public static <T> T fromJson(String json, Class<T> targetClass) {
        try {
            return OBJECT_MAPPER.readValue(json, targetClass);
        } catch (JsonProcessingException exception) {
            throw new BadRequestException("Unable to convert JSON to object");
        }
    }
}