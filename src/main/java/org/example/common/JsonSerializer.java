package org.example.common;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonSerializer implements Serializer{

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public byte[] serialize(Object obj) {
        try {
            return mapper.writeValueAsBytes(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) {
        try {
            return mapper.readValue(data, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
