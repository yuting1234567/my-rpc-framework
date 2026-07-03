package org.example.common;

public enum SerializerType {
    JSON(new JsonSerializer()),
    JAVA(new JavaSerializer());

    private final Serializer serializer;

    SerializerType(Serializer serializer) {
        this.serializer = serializer;
    }

    public Serializer getSerializer() {
        return serializer;
    }
}
