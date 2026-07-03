package org.example.common;

public interface Serializer {
    byte[] serialize(Object obj);
    <T> T deserialize(byte[] data, Class<T> clazz);
}
